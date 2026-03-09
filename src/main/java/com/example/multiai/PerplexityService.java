package com.example.multiai;

import jakarta.enterprise.context.ApplicationScoped;
import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class PerplexityService {

    private static final String URL = "https://api.perplexity.ai/search";

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String askPerplexity(String question) throws Exception {
        String apiKey = System.getenv("PERPLEXITY_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return "Perplexity API key not configured.";
        }

        String bodyJson = """
          {
            "query": %s,
            "max_results": 5,
            "max_tokens_per_page": 1024
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
                return "Perplexity error: " + response.code() + " " + response.message();
            }
            String json = response.body().string();
            JsonNode root = mapper.readTree(json);

            StringBuilder sb = new StringBuilder();
            for (JsonNode r : root.path("results")) {
                String title = r.path("title").asText("");
                String url = r.path("url").asText("");
                String snippet = r.path("snippet").asText("");
                if (!title.isEmpty()) {
                    sb.append("- ").append(title);
                    if (!url.isEmpty()) {
                        sb.append(" (").append(url).append(")");
                    }
                    sb.append("\n");
                }
                if (!snippet.isEmpty()) {
                    sb.append("  ").append(snippet).append("\n");
                }
            }
            return sb.length() == 0 ? "(no results from Perplexity)" : sb.toString();
        }
    }
}
