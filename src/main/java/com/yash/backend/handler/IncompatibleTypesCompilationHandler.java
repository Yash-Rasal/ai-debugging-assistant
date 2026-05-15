package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.DiagnosticCode;
import com.yash.backend.executor.ExecutionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(12)
@Component
public class IncompatibleTypesCompilationHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return null;
    }

    @Override
    public boolean canHandle(ExecutionResult result) {
        return result != null
                && result.isCompilationError()
                && result.getDiagnosticCode() == DiagnosticCode.INCOMPATIBLE_TYPES;
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The compiler found a type mismatch.",
                "Make the assigned value, method return type, or argument type match the expected Java type.",
                95,
                "HIGH",
                ErrorCategory.COMPILATION_ERROR.name(),
                result.getLineNumber()
        );
    }
}
