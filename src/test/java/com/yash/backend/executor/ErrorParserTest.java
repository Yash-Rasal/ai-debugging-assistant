package com.yash.backend.executor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorParserTest {

    private final ErrorParser errorParser = new ErrorParser();

    @Test
    void runtimeOutputFromGeneratedWrapperUsesUserLineNumber() {
        ExecutionResult result = errorParser.runtime(
                "Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n" +
                        "\tat UserCode.main(UserCode.java:3)\n"
        );

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.RUNTIME_ERROR);
        assertThat(result.getExceptionType()).isEqualTo("ArithmeticException");
        assertThat(result.getLineNumber()).isEqualTo(1);
    }

    @Test
    void suppliedRuntimeDiagnosticKeepsReportedLineNumber() {
        ExecutionResult result = errorParser.fromDiagnostic(
                null,
                "java.lang.NullPointerException: Cannot invoke method\n" +
                        "\tat Example.main(Example.java:12)\n"
        );

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.RUNTIME_ERROR);
        assertThat(result.getExceptionType()).isEqualTo("NullPointerException");
        assertThat(result.getLineNumber()).isEqualTo(12);
    }

    @Test
    void suppliedCompilerDiagnosticKeepsReportedLineNumber() {
        ExecutionResult result = errorParser.fromDiagnostic(
                "Example.java:8: error: ';' expected",
                null
        );

        assertThat(result.getStatus()).isEqualTo(ExecutionStatus.COMPILATION_ERROR);
        assertThat(result.getDiagnosticCode()).isEqualTo(DiagnosticCode.MISSING_SEMICOLON);
        assertThat(result.getLineNumber()).isEqualTo(8);
    }

    @Test
    void classifiesCannotFindSymbolCompilerError() {
        ExecutionResult result = errorParser.compilation(
                "UserCode.java:4: error: cannot find symbol\n" +
                        "        System.out.println(total);\n" +
                        "                           ^\n" +
                        "  symbol:   variable total\n" +
                        "  location: class UserCode\n"
        );

        assertThat(result.getDiagnosticCode()).isEqualTo(DiagnosticCode.CANNOT_FIND_SYMBOL);
        assertThat(result.getLineNumber()).isEqualTo(2);
    }
}
