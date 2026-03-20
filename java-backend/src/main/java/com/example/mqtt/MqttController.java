package com.example.mqtt;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.auth.AuthService;
import com.example.controllers.ControllersRepository;
import com.example.game.GameRepository;
import com.example.lobby.LobbyRepository;
import com.example.lobby.LobbyService;
import com.example.player.PlayerService;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MqttController {

    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);
    private static final int PING_INTERVAL_MS = 10_000;
    private static final int MISSED_PINGS_DISCONNECT = 2;
    private static final int PRE_QUESTION_PING_TIMEOUT_MS = 3_000;

    private final MqttService mqttService;
    private final MqttClient mqttClient;
    private final EventBus eventBus;
    private final Vertx vertx;
    private final ControllersRepository controllersRepository;
    private final LobbyRepository lobbyRepository;
    private final GameRepository gameRepository;
    private final AuthService authService;
    private final PlayerService playerService;
    private final LobbyService lobbyService;
    private final Map<String, Integer> controllerMissedPings = new ConcurrentHashMap<>();
    private final Set<String> preQuestionWaitingControllers = ConcurrentHashMap.newKeySet();

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "group-16/";
    private static final String CONTROLLER_REGISTER_TOPIC_PREFIX = "controller/";
    private static final String CONTROLLER_REGISTER_TOPIC_SUFFIX = "/register";
    private static final String CONTROLLER_PONG_TOPIC_SUFFIX = "/pong";
    private static final String CONTROLLER_REQUEST_STATUS_TOPIC_SUFFIX = "/request-status";
    private static final String PLAYER_TOPIC_PREFIX = "player/";
    private static final String PLAYER_READY_TOPIC_SUFFIX = "/ready";
    private static final String PLAYER_ANSWER_TOPIC_SUFFIX = "/answer";

    public MqttController(MqttClient mqttClient, Vertx vertx) {
        this.mqttClient = mqttClient;
        this.vertx = vertx;
        this.mqttService = new MqttService(mqttClient);
        this.eventBus = vertx.eventBus();
        this.controllersRepository = new ControllersRepository();
        this.lobbyRepository = new LobbyRepository();
        this.gameRepository = new GameRepository();
        this.authService = new AuthService();
        this.playerService = new PlayerService();
        this.lobbyService = new LobbyService();
    }

    /* EVENTS empfangen und verarbeiten */
    public void registerEventBusConsumers() {
        this.eventBus.consumer("game.start", msg -> {
            logger.info("Message received via EventBus: 'game.start'");
            mqttService.publishGameStart();
        });

        this.eventBus.consumer("game.stop", msg -> {
            logger.info("Message received via EventBus: 'game.stop'");
            mqttService.publishGameStop();
        });

        this.eventBus.consumer("object.created", msg -> {
            logger.info("Message received via EventBus: 'object.created'");
            mqttService.publishObjectCreated(msg.body().toString());
        });
        this.eventBus.consumer("mqtt.demo.message", msg -> {
            logger.info("Message received via EventBus: 'mqtt.message'");
            mqttService.publishDemoMessage(msg.body().toString());
        });

        this.eventBus.consumer("game.state", msg -> {
            mqttService.publishGameState(msg.body().toString());
        });

        this.eventBus.consumer("game.countdown", msg -> {
            mqttService.publishCountdown(Integer.parseInt(msg.body().toString()));
        });

        this.eventBus.consumer("game.question", msg -> {
            mqttService.publishQuestion(new io.vertx.core.json.JsonObject(msg.body().toString()));
        });

        this.eventBus.consumer("game.player.result", msg -> {
            io.vertx.core.json.JsonObject data = new io.vertx.core.json.JsonObject(msg.body().toString());
            mqttService.publishPlayerResult(data.getString("playerId"), data);
        });

        this.eventBus.consumer("game.evaluation", msg -> {
            mqttService.publishEvaluation(new io.vertx.core.json.JsonObject(msg.body().toString()));
        });

        this.eventBus.consumer("game.ended", msg -> {
            mqttService.publishGameEnded(new io.vertx.core.json.JsonObject(msg.body().toString()));
        });

        this.eventBus.consumer("controller.bound", msg -> {
            JsonObject data = (JsonObject) msg.body();
            String controllerId = data.getString("controllerId");
            String playerId = data.getString("playerId");
            boolean ready = Boolean.TRUE.equals(data.getBoolean("ready"));
            if (controllerId != null && !controllerId.isBlank()) {
                mqttService.publishControllerStatus(controllerId, playerId, ready);
                logger.info("Controller bound: {} -> player {} (ready={})", controllerId, playerId, ready);
            }
        });

        this.eventBus.consumer("lobby.updated", msg -> {
            lobbyRepository.fetchPlayersWithStatus(ar -> {
                if (ar.succeeded()) {
                    mqttService.publishLobbyStatus(ar.result());
                }
            });
        });

        this.eventBus.consumer("game.pre_question_ping", msg -> {
            JsonObject body = msg.body() instanceof JsonObject ? (JsonObject) msg.body() : new JsonObject(msg.body().toString());
            long sessionId = body.getLong("sessionId", 0L);
            preQuestionWaitingControllers.clear();
            gameRepository.fetchControllerIdsForSession(sessionId, ar -> {
                if (ar.failed()) {
                    logger.warn("Pre-question-ping: failed to fetch controllers: {}", ar.cause().getMessage());
                    eventBus.publish("game.pre_question_ping.done", String.valueOf(sessionId));
                    return;
                }
                java.util.List<String> controllerIds = ar.result();
                if (controllerIds.isEmpty()) {
                    eventBus.publish("game.pre_question_ping.done", String.valueOf(sessionId));
                    return;
                }
                preQuestionWaitingControllers.addAll(controllerIds);
                for (String cid : controllerIds) {
                    mqttService.publishPing(cid);
                }
                logger.info("Pre-question-ping: sent to {} controllers, waiting 3s", controllerIds.size());
                vertx.setTimer(PRE_QUESTION_PING_TIMEOUT_MS, timerId -> {
                    Set<String> noResponse = new java.util.HashSet<>(preQuestionWaitingControllers);
                    preQuestionWaitingControllers.clear();
                    for (String cid : noResponse) {
                        controllersRepository.updateStatus(cid, "OFFLINE", statusAr -> {
                            if (statusAr.succeeded()) {
                                logger.info("Pre-question-ping: controller {} no response, marked OFFLINE", cid);
                            }
                        });
                    }
                    if (!noResponse.isEmpty()) {
                        eventBus.publish("lobby.updated", "");
                    }
                    eventBus.publish("game.pre_question_ping.done", String.valueOf(sessionId));
                });
            });
        });

        this.eventBus.consumer("game.replay", msg -> {
            io.vertx.core.json.JsonArray usernames = (io.vertx.core.json.JsonArray) msg.body();
            if (usernames != null) {
                for (int i = 0; i < usernames.size(); i++) {
                    String playerId = usernames.getString(i);
                    if (playerId != null && !playerId.isBlank()) {
                        mqttService.publishPlayerStatus(playerId, false);
                    }
                }
                logger.info("Replay: published ready=false to {} players", usernames.size());
            }
        });
    }

    public void registerMqttConsumers() {

        mqttClient.publishHandler(message -> {

            Buffer payload = message.payload();
            String topic = message.topicName();

            logger.info("Message received via Mqtt. Topic: {}, Payload: {}", topic, payload);

            if (topic.equals(mqttMessagePrefix + "demo/hello_world")) {
                this.eventBus.publish("mqtt.demo.message", payload);
            } else if (topic.equals(mqttMessagePrefix + "output")) {
                logger.info("Output message received: {}", payload.toString());
            } else if (topic.startsWith(mqttMessagePrefix + CONTROLLER_REGISTER_TOPIC_PREFIX) && topic.endsWith(CONTROLLER_REGISTER_TOPIC_SUFFIX)) {
                String controllerId = topic.substring(
                        (mqttMessagePrefix + CONTROLLER_REGISTER_TOPIC_PREFIX).length(),
                        topic.length() - CONTROLLER_REGISTER_TOPIC_SUFFIX.length()
                );
                if (!controllerId.isEmpty()) {
                    String controllerType = "WEB";
                    try {
                        JsonObject regPayload = new JsonObject(payload.toString());
                        String ct = regPayload.getString("controllerType");
                        if (ct != null && !ct.isBlank()) controllerType = ct;
                    } catch (Exception ignored) { }
                    controllerMissedPings.put(controllerId, 0);
                    controllersRepository.createControllerIfNotExists(controllerId, controllerType, ar -> {
                        if (ar.succeeded()) {
                            logger.info("Controller register: {} (insert or update, ping started)", controllerId);
                        } else {
                            logger.warn("Controller register: {} — create/update failed: {}", controllerId, ar.cause().getMessage());
                        }
                    });
                }
            } else if (topic.startsWith(mqttMessagePrefix + CONTROLLER_REGISTER_TOPIC_PREFIX) && topic.endsWith(CONTROLLER_PONG_TOPIC_SUFFIX)) {
                String controllerId = topic.substring(
                        (mqttMessagePrefix + CONTROLLER_REGISTER_TOPIC_PREFIX).length(),
                        topic.length() - CONTROLLER_PONG_TOPIC_SUFFIX.length()
                );
                if (!controllerId.isEmpty()) {
                    controllerMissedPings.put(controllerId, 0);
                    preQuestionWaitingControllers.remove(controllerId);
                    controllersRepository.updateLastSeen(controllerId, ar -> {
                        if (ar.succeeded()) {
                            logger.debug("Controller pong: {} (last_seen_at updated)", controllerId);
                        }
                    });
                }
            } else if (topic.startsWith(mqttMessagePrefix + PLAYER_TOPIC_PREFIX) && topic.endsWith(PLAYER_READY_TOPIC_SUFFIX)) {
                String playerId = topic.substring(
                        (mqttMessagePrefix + PLAYER_TOPIC_PREFIX).length(),
                        topic.length() - PLAYER_READY_TOPIC_SUFFIX.length()
                );
                if (!playerId.isEmpty()) {
                    boolean ready = parseReadyPayload(payload);
                    if (!ready) {
                        eventBus.publish("game.cancel_countdown", "");
                    }
                    lobbyRepository.updatePlayerReady(playerId, ready, ar -> {
                        if (ar.succeeded()) {
                            mqttService.publishPlayerStatus(playerId, ready);
                            eventBus.publish("lobby.updated", "");
                            logger.info("Player {} ready updated to {}", playerId, ready);
                        } else {
                            logger.warn("Player ready update failed for {}: {}", playerId, ar.cause().getMessage());
                        }
                    });
                }
            } else if (topic.startsWith(mqttMessagePrefix + CONTROLLER_REGISTER_TOPIC_PREFIX) && topic.endsWith(CONTROLLER_REQUEST_STATUS_TOPIC_SUFFIX)) {
                String controllerId = topic.substring(
                        (mqttMessagePrefix + CONTROLLER_REGISTER_TOPIC_PREFIX).length(),
                        topic.length() - CONTROLLER_REQUEST_STATUS_TOPIC_SUFFIX.length()
                );
                if (!controllerId.isEmpty()) {
                    controllersRepository.fetchControllerPlayerStatus(controllerId, ar -> {
                        if (ar.succeeded()) {
                            JsonObject status = ar.result();
                            String playerId = status.getString("playerId");
                            boolean ready = Boolean.TRUE.equals(status.getBoolean("ready"));
                            mqttService.publishControllerStatus(controllerId, playerId != null ? playerId : "", ready);
                            logger.info("Controller {} request-status: playerId={}, ready={}", controllerId, playerId, ready);
                        }
                    });
                }
            } else if (topic.startsWith(mqttMessagePrefix + PLAYER_TOPIC_PREFIX) && topic.endsWith(PLAYER_ANSWER_TOPIC_SUFFIX)) {
                String playerId = topic.substring(
                        (mqttMessagePrefix + PLAYER_TOPIC_PREFIX).length(),
                        topic.length() - PLAYER_ANSWER_TOPIC_SUFFIX.length()
                );
                if (!playerId.isEmpty()) {
                    JsonObject answerData = new JsonObject(payload.toString());
                    answerData.put("playerId", playerId);
                    this.eventBus.publish("game.answer", answerData.encode());
                    logger.info("Player {} answer received via MQTT", playerId);
                }
            } else if (topic.equals(mqttMessagePrefix + "auth/rfid/lookup")) {
                handleRfidLookup(payload, topic);
            } else {
                logger.info("Handler for topic: {} not implemented", topic);
            }

        });

        mqttClient.subscribe(Map.of(
                mqttMessagePrefix + "demo/hello_world", 0,
                mqttMessagePrefix + "output", 0,
                mqttMessagePrefix + "auth/rfid/lookup", 0,
                mqttMessagePrefix + "controller/+/register", 0,
                mqttMessagePrefix + "controller/+/pong", 0,
                mqttMessagePrefix + "controller/+/request-status", 0,
                mqttMessagePrefix + "player/+/ready", 0,
                mqttMessagePrefix + "player/+/answer", 0
        ));
        logger.info("MQTT prefix: '{}', subscribed to controller/+/register, controller/+/pong and player/+/ready", mqttMessagePrefix);

        vertx.setPeriodic(PING_INTERVAL_MS, id -> {
            controllerMissedPings.forEach((controllerId, missed) -> {
                int next = missed + 1;
                if (next >= MISSED_PINGS_DISCONNECT) {
                    controllerMissedPings.remove(controllerId);
                    controllersRepository.updateStatus(controllerId, "OFFLINE", ar -> {
                        if (ar.succeeded()) {
                            logger.info("Controller {} disconnected ({} missed pings)", controllerId, MISSED_PINGS_DISCONNECT);
                            eventBus.publish("lobby.updated", "");
                        }
                    });
                } else {
                    controllerMissedPings.put(controllerId, next);
                    mqttService.publishPing(controllerId);
                }
            });
        });
    }

    private void handleRfidLookup(Buffer payload, String topic) {
        try {
            JsonObject body = new JsonObject(payload.toString());
            String uid = body.getString("uid");
            String mac = body.getString("mac");
            if (uid == null || uid.isBlank() || mac == null || mac.isBlank()) {
                mqttService.publishRfidReply(mac != null ? mac : "", null);
                return;
            }
            authService.lookupByRfid(uid.trim(), ar -> {
                if (ar.failed() || ar.result() == null) {
                    mqttService.publishRfidReply(mac, null);
                    return;
                }
                String username = ar.result();
                playerService.bindControllerToPlayer(username, mac, "HARDWARE", bindAr -> {
                    if (bindAr.succeeded()) {
                        eventBus.publish("controller.bound", new JsonObject()
                                .put("controllerId", mac)
                                .put("playerId", username)
                                .put("ready", false));
                    }
                    // Whether bind succeeded or failed (e.g. controller already assigned to this user
                    // after a page refresh), try to add to lobby and always reply username if we can.
                    lobbyService.addPlayerToLobby(username, joinAr -> {
                        if (joinAr.succeeded()) {
                            eventBus.publish("lobby.updated", "");
                        } else {
                            String msg = joinAr.cause() != null ? joinAr.cause().getMessage() : "";
                            if (msg != null && msg.contains("bereits in der Lobby")) {
                                // Re-scan: already in lobby, still success for hardware
                                logger.info("RFID re-scan: {} already in lobby", username);
                            } else {
                                logger.warn("RFID lobby join failed for {}: {}", username, msg);
                            }
                        }
                        mqttService.publishRfidReply(mac, username);
                    });
                });
            });
        } catch (Exception e) {
            logger.warn("RFID lookup parse error: {}", e.getMessage());
            mqttService.publishRfidReply("", null);
        }
    }

    private boolean parseReadyPayload(Buffer payload) {
        try {
            JsonObject json = new JsonObject(payload.toString());
            if (json.containsKey("ready")) {
                return Boolean.TRUE.equals(json.getBoolean("ready"));
            }

            String action = json.getString("action", "ready");
            return !("not-ready".equalsIgnoreCase(action)
                    || "not_ready".equalsIgnoreCase(action)
                    || "unready".equalsIgnoreCase(action)
                    || "false".equalsIgnoreCase(action));
        } catch (Exception e) {
            String raw = payload.toString().trim().toLowerCase();
            return "true".equals(raw) || "ready".equals(raw) || "1".equals(raw);
        }
    }
}
