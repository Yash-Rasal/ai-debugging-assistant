package com.yash.backend.executor;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ErrorParser {

    private static final int WRAPPER_OFFSET = 2;
    private static final Pattern COMPILE_LINE_PATTERN = Pattern.compile("\\.java:(\\d+):");
    private static final Pattern RUNTIME_LINE_PATTERN = Pattern.compile("\\.java:(\\d+)\\)");
    private static final Pattern RUNTIME_EXCEPTION_PATTERN =
            Pattern.compile("(?:^|\\s)(?:[a-zA-Z_$][\\w$]*\\.)*([A-Z][\\w$]*(?:Exception|Error))(?::|\\s|$)");

    public ExecutionResult compilation(String compilerOutput) {
        return new ExecutionResult(
                "COMPILATION",
                null,
                firstLineOrDefault(compilerOutput, "Compilation failed"),
                compilerOutput,
                extractLineNumber(compilerOutput, COMPILE_LINE_PATTERN)
        );
    }

    public ExecutionResult runtime(String runtimeOutput) {
        return new ExecutionResult(
                "RUNTIME",
                extractExceptionType(runtimeOutput),
                firstLineOrDefault(runtimeOutput, "Runtime error"),
                runtimeOutput,
                extractLineNumber(runtimeOutput, RUNTIME_LINE_PATTERN)
        );
    }

    public ExecutionResult fromDiagnostic(String error, String stackTrace) {
        String diagnostic = hasText(stackTrace) ? stackTrace : error;
        String exceptionType = extractExceptionType(diagnostic);
        String errorType = exceptionType == null ? "COMPILATION" : "RUNTIME";

        return new ExecutionResult(
                errorType,
                exceptionType,
                hasText(error) ? error : firstLineOrDefault(diagnostic, "Diagnostic provided"),
                diagnostic,
                extractLineNumber(diagnostic, errorType.equals("RUNTIME") ? RUNTIME_LINE_PATTERN : COMPILE_LINE_PATTERN)
        );
    }

    private int extractLineNumber(String errorOutput, Pattern pattern) {
        if (!hasText(errorOutput)) {
            return -1;
        }

        try {
            Matcher matcher = pattern.matcher(errorOutput);

            if (matcher.find()) {
                int parsed = Integer.parseInt(matcher.group(1));
                return Math.max(1, parsed - WRAPPER_OFFSET);
            }
        } catch (Exception ignored) {
        }

        return -1;
    }

    private String extractExceptionType(String output) {
        if (!hasText(output)) {
            return null;
        }

        Matcher matcher = RUNTIME_EXCEPTION_PATTERN.matcher(output);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    private String firstLineOrDefault(String value, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }

        String[] lines = value.strip().split("\\R", 2);
        return lines.length == 0 || lines[0].isBlank() ? fallback : lines[0];
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
