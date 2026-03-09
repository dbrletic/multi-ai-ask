package com.example.multiai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;

import java.time.Duration;

@ApplicationScoped
public class MultiAiService {

    @Inject
    ClaudeService claudeService;

    @Inject
    OpenAIService openAIService;

    @Inject
    PerplexityService perplexityService;

    public record MultiAiResult(
        String claude,
        String openai,
        String perplexity,
        String unified
    ) {}

    public MultiAiResult askAll(String question) {
        Uni<String> claudeUni = Uni.createFrom().item(() -> {
                try { return claudeService.askClaude(question); }
                catch (Exception e) { return "Claude exception: " + e.getMessage(); }
            })
            .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor());

        Uni<String> openaiUni = Uni.createFrom().item(() -> {
                try { return openAIService.askOpenAI(question); }
                catch (Exception e) { return "OpenAI exception: " + e.getMessage(); }
            })
            .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor());

        Uni<String> perplexityUni = Uni.createFrom().item(() -> {
                try { return perplexityService.askPerplexity(question); }
                catch (Exception e) { return "Perplexity exception: " + e.getMessage(); }
            })
            .runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultExecutor());

        return Uni.combine().all().unis(claudeUni, openaiUni, perplexityUni)
            .with((c, o, p) -> {
                String unified = buildUnifiedAnswer((String) c, (String) o, (String) p);
                return new MultiAiResult((String) c, (String) o, (String) p, unified);
            })
            .await().atMost(Duration.ofSeconds(60));
    }

    private String buildUnifiedAnswer(String claude, String openai, String perplexity) {
        StringBuilder sb = new StringBuilder();

        sb.append("Unified answer combining three sources (Claude, OpenAI, Perplexity):\n\n");

        sb.append("Summary (priority order: Claude → OpenAI → Perplexity):\n");
        sb.append("- Claude: ").append(firstSentence(claude)).append("\n");
        sb.append("- OpenAI: ").append(firstSentence(openai)).append("\n");
        sb.append("- Perplexity: ").append(firstSentence(perplexity)).append("\n\n");

        sb.append("=== Claude ===\n")
          .append(claude).append("\n\n");

        sb.append("=== OpenAI ===\n")
          .append(openai).append("\n\n");

        sb.append("=== Perplexity (search-based) ===\n")
          .append(perplexity).append("\n");

        return sb.toString();
    }

    private String firstSentence(String text) {
        if (text == null || text.isBlank()) return "(no answer)";
        String trimmed = text.trim();
        int idx = trimmed.indexOf('.');
        if (idx <= 0 || idx > 300) {
            return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
        }
        return trimmed.substring(0, idx + 1);
    }
}
