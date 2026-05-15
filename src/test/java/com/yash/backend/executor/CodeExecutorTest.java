package com.yash.backend.executor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeExecutorTest {

    private final CodeExecutor codeExecutor = new CodeExecutor();

    @Test
    void executesValidSnippetSuccessfully() {
        ExecutionResult result = codeExecutor.execute("int x = 1 + 1;");

        assertThat(result.getErrorType()).isEqualTo("SUCCESS");
    }

    @Test
    void reportsRuntimeErrorWithUserLineNumber() {
        ExecutionResult result = codeExecutor.execute("int x = 1 / 0;");

        assertThat(result.getErrorType()).isEqualTo("RUNTIME");
        assertThat(result.getExceptionType()).isEqualTo("ArithmeticException");
        assertThat(result.getMessage()).contains("ArithmeticException");
        assertThat(result.getStackTrace()).contains("UserCode.java");
        assertThat(result.getLineNumber()).isEqualTo(1);
    }

    @Test
    void timesOutInfiniteLoop() {
        ExecutionResult result = codeExecutor.execute("while (true) {}");

        assertThat(result.getErrorType()).isEqualTo("TIMEOUT");
        assertThat(result.getMessage()).contains("timed out");
    }
}
