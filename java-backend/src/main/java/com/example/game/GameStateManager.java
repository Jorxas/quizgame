package com.example.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;

public class GameStateManager {

    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);
    private static final int QUESTION_DURATION_MS = 30_000;
    private static final int EVALUATION_DURATION_MS = 3_000;

    private static GameStateManager instance;
    private final Vertx vertx;
    private final EventBus eventBus;
    private final GameRepository gameRepository;

    private long sessionId;
    private final List<Long> questionIds = Collections.synchronizedList(new ArrayList<>());
    private final List<String> players = Collections.synchronizedList(new ArrayList<>());
    private int currentQuestionIndex = -1;
    private long currentQuestionId;
    private long questionStartTime;
    private final Set<String> answeredPlayers = Collections.synchronizedSet(new HashSet<>());
    private long questionTimerId = -1;
    private long countdownTimerId = -1;
    private boolean gameActive = false;
    private boolean questionEnded = false;
    private boolean inCountdown = false;

    private GameStateManager(Vertx vertx) {
        this.vertx = vertx;
        this.eventBus = vertx.eventBus();
        this.gameRepository = new GameRepository();
        registerAnswerListener();
        registerCancelCountdownListener();
    }

    private void registerCancelCountdownListener() {
        eventBus.consumer("game.cancel_countdown", msg -> {
            if (inCountdown) cancelCountdownToLobby();
        });
    }

    private void cancelCountdownToLobby() {
        if (!inCountdown) return;
        inCountdown = false;
        if (countdownTimerId != -1) {
            vertx.cancelTimer(countdownTimerId);
            countdownTimerId = -1;
        }
        gameActive = false;
        gameRepository.updateSessionState(sessionId, "LOBBY", ar -> {
            if (ar.failed()) logger.warn("Failed to reset session to LOBBY: {}", ar.cause().getMessage());
            eventBus.publish("game.state", "LOBBY");
            logger.info("Countdown cancelled, back to LOBBY");
        });
    }

    public static synchronized GameStateManager getInstance(Vertx vertx) {
        if (instance == null) instance = new GameStateManager(vertx);
        return instance;
    }

    private void registerAnswerListener() {
        eventBus.consumer("game.answer", msg -> {
            if (!gameActive) return;
            try {
                JsonObject data = new JsonObject(msg.body().toString());
                String playerId = data.getString("playerId");
                long questionId = data.getLong("questionId", 0L);
                String selectedOption = data.getString("selectedOption");
                if (questionId != currentQuestionId || answeredPlayers.contains(playerId)) return;
                answeredPlayers.add(playerId);
                int responseTimeMs = (int) (System.currentTimeMillis() - questionStartTime);
                gameRepository.saveAnswer(sessionId, playerId, questionId, selectedOption, responseTimeMs, ar -> {
                    if (ar.succeeded()) {
                        JsonObject result = ar.result();
                        JsonObject playerResult = new JsonObject()
                                .put("playerId", playerId)
                                .put("questionId", questionId)
                                .put("correct", result.getBoolean("is_correct"))
                                .put("points", result.getDouble("points_awarded"))
                                .put("correctOption", result.getString("correct_option"))
                                .put("selectedOption", selectedOption);
                        eventBus.publish("game.player.result", playerResult.encode());
                        logger.info("Answer from {}: correct={}, points={}", playerId, result.getBoolean("is_correct"), result.getDouble("points_awarded"));
                        gameRepository.countConnectedPlayersInSession(sessionId, countAr -> {
                            if (countAr.succeeded() && !questionEnded) {
                                int connectedCount = countAr.result();
                                if (answeredPlayers.size() >= connectedCount) {
                                    cancelQuestionTimer();
                                    endQuestion();
                                }
                            }
                        });
                    } else {
                        logger.warn("Failed to save answer for {}: {}", playerId, ar.cause().getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("Error processing answer: {}", e.getMessage());
            }
        });
    }

    public void startGame(long sessionId) {
        this.sessionId = sessionId;
        this.currentQuestionIndex = -1;
        this.gameActive = true;
        this.questionIds.clear();
        this.players.clear();
        this.answeredPlayers.clear();
        gameRepository.fetchSessionPlayers(sessionId, ar -> {
            if (ar.failed()) {
                logger.error("Failed to fetch session players: {}", ar.cause().getMessage());
                return;
            }
            players.addAll(ar.result());
            logger.info("Game starting for session {} with {} players", sessionId, players.size());
            startCountdown();
        });
    }

    private void startCountdown() {
        inCountdown = true;
        eventBus.publish("game.state", "COUNTDOWN");
        doCountdownTick(3);
    }

    private void doCountdownTick(int tick) {
        if (!inCountdown) return;
        if (tick <= 0) {
            inCountdown = false;
            countdownTimerId = -1;
            fetchQuestionsAndStart();
            return;
        }
        eventBus.publish("game.countdown", String.valueOf(tick));
        countdownTimerId = vertx.setTimer(1000, id -> doCountdownTick(tick - 1));
    }

    private void fetchQuestionsAndStart() {
        com.example.database.DatabaseClient.getInstance()
                .preparedQuery("SELECT round_length FROM game_sessions WHERE id = ?")
                .execute(Tuple.of(sessionId), ar -> {
                    if (ar.succeeded() && ar.result().iterator().hasNext()) {
                        String rl = ar.result().iterator().next().getString("round_length");
                        if ("Q10".equals(rl)) fetchQuestions(10);
                        else if ("Q20".equals(rl)) fetchQuestions(20);
                        else fetchQuestions(5);
                    } else {
                        fetchQuestions(5);
                    }
                });
    }

    private void fetchQuestions(int count) {
        gameRepository.fetchRandomQuestions(sessionId, count, ar -> {
            if (ar.failed()) {
                logger.error("Failed to fetch questions: {}", ar.cause().getMessage());
                gameActive = false;
                return;
            }
            questionIds.addAll(ar.result());
            logger.info("Fetched {} questions for session {}", questionIds.size(), sessionId);
            nextQuestion();
        });
    }

    private void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex >= questionIds.size()) {
            endGame();
            return;
        }
        currentQuestionId = questionIds.get(currentQuestionIndex);
        answeredPlayers.clear();
        questionEnded = false;

        eventBus.publish("game.pre_question_ping", new JsonObject().put("sessionId", sessionId));
        final MessageConsumer<String>[] consumerRef = new MessageConsumer[1];
        consumerRef[0] = eventBus.consumer("game.pre_question_ping.done", msg -> {
            String doneSessionId = (String) msg.body();
            consumerRef[0].unregister();
            if (Long.toString(sessionId).equals(doneSessionId) && gameActive) {
                doSendQuestion();
            }
        });
    }

    private void doSendQuestion() {
        gameRepository.updateSessionState(sessionId, "QUESTION", stateAr -> {
            gameRepository.fetchQuestionWithOptions(currentQuestionId, ar -> {
                if (ar.failed()) {
                    logger.error("Failed to fetch question {}: {}", currentQuestionId, ar.cause().getMessage());
                    nextQuestion();
                    return;
                }
                JsonObject questionData = ar.result();
                questionData.remove("correctOption");
                questionData.put("questionIndex", currentQuestionIndex + 1);
                questionData.put("totalQuestions", questionIds.size());
                questionData.put("duration", QUESTION_DURATION_MS / 1000);
                eventBus.publish("game.state", "QUESTION");
                eventBus.publish("game.question", questionData.encode());
                questionStartTime = System.currentTimeMillis();
                questionTimerId = vertx.setTimer(QUESTION_DURATION_MS, timerId -> endQuestion());
                logger.info("Question {}/{} sent: id={}", currentQuestionIndex + 1, questionIds.size(), currentQuestionId);
            });
        });
    }

    private void cancelQuestionTimer() {
        if (questionTimerId != -1) {
            vertx.cancelTimer(questionTimerId);
            questionTimerId = -1;
        }
    }

    private void endQuestion() {
        if (questionEnded) return;
        questionEnded = true;
        cancelQuestionTimer();
        gameRepository.updateSessionState(sessionId, "EVALUATION", stateAr -> {
            eventBus.publish("game.state", "EVALUATION");
            gameRepository.fetchRunningTotals(sessionId, totalsAr -> {
                JsonArray rankings = totalsAr.succeeded() ? totalsAr.result() : new JsonArray();
                gameRepository.fetchQuestionResults(sessionId, currentQuestionId, resultsAr -> {
                    JsonArray questionResults = resultsAr.succeeded() ? resultsAr.result() : new JsonArray();
                    gameRepository.fetchQuestionWithOptions(currentQuestionId, qAr -> {
                        String correctOption = qAr.succeeded() ? qAr.result().getString("correctOption", "") : "";
                        JsonObject evaluation = new JsonObject()
                                .put("questionIndex", currentQuestionIndex + 1)
                                .put("totalQuestions", questionIds.size())
                                .put("correctOption", correctOption)
                                .put("questionResults", questionResults)
                                .put("rankings", rankings);
                        eventBus.publish("game.evaluation", evaluation.encode());
                        logger.info("Evaluation published for question {}/{}", currentQuestionIndex + 1, questionIds.size());
                        vertx.setTimer(EVALUATION_DURATION_MS, timerId -> nextQuestion());
                    });
                });
            });
        });
    }

    private void endGame() {
        gameActive = false;
        gameRepository.computeSessionResults(sessionId, resultsAr -> {
            JsonArray finalRankings = resultsAr.succeeded() ? resultsAr.result() : new JsonArray();
            gameRepository.saveHighscores(sessionId, hsAr -> {
                if (hsAr.failed()) logger.warn("Failed to save highscores: {}", hsAr.cause().getMessage());
                JsonObject endData = new JsonObject().put("sessionId", sessionId).put("rankings", finalRankings);
                eventBus.publish("game.ended", endData.encode());
                logger.info("Game ended for session {} (results saved)", sessionId);
                gameRepository.resetSessionToLobbyForReplay(sessionId, replayAr -> {
                    if (replayAr.succeeded()) {
                        eventBus.publish("game.state", "LOBBY");
                        eventBus.publish("game.replay", replayAr.result());
                        eventBus.publish("lobby.updated", "");
                        logger.info("Session {} reset to LOBBY for replay, {} players", sessionId, replayAr.result().size());
                    } else {
                        logger.warn("Failed to reset session for replay: {}", replayAr.cause().getMessage());
                    }
                });
            });
        });
    }
}
