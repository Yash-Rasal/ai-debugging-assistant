package com.yash.backend.executor;

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

    private static final long PROCESS_TIMEOUT_SECONDS = 5;
    private final ErrorParser errorParser;

    public CodeExecutor() {
        this(new ErrorParser());
    }

    public CodeExecutor(ErrorParser errorParser) {
        this.errorParser = errorParser;
    }

    public ExecutionResult execute(String code) {
        if (code == null || code.isBlank()) {
            return new ExecutionResult("INVALID_INPUT", "No Java code was provided", -1);
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
                return new ExecutionResult("TIMEOUT", "Compilation timed out", -1);
            }

            String compileErrors = readErrorOutput(compile);

            if (!compileErrors.isBlank() || compile.exitValue() != 0) {
                return errorParser.compilation(compileErrors);
            }

            Process run = new ProcessBuilder("java", "-cp", tempDirectory.toString(), className)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!run.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                run.destroyForcibly();
                return new ExecutionResult("TIMEOUT", "Code execution timed out", -1);
            }

            String runtimeErrors = readErrorOutput(run);

            if (!runtimeErrors.isBlank() || run.exitValue() != 0) {
                return errorParser.runtime(runtimeErrors);
            }

            return new ExecutionResult("SUCCESS", "Code executed successfully", -1);
        } catch (Exception e) {
            return new ExecutionResult("SYSTEM_ERROR", e.getMessage(), -1);
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
        } catch (IOException ignored) {
        }
    }
}
