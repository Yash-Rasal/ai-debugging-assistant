package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.ExecutionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
public class ArithmeticExceptionHandler implements DebugHandler {

    @Override
    public boolean canHandle(String exceptionType) {
        return "ArithmeticException".equals(exceptionType);
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The code performs an invalid arithmetic operation.",
                "Check arithmetic expressions, especially division by zero.",
                95,
                "HIGH",
                ErrorCategory.RUNTIME_ERROR.name(),
                result.getLineNumber()
        );
    }
}
