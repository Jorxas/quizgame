let mqttClient = null;

function connectMQTT() {
  const brokerURL = document.getElementById('mqtt-broker').value;
  const username = document.getElementById('mqtt-username').value;
  const password = document.getElementById('mqtt-password').value;

  const options = {};
  
  if (username) options.username = username;
  if (password) options.password = password;

  mqttClient = mqtt.connect(brokerURL, options);

  mqttClient.on('connect', function () {
    logMessage('Connected to MQTT Broker.');
    mqttClient.subscribe('#', function (err) {
      if (!err) logMessage('Subscribed to all topics (#).');
      else logMessage(`Subscription error: ${err}`);
    });
  });

  mqttClient.on('message', function (topic, message) {
    logMessage(`${topic}: ${message.toString()}`);
  });

  mqttClient.on('error', function (error) {
    logMessage(`Connection Error: ${error}`);
  });

  mqttClient.on('close', function () {
    logMessage('Disconnected from MQTT Broker.');
  });
}

function disconnectMQTT() {
  if (mqttClient) {
    mqttClient.end();
  }
}

function logMessage(msg) {
  const messagesBox = document.getElementById('mqtt-messages-box');
  const timestamp = new Date().toLocaleTimeString();
  const newMessage = document.createElement('div');
  newMessage.textContent = `[${timestamp}] ${msg}`;
  messagesBox.appendChild(newMessage);
  messagesBox.scrollTop = messagesBox.scrollHeight;
}

// Event Listeners
document.getElementById('mqtt-connect-btn').addEventListener('click', function () {
  if (mqttClient && mqttClient.connected) {
    alert('Already connected.');
  } else {
    connectMQTT();
  }
});

document.getElementById('mqtt-disconnect-btn').addEventListener('click', function () {
  disconnectMQTT();
});

document.getElementById('mqtt-publish-btn').addEventListener('click', function () {
  if (!mqttClient || !mqttClient.connected) {
    alert('Please connect to an MQTT Broker first!');
    return;
  }

  const topic = document.getElementById('mqtt-topic').value;
  const message = document.getElementById('mqtt-message').value;

  if (!topic.trim() || !message.trim()) {
    alert('Please fill out both Topic and Message.');
    return;
  }

  mqttClient.publish(topic, message);
  logMessage(`Published "${message}" to topic "${topic}".`);
});
