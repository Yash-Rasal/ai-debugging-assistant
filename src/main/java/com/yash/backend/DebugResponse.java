package com.yash.backend;

public class DebugResponse {

    private String cause;
    private String suggestion;
    private int confidence;
    private String severity;
    private String category;
    private int lineNumber;

    public DebugResponse() {}

    public DebugResponse(String cause, String suggestion, int confidence,
                         String severity, String category, int lineNumber) {
        this.cause = cause;
        this.suggestion = suggestion;
        this.confidence = confidence;
        this.severity = severity;
        this.category = category;
        this.lineNumber = lineNumber;
    }

    // getters & setters
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
}