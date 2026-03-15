package com.example.auth;

import com.example.http.HttpController;
import com.example.lobby.LobbyService;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class AuthController implements HttpController {

    private final AuthService authService;
    private final LobbyService lobbyService;

    public AuthController(Vertx vertx) {
        this.authService = new AuthService();
        this.lobbyService = new LobbyService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.post("/api/auth/register").handler(this::handleRegister);
        router.post("/api/auth/login").handler(this::handleLogin);
    }

    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Ungültige Anfrage.");
            return;
        }

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

    /** Verarbeitet POST /api/auth/login – authentifiziert Benutzer. */
    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null) {
            ctx.response().setStatusCode(400).end("Ungültige Anfrage.");
            return;
        }
        String username = body.getString("username");
        String password = body.getString("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            ctx.response().setStatusCode(400).end("Benutzername und Passwort erforderlich.");
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
        authService.login(username.trim(), password, ar -> {
            if (ar.succeeded()) {
                boolean ok = ar.result();
                if (ok) {
                    lobbyService.isUserInCurrentLobby(username.trim(), inLobbyAr -> {
                        if (inLobbyAr.succeeded() && Boolean.TRUE.equals(inLobbyAr.result())) {
                            ctx.response().setStatusCode(409).end("Du bist bereits in der Session angemeldet.");
                            return;
                        }
                        ctx.response().setStatusCode(200).end("Anmeldung erfolgreich.");
                    });
                } else {
                    ctx.response().setStatusCode(401).end("Benutzername oder Passwort falsch.");
                }
            } else {
                ctx.response().setStatusCode(500).end("Anmeldung fehlgeschlagen.");
            }
        });
    }
}
