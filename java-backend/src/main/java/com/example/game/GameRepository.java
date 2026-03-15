package com.example.game;

import com.example.database.DatabaseClient;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameRepository {

    private final JDBCPool jdbcPool;

    public GameRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    /** Zählt verfügbare Fragen (Kategorien, Schwierigkeiten). */
    public void countAvailableQuestions(List<Long> categoryIds, List<String> difficulties,
                                        Handler<AsyncResult<Integer>> resultHandler) {
        if (categoryIds == null || categoryIds.isEmpty() || difficulties == null || difficulties.isEmpty()) {
            resultHandler.handle(Future.succeededFuture(0));
            return;
        }
        String catPlaceholders = categoryIds.stream().map(c -> "?").collect(Collectors.joining(","));
        String diffPlaceholders = difficulties.stream().map(d -> "?").collect(Collectors.joining(","));
        String sql = "SELECT COUNT(*) AS cnt FROM questions WHERE is_active = 1 " +
                "AND category_id IN (" + catPlaceholders + ") AND difficulty IN (" + diffPlaceholders + ")";
        Tuple tuple = Tuple.tuple();
        for (Long id : categoryIds) tuple.addLong(id);
        for (String d : difficulties) tuple.addString(d);
        jdbcPool.preparedQuery(sql).execute(tuple, ar -> {
            if (ar.succeeded()) {
                int count = ar.result().iterator().hasNext() ? ar.result().iterator().next().getInteger("cnt") : 0;
                resultHandler.handle(Future.succeededFuture(count));
            } else {
                resultHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /** Prüft, ob alle Spieler ready sind. */
    public void areAllPlayersReady(long sessionId, Handler<AsyncResult<Boolean>> resultHandler) {
        String sql = "SELECT COUNT(*) AS total, SUM(CASE WHEN is_ready = 1 THEN 1 ELSE 0 END) AS ready_count " +
                "FROM game_session_players WHERE game_session_id = ?";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            Row row = ar.result().iterator().hasNext() ? ar.result().iterator().next() : null;
            if (row == null) {
                resultHandler.handle(Future.succeededFuture(false));
                return;
            }
            long total = ((Number) row.getValue("total")).longValue();
            Number readyVal = (Number) row.getValue("ready_count");
            long readyCount = readyVal != null ? readyVal.longValue() : 0;
            resultHandler.handle(Future.succeededFuture(total > 0 && readyCount == total));
        });
    }

    /** Ermittelt Lobby-Session-ID. */
    public void getLobbySessionId(Handler<AsyncResult<Long>> resultHandler) {
        String sql = "SELECT id FROM game_sessions WHERE state = 'LOBBY' ORDER BY id DESC LIMIT 1";
        jdbcPool.preparedQuery(sql).execute(Tuple.tuple(), ar -> {
            if (ar.succeeded()) {
                if (ar.result().iterator().hasNext()) {
                    resultHandler.handle(Future.succeededFuture(ar.result().iterator().next().getLong("id")));
                } else {
                    resultHandler.handle(Future.succeededFuture(null));
                }
            } else {
                resultHandler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    /** Aktualisiert Session-Config (round_length, Schwierigkeiten). */
    public void updateSessionConfig(long sessionId, String roundLength,
                                    boolean allowEasy, boolean allowMedium, boolean allowHard,
                                    Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE game_sessions SET round_length = ?, allow_easy = ?, allow_medium = ?, allow_hard = ? WHERE id = ?";
        Tuple tuple = Tuple.of(roundLength, allowEasy ? 1 : 0, allowMedium ? 1 : 0, allowHard ? 1 : 0, sessionId);
        jdbcPool.preparedQuery(sql).execute(tuple, ar -> {
            if (ar.succeeded()) resultHandler.handle(Future.succeededFuture());
            else resultHandler.handle(Future.failedFuture(ar.cause()));
        });
    }

    /** Setzt Session-State (z.B. COUNTDOWN). */
    public void updateSessionState(long sessionId, String state, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "UPDATE game_sessions SET state = ?, started_at = COALESCE(started_at, NOW()) WHERE id = ?";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(state, sessionId), ar -> {
            if (ar.succeeded()) resultHandler.handle(Future.succeededFuture());
            else resultHandler.handle(Future.failedFuture(ar.cause()));
        });
    }

    /** Ersetzt Kategorien der Session. */
    public void replaceSessionCategories(long sessionId, List<Long> categoryIds,
                                         Handler<AsyncResult<Void>> resultHandler) {
        jdbcPool.preparedQuery("DELETE FROM game_session_categories WHERE game_session_id = ?")
                .execute(Tuple.of(sessionId), delAr -> {
                    if (delAr.failed()) {
                        resultHandler.handle(Future.failedFuture(delAr.cause()));
                        return;
                    }
                    if (categoryIds == null || categoryIds.isEmpty()) {
                        resultHandler.handle(Future.succeededFuture());
                        return;
                    }
                    runInserts(sessionId, categoryIds, 0, resultHandler);
                });
    }

    private void runInserts(long sessionId, List<Long> categoryIds, int index,
                            Handler<AsyncResult<Void>> resultHandler) {
        if (index >= categoryIds.size()) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }
        String sql = "INSERT INTO game_session_categories (game_session_id, category_id) VALUES (?, ?)";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId, categoryIds.get(index)), ar -> {
            if (ar.failed()) resultHandler.handle(Future.failedFuture(ar.cause()));
            else runInserts(sessionId, categoryIds, index + 1, resultHandler);
        });
    }

    /** Lädt zufällige Fragen für die Session. */
    public void fetchRandomQuestions(long sessionId, int count, Handler<AsyncResult<List<Long>>> resultHandler) {
        String sql = "SELECT q.id FROM questions q " +
                "JOIN game_session_categories gsc ON gsc.category_id = q.category_id AND gsc.game_session_id = ? " +
                "JOIN game_sessions gs ON gs.id = gsc.game_session_id " +
                "WHERE q.is_active = 1 " +
                "AND (gs.allow_easy = 1 AND q.difficulty = 'EASY' OR gs.allow_medium = 1 AND q.difficulty = 'MEDIUM' OR gs.allow_hard = 1 AND q.difficulty = 'HARD') " +
                "ORDER BY RAND() LIMIT ?";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId, count), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            List<Long> questionIds = new ArrayList<>();
            for (Row row : ar.result()) questionIds.add(row.getLong("id"));
            if (questionIds.isEmpty()) {
                resultHandler.handle(Future.failedFuture("Keine passenden Fragen gefunden"));
                return;
            }
            insertSessionQuestions(sessionId, questionIds, 0, insertAr -> {
                if (insertAr.failed()) resultHandler.handle(Future.failedFuture(insertAr.cause()));
                else resultHandler.handle(Future.succeededFuture(questionIds));
            });
        });
    }

    private void insertSessionQuestions(long sessionId, List<Long> questionIds, int index,
                                        Handler<AsyncResult<Void>> resultHandler) {
        if (index >= questionIds.size()) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }
        String sql = "INSERT IGNORE INTO game_session_questions (game_session_id, question_index, question_id, asked_at) VALUES (?, ?, ?, NULL)";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId, index + 1, questionIds.get(index)), ar -> {
            if (ar.failed()) resultHandler.handle(Future.failedFuture(ar.cause()));
            else insertSessionQuestions(sessionId, questionIds, index + 1, resultHandler);
        });
    }

    /** Lädt Frage mit Optionen. */
    public void fetchQuestionWithOptions(long questionId, Handler<AsyncResult<JsonObject>> resultHandler) {
        String sql = "SELECT q.id, q.question_text, q.difficulty, q.correct_option, qo.option_letter, qo.option_text " +
                "FROM questions q LEFT JOIN question_options qo ON qo.question_id = q.id " +
                "WHERE q.id = ? ORDER BY qo.option_letter";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(questionId), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            try {
                RowSet<Row> rows = ar.result();
                JsonObject question = null;
                JsonObject options = new JsonObject();
                for (Row row : rows) {
                    if (question == null) {
                        question = new JsonObject()
                                .put("questionId", row.getLong("id"))
                                .put("text", row.getString("question_text"))
                                .put("difficulty", row.getString("difficulty"))
                                .put("correctOption", row.getString("correct_option"));
                    }
                    String letter = row.getString("option_letter");
                    if (letter != null) options.put(letter, row.getString("option_text"));
                }
                if (question == null) {
                    resultHandler.handle(Future.failedFuture("Question not found: " + questionId));
                    return;
                }
                question.put("options", options);
                resultHandler.handle(Future.succeededFuture(question));
            } catch (Exception e) {
                resultHandler.handle(Future.failedFuture(e));
            }
        });
    }

    private static final Map<String, Double> BASE_POINTS = Map.of("EASY", 1.0, "MEDIUM", 2.0, "HARD", 3.0);

    private static double getTimeFactor(int responseTimeMs) {
        if (responseTimeMs < 5000) return 1.0;
        if (responseTimeMs < 10000) return 0.9;
        if (responseTimeMs < 15000) return 0.8;
        if (responseTimeMs < 20000) return 0.7;
        if (responseTimeMs < 25000) return 0.6;
        if (responseTimeMs < 30000) return 0.5;
        return 0.0;
    }

    private static String getTimeBucket(int responseTimeMs) {
        if (responseTimeMs < 5000) return "T0_5";
        if (responseTimeMs < 10000) return "T5_10";
        if (responseTimeMs < 15000) return "T10_15";
        if (responseTimeMs < 20000) return "T15_20";
        if (responseTimeMs < 25000) return "T20_25";
        if (responseTimeMs < 30000) return "T25_30";
        return "T30P";
    }

    /** Speichert Antwort und berechnet Punkte. */
    public void saveAnswer(long sessionId, String username, long questionId, String selectedOption,
                           int responseTimeMs, Handler<AsyncResult<JsonObject>> resultHandler) {
        String fetchSql = "SELECT q.correct_option, q.difficulty FROM questions q WHERE q.id = ?";
        jdbcPool.preparedQuery(fetchSql).execute(Tuple.of(questionId), qAr -> {
            if (qAr.failed()) {
                resultHandler.handle(Future.failedFuture(qAr.cause()));
                return;
            }
            if (!qAr.result().iterator().hasNext()) {
                resultHandler.handle(Future.failedFuture("Question not found"));
                return;
            }
            try {
                Row qRow = qAr.result().iterator().next();
                String correctOption = qRow.getString("correct_option");
                String difficulty = qRow.getString("difficulty");
                boolean isCorrect = correctOption.equalsIgnoreCase(selectedOption);
                double basePoints = BASE_POINTS.getOrDefault(difficulty, 1.0);
                double timeFactor = isCorrect ? getTimeFactor(responseTimeMs) : 0.0;
                double pointsAwarded = isCorrect ? basePoints * timeFactor : 0.0;
                String timeBucket = getTimeBucket(responseTimeMs);
                String insertSql = "INSERT INTO game_answers (game_session_id, user_id, question_id, answered_option, " +
                        "response_time_ms, time_bucket, is_correct, points_awarded) " +
                        "SELECT ?, u.id, ?, ?, ?, ?, ?, ? FROM users u WHERE u.username = ? LIMIT 1";
                jdbcPool.preparedQuery(insertSql).execute(
                        Tuple.of(sessionId, questionId, selectedOption, responseTimeMs, timeBucket, isCorrect ? 1 : 0, pointsAwarded, username),
                        insertAr -> {
                            if (insertAr.failed()) {
                                String err = insertAr.cause().getMessage();
                                if (err != null && (err.contains("Duplicate") || err.contains("PRIMARY"))) {
                                    resultHandler.handle(Future.succeededFuture(
                                            new JsonObject().put("duplicate", true).put("is_correct", isCorrect)
                                                    .put("points_awarded", pointsAwarded).put("correct_option", correctOption)));
                                } else {
                                    resultHandler.handle(Future.failedFuture(insertAr.cause()));
                                }
                                return;
                            }
                            resultHandler.handle(Future.succeededFuture(
                                    new JsonObject().put("is_correct", isCorrect).put("points_awarded", pointsAwarded).put("correct_option", correctOption)));
                        });
            } catch (Exception e) {
                resultHandler.handle(Future.failedFuture(e));
            }
        });
    }

    /** Berechnet Session-Ergebnisse. */
    public void computeSessionResults(long sessionId, Handler<AsyncResult<JsonArray>> resultHandler) {
        String sql = "INSERT INTO game_session_results (game_session_id, user_id, total_points, total_response_time_ms, correct_count, answered_count) " +
                "SELECT ga.game_session_id, ga.user_id, SUM(ga.points_awarded), SUM(ga.response_time_ms), SUM(ga.is_correct), COUNT(*) " +
                "FROM game_answers ga WHERE ga.game_session_id = ? GROUP BY ga.game_session_id, ga.user_id " +
                "ON DUPLICATE KEY UPDATE total_points = VALUES(total_points), total_response_time_ms = VALUES(total_response_time_ms), " +
                "correct_count = VALUES(correct_count), answered_count = VALUES(answered_count)";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            String fetchSql = "SELECT u.username, gsr.total_points, gsr.correct_count, gsr.answered_count " +
                    "FROM game_session_results gsr JOIN users u ON u.id = gsr.user_id " +
                    "WHERE gsr.game_session_id = ? ORDER BY gsr.total_points DESC";
            jdbcPool.preparedQuery(fetchSql).execute(Tuple.of(sessionId), fetchAr -> {
                if (fetchAr.failed()) {
                    resultHandler.handle(Future.failedFuture(fetchAr.cause()));
                    return;
                }
                JsonArray rankings = new JsonArray();
                for (Row row : fetchAr.result()) {
                    rankings.add(new JsonObject()
                            .put("username", row.getString("username"))
                            .put("totalPoints", ((Number) row.getValue("total_points")).doubleValue())
                            .put("correctCount", row.getInteger("correct_count"))
                            .put("answeredCount", row.getInteger("answered_count")));
                }
                resultHandler.handle(Future.succeededFuture(rankings));
            });
        });
    }

    /** Speichert Highscores. */
    public void saveHighscores(long sessionId, Handler<AsyncResult<Void>> resultHandler) {
        String sql = "INSERT INTO highscores (round_length, user_id, game_session_id, total_points, total_response_time_ms) " +
                "SELECT gs.round_length, gsr.user_id, gsr.game_session_id, gsr.total_points, gsr.total_response_time_ms " +
                "FROM game_session_results gsr JOIN game_sessions gs ON gs.id = gsr.game_session_id " +
                "WHERE gsr.game_session_id = ? ORDER BY gsr.total_points DESC, gsr.total_response_time_ms ASC";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId), ar -> {
            if (ar.succeeded()) resultHandler.handle(Future.succeededFuture());
            else resultHandler.handle(Future.failedFuture(ar.cause()));
        });
    }

    /** Liefert Spieler einer Session. */
    public void fetchSessionPlayers(long sessionId, Handler<AsyncResult<List<String>>> resultHandler) {
        String sql = "SELECT u.username FROM game_session_players gsp JOIN users u ON u.id = gsp.user_id WHERE gsp.game_session_id = ?";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            List<String> players = new ArrayList<>();
            for (Row row : ar.result()) players.add(row.getString("username"));
            resultHandler.handle(Future.succeededFuture(players));
        });
    }

    /** Setzt Session für Replay zurück (LOBBY). */
    public void resetSessionToLobbyForReplay(long sessionId, Handler<AsyncResult<JsonArray>> resultHandler) {
        jdbcPool.preparedQuery("DELETE FROM game_answers WHERE game_session_id = ?").execute(Tuple.of(sessionId), delAnswersAr -> {
            if (delAnswersAr.failed()) {
                resultHandler.handle(Future.failedFuture(delAnswersAr.cause()));
                return;
            }
            jdbcPool.preparedQuery("DELETE FROM game_session_questions WHERE game_session_id = ?").execute(Tuple.of(sessionId), delQsAr -> {
                if (delQsAr.failed()) {
                    resultHandler.handle(Future.failedFuture(delQsAr.cause()));
                    return;
                }
                doResetPlayersAndState(sessionId, resultHandler);
            });
        });
    }

    private void doResetPlayersAndState(long sessionId, Handler<AsyncResult<JsonArray>> resultHandler) {
        jdbcPool.preparedQuery("UPDATE game_session_players SET is_ready = 0 WHERE game_session_id = ?").execute(Tuple.of(sessionId), updateAr -> {
            if (updateAr.failed()) {
                resultHandler.handle(Future.failedFuture(updateAr.cause()));
                return;
            }
            jdbcPool.preparedQuery("UPDATE game_sessions SET state = 'LOBBY' WHERE id = ?").execute(Tuple.of(sessionId), stateAr -> {
                if (stateAr.failed()) {
                    resultHandler.handle(Future.failedFuture(stateAr.cause()));
                    return;
                }
                String sql = "SELECT u.username FROM game_session_players gsp JOIN users u ON u.id = gsp.user_id WHERE gsp.game_session_id = ?";
                jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId), userAr -> {
                    if (userAr.failed()) {
                        resultHandler.handle(Future.failedFuture(userAr.cause()));
                        return;
                    }
                    JsonArray usernames = new JsonArray();
                    for (Row row : userAr.result()) usernames.add(row.getString("username"));
                    resultHandler.handle(Future.succeededFuture(usernames));
                });
            });
        });
    }

    /** Liefert Frage-Ergebnisse pro Spieler. */
    public void fetchQuestionResults(long sessionId, long questionId, Handler<AsyncResult<JsonArray>> resultHandler) {
        String sql = "SELECT u.username, COALESCE(ga.is_correct, 0) AS is_correct, COALESCE(ga.points_awarded, 0) AS points_awarded, COALESCE(ga.response_time_ms, 0) AS response_time_ms " +
                "FROM game_session_players gsp JOIN users u ON u.id = gsp.user_id " +
                "LEFT JOIN game_answers ga ON ga.user_id = gsp.user_id AND ga.game_session_id = gsp.game_session_id AND ga.question_id = ? " +
                "WHERE gsp.game_session_id = ? ORDER BY points_awarded DESC, u.username";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(questionId, sessionId), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            try {
                JsonArray results = new JsonArray();
                for (Row row : ar.result()) {
                    Object isCorrectValue = row.getValue("is_correct");
                    boolean correct = Boolean.TRUE.equals(isCorrectValue) || (isCorrectValue instanceof Number && ((Number) isCorrectValue).intValue() == 1);
                    Number pointsValue = (Number) row.getValue("points_awarded");
                    Number responseTimeValue = (Number) row.getValue("response_time_ms");
                    results.add(new JsonObject()
                            .put("username", row.getString("username"))
                            .put("correct", correct)
                            .put("points", pointsValue != null ? pointsValue.doubleValue() : 0.0)
                            .put("responseTimeMs", responseTimeValue != null ? responseTimeValue.intValue() : 0));
                }
                resultHandler.handle(Future.succeededFuture(results));
            } catch (Exception e) {
                resultHandler.handle(Future.failedFuture(e));
            }
        });
    }

    /** Liefert Laufende Punktestände. */
    public void fetchRunningTotals(long sessionId, Handler<AsyncResult<JsonArray>> resultHandler) {
        String sql = "SELECT u.username, COALESCE(SUM(ga.points_awarded), 0) AS total_points " +
                "FROM game_session_players gsp JOIN users u ON u.id = gsp.user_id " +
                "LEFT JOIN game_answers ga ON ga.user_id = gsp.user_id AND ga.game_session_id = gsp.game_session_id " +
                "WHERE gsp.game_session_id = ? GROUP BY u.username ORDER BY total_points DESC";
        jdbcPool.preparedQuery(sql).execute(Tuple.of(sessionId), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture(ar.cause()));
                return;
            }
            try {
                JsonArray totals = new JsonArray();
                for (Row row : ar.result()) {
                    Number totalPointsValue = (Number) row.getValue("total_points");
                    totals.add(new JsonObject()
                            .put("username", row.getString("username"))
                            .put("totalPoints", totalPointsValue != null ? totalPointsValue.doubleValue() : 0.0));
                }
                resultHandler.handle(Future.succeededFuture(totals));
            } catch (Exception e) {
                resultHandler.handle(Future.failedFuture(e));
            }
        });
    }
}
