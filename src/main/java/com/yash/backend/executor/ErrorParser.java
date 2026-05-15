package com.yash.backend.executor;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ErrorParser {

    private static final int WRAPPER_OFFSET = 2;
    private static final Pattern COMPILE_LINE_PATTERN = Pattern.compile("(?m)^(?:[^:\\r\\n]*\\.java|.*?\\.java):(\\d+):");
    private static final Pattern RUNTIME_LINE_PATTERN = Pattern.compile("\\([^():]+\\.java:(\\d+)\\)");
    private static final Pattern GENERIC_LINE_PATTERN = Pattern.compile("(?i)\\bline\\s+(\\d+)\\b");
    private static final Pattern JAVAC_ERROR_PATTERN = Pattern.compile("(?m)^.*\\.java:\\d+:\\s*error:\\s*(.+)$");
    private static final Pattern RUNTIME_EXCEPTION_PATTERN =
            Pattern.compile("(?:^|\\s)(?:[a-zA-Z_$][\\w$]*\\.)*([A-Z][\\w$]*(?:Exception|Error))(?::|\\s|$)");

    public ExecutionResult compilation(String compilerOutput) {
        return ExecutionResult.compilationError(
                classifyCompilation(compilerOutput),
                firstCompilerErrorOrDefault(compilerOutput, "Compilation failed"),
                compilerOutput,
                extractLineNumber(compilerOutput, COMPILE_LINE_PATTERN, true)
        );
    }

    public ExecutionResult runtime(String runtimeOutput) {
        return ExecutionResult.runtimeError(
                extractExceptionType(runtimeOutput),
                firstLineOrDefault(runtimeOutput, "Runtime error"),
                runtimeOutput,
                extractLineNumber(runtimeOutput, RUNTIME_LINE_PATTERN, true)
        );
    }

    public ExecutionResult fromDiagnostic(String error, String stackTrace) {
        String diagnostic = hasText(stackTrace) ? stackTrace : error;
        String exceptionType = extractExceptionType(diagnostic);
        int lineNumber = exceptionType == null
                ? extractLineNumber(diagnostic, COMPILE_LINE_PATTERN, false)
                : extractLineNumber(diagnostic, RUNTIME_LINE_PATTERN, false);

        if (exceptionType == null) {
            return ExecutionResult.compilationError(
                    classifyCompilation(diagnostic),
                    hasText(error) ? error : firstLineOrDefault(diagnostic, "Diagnostic provided"),
                    diagnostic,
                    lineNumber
            );
        }

        return ExecutionResult.runtimeError(
                exceptionType,
                hasText(error) ? error : firstLineOrDefault(diagnostic, "Diagnostic provided"),
                diagnostic,
                lineNumber
        );
    }

    private DiagnosticCode classifyCompilation(String compilerOutput) {
        if (!hasText(compilerOutput)) {
            return DiagnosticCode.UNKNOWN;
        }

        String normalized = compilerOutput.toLowerCase();

        if (normalized.contains("';' expected")) {
            return DiagnosticCode.MISSING_SEMICOLON;
        }

        if (normalized.contains("cannot find symbol")) {
            return DiagnosticCode.CANNOT_FIND_SYMBOL;
        }

        if (normalized.contains("incompatible types")) {
            return DiagnosticCode.INCOMPATIBLE_TYPES;
        }

        if (normalized.contains("reached end of file while parsing")
                || normalized.contains("illegal start of expression")
                || normalized.contains("not a statement")
                || normalized.contains("expected")) {
            return DiagnosticCode.SYNTAX_ERROR;
        }

        return DiagnosticCode.UNKNOWN;
    }

    private int extractLineNumber(String errorOutput, Pattern pattern, boolean unwrapGeneratedMain) {
        if (!hasText(errorOutput)) {
            return -1;
        }

        try {
            Matcher matcher = pattern.matcher(errorOutput);

            if (matcher.find()) {
                int parsed = Integer.parseInt(matcher.group(1));
                return normalizeLineNumber(parsed, unwrapGeneratedMain);
            }

            Matcher fallbackMatcher = GENERIC_LINE_PATTERN.matcher(errorOutput);
            if (fallbackMatcher.find()) {
                int parsed = Integer.parseInt(fallbackMatcher.group(1));
                return normalizeLineNumber(parsed, unwrapGeneratedMain);
            }
        } catch (Exception ignored) {
        }

        return -1;
    }

    private int normalizeLineNumber(int parsed, boolean unwrapGeneratedMain) {
        if (!unwrapGeneratedMain) {
            return Math.max(1, parsed);
        }

        return Math.max(1, parsed - WRAPPER_OFFSET);
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

    private String firstCompilerErrorOrDefault(String value, String fallback) {
        if (!hasText(value)) {
            return fallback;
        }

        Matcher matcher = JAVAC_ERROR_PATTERN.matcher(value);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return firstLineOrDefault(value, fallback);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
