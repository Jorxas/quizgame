/**
 * Web-Controller — Controller-ID aus URL (?id=WEB-xxx).
 * MQTT-Topics: register, ping/pong, ready, game events, answers.
 */

(function () {
  var params = new URLSearchParams(window.location.search);
  var controllerId = params.get("id") || "";
  var controllerIdDisplay = document.getElementById("controllerIdDisplay");
  var controllerPlayerDisplay = document.getElementById("controllerPlayerDisplay");
  var controllerStatusText = document.getElementById("controllerStatus");
  var controllerStatusBox = controllerStatusText ? controllerStatusText.closest(".player-status") : null;
  var readyButton = document.getElementById("answerB");
  var notReadyButton = document.getElementById("answerD");
  var controllerAnswers = document.getElementById("controllerAnswers");
  var countdownOverlay = document.getElementById("countdownOverlay");
  var countdownNumber = document.getElementById("countdownNumber");
  var scoreValue = document.getElementById("scoreValue");
  var resultFeedback = document.getElementById("resultFeedback");
  var resultIcon = document.getElementById("resultIcon");
  var resultText = document.getElementById("resultText");
  var hintBox = document.getElementById("hintBox");

  var currentPlayerId = "";
  var currentReady = false;
  var client = null;
  var prefix = "group-16/";
  var subscribedStatusTopic = "";
  var subscribedResultTopic = "";
  var gameState = "LOBBY";
  var currentQuestionId = 0;
  var totalScore = 0;

  /** Initialisiert den Controller mit gegebener ID und startet die MQTT-Verbindung. */
  function initController(id) {
    controllerId = id;
    if (controllerIdDisplay) controllerIdDisplay.textContent = controllerId || "—";
    connectMqtt();
  }

  /** Aktualisiert die Ready/Not-ready-Anzeige in der UI. */
  function setReadyUi(ready) {
    currentReady = !!ready;
    if (controllerStatusText) controllerStatusText.textContent = currentReady ? "Ready" : "Not ready";
    if (controllerStatusBox) {
      controllerStatusBox.classList.toggle("ready", currentReady);
      controllerStatusBox.classList.toggle("not-ready", !currentReady);
    }
    if (readyButton) readyButton.classList.toggle("is-selected", currentReady);
    if (notReadyButton) notReadyButton.classList.toggle("is-selected", !currentReady);
  }

  /** Zeigt den verbundenen Spieler und dessen Bereitschaft an. */
  function setPlayerUi(playerId, ready) {
    currentPlayerId = playerId || "";
    if (controllerPlayerDisplay) controllerPlayerDisplay.textContent = currentPlayerId || "Nicht verbunden";
    setReadyUi(ready);
    subscribePlayerTopics();
  }

  function subscribePlayerTopics() {
    if (!client || !currentPlayerId) return;

    var statusTopic = prefix + "player/" + currentPlayerId + "/status";
    if (subscribedStatusTopic !== statusTopic) {
      if (subscribedStatusTopic) client.unsubscribe(subscribedStatusTopic);
      client.subscribe(statusTopic, { qos: 0 });
      subscribedStatusTopic = statusTopic;
    }

    var resultTopic = prefix + "player/" + currentPlayerId + "/result";
    if (subscribedResultTopic !== resultTopic) {
      if (subscribedResultTopic) client.unsubscribe(subscribedResultTopic);
      client.subscribe(resultTopic, { qos: 0 });
      subscribedResultTopic = resultTopic;
    }
  }

  function publishReadyState(ready) {
    if (!client || !currentPlayerId) return;
    var topic = prefix + "player/" + currentPlayerId + "/ready";
    client.publish(topic, JSON.stringify({ action: ready ? "ready" : "not-ready", ready: ready, playerId: currentPlayerId, controllerId: controllerId, ts: Date.now() }), { qos: 0 });
  }

  /** Sendet die gewählte Antwort (A/B/C/D) per MQTT. */
  function publishAnswer(option) {
    if (!client || !currentPlayerId || !currentQuestionId) return;
    var topic = prefix + "player/" + currentPlayerId + "/answer";
    client.publish(topic, JSON.stringify({ questionId: currentQuestionId, selectedOption: option, playerId: currentPlayerId, controllerId: controllerId, ts: Date.now() }), { qos: 0 });
    console.log("Answer published:", option, "for question", currentQuestionId);
  }

  function showCountdown(tick) {
    if (countdownOverlay) { countdownOverlay.style.display = "flex"; }
    if (countdownNumber) countdownNumber.textContent = tick;
    if (resultFeedback) resultFeedback.style.display = "none";
    if (controllerAnswers) controllerAnswers.style.display = "none";
  }

  function showQuestion(data) {
    if (countdownOverlay) countdownOverlay.style.display = "none";
    if (resultFeedback) resultFeedback.style.display = "none";

    currentQuestionId = data.questionId;

    var btnA = document.getElementById("answerA");
    var btnB = document.getElementById("answerB");
    var btnC = document.getElementById("answerC");
    var btnD = document.getElementById("answerD");
    if (btnA) btnA.textContent = "A";
    if (btnB) btnB.textContent = "B";
    if (btnC) btnC.textContent = "C";
    if (btnD) btnD.textContent = "D";

    if (controllerAnswers) {
      controllerAnswers.style.display = "grid";
      controllerAnswers.classList.remove("is-locked");
      controllerAnswers.querySelectorAll(".answer-btn").forEach(function (btn) {
        btn.classList.remove("is-selected");
      });
    }
    if (hintBox) hintBox.textContent = "Wähle deine Antwort!";
  }

  function showResult(data) {
    if (resultFeedback) {
      resultFeedback.style.display = "flex";
      resultFeedback.className = "result-feedback " + (data.correct ? "is-correct" : "is-wrong");
    }
    if (resultIcon) resultIcon.textContent = data.correct ? "✓" : "✗";
    if (resultText) {
      resultText.textContent = data.correct
        ? "Richtig! +" + data.points.toFixed(1) + " Punkte"
        : "Falsch! Richtig: " + data.correctOption;
    }
    if (data.correct) totalScore += data.points;
    if (scoreValue) scoreValue.textContent = totalScore.toFixed(1);
  }

  function showEvaluation() {
    if (hintBox) hintBox.textContent = "Auswertung läuft...";
    if (controllerAnswers) controllerAnswers.classList.add("is-locked");
  }

  function showEnded(data) {
    gameState = "LOBBY";
    currentQuestionId = 0;
    if (countdownOverlay) countdownOverlay.style.display = "none";
    if (resultFeedback) resultFeedback.style.display = "none";
    if (hintBox) hintBox.textContent = "Spiel beendet! Score: " + totalScore.toFixed(1);

    var btnA = document.getElementById("answerA");
    var btnB = document.getElementById("answerB");
    var btnC = document.getElementById("answerC");
    var btnD = document.getElementById("answerD");
    if (btnA) btnA.textContent = "A";
    if (btnB) btnB.textContent = "B (ready)";
    if (btnC) btnC.textContent = "C";
    if (btnD) btnD.textContent = "D (not ready)";

    if (controllerAnswers) {
      controllerAnswers.style.display = "grid";
      controllerAnswers.classList.remove("is-locked");
    }
    totalScore = 0;
    if (scoreValue) scoreValue.textContent = "0";
  }

  // ── MQTT Setup ──
  function connectMqtt() {
    if (!controllerId || typeof mqtt === "undefined" || !window.__ENV__) return;
    var env = window.__ENV__;
    prefix = (env.MQTT_MESSAGE_PREFIX || "group-16/").replace(/\/?$/, "/");
    var host = env.MQTT_BROKER_URL || window.location.hostname;
    if (host === "mosquitto") host = "localhost";
    if (!host) host = "localhost";
    var port = env.MQTT_BROKER_PORT || "9001";
    var wsUrl = "ws://" + host + ":" + port;

    client = mqtt.connect(wsUrl, {
      username: env.MQTT_USERNAME || "",
      password: env.MQTT_PASSWORD || "",
      reconnectPeriod: 5000
    });

    client.on("connect", function () {
      console.log("MQTT connected:", wsUrl);
      var topicRegister = prefix + "controller/" + controllerId + "/register";
      client.publish(topicRegister, JSON.stringify({ action: "register", controllerId: controllerId }), { qos: 0 });
      client.subscribe(prefix + "controller/" + controllerId + "/ping", { qos: 0 });
      client.subscribe(prefix + "controller/" + controllerId + "/status", { qos: 0 });
      setInterval(function () {
        if (client && client.connected) {
          client.publish(prefix + "controller/" + controllerId + "/request-status", JSON.stringify({}), { qos: 0 });
        }
      }, 5000);
      client.subscribe(prefix + "game/state", { qos: 0 });
      client.subscribe(prefix + "game/countdown", { qos: 0 });
      client.subscribe(prefix + "game/question", { qos: 0 });
      client.subscribe(prefix + "game/evaluation", { qos: 0 });
      client.subscribe(prefix + "game/ended", { qos: 0 });
      client.publish(prefix + "controller/" + controllerId + "/request-status", JSON.stringify({}), { qos: 0 });
    });

    client.on("message", function (topic, message) {
      var msg = message ? message.toString() : "";

      if (topic.endsWith("/ping")) {
        var pongTopic = prefix + "controller/" + controllerId + "/pong";
        client.publish(pongTopic, JSON.stringify({ action: "pong", controllerId: controllerId, ts: Date.now() }), { qos: 0 });
        return;
      }

      if (topic === prefix + "controller/" + controllerId + "/status") {
        try {
          var sd = JSON.parse(msg);
          setPlayerUi(sd.playerId || "", !!sd.ready);
        } catch (e) {}
        return;
      }

      if (subscribedStatusTopic && topic === subscribedStatusTopic) {
        try {
          var sd = JSON.parse(msg);
          setReadyUi(typeof sd.ready === "boolean" ? sd.ready : sd.status === "READY");
        } catch (e) {}
        return;
      }

      if (subscribedResultTopic && topic === subscribedResultTopic) {
        try { showResult(JSON.parse(msg)); } catch (e) {}
        return;
      }

      try { var data = JSON.parse(msg); } catch (e) { return; }

      if (topic === prefix + "game/state") {
        if (data.state === "COUNTDOWN") {
          gameState = "LOBBY";
        } else {
          gameState = data.state;
          if (data.state === "EVALUATION") showEvaluation();
          else if (data.state === "ENDED") showEnded(data);
        }
      } else if (topic === prefix + "game/countdown") {
        /* Controller zeigt keinen Countdown – nur die Hauptseite */
      } else if (topic === prefix + "game/question") {
        showQuestion(data);
      } else if (topic === prefix + "game/ended") {
        showEnded(data);
      }
    });

    client.on("error", function (e) { console.warn("MQTT error:", e); });
  }

  function generateControllerId() {
    var hex = "0123456789abcdef";
    var s = "WEB-";
    if (typeof crypto !== "undefined" && crypto.getRandomValues) {
      var arr = new Uint8Array(6);
      crypto.getRandomValues(arr);
      for (var i = 0; i < 6; i++) s += hex[arr[i] >> 4] + hex[arr[i] & 15];
    } else {
      for (var j = 0; j < 12; j++) s += hex[Math.floor(Math.random() * 16)];
    }
    return s;
  }

  // ── Generate ID locally if none in URL, then connect MQTT (no HTTP) ──
  if (controllerId) {
    initController(controllerId);
  } else {
    var newId = generateControllerId();
    history.replaceState(null, "", "controller.html?id=" + encodeURIComponent(newId));
    initController(newId);
  }

  // ── Answer Handlers ──
  function attachAnswerHandlers(container) {
    if (!container) return;
    container.querySelectorAll(".answer-btn").forEach(function (btn) {
      btn.addEventListener("click", function () {
        if (container.classList.contains("is-locked")) return;

        if (gameState === "QUESTION") {
          var option = "";
          if (btn.id === "answerA") option = "A";
          else if (btn.id === "answerB") option = "B";
          else if (btn.id === "answerC") option = "C";
          else if (btn.id === "answerD") option = "D";
          if (!option) return;

          container.querySelectorAll(".answer-btn").forEach(function (b) { b.classList.remove("is-selected"); });
          btn.classList.add("is-selected");
          container.classList.add("is-locked");
          publishAnswer(option);
        } else if (gameState === "LOBBY") {
          if (btn.id === "answerB") publishReadyState(true);
          else if (btn.id === "answerD") publishReadyState(false);
        }
      });
    });
  }

  setReadyUi(false);
  attachAnswerHandlers(controllerAnswers);

  /** Bei Schließen der Seite: Controller sofort als getrennt melden (sendBeacon ist zuverlässig bei unload). */
  function getApiBase() {
    var env = window.__ENV__ || {};
    if (env.API_BASE_URL) return env.API_BASE_URL.replace(/\/$/, "");
    if (window.location.port === "81") {
      return window.location.protocol + "//" + window.location.hostname + ":80";
    }
    return window.location.origin;
  }

  function notifyDisconnect() {
    if (!controllerId) return;
    var url = getApiBase() + "/api/controllers/disconnect";
    var body = JSON.stringify({ controllerId: controllerId });
    if (navigator.sendBeacon) {
      navigator.sendBeacon(url, new Blob([body], { type: "application/json" }));
    } else {
      var xhr = new XMLHttpRequest();
      xhr.open("POST", url, false);
      xhr.setRequestHeader("Content-Type", "application/json");
      try { xhr.send(body); } catch (e) {}
    }
  }

  window.addEventListener("pagehide", notifyDisconnect);
  window.addEventListener("beforeunload", notifyDisconnect);
})();
