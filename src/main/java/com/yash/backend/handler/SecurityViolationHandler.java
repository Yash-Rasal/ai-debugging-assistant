package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.executor.ExecutionStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(3)
@Component
public class SecurityViolationHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return null;
    }

    @Override
    public boolean canHandle(ExecutionResult result) {
        return result != null && result.getStatus() == ExecutionStatus.SECURITY_VIOLATION;
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The submitted code uses an API that is blocked by the execution safety policy.",
                result.getMessage() + ". Remove unsafe system, process, file, or network access from the snippet.",
                95,
                "HIGH",
                ErrorCategory.LOGIC_ERROR.name(),
                result.getLineNumber()
        );
    }
}
