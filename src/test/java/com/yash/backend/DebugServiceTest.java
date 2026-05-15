package com.yash.backend;

import com.yash.backend.ai.AIService;
import com.yash.backend.executor.CodeExecutor;
import com.yash.backend.executor.ErrorParser;
import com.yash.backend.executor.ExecutionResult;
import com.yash.backend.handler.ArithmeticExceptionHandler;
import com.yash.backend.handler.DebugHandler;
import com.yash.backend.handler.MissingSemicolonCompilationHandler;
import com.yash.backend.handler.NullPointerExceptionHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void compileDiagnosticCanUseDeterministicHandler() {
        CodeExecutor localExecutor = mock(CodeExecutor.class);
        DebugService localService = new DebugService(
                List.of(new MissingSemicolonCompilationHandler()),
                aiService,
                localExecutor,
                errorParser
        );

        when(localExecutor.execute("int x = 1"))
                .thenReturn(errorParser.compilation("UserCode.java:3: error: ';' expected"));

        DebugResponse response = localService.analyze("int x = 1");

        assertThat(response.getCategory()).isEqualTo("COMPILATION_ERROR");
        assertThat(response.getCause()).contains("semicolon");
        assertThat(response.getLineNumber()).isEqualTo(1);
        verifyNoInteractions(aiService);
    }

    @Test
    void nullRequestReturnsInvalidRequestResponse() {
        DebugResponse response = debugService.analyze((DebugRequest) null);

        assertThat(response.getCause()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getCategory()).isEqualTo("LOGIC_ERROR");
        assertThat(response.getLineNumber()).isEqualTo(-1);
        verifyNoInteractions(aiService, codeExecutor);
    }

    @Test
    void blankCodeReturnsInvalidRequestResponse() {
        DebugResponse response = debugService.analyze("   ");

        assertThat(response.getCause()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getSuggestion()).contains("Provide Java code");
        assertThat(response.getCategory()).isEqualTo("LOGIC_ERROR");
        verifyNoInteractions(aiService, codeExecutor);
    }

    @Test
    void unknownRuntimeErrorUsesAiFallbackAndPreservesSystemClassification() {
        String stackTrace = "Exception in thread \"main\" java.lang.IllegalStateException: bad state\n" +
                "\tat UserCode.main(UserCode.java:5)\n";

        when(codeExecutor.execute("throw new IllegalStateException(\"bad state\");"))
                .thenReturn(new ExecutionResult("RUNTIME", "IllegalStateException", stackTrace, stackTrace, 3));
        when(aiService.getAISuggestion("throw new IllegalStateException(\"bad state\");", stackTrace, stackTrace))
                .thenReturn(new DebugResponse(
                        "AI_CAUSE",
                        "AI_SUGGESTION",
                        70,
                        "LOW",
                        "LOGIC_ERROR",
                        -1
                ));

        DebugResponse response = debugService.analyze("throw new IllegalStateException(\"bad state\");");

        assertThat(response.getCause()).isEqualTo("AI_CAUSE");
        assertThat(response.getCategory()).isEqualTo("RUNTIME_ERROR");
        assertThat(response.getSeverity()).isEqualTo("HIGH");
        assertThat(response.getLineNumber()).isEqualTo(3);
        verify(aiService).getAISuggestion("throw new IllegalStateException(\"bad state\");", stackTrace, stackTrace);
    }

    @Test
    void codeTakesPriorityWhenRequestAlsoContainsDiagnostic() {
        DebugRequest request = new DebugRequest();
        request.setCode("int x = 1;");
        request.setError("Some old error");

        when(codeExecutor.execute("int x = 1;"))
                .thenReturn(ExecutionResult.success());

        DebugResponse response = debugService.analyze(request);

        assertThat(response.getCategory()).isEqualTo("SUCCESS");
        verify(codeExecutor).execute("int x = 1;");
        verifyNoInteractions(aiService);
    }
}
