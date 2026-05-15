package com.yash.backend.util;

public class CodeAnalyzerUtil {

    public static int findLineNumber(String code, String keyword) {
        if (code == null) return -1;

        String[] lines = code.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(keyword)) {
                return i + 1;
            }
        }

        return -1;
    }

    public static boolean containsPattern(String code, String pattern) {
        return code != null && code.contains(pattern);
    }

    public static int findNullUsageLine(String code) {
        if (code == null) return -1;

        String[] lines = code.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // detect risky usage like obj.method() or obj.property
            if (line.contains(".") && line.contains("(") && !line.contains("new")) {
                return i + 1;
            }
        }

        return findLineNumber(code, "null");
    }

    public static int findDivisionByZeroLine(String code) {
        if (code == null) return -1;

        String[] lines = code.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].matches(".*[/] *0([^0-9].*)?$")) {
                return i + 1;
            }
        }

        return -1;
    }
}
