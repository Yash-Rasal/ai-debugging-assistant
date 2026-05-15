package com.yash.backend.executor;

public enum ExecutionStatus {
    SUCCESS("SUCCESS"),
    COMPILATION_ERROR("COMPILATION"),
    RUNTIME_ERROR("RUNTIME"),
    TIMEOUT("TIMEOUT"),
    INVALID_INPUT("INVALID_INPUT"),
    SECURITY_VIOLATION("SECURITY_VIOLATION"),
    SYSTEM_ERROR("SYSTEM_ERROR");

    private final String legacyValue;

    ExecutionStatus(String legacyValue) {
        this.legacyValue = legacyValue;
    }

    public String legacyValue() {
        return legacyValue;
    }

    public static ExecutionStatus fromLegacyValue(String value) {
        for (ExecutionStatus status : values()) {
            if (status.legacyValue.equals(value)) {
                return status;
            }
        }

        return SYSTEM_ERROR;
    }
}
