package com.example.mqtt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.controllers.ControllersRepository;
import com.example.lobby.LobbyRepository;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MqttController {

    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);
    private static final int PING_INTERVAL_MS = 10_000;
    private static final int MISSED_PINGS_DISCONNECT = 2;

    private final MqttService mqttService;
    private final MqttClient mqttClient;
    private final EventBus eventBus;
    private final Vertx vertx;
    private final ControllersRepository controllersRepository;
    private final LobbyRepository lobbyRepository;
    private final Map<String, Integer> controllerMissedPings = new ConcurrentHashMap<>();

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null
            ? System.getenv("MQTT_MESSAGE_PREFIX")
            : "group-16/";
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
                    controllerMissedPings.put(controllerId, 0);
                    controllersRepository.createControllerIfNotExists(controllerId, ar -> {
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
            } else {
                logger.info("Handler for topic: {} not implemented", topic);
            }

        });

        mqttClient.subscribe(Map.of(
                mqttMessagePrefix + "demo/hello_world", 0,
                mqttMessagePrefix + "output", 0,
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
                        }
                    });
                } else {
                    controllerMissedPings.put(controllerId, next);
                    mqttService.publishPing(controllerId);
                }
            });
        });
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
