package com.example.multiai;

import jakarta.enterprise.context.ApplicationScoped;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class OllamaService {

    // Default Ollama endpoint
    private static final String URL = "http://localhost:11434/api/chat";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Simple wrapper: ask a single-turn question
    public String askOllama(String question) throws Exception {
        // Pick a local model name you have pulled in Ollama (e.g. "llama3", "qwen2.5", etc.)
        String model = System.getenv("OLLAMA_MODEL");
        if (model == null || model.isBlank()) {
            model = "llama3";
        }

        // Basic chat format for Ollama chat API
        String bodyJson = """
          {
            "model": %s,
            "messages": [
              { "role": "user", "content": %s }
            ],
            "stream": false
          }
          """.formatted(
            mapper.writeValueAsString(model),
            mapper.writeValueAsString(question)
          );

        Request request = new Request.Builder()
            .url(URL)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Ollama error: " + response.code() + " " + response.message();
            }
            String json = response.body().string();
            JsonNode root = mapper.readTree(json);

            // Standard Ollama chat response: pick the assistant message content
            JsonNode messageNode = root.path("message");
            if (!messageNode.isMissingNode()) {
                String content = messageNode.path("content").asText("");
                if (!content.isEmpty()) {
                    return content;
                }
            }
            // Some variants return an array of messages; handle generically
            JsonNode messages = root.path("messages");
            if (messages.isArray() && messages.size() > 0) {
                for (JsonNode m : messages) {
                    if ("assistant".equals(m.path("role").asText())) {
                        String content = m.path("content").asText("");
                        if (!content.isEmpty()) {
                            return content;
                        }
                    }
                }
            }
            return "(no text from Ollama)";
        }
    }
}
