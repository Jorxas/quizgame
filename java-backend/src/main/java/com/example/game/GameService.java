package com.example.game;

/**
 * Game-Service – Spielablauf, Fragenauswahl, Bewertung, Highscores.
 */
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GameService {

    private static final Set<Integer> ALLOWED_MODES = Set.of(5, 10, 20);
    private final GameRepository gameRepository;

    public GameService() {
        this.gameRepository = new GameRepository();
    }

    /** Konfiguration anwenden (mode, categories, difficulties). */
    public void applyConfig(JsonObject body, Handler<AsyncResult<JsonObject>> resultHandler) {
        Integer mode = body.getInteger("mode");
        JsonArray categoriesArr = body.getJsonArray("categories");
        JsonArray difficultiesArr = body.getJsonArray("difficulties");
        if (mode == null || !ALLOWED_MODES.contains(mode)) {
            resultHandler.handle(Future.failedFuture(new IllegalArgumentException("mode muss 5, 10 oder 20 sein")));
            return;
        }
        List<Long> categoryIds = toLongList(categoriesArr);
        List<String> difficulties = toStringList(difficultiesArr);
        if (categoryIds.isEmpty() || difficulties.isEmpty()) {
            resultHandler.handle(Future.failedFuture(new IllegalArgumentException("categories und difficulties dürfen nicht leer sein")));
            return;
        }
        gameRepository.countAvailableQuestions(categoryIds, difficulties, countAr -> {
            if (countAr.failed()) {
                resultHandler.handle(Future.failedFuture(countAr.cause()));
                return;
            }
            int available = countAr.result();
            if (available < mode) {
                resultHandler.handle(Future.failedFuture(
                        new IllegalArgumentException("Nur " + available + " Fragen verfügbar, mindestens " + mode + " erforderlich")));
                return;
            }
            gameRepository.getLobbySessionId(lobbyAr -> {
                if (lobbyAr.failed()) {
                    resultHandler.handle(Future.failedFuture(lobbyAr.cause()));
                    return;
                }
                Long sessionId = lobbyAr.result();
                if (sessionId == null) {
                    resultHandler.handle(Future.failedFuture(new IllegalStateException("Keine Lobby-Session vorhanden")));
                    return;
                }
                String roundLength = "Q" + mode;
                boolean allowEasy = difficulties.contains("EASY");
                boolean allowMedium = difficulties.contains("MEDIUM");
                boolean allowHard = difficulties.contains("HARD");
                gameRepository.updateSessionConfig(sessionId, roundLength, allowEasy, allowMedium, allowHard, updateAr -> {
                    if (updateAr.failed()) {
                        resultHandler.handle(Future.failedFuture(updateAr.cause()));
                        return;
                    }
                    gameRepository.replaceSessionCategories(sessionId, categoryIds, replaceAr -> {
                        if (replaceAr.failed()) {
                            resultHandler.handle(Future.failedFuture(replaceAr.cause()));
                            return;
                        }
                        resultHandler.handle(Future.succeededFuture(new JsonObject().put("ok", true).put("gameSessionId", sessionId)));
                    });
                });
            });
        });
    }

    /** Startet Spiel (alle Spieler müssen ready sein). */
    public void startGame(Handler<AsyncResult<Long>> resultHandler) {
        gameRepository.getLobbySessionId(lobbyAr -> {
            if (lobbyAr.failed()) {
                resultHandler.handle(Future.failedFuture(lobbyAr.cause()));
                return;
            }
            Long sessionId = lobbyAr.result();
            if (sessionId == null) {
                resultHandler.handle(Future.failedFuture(new IllegalStateException("Keine Lobby-Session vorhanden")));
                return;
            }
            gameRepository.areConnectedPlayersReady(sessionId, readyAr -> {
                if (readyAr.failed()) {
                    resultHandler.handle(Future.failedFuture(readyAr.cause()));
                    return;
                }
                if (!Boolean.TRUE.equals(readyAr.result())) {
                    resultHandler.handle(Future.failedFuture(new IllegalStateException("Nicht alle Spieler sind bereit.")));
                    return;
                }
                gameRepository.updateSessionState(sessionId, "COUNTDOWN", updateAr -> {
                    if (updateAr.failed()) {
                        resultHandler.handle(Future.failedFuture(updateAr.cause()));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture(sessionId));
                });
            });
        });
    }

    /** Konvertiert JsonArray zu Liste von Long. */
    private static List<Long> toLongList(JsonArray arr) {
        List<Long> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                Object v = arr.getValue(i);
                if (v instanceof Number) list.add(((Number) v).longValue());
            }
        }
        return list;
    }

    /** Konvertiert JsonArray zu Liste von String. */
    private static List<String> toStringList(JsonArray arr) {
        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                Object v = arr.getValue(i);
                if (v != null) list.add(v.toString());
            }
        }
        return list;
    }
}
