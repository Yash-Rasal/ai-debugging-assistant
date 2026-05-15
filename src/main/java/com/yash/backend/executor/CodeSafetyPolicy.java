package com.yash.backend.executor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class CodeSafetyPolicy {

    private final List<ForbiddenPattern> forbiddenPatterns = List.of(
            new ForbiddenPattern(Pattern.compile("\\bSystem\\s*\\.\\s*exit\\s*\\("), "System.exit is not allowed"),
            new ForbiddenPattern(Pattern.compile("\\bRuntime\\s*\\.\\s*getRuntime\\s*\\("), "Runtime process access is not allowed"),
            new ForbiddenPattern(Pattern.compile("\\bProcessBuilder\\b"), "ProcessBuilder is not allowed"),
            new ForbiddenPattern(Pattern.compile("\\bexec\\s*\\("), "Process execution is not allowed"),
            new ForbiddenPattern(Pattern.compile("\\bFiles\\s*\\.\\s*(delete|write|writeString|move|copy)\\s*\\("), "File mutation APIs are not allowed"),
            new ForbiddenPattern(Pattern.compile("\\bnew\\s+File(OutputStream|Writer)?\\b"), "Direct file access is not allowed"),
            new ForbiddenPattern(Pattern.compile("\\bSocket\\b|\\bServerSocket\\b"), "Network socket access is not allowed")
    );

    public SafetyCheck inspect(String code) {
        if (code == null) {
            return SafetyCheck.allowedCheck();
        }

        for (ForbiddenPattern forbiddenPattern : forbiddenPatterns) {
            var matcher = forbiddenPattern.pattern().matcher(code);
            if (matcher.find()) {
                return SafetyCheck.blockedCheck(forbiddenPattern.message(), lineNumberForOffset(code, matcher.start()));
            }
        }

        return SafetyCheck.allowedCheck();
    }

    private int lineNumberForOffset(String code, int offset) {
        int lineNumber = 1;

        for (int i = 0; i < offset && i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                lineNumber++;
            }
        }

        return lineNumber;
    }

    private record ForbiddenPattern(Pattern pattern, String message) {
    }

    public record SafetyCheck(boolean allowed, String message, int lineNumber) {

        private static SafetyCheck allowedCheck() {
            return new SafetyCheck(true, null, -1);
        }

        private static SafetyCheck blockedCheck(String message, int lineNumber) {
            return new SafetyCheck(false, message, lineNumber);
        }
    }
}
