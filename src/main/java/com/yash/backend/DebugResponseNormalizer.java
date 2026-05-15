package com.yash.backend;

import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.executor.ExecutionStatus;
import com.yash.backend.executor.DiagnosticCode;
import org.springframework.stereotype.Component;

@Component
public class DebugResponseNormalizer {

    public DebugResponse normalize(DebugResponse response, ExecutionResult result, int fallbackConfidence) {
        if (response == null) {
            response = new DebugResponse(
                    "ANALYSIS_FAILED",
                    "No analysis response was produced.",
                    50,
                    "LOW",
                    categoryFor(result),
                    result.getLineNumber()
            );
        }

        response.setSeverity(severityFor(result));
        response.setCategory(categoryFor(result));
        response.setLineNumber(result.getLineNumber());
        response.setConfidence(clampConfidence(confidenceFor(response, fallbackConfidence), result));

        return response;
    }

    private int confidenceFor(DebugResponse response, int fallbackConfidence) {
        if (response.getCause() != null && response.getCause().startsWith("AI_")) {
            return 50;
        }

        return fallbackConfidence;
    }

    private int clampConfidence(int confidence, ExecutionResult result) {
        boolean hasStructuredDiagnostic = result.getDiagnosticCode() != DiagnosticCode.UNKNOWN
                && result.getDiagnosticCode() != DiagnosticCode.NONE;
        int maxConfidence = result.getExceptionType() == null && !hasStructuredDiagnostic ? 80 : 95;
        return Math.max(50, Math.min(confidence, maxConfidence));
    }

    private String categoryFor(ExecutionResult result) {
        if (result.isCompilationError()) {
            return ErrorCategory.COMPILATION_ERROR.name();
        }

        if (result.isRuntimeError()) {
            return ErrorCategory.RUNTIME_ERROR.name();
        }

        if (result.getStatus() == ExecutionStatus.TIMEOUT) {
            return ErrorCategory.RUNTIME_ERROR.name();
        }

        return ErrorCategory.LOGIC_ERROR.name();
    }

    private String severityFor(ExecutionResult result) {
        if (result.isCompilationError() || result.isRuntimeError()) {
            return "HIGH";
        }

        if (result.getStatus() == ExecutionStatus.TIMEOUT
                || result.getStatus() == ExecutionStatus.SECURITY_VIOLATION) {
            return "HIGH";
        }

        return "MEDIUM";
    }
}
