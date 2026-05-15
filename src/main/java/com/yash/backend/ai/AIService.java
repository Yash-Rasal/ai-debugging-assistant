package com.yash.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yash.backend.DebugResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import java.util.*;

@Service
public class AIService {

    @Value("${ai.enabled}")
    private boolean aiEnabled;

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DebugResponse getAISuggestion(String code, String error, String stackTrace) {

        if (!aiEnabled) {
            return new DebugResponse(
                    "AI_DISABLED",
                    "AI is turned off",
                    0,
                    "LOW",
                    "SYSTEM",
                    -1
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            return new DebugResponse(
                    "AI_NOT_CONFIGURED",
                    "Set GROQ_API_KEY before using AI suggestions.",
                    0,
                    "LOW",
                    "SYSTEM",
                    -1
            );
        }

        String prompt =
                "You are a STRICT Java debugging engine.\n\n" +

                        "SYSTEM RULES (MANDATORY):\n" +
                        "1. ALWAYS rely on STACK TRACE as primary source\n" +
                        "2. Code is secondary context only\n" +
                        "3. NEVER guess - only infer from given data\n" +
                        "4. ALWAYS return valid JSON (no extra text)\n" +
                        "5. You are NOT allowed to classify category, severity, confidence, or lineNumber\n" +
                        "6. Only explain cause and suggestion\n\n" +

                        "OUTPUT FORMAT RULES:\n" +
                        "- cause must be short and precise\n" +
                        "- suggestion must be actionable and specific\n" +
                        "- do not include category, severity, confidence, or lineNumber in the JSON\n\n" +

                        "INPUT:\n" +
                        "Code:\n" + code + "\n\n" +
                        "Error:\n" + error + "\n\n" +
                        "StackTrace:\n" + stackTrace + "\n\n" +

                        "OUTPUT (STRICT JSON ONLY):\n" +
                        "{\n" +
                        "  \"cause\": \"...\",\n" +
                        "  \"suggestion\": \"...\"\n" +
                        "}";

        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.3-70b-versatile");
            requestBody.put("temperature", 0.0);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(requestBody, headers);

            Map response = restTemplate.postForObject(url, request, Map.class);

            List choices = (List) response.get("choices");
            Map first = (Map) choices.get(0);
            Map msg = (Map) first.get("message");

            String content = (String) msg.get("content");

            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");

            if (start != -1 && end != -1) {
                String json = content.substring(start, end + 1);
                return objectMapper.readValue(json, DebugResponse.class);
            }

            return new DebugResponse(
                    "PARSE_ERROR",
                    "AI response invalid",
                    0,
                    "LOW",
                    "AI_ERROR",
                    -1
            );

        } catch (Exception e) {
            return new DebugResponse(
                    "AI_FAILED",
                    e.getMessage(),
                    0,
                    "LOW",
                    "SYSTEM",
                    -1
            );
        }
    }
}
