package com.example.lobby;

import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class LobbyController implements HttpController {

    private final LobbyService lobbyService;

    public LobbyController(Vertx vertx) {
        this.lobbyService = new LobbyService();
    }

    /** Registriert die Lobby-Routen. */
    @Override
    public void registerRoutes(Router router) {
        router.post("/api/lobby/create").handler(this::handleCreate);
        router.get("/api/lobby/status").handler(this::handleStatus);
        router.post("/api/lobby/join").handler(this::handleJoin);
    }

    /** Erstellt neue Lobby-Session (POST /api/lobby/create). */
    private void handleCreate(RoutingContext ctx) {
        lobbyService.createLobbySession(ar -> {
            if (ar.succeeded()) {
                ctx.response().setStatusCode(200).end("OK");
            } else {
                ctx.response().setStatusCode(500).end();
            }
        });
    }

    /** Liefert Lobby-Status (GET /api/lobby/status). */
    private void handleStatus(RoutingContext ctx) {
        lobbyService.getLobbyStatus(ar -> {
            if (ar.succeeded()) {
                JsonArray players = ar.result();
                JsonObject response = new JsonObject().put("players", players);
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .setStatusCode(200)
                        .end(response.encode());
            } else {
                ctx.response().setStatusCode(500).end();
            }
        });
    }

    /** Spieler tritt Lobby bei (POST /api/lobby/join). */
    private void handleJoin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Ungültige Anfrage.");
            return;
        }
        String username = body.getString("username");
        if (username == null || username.isBlank()) {
            ctx.response().setStatusCode(400).end("Benutzername erforderlich.");
            return;
        }

        lobbyService.addPlayerToLobby(username, ar -> {
            if (ar.succeeded()) {
                ctx.response().setStatusCode(200).end("Lobby erfolgreich beigetreten.");
            } else {
                String msg = ar.cause() != null ? ar.cause().getMessage() : "Lobby-Beitritt fehlgeschlagen.";
                int status = msg.contains("Keine Lobby") ? 404 : 400;
                ctx.response().setStatusCode(status).end(msg);
            }
        });
    }
}
