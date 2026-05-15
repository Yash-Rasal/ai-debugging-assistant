package com.yash.backend.handler;

import com.yash.backend.DebugResponse;
import com.yash.backend.executor.ExecutionResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NullPointerExceptionHandlerTest {

    private final NullPointerExceptionHandler handler = new NullPointerExceptionHandler();

    @Test
    void handlesNullPointerExceptionResults() {
        ExecutionResult result = ExecutionResult.runtimeError(
                "NullPointerException",
                "java.lang.NullPointerException",
                "java.lang.NullPointerException",
                3
        );

        assertThat(handler.canHandle(result)).isTrue();
    }

    @Test
    void doesNotHandleOtherRuntimeExceptions() {
        ExecutionResult result = ExecutionResult.runtimeError(
                "ArithmeticException",
                "java.lang.ArithmeticException: / by zero",
                "java.lang.ArithmeticException: / by zero",
                3
        );

        assertThat(handler.canHandle(result)).isFalse();
    }

    @Test
    void returnsDeterministicNullPointerGuidance() {
        ExecutionResult result = ExecutionResult.runtimeError(
                "NullPointerException",
                "java.lang.NullPointerException",
                "java.lang.NullPointerException",
                5
        );

        DebugResponse response = handler.handle("value.length();", result);

        assertThat(response.getCategory()).isEqualTo("RUNTIME_ERROR");
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getCause()).contains("null object");
        assertThat(response.getSuggestion()).contains("initialization");
        assertThat(response.getLineNumber()).isEqualTo(5);
    }
}
