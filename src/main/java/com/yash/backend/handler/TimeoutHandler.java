package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.executor.ExecutionStatus;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(4)
@Component
public class TimeoutHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return null;
    }

    @Override
    public boolean canHandle(ExecutionResult result) {
        return result != null && result.getStatus() == ExecutionStatus.TIMEOUT;
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The submitted code did not finish within the execution time limit.",
                "Check for infinite loops, blocking operations, or loops without a reliable termination condition.",
                95,
                "HIGH",
                ErrorCategory.RUNTIME_ERROR.name(),
                result.getLineNumber()
        );
    }
}
