package com.yash.backend.executor;

public class ExecutionResult {

    private final ExecutionStatus status;
    private final String exceptionType;
    private final DiagnosticCode diagnosticCode;
    private final String message;
    private final String stackTrace;
    private final int lineNumber;

    public ExecutionResult(String errorType, String message, int lineNumber) {
        this(ExecutionStatus.fromLegacyValue(errorType), null, DiagnosticCode.UNKNOWN, message, message, lineNumber);
    }

    public ExecutionResult(String errorType, String exceptionType, String message, String stackTrace, int lineNumber) {
        this(ExecutionStatus.fromLegacyValue(errorType), exceptionType, DiagnosticCode.UNKNOWN, message, stackTrace, lineNumber);
    }

    public ExecutionResult(ExecutionStatus status, String exceptionType, String message, String stackTrace, int lineNumber) {
        this(status, exceptionType, DiagnosticCode.UNKNOWN, message, stackTrace, lineNumber);
    }

    public ExecutionResult(ExecutionStatus status,
                           String exceptionType,
                           DiagnosticCode diagnosticCode,
                           String message,
                           String stackTrace,
                           int lineNumber) {
        this.status = status;
        this.exceptionType = exceptionType;
        this.diagnosticCode = diagnosticCode == null ? DiagnosticCode.UNKNOWN : diagnosticCode;
        this.message = message;
        this.stackTrace = stackTrace;
        this.lineNumber = lineNumber;
    }

    public static ExecutionResult success() {
        return new ExecutionResult(ExecutionStatus.SUCCESS, null, DiagnosticCode.NONE, "Code executed successfully", "Code executed successfully", -1);
    }

    public static ExecutionResult compilationError(DiagnosticCode diagnosticCode, String message, String compilerOutput, int lineNumber) {
        return new ExecutionResult(ExecutionStatus.COMPILATION_ERROR, null, diagnosticCode, message, compilerOutput, lineNumber);
    }

    public static ExecutionResult runtimeError(String exceptionType, String message, String stackTrace, int lineNumber) {
        return new ExecutionResult(ExecutionStatus.RUNTIME_ERROR, exceptionType, DiagnosticCode.NONE, message, stackTrace, lineNumber);
    }

    public static ExecutionResult timeout(String message) {
        return new ExecutionResult(ExecutionStatus.TIMEOUT, null, DiagnosticCode.NONE, message, message, -1);
    }

    public static ExecutionResult invalidInput(String message) {
        return new ExecutionResult(ExecutionStatus.INVALID_INPUT, null, DiagnosticCode.NONE, message, message, -1);
    }

    public static ExecutionResult securityViolation(String message, int lineNumber) {
        return new ExecutionResult(ExecutionStatus.SECURITY_VIOLATION, null, DiagnosticCode.SECURITY_VIOLATION, message, message, lineNumber);
    }

    public static ExecutionResult systemError(String message) {
        return new ExecutionResult(ExecutionStatus.SYSTEM_ERROR, null, DiagnosticCode.NONE, message, message, -1);
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getErrorType() {
        return status.legacyValue();
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public DiagnosticCode getDiagnosticCode() {
        return diagnosticCode;
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
        return status == ExecutionStatus.SUCCESS;
    }

    public boolean isCompilationError() {
        return status == ExecutionStatus.COMPILATION_ERROR;
    }

    public boolean isRuntimeError() {
        return status == ExecutionStatus.RUNTIME_ERROR;
    }
}
