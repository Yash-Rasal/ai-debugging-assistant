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

    @Test
    void blocksDangerousProcessTerminationBeforeExecution() {
        ExecutionResult result = codeExecutor.execute("System.exit(0);");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SECURITY_VIOLATION);
        assertThat(result.getDiagnosticCode()).isEqualTo(DiagnosticCode.SECURITY_VIOLATION);
        assertThat(result.getLineNumber()).isEqualTo(1);
    }

    @Test
    void reportsCompilationErrorWithStructuredDiagnostic() {
        ExecutionResult result = codeExecutor.execute("int x = 1");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPILATION_ERROR);
        assertThat(result.getDiagnosticCode()).isEqualTo(DiagnosticCode.MISSING_SEMICOLON);
        assertThat(result.getLineNumber()).isEqualTo(1);
    }

    @Test
    void reportsCannotFindSymbolCompileError() {
        ExecutionResult result = codeExecutor.execute("System.out.println(total);");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPILATION_ERROR);
        assertThat(result.getDiagnosticCode()).isEqualTo(DiagnosticCode.CANNOT_FIND_SYMBOL);
        assertThat(result.getMessage()).contains("cannot find symbol");
        assertThat(result.getLineNumber()).isEqualTo(1);
    }

    @Test
    void reportsNullPointerRuntimeErrorWithLineNumber() {
        ExecutionResult result = codeExecutor.execute("""
                String value = null;
                value.length();
                """);

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.RUNTIME_ERROR);
        assertThat(result.getExceptionType()).isEqualTo("NullPointerException");
        assertThat(result.getLineNumber()).isEqualTo(2);
    }

    @Test
    void rejectsBlankCodeWithoutCompilation() {
        ExecutionResult result = codeExecutor.execute("   ");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.INVALID_INPUT);
        assertThat(result.getMessage()).contains("No Java code");
        assertThat(result.getLineNumber()).isEqualTo(-1);
    }

    @Test
    void blocksDangerousProcessBuilderUsage() {
        ExecutionResult result = codeExecutor.execute("new ProcessBuilder(\"cmd\");");

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.SECURITY_VIOLATION);
        assertThat(result.getMessage()).contains("ProcessBuilder");
        assertThat(result.getLineNumber()).isEqualTo(1);
    }
}
