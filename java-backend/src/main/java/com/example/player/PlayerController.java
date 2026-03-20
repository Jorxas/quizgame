package com.example.player;

/**
 * Player-Controller – Controller an Spieler binden (Bind).
 */
import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class PlayerController implements HttpController {

    private final PlayerService playerService;

    public PlayerController(Vertx vertx) {
        this.playerService = new PlayerService();
    }

    /** Registriert die Player-Routen. */
    @Override
    public void registerRoutes(Router router) {
        router.post("/api/players/bind").handler(this::handleBindController);
    }

    /** Bindet Controller an Spieler (POST /api/players/bind). */
    private void handleBindController(RoutingContext ctx) {
        var body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Ungültige Anfrage.");
            return;
        }

        String username = body.getString("username");
        String controllerId = body.getString("controllerId");
        String controllerType = body.getString("controllerType");

        if (username == null || username.isBlank() || controllerId == null || controllerId.isBlank()) {
            ctx.response().setStatusCode(400).end("Benutzername und controllerId erforderlich.");
            return;
        }

        if (controllerType == null || controllerType.isBlank()) {
            controllerType = "WEB";
        }

        playerService.bindControllerToPlayer(username, controllerId, controllerType, ar -> {
            if (ar.succeeded()) {
                ctx.response().setStatusCode(200).end("Controller erfolgreich zugewiesen.");
            } else {
                String message = ar.cause() != null ? ar.cause().getMessage() : "Controller-Zuweisung fehlgeschlagen.";
                int status = message.contains("not found") || message.contains("nicht gefunden")
                        || message.contains("nicht verfügbar") || message.contains("required") ? 400 : 500;
                ctx.response().setStatusCode(status).end(message);
            }
        });
    }
}
