package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.ExecutionResult;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Order(2)
@Component
public class NullPointerExceptionHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return NullPointerException.class.getSimpleName();
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {

        return new DebugResponse(
                "You are calling a method or property on a null object.",
                "Check object initialization before use.",
                95,
                "HIGH",
                ErrorCategory.RUNTIME_ERROR.name(),
                result.getLineNumber()
        );
    }


}
