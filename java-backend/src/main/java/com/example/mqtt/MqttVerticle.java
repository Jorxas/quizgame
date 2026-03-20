package com.example.mqtt;

/**
 * MQTT-Verticle – Verbindung zum Broker, startet MqttController.
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MqttVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MqttVerticle.class);

    @Override
    public void start() {

        String mqttBrokerUrl = System.getenv("MQTT_BROKER_URL") != null ? System.getenv("MQTT_BROKER_URL") : "ws://iti-mqtt.mni.thm.de:9001";
        int mqttBrokerPort = System.getenv("MQTT_BROKER_PORT") != null ? Integer.parseInt(System.getenv("MQTT_BROKER_PORT")) : 1883;
        String mqttUsername = System.getenv("MQTT_USERNAME") != null ? System.getenv("MQTT_USERNAME") : "your_mqtt_username";
        String mqttPassword = System.getenv("MQTT_PASSWORD") != null ? System.getenv("MQTT_PASSWORD") : "your_mqtt_password";

        MqttClientOptions options = new MqttClientOptions()
                .setAutoKeepAlive(true)
                .setUsername(mqttUsername)
                .setPassword(mqttPassword);

        MqttClient mqttClient = MqttClient.create(vertx, options);

        mqttClient.connect(mqttBrokerPort, mqttBrokerUrl, ar -> {
            if (ar.succeeded()) {
                logger.info("Connected to MQTT broker successfully!");

                MqttController mqttController = new MqttController(mqttClient, vertx);
                mqttController.registerEventBusConsumers();
                mqttController.registerMqttConsumers();

            } else {
                logger.error("Failed to connect to MQTT: {}", ar.cause().getMessage());
            }
        }
        );

    }
}
