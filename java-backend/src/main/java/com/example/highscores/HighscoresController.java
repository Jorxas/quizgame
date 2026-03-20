package com.example.highscores;

/**
 * Highscores-Controller – Bestenliste pro Rundenlänge (5/10/20 Fragen).
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HighscoresController implements HttpController {

    private static final Logger logger = LoggerFactory.getLogger(HighscoresController.class);
    private final HighscoresService highscoresService;

    public HighscoresController(Vertx vertx) {
        this.highscoresService = new HighscoresService();
    }

    /** Registriert die Highscores-Routen. */
    @Override
    public void registerRoutes(Router router) {
        router.get("/api/highscores/:mode").handler(this::handleGetHighscores);
    }

    /** Liefert Highscores für Modus (GET /api/highscores/:mode, optional ?limit=20). */
    private void handleGetHighscores(RoutingContext ctx) {
        String modeParam = ctx.pathParam("mode");
        Integer limitParam = ctx.queryParam("limit").isEmpty() ? null : parseInt(ctx.queryParam("limit").get(0), null);
        int mode;
        try {
            mode = Integer.parseInt(modeParam);
        } catch (NumberFormatException e) {
            logger.debug("GET /api/highscores/{}: mode ungültig", modeParam);
            ctx.response().setStatusCode(400).putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "mode muss 5, 10 oder 20 sein").encode());
            return;
        }
        highscoresService.getHighscores(mode, limitParam, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                String message = ar.cause().getMessage();
                int status = message != null && message.contains("mode") ? 400 : 500;
                logger.error("Highscores fehlgeschlagen: {}", message);
                ctx.response().setStatusCode(status).putHeader("content-type", "application/json")
                        .end(new JsonObject().put("error", message != null ? message : "Fehler").encode());
            }
        });
    }

    private static Integer parseInt(String s, Integer defaultValue) {
        if (s == null || s.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
