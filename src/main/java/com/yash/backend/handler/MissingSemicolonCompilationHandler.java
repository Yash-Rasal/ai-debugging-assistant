package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.DiagnosticCode;
import com.yash.backend.executor.ExecutionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(10)
@Component
public class MissingSemicolonCompilationHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return null;
    }

    @Override
    public boolean canHandle(ExecutionResult result) {
        return result != null
                && result.isCompilationError()
                && result.getDiagnosticCode() == DiagnosticCode.MISSING_SEMICOLON;
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The Java compiler expected a semicolon near the highlighted line.",
                "Add the missing semicolon or complete the statement before continuing.",
                95,
                "HIGH",
                ErrorCategory.COMPILATION_ERROR.name(),
                result.getLineNumber()
        );
    }
}
