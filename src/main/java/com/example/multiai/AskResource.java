package com.example.multiai;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/ask")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AskResource {

    @Inject
    MultiAiService multiAiService;

    public static class AskRequest {
        public String question;
    }

    public static class AskResponse {
        public String claude;
        public String openai;
        public String perplexity;
        public String unified;
    }

    @POST
    public AskResponse ask(AskRequest request) {
        String q = request == null ? null : request.question;
        if (q == null || q.isBlank()) {
            throw new BadRequestException("question is required");
        }

        MultiAiService.MultiAiResult result = multiAiService.askAll(q);

        AskResponse resp = new AskResponse();
        resp.claude = result.claude();
        resp.openai = result.openai();
        resp.perplexity = result.perplexity();
        resp.unified = result.unified();
        return resp;
    }
}
