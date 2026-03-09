package com.example.multiai;

import jakarta.enterprise.context.ApplicationScoped;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ClaudeService {

    private static final String URL = "https://api.anthropic.com/v1/messages";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String askClaude(String question) throws Exception {
        String apiKey = System.getenv("CLAUDE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Claude API key not configured.";
        }

        String bodyJson = """
          {
            "model": "claude-3-7-sonnet-latest",
            "max_tokens": 1024,
            "messages": [
              { "role": "user", "content": %s }
            ]
          }
          """.formatted(mapper.writeValueAsString(question));

        Request request = new Request.Builder()
            .url(URL)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Claude error: " + response.code() + " " + response.message();
            }
            String json = response.body().string();
            JsonNode root = mapper.readTree(json);

            StringBuilder sb = new StringBuilder();
            for (JsonNode c : root.path("content")) {
                if ("text".equals(c.path("type").asText())) {
                    sb.append(c.path("text").asText());
                }
            }
            return sb.length() == 0 ? "(no text from Claude)" : sb.toString();
        }
    }
}
