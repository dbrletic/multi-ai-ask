package com.example.multiai;

import jakarta.enterprise.context.ApplicationScoped;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class OpenAIService {

    private static final String URL = "https://api.openai.com/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String askOpenAI(String question) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "OpenAI API key not configured.";
        }

        String bodyJson = """
          {
            "model": "gpt-4.1-mini",
            "temperature": 0.7,
            "messages": [
              { "role": "user", "content": %s }
            ]
          }
          """.formatted(mapper.writeValueAsString(question));

        Request request = new Request.Builder()
            .url(URL)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(bodyJson, MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "OpenAI error: " + response.code() + " " + response.message();
            }
            String json = response.body().string();
            JsonNode root = mapper.readTree(json);
            return root.path("choices").path(0).path("message").path("content")
                .asText("(no text from OpenAI)");
        }
    }
}
