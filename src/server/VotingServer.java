package server;

import com.sun.net.httpserver.HttpExchange;
import entity.Candidate;
import entity.Session;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static entity.Session.createSession;

public class VotingServer extends BasicServer{

    private final static Configuration freemarker = initFreeMarker();

    List<Candidate> candidateList;

    public VotingServer(String host, int port) throws IOException {
        super(host, port);
        candidateList = Util.readCandidatesFromJson("data/candidates.json");
        registerGet("/", this::mainPageHandler);
        registerGet("/votes", this::votesPercentageHandler);
        registerPost("/", this::voteHandler);
        registerGet("/thankyou", this::thankYouHandler);
    }

    private void voteHandler(HttpExchange exchange) {
        if ("POST".equals(exchange.getRequestMethod())){
            try{
                Map<String, String> formData = Util.getFormData(exchange);
                String candidateIndexStr = formData.get("candidateIndex");

                if (candidateIndexStr != null) {
                    try {
                        int candidateIndex = Integer.parseInt(candidateIndexStr);

                        Candidate selectedCandidate = candidateList.get(candidateIndex);
                        selectedCandidate.incrementVotes();

                        String sessionId = createSession(selectedCandidate);

                        exchange.getResponseHeaders().set("Location", "/thankyou");
                        exchange.getResponseHeaders().add("Set-Cookie", "sessionId=" + sessionId);
                        exchange.sendResponseHeaders(302, -1);
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        sendErrorResponse(exchange, 400, "Invalid candidate index");
                    }
                } else {
                    sendErrorResponse(exchange, 400, "Missing candidate index");
                }
            } catch (IOException e){
                e.printStackTrace();
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMessage) {
        try {
            exchange.sendResponseHeaders(statusCode, errorMessage.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorMessage.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void thankYouHandler(HttpExchange exchange) {
        String sessionId = extractSessionIdFromRequest(exchange);

        Candidate selectedCandidate = Session.getCandidateForSession(sessionId);

        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("selectedCandidate", selectedCandidate);
        dataModel.put("totalVotes", calculateTotalVotes());
        renderTemplate(exchange, "data/thankyou.ftlh", dataModel);
    }

    private String extractSessionIdFromRequest(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");

        if (cookies != null) {
            for (String cookie : cookies) {
                String[] parts = cookie.split(";");
                for (String part : parts) {
                    String[] pair = part.trim().split("=");
                    if (pair.length == 2 && pair[0].equals("sessionId")) {
                        return pair[1];
                    }
                }
            }
        }

        return null;
    }

    private void votesPercentageHandler(HttpExchange exchange) {
        Map<String, Object> dataModel = new HashMap<>();

        dataModel.put("candidates", candidateList);
        dataModel.put("totalVotes", calculateTotalVotes());
        renderTemplate(exchange, "data/votes.ftlh", dataModel);
    }

    private int calculateTotalVotes() {
        return candidateList.stream().mapToInt(Candidate::getAmountOfVotes).sum();
    }

    private void mainPageHandler(HttpExchange exchange) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("candidates", candidateList);
        renderTemplate(exchange, "data/candidates.ftlh", dataModel);
    }

    protected final void registerPost(String route, RouteHandler handler) {
        getRoutes().put("POST " + route, handler);
    }

    private static Configuration initFreeMarker() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
        try {
            cfg.setDirectoryForTemplateLoading(new File("."));
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setWrapUncheckedExceptions(true);
            cfg.setFallbackOnNullLoopVariable(false);
        } catch (IOException e) {
            throw new RuntimeException("Failed to set directory for template loading", e);
        }
        return cfg;
    }

    protected void renderTemplate(HttpExchange exchange, String templateFile, Object dataModel) {
        try {
            Template template = freemarker.getTemplate(templateFile);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {
                template.process(dataModel, writer);
                writer.flush();
                byte[] data = stream.toByteArray();
                sendByteData(exchange, ResponseCodes.OK, ContentType.TEXT_HTML, data);
            }
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}


