package com.example.game;

/**
 * Game-Controller – Spiel starten, Countdown, Fragen, Auswertung, Replay.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class GameController implements HttpController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final GameService gameService;
    private final Vertx vertx;

    public GameController(Vertx vertx) {
        this.vertx = vertx;
        this.gameService = new GameService();
    }

    /** Registriert die Game-Routen. */
    @Override
    public void registerRoutes(Router router) {
        router.post("/api/game/config").handler(this::handleGameConfig);
        router.post("/api/game/start").handler(this::handleGameStart);
    }

    /** Konfiguriert Spiel (POST /api/game/config). */
    private void handleGameConfig(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Ungültige Anfrage.");
            return;
        }
        gameService.applyConfig(body, ar -> {
            if (ar.succeeded()) {
                logger.debug("Game config saved for session {}", ar.result().getLong("gameSessionId"));
                ctx.response().setStatusCode(200).end("Spielkonfiguration gespeichert.");
            } else {
                String message = ar.cause().getMessage();
                int status = 400;
                if (message != null) {
                    if (message.contains("mindestens") || message.contains("verfügbar")) status = 422;
                    else if (message.contains("Keine Lobby")) status = 409;
                }
                logger.error("Game config failed: {}", message);
                ctx.response().setStatusCode(status).end(message);
            }
        });
    }

    /** Startet Spiel (POST /api/game/start). */
    private void handleGameStart(RoutingContext ctx) {
        gameService.startGame(ar -> {
            if (ar.succeeded()) {
                Long sessionId = ar.result();
                GameStateManager.getInstance(vertx).startGame(sessionId);
                logger.info("Game started, session {}", sessionId);
                ctx.response().setStatusCode(200).end("Spiel gestartet.");
            } else {
                String message = ar.cause().getMessage();
                int status = 500;
                if (message != null && (message.contains("Keine Lobby") || message.contains("Nicht alle Spieler"))) {
                    status = 409;
                }
                logger.error("Game start failed: {}", message);
                ctx.response().setStatusCode(status).end(message);
            }
        });
    }
}
