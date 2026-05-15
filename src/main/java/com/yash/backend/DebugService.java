package com.yash.backend;

import com.yash.backend.ai.AIService;
import com.yash.backend.executor.CodeExecutor;
import com.yash.backend.executor.ErrorParser;
import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.handler.DebugHandler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebugService {

    private final List<DebugHandler> handlers;
    private final AIService aiService;
    private final CodeExecutor codeExecutor;
    private final ErrorParser errorParser;

    public DebugService(List<DebugHandler> handlers,
                        AIService aiService,
                        CodeExecutor codeExecutor,
                        ErrorParser errorParser) {

        this.handlers = handlers;
        this.aiService = aiService;
        this.codeExecutor = codeExecutor;
        this.errorParser = errorParser;
    }

    public DebugResponse analyze(DebugRequest request) {
        if (request == null) {
            return invalidRequestResponse("Request body is required.");
        }

        String code = request.getCode();
        String suppliedDiagnostic = firstPresent(request.getStackTrace(), request.getError());

        if (hasText(code)) {
            return analyze(code);
        }

        if (hasText(suppliedDiagnostic)) {
            ExecutionResult result = errorParser.fromDiagnostic(request.getError(), request.getStackTrace());
            return analyzeResult(code, result);
        }

        return invalidRequestResponse("Provide Java code or an error/stackTrace to analyze.");
    }

    public DebugResponse analyze(String code) {
        if (!hasText(code)) {
            return invalidRequestResponse("Provide Java code to analyze.");
        }

        ExecutionResult result = codeExecutor.execute(code);

        if (result.isSuccess()) {
            return new DebugResponse(
                    "No error detected.",
                    "The submitted code compiled and ran successfully.",
                    100,
                    "LOW",
                    "SUCCESS",
                    -1
            );
        }

        return analyzeResult(code, result);
    }

    private DebugResponse analyzeResult(String code, ExecutionResult result) {
        for (DebugHandler handler : handlers) {
            if (handler.canHandle(result.getExceptionType())) {
                return normalize(handler.handle(code, result), result, 95);
            }
        }

        return runAiFallback(code, result);
    }

    private DebugResponse runAiFallback(String code, ExecutionResult result) {
        DebugResponse response = aiService.getAISuggestion(code, result.getMessage(), result.getStackTrace());
        return normalize(response, result, 70);
    }

    private DebugResponse normalize(DebugResponse response, ExecutionResult result, int fallbackConfidence) {
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
        int maxConfidence = result.getExceptionType() == null ? 80 : 95;
        return Math.max(50, Math.min(confidence, maxConfidence));
    }

    private DebugResponse invalidRequestResponse(String suggestion) {
        return new DebugResponse(
                "INVALID_REQUEST",
                suggestion,
                100,
                "LOW",
                ErrorCategory.LOGIC_ERROR.name(),
                -1
        );
    }

    private String firstPresent(String first, String second) {
        if (hasText(first)) {
            return first;
        }

        return second;
    }

    private String categoryFor(ExecutionResult result) {
        if (result.isCompilationError()) {
            return ErrorCategory.COMPILATION_ERROR.name();
        }

        if (result.isRuntimeError()) {
            return ErrorCategory.RUNTIME_ERROR.name();
        }

        return ErrorCategory.LOGIC_ERROR.name();
    }

    private String severityFor(ExecutionResult result) {
        if (result.isCompilationError() || result.isRuntimeError()) {
            return "HIGH";
        }

        return "MEDIUM";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
