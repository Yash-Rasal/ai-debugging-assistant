package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.ExecutionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(50)
@Component
public class GenericCompilationHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return null;
    }

    @Override
    public boolean canHandle(ExecutionResult result) {
        return result != null && result.isCompilationError();
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The submitted code failed Java compilation.",
                result.getMessage(),
                85,
                "HIGH",
                ErrorCategory.COMPILATION_ERROR.name(),
                result.getLineNumber()
        );
    }
}
