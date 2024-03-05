package server;

import com.sun.net.httpserver.HttpExchange;
import entity.Candidate;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import util.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingServer extends BasicServer{

    private final static Configuration freemarker = initFreeMarker();

    List<Candidate> candidateList;

    public VotingServer(String host, int port) throws IOException {
        super(host, port);
        candidateList = Util.readCandidatesFromJson("data/candidates.json");
        registerGet("/", this::mainPageHandler);
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

