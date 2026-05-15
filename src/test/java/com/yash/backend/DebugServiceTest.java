package com.yash.backend;

import com.yash.backend.ai.AIService;
import com.yash.backend.executor.CodeExecutor;
import com.yash.backend.executor.ErrorParser;
import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.handler.ArithmeticExceptionHandler;
import com.yash.backend.handler.DebugHandler;
import com.yash.backend.handler.NullPointerExceptionHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DebugServiceTest {

    private final AIService aiService = mock(AIService.class);
    private final CodeExecutor codeExecutor = mock(CodeExecutor.class);
    private final ErrorParser errorParser = new ErrorParser();
    private final List<DebugHandler> handlers = List.of(
            new ArithmeticExceptionHandler(),
            new NullPointerExceptionHandler()
    );
    private final DebugService debugService = new DebugService(handlers, aiService, codeExecutor, errorParser);

    @Test
    void successfulCodeDoesNotCallAiFallback() {
        when(codeExecutor.execute("int x = 1;"))
                .thenReturn(new ExecutionResult("SUCCESS", "Code executed successfully", -1));

        DebugResponse response = debugService.analyze("int x = 1;");

        assertThat(response.getCategory()).isEqualTo("SUCCESS");
        assertThat(response.getConfidence()).isEqualTo(100);
        verifyNoInteractions(aiService);
    }

    @Test
    void arithmeticExceptionUsesLocalHandlerAndExecutorLineNumber() {
        String error = "Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n" +
                "\tat UserCode.main(UserCode.java:3)\n";

        when(codeExecutor.execute("int x = 1 / 0;"))
                .thenReturn(new ExecutionResult("RUNTIME", "ArithmeticException", error, error, 1));

        DebugResponse response = debugService.analyze("int x = 1 / 0;");

        assertThat(response.getCategory()).isEqualTo("RUNTIME_ERROR");
        assertThat(response.getCause()).contains("invalid arithmetic");
        assertThat(response.getLineNumber()).isEqualTo(1);
        verifyNoInteractions(aiService);
    }

    @Test
    void requestWithOnlyStackTraceStillUsesKnownHandlers() {
        DebugRequest request = new DebugRequest();
        request.setStackTrace("java.lang.NullPointerException: Cannot invoke method");

        DebugResponse response = debugService.analyze(request);

        assertThat(response.getCategory()).isEqualTo("RUNTIME_ERROR");
        assertThat(response.getCause()).contains("null object");
        verifyNoInteractions(aiService);
    }

    @Test
    void unknownDiagnosticUsesAiFallback() {
        DebugRequest request = new DebugRequest();
        request.setError("Some unknown compiler message");

        when(aiService.getAISuggestion(null, "Some unknown compiler message", "Some unknown compiler message"))
                .thenReturn(new DebugResponse(
                        "AI_CAUSE",
                        "AI_SUGGESTION",
                        70,
                        "MEDIUM",
                        "COMPILATION_ERROR",
                        -1
                ));

        DebugResponse response = debugService.analyze(request);

        assertThat(response.getCause()).isEqualTo("AI_CAUSE");
        assertThat(response.getCategory()).isEqualTo("COMPILATION_ERROR");
    }
}
