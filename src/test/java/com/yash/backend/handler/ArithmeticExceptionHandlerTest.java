package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.executor.ExecutionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArithmeticExceptionHandlerTest {

    private final ArithmeticExceptionHandler handler = new ArithmeticExceptionHandler();

    @Test
    void handlesArithmeticExceptionResults() {
        ExecutionResult result = ExecutionResult.runtimeError(
                "ArithmeticException",
                "java.lang.ArithmeticException: / by zero",
                "java.lang.ArithmeticException: / by zero",
                4
        );

        assertThat(handler.canHandle(result)).isTrue();
    }

    @Test
    void doesNotHandleOtherRuntimeExceptions() {
        ExecutionResult result = ExecutionResult.runtimeError(
                "NullPointerException",
                "java.lang.NullPointerException",
                "java.lang.NullPointerException",
                2
        );

        assertThat(handler.canHandle(result)).isFalse();
    }

    @Test
    void returnsDeterministicArithmeticGuidance() {
        ExecutionResult result = ExecutionResult.runtimeError(
                "ArithmeticException",
                "java.lang.ArithmeticException: / by zero",
                "java.lang.ArithmeticException: / by zero",
                7
        );

        DebugResponse response = handler.handle("int x = 1 / 0;", result);

        assertThat(response.getCategory()).isEqualTo("RUNTIME_ERROR");
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getCause()).contains("invalid arithmetic");
        assertThat(response.getSuggestion()).contains("division by zero");
        assertThat(response.getLineNumber()).isEqualTo(7);
    }
}
