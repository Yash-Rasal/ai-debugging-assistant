package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.ErrorCategory;
import com.yash.backend.executor.DiagnosticCode;
import com.yash.backend.executor.ExecutionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(11)
@Component
public class CannotFindSymbolCompilationHandler implements DebugHandler {

    @Override
    public String exceptionType() {
        return null;
    }

    @Override
    public boolean canHandle(ExecutionResult result) {
        return result != null
                && result.isCompilationError()
                && result.getDiagnosticCode() == DiagnosticCode.CANNOT_FIND_SYMBOL;
    }

    @Override
    public DebugResponse handle(String code, ExecutionResult result) {
        return new DebugResponse(
                "The compiler cannot resolve a variable, method, class, or field name.",
                "Check the spelling, declaration scope, imports, and whether the symbol is defined before use.",
                95,
                "HIGH",
                ErrorCategory.COMPILATION_ERROR.name(),
                result.getLineNumber()
        );
    }
}
