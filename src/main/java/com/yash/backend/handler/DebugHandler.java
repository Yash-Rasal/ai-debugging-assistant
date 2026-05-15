package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.executor.ExecutionResult;

public interface DebugHandler {

    String exceptionType();

    default boolean canHandle(ExecutionResult result) {
        String supportedExceptionType = exceptionType();
        return result != null
                && supportedExceptionType != null
                && supportedExceptionType.equals(result.getExceptionType());
    }

    DebugResponse handle(String code, ExecutionResult result);
}
