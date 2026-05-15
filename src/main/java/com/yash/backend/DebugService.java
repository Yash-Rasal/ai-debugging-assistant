package com.yash.backend;

import com.yash.backend.ai.AIService;
import com.yash.backend.executor.CodeExecutor;
import com.yash.backend.executor.ErrorParser;
import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.handler.DebugHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DebugService {

    private static final Logger log = LoggerFactory.getLogger(DebugService.class);
    private final List<DebugHandler> handlers;
    private final AIService aiService;
    private final CodeExecutor codeExecutor;
    private final ErrorParser errorParser;
    private final DebugResponseNormalizer responseNormalizer;

    public DebugService(List<DebugHandler> handlers,
                        AIService aiService,
                        CodeExecutor codeExecutor,
                        ErrorParser errorParser) {
        this(handlers, aiService, codeExecutor, errorParser, new DebugResponseNormalizer());
    }

    @Autowired
    public DebugService(List<DebugHandler> handlers,
                        AIService aiService,
                        CodeExecutor codeExecutor,
                        ErrorParser errorParser,
                        DebugResponseNormalizer responseNormalizer) {

        this.handlers = handlers;
        this.aiService = aiService;
        this.codeExecutor = codeExecutor;
        this.errorParser = errorParser;
        this.responseNormalizer = responseNormalizer;
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
        log.debug("Execution completed with status {}, diagnostic {}, line {}",
                result.getStatus(), result.getDiagnosticCode(), result.getLineNumber());

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
            if (handler.canHandle(result)) {
                log.debug("Using deterministic handler {} for status {}",
                        handler.getClass().getSimpleName(), result.getStatus());
                return responseNormalizer.normalize(handler.handle(code, result), result, 95);
            }
        }

        return runAiFallback(code, result);
    }

    private DebugResponse runAiFallback(String code, ExecutionResult result) {
        log.debug("Using AI fallback for status {}, diagnostic {}",
                result.getStatus(), result.getDiagnosticCode());
        DebugResponse response = aiService.getAISuggestion(code, result.getMessage(), result.getStackTrace());
        return responseNormalizer.normalize(response, result, 70);
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
