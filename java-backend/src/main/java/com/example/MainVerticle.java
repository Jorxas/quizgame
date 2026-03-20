package com.example;

/**
 * Haupt-Verticle – startet JDBC-Pool, GameStateManager, MQTT- und HTTP-Verticles.
 * Registriert alle REST-Controller (Auth, Controllers, Lobby, Player, Game, Highscores).
 */
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.AuthController;
import com.example.controllers.ControllersController;
import com.example.lobby.LobbyController;
import com.example.player.PlayerController;
import com.example.database.DatabaseClient;
import com.example.game.GameController;
import com.example.highscores.HighscoresController;
import com.example.game.GameStateManager;
import com.example.http.HttpController;
import com.example.http.HttpServerVerticle;
import com.example.mqtt.MqttVerticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    /** Startet alle Verticles (MQTT, HTTP), JDBC-Pool und GameStateManager. */
    @Override
    public void start(Promise<Void> startPromise) {
        setupJDBCPool();

        GameStateManager.getInstance(vertx);

        MqttVerticle mqtt = new MqttVerticle();
        HttpServerVerticle http = new HttpServerVerticle();

        vertx.deployVerticle(mqtt);
        vertx.deployVerticle(http);

        setupHttpVerticle(http);

        logger.info("🚀 Alle Verticles und GameStateManager gestartet.");

        startPromise.complete();

    }

    /** Konfiguriert und initialisiert den JDBC-Pool mit Umgebungsvariablen. */
    private void setupJDBCPool() {
        JsonObject config = new JsonObject()
                .put("DB_HOST", System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "mariadb")
                .put("DB_PORT", System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : 3306)
                .put("DB_NAME", System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "game")
                .put("DB_USER", System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "user")
                .put("DB_PASSWORD", System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "userpassword");

        DatabaseClient.initialize(vertx, config);
    }

    /** Registriert alle REST-Controller an den HTTP-Router. */
    private void setupHttpVerticle(HttpServerVerticle httpVerticle) {

        final List<HttpController> controllers = List.of(
                new AuthController(vertx),
                new ControllersController(vertx),
                new LobbyController(vertx),
                new PlayerController(vertx),
                new GameController(vertx),
                new HighscoresController(vertx)
        );

        controllers.forEach(it -> it.registerRoutes(httpVerticle.router));
    }

    /** Einstiegspunkt: startet Vert.x und deployed das MainVerticle. */
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), res -> {
            if (res.succeeded()) {
                logger.info("Verticle deployment succeeded");
            } else {
                logger.error("Verticle deployment failed: {}", res.cause().getMessage());
            }
        });
    }
}
