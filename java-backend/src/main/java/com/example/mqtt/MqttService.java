package com.example.mqtt;

/**
 * MQTT-Service – Publish von game/state, countdown, question, evaluation, lobby/status.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MqttService {

    private final MqttClient mqttClient;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "group-16/";

    public MqttService(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    /** Sendet eine Demo-Nachricht an demo/message. */
    public void publishDemoMessage(String message) {
        JsonObject data = new JsonObject().put("message", message);
        mqttClient.publish(mqttMessagePrefix + "demo/message", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published demo message: {}", data);
    }

    /** Sendet Controller-Status (playerId, ready) an controller/{controllerId}/status. */
    public void publishControllerStatus(String controllerId, String playerId, boolean ready) {
        String topic = mqttMessagePrefix + "controller/" + controllerId + "/status";
        JsonObject data = new JsonObject()
                .put("playerId", playerId != null ? playerId : "")
                .put("ready", ready)
                .put("ts", System.currentTimeMillis());
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT controller/{}/status: playerId={}, ready={}", controllerId, playerId, ready);
    }

    /** Sendet Heartbeat-Ping an Controller (controller/{controllerId}/ping). */
    public void publishPing(String controllerId) {
        String topic = mqttMessagePrefix + "controller/" + controllerId + "/ping";
        JsonObject data = new JsonObject().put("action", "ping").put("ts", System.currentTimeMillis());
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT ping: {}", topic);
    }

    /** Sendet Ready-Status eines Spielers an player/{playerId}/status. */
    public void publishPlayerStatus(String playerId, boolean ready) {
        String topic = mqttMessagePrefix + "player/" + playerId + "/status";
        JsonObject data = new JsonObject()
                .put("playerId", playerId)
                .put("status", ready ? "READY" : "NOT_READY")
                .put("ready", ready)
                .put("ts", System.currentTimeMillis());
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT player status: {} -> {}", topic, ready ? "READY" : "NOT_READY");
    }

    /** Sendet Spielzustand an game/state. */
    public void publishGameState(String state) {
        String topic = mqttMessagePrefix + "game/state";
        JsonObject data = new JsonObject().put("state", state).put("ts", System.currentTimeMillis());
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT game/state: {}", state);
    }

    /** Sendet Countdown-Tick an game/countdown. */
    public void publishCountdown(int tick) {
        String topic = mqttMessagePrefix + "game/countdown";
        JsonObject data = new JsonObject().put("tick", tick).put("ts", System.currentTimeMillis());
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT game/countdown: {}", tick);
    }

    /** Sendet die aktuelle Frage an game/question. */
    public void publishQuestion(JsonObject questionData) {
        String topic = mqttMessagePrefix + "game/question";
        mqttClient.publish(topic, questionData.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT game/question: id={}", questionData.getLong("questionId"));
    }

    /** Sendet das Spieler-Ergebnis an player/{playerId}/result. */
    public void publishPlayerResult(String playerId, JsonObject result) {
        String topic = mqttMessagePrefix + "player/" + playerId + "/result";
        mqttClient.publish(topic, result.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT player/{}/result: correct={}", playerId, result.getBoolean("correct"));
    }

    /** Sendet die Auswertung an game/evaluation. */
    public void publishEvaluation(JsonObject evaluationData) {
        String topic = mqttMessagePrefix + "game/evaluation";
        mqttClient.publish(topic, evaluationData.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT game/evaluation published");
    }

    /** Sendet Spielende an game/ended. */
    public void publishGameEnded(JsonObject finalData) {
        String topic = mqttMessagePrefix + "game/ended";
        mqttClient.publish(topic, finalData.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT game/ended published");
    }

    /** Sendet Lobby-Status (Spielerliste) an game/lobby/status. */
    public void publishLobbyStatus(JsonArray players) {
        String topic = mqttMessagePrefix + "game/lobby/status";
        JsonObject data = new JsonObject().put("players", players).put("ts", System.currentTimeMillis());
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT game/lobby/status: {} players", players.size());
    }

    /** Sendet RFID-Lookup-Antwort an controller/{mac}/rfid/reply (username null = nicht gefunden). */
    public void publishRfidReply(String controllerMac, String username) {
        String topic = mqttMessagePrefix + "controller/" + controllerMac + "/rfid/reply";
        JsonObject data = new JsonObject().put("username", username);
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("MQTT RFID reply to {}: {}", controllerMac, username != null ? username : "not found");
    }
}
