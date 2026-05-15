package com.yash.backend.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

@Component
public class CodeExecutor {

    private static final Logger log = LoggerFactory.getLogger(CodeExecutor.class);
    private static final long PROCESS_TIMEOUT_SECONDS = 5;
    private final ErrorParser errorParser;
    private final CodeSafetyPolicy codeSafetyPolicy;

    public CodeExecutor() {
        this(new ErrorParser(), new CodeSafetyPolicy());
    }

    public CodeExecutor(ErrorParser errorParser) {
        this(errorParser, new CodeSafetyPolicy());
    }

    public CodeExecutor(ErrorParser errorParser, CodeSafetyPolicy codeSafetyPolicy) {
        this.errorParser = errorParser;
        this.codeSafetyPolicy = codeSafetyPolicy;
    }

    public ExecutionResult execute(String code) {
        if (code == null || code.isBlank()) {
            log.debug("Rejected empty code execution request");
            return ExecutionResult.invalidInput("No Java code was provided");
        }

        CodeSafetyPolicy.SafetyCheck safetyCheck = codeSafetyPolicy.inspect(code);
        if (!safetyCheck.allowed()) {
            log.warn("Blocked unsafe code execution at line {}: {}", safetyCheck.lineNumber(), safetyCheck.message());
            return ExecutionResult.securityViolation(safetyCheck.message(), safetyCheck.lineNumber());
        }

        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory("debug-user-code-");
            String className = "UserCode";
            Path javaFile = tempDirectory.resolve(className + ".java");

            String finalCode =
                    "public class " + className + " {\n" +
                            "    public static void main(String[] args) {\n" +
                            code + "\n" +
                            "    }\n" +
                            "}";

            Files.writeString(javaFile, finalCode, StandardCharsets.UTF_8);

            Process compile = new ProcessBuilder("javac", javaFile.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!compile.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                compile.destroyForcibly();
                log.warn("Compilation timed out after {} seconds", PROCESS_TIMEOUT_SECONDS);
                return ExecutionResult.timeout("Compilation timed out");
            }

            String compileErrors = readErrorOutput(compile);

            if (!compileErrors.isBlank() || compile.exitValue() != 0) {
                log.info("Compilation failed with exit code {}", compile.exitValue());
                return errorParser.compilation(compileErrors);
            }

            Process run = new ProcessBuilder("java", "-cp", tempDirectory.toString(), className)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!run.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                run.destroyForcibly();
                log.warn("Execution timed out after {} seconds", PROCESS_TIMEOUT_SECONDS);
                return ExecutionResult.timeout("Code execution timed out");
            }

            String runtimeErrors = readErrorOutput(run);

            if (!runtimeErrors.isBlank() || run.exitValue() != 0) {
                log.info("Runtime failed with exit code {}", run.exitValue());
                return errorParser.runtime(runtimeErrors);
            }

            log.debug("Code executed successfully");
            return ExecutionResult.success();
        } catch (Exception e) {
            log.error("Code execution failed due to system error", e);
            return ExecutionResult.systemError(e.getMessage());
        } finally {
            deleteTempDirectory(tempDirectory);
        }
    }

    private String readErrorOutput(Process process) throws IOException {
        StringBuilder errors = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                errors.append(line).append(System.lineSeparator());
            }
        }

        return errors.toString();
    }

    private void deleteTempDirectory(Path tempDirectory) {
        if (tempDirectory == null) {
            return;
        }

        try {
            Files.walk(tempDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.debug("Failed to delete temporary directory {}", tempDirectory, e);
        }
    }
}
