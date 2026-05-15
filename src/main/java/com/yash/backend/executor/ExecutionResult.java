package com.yash.backend.executor;

public class ExecutionResult {

    private final String errorType;
    private final String exceptionType;
    private final String message;
    private final String stackTrace;
    private final int lineNumber;

    public ExecutionResult(String errorType, String message, int lineNumber) {
        this(errorType, null, message, message, lineNumber);
    }

    public ExecutionResult(String errorType, String exceptionType, String message, String stackTrace, int lineNumber) {
        this.errorType = errorType;
        this.exceptionType = exceptionType;
        this.message = message;
        this.stackTrace = stackTrace;
        this.lineNumber = lineNumber;
    }

    public String getErrorType() {
        return errorType;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(errorType);
    }

    public boolean isCompilationError() {
        return "COMPILATION".equals(errorType);
    }

    public boolean isRuntimeError() {
        return "RUNTIME".equals(errorType);
    }
}
