package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.executor.ExecutionResult;

public interface DebugHandler {

    boolean canHandle(String exceptionType);

    DebugResponse handle(String code, ExecutionResult result);
}
