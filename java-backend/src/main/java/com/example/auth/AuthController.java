package com.example.auth;

import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AuthController implements HttpController {

    private final AuthService authService;
    private final EventBus eventBus;

    public AuthController(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.authService = new AuthService();
    }

    /** Registriert die Auth-Routen. */
    @Override
    public void registerRoutes(Router router) {
        router.post("/api/auth/register").handler(this::handleRegister);
    }

    /** Verarbeitet POST /api/auth/register – legt neuen Benutzer an. */
    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Ungültige Anfrage.");
            return;
        }

        // Username, Passwort und Wiederholung aus dem Body holen
        String username = body.getString("username");
        String password = body.getString("password");
        String passwordRepeat = body.getString("passwordRepeat");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            ctx.response().setStatusCode(400).end("Benutzername und Passwort erforderlich.");
            return;
        }
        if (passwordRepeat == null || !password.equals(passwordRepeat)) {
            ctx.response().setStatusCode(400).end("Die Passwörter stimmen nicht überein.");
            return;
        }
        if (username.trim().length() < 3) {
            ctx.response().setStatusCode(400).end("Benutzername muss mindestens 3 Zeichen haben.");
            return;
        }
        if (password.length() < 4) {
            ctx.response().setStatusCode(400).end("Passwort muss mindestens 4 Zeichen haben.");
            return;
        }

        authService.register(username.trim(), password, res -> {
            if (res.succeeded()) {
                ctx.response().setStatusCode(201).end("Benutzer erfolgreich erstellt.");
                eventBus.publish("player.created", username);
            } else {
                String message = res.cause() != null ? res.cause().getMessage() : "Registrierung fehlgeschlagen.";
                if (message.contains("Duplicate")) {
                    ctx.response().setStatusCode(409).end("Benutzername existiert bereits.");
                } else {
                    ctx.response().setStatusCode(500).end("Registrierung fehlgeschlagen.");
                }
            }
        });
    }
}
