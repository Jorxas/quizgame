window.currentUsername = "";

// Erstellt eine neue Lobby-Session bei jedem Seitenaufruf.
fetch("/api/lobby/create", { method: "POST" }).catch(function () {});

function showInlineMessage(elementId, text, isSuccess) {
  var el = document.getElementById(elementId);
  if (!el) return;
  el.textContent = text || "";
  el.classList.remove("config-error", "config-success", "is-visible");
  if (text) {
    el.classList.add(isSuccess ? "config-success" : "config-error", "is-visible");
  }
}

/**
 * Mappt bekannte englische API-Fehlermeldungen auf deutsche Texte (Fallback für Netzwerk-/Alt-Backend).
 */
function mapApiErrorToGerman(result) {
  var t = (result && result.text) ? String(result.text).toLowerCase() : "";
  if (t.indexOf("invalid credentials") >= 0) return "Benutzername oder Passwort falsch.";
  if (t.indexOf("invalid request") >= 0 || t.indexOf("ungültige anfrage") >= 0) return "Ungültige Anfrage.";
  if (t.indexOf("username and password required") >= 0 || t.indexOf("benutzername und passwort erforderlich") >= 0) return "Benutzername und Passwort erforderlich.";
  if (t.indexOf("username already exists") >= 0 || t.indexOf("benutzername existiert bereits") >= 0) return "Benutzername existiert bereits.";
  if (t.indexOf("username required") >= 0 || t.indexOf("benutzername erforderlich") >= 0) return "Benutzername erforderlich.";
  if (t.indexOf("nicht alle spieler") >= 0 || t.indexOf("alle spieler sind bereit") >= 0) return "Nicht alle Spieler sind bereit.";
  if (t.indexOf("bereits in der session") >= 0) return "Du bist bereits in der Session angemeldet.";
  if (t.indexOf("rfid-karte bereits vergeben") >= 0) return "RFID-Karte bereits vergeben.";
  if (result && result.text) return result.text;
  return "Netzwerkfehler. Bitte erneut versuchen.";
}

var CATEGORY_MAP = {
  "programmierung": 1,
  "datenbanken": 2,
  "web-technologien": 3,
  "netzwerke": 4,
  "betriebssysteme": 5,
  "it-sicherheit": 6,
  "algorithmen": 7,
  "software-engineering": 8,
  "embedded-systems": 9,
  "linux-tools": 10
};

var DIFFICULTY_MAP = {
  "leicht": "EASY",
  "mittel": "MEDIUM",
  "schwer": "HARD"
};

function getWebControllerUrl(controllerId) {
  var base = window.location.protocol + "//" + window.location.hostname + ":81";
  if (!controllerId) return base + "/controller.html";
  return base + "/controller.html?id=" + encodeURIComponent(controllerId);
}

function readGameConfig() {
  var activePill = document.querySelector("#modePills .pill.is-active");
  var mode = activePill ? parseInt(activePill.getAttribute("data-count"), 10) : 5;
  var categories = [];
  var difficulties = [];

  document.querySelectorAll("#categoryCheckboxes input[name=category]:checked").forEach(function (checkbox) {
    var categoryId = CATEGORY_MAP[checkbox.value];
    if (categoryId) categories.push(categoryId);
  });

  document.querySelectorAll("#difficultyCheckboxes input[name=difficulty]:checked").forEach(function (checkbox) {
    var difficulty = DIFFICULTY_MAP[checkbox.value];
    if (difficulty) difficulties.push(difficulty);
  });

  return {
    mode: mode,
    categories: categories,
    difficulties: difficulties
  };
}

function loadUserRfid() {
  if (!window.currentUsername) return;
  fetch("/api/auth/rfid?username=" + encodeURIComponent(window.currentUsername), { cache: "no-store" })
    .then(function (r) { return r.json(); })
    .then(function (data) {
      var input = document.getElementById("rfid-card-input");
      if (input) input.value = (data && data.rfidUid) ? data.rfidUid : "";
    })
    .catch(function () {});
}

function saveUserRfid() {
  if (!window.currentUsername) {
    showInlineMessage("controllerError", "Bitte zuerst anmelden.", false);
    return;
  }
  var input = document.getElementById("rfid-card-input");
  if (!input) return;
  var rfidRaw = input.value.trim();
  var rfidUid = rfidRaw ? rfidRaw.replace(/\s+/g, "").toUpperCase() : "";

  showInlineMessage("controllerError", "", false);
  fetch("/api/auth/rfid", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: window.currentUsername, rfidUid: rfidUid })
  })
    .then(function (r) { return r.text().then(function (t) { return { ok: r.ok, text: t }; }); })
    .then(function (result) {
      if (result.ok) {
        showInlineMessage("controllerError", rfidUid ? "RFID-Karte gespeichert." : "RFID-Karte entfernt.", true);
      } else {
        showInlineMessage("controllerError", mapApiErrorToGerman(result), false);
      }
    })
    .catch(function () {
      showInlineMessage("controllerError", "Verbindungsfehler.", false);
    });
}

function loadAvailableControllers(preferredControllerId) {
  var list = document.getElementById("controllerList");
  if (!list) return;

  var selectedCard = list.querySelector(".controller-card.is-selected");
  var selectedId = preferredControllerId || (selectedCard ? selectedCard.getAttribute("data-controller-id") : "");

  fetch("/api/controllers/available?t=" + Date.now(), { cache: "no-store" })
    .then(function (response) { return response.json(); })
    .then(function (data) {
      var controllers = data.controllers || [];
      list.innerHTML = "";

      if (controllers.length === 0) {
        var empty = document.createElement("p");
        empty.className = "muted";
        empty.id = "controllerListEmpty";
        empty.textContent = "Keine freien Controller.";
        list.appendChild(empty);
        return;
      }

      controllers.forEach(function (controller, index) {
        var id = controller.controllerId || controller.controller_id || "";
        var type = controller.type || "WEB";
        var card = document.createElement("div");
        var isSelected = selectedId ? id === selectedId : index === 0;

        card.className = "controller-card is-available" + (isSelected ? " is-selected" : "");
        card.setAttribute("data-controller-id", id);
        card.setAttribute("data-controller-type", type);
        card.setAttribute("role", "button");
        card.setAttribute("tabindex", "0");
        card.innerHTML = "<div class=\"controller-card-body\"><span class=\"controller-name\">" + type + "-Controller</span><span class=\"controller-id\">" + id + "</span></div><span class=\"tag tag-available\">Verfügbar</span>";
        list.appendChild(card);
      });
    })
    .catch(function (error) {
      console.error("Error:", error);
    });
}

function applyLobbyPlayersData(players) {
  var list = document.getElementById("playerList");
  if (!list) return;
  var arr = players || [];
  window.lobbyPlayers = arr;
  list.innerHTML = "";
  if (arr.length === 0) {
    var empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent = "Keine Teilnehmer in der Lobby.";
    list.appendChild(empty);
    if (window.updateSpielStartenState) window.updateSpielStartenState();
    return;
  }
  arr.forEach(function (player) {
    var row = document.createElement("div");
    var name = document.createElement("span");
    var statusWrap = document.createElement("span");
    var statusDot = document.createElement("span");
    var statusText = document.createElement("span");
    row.className = "player-row";
    name.className = "player-name";
    name.textContent = player.username || "?";
    var controllerId = player.controllerId || player.controller_id;
    var controllerStatus = (player.controllerStatus || player.controller_status || "").toString().toUpperCase();
    if (controllerId) {
      var cidSpan = document.createElement("span");
      cidSpan.className = "player-controller-id";
      cidSpan.textContent = controllerId;
      name.appendChild(cidSpan);
    }
    var isOffline = controllerStatus === "OFFLINE";
    statusWrap.className = "player-status-wrap " + (isOffline ? "is-offline" : (player.ready ? "is-ready" : "is-not-ready"));
    statusDot.className = "player-status-dot";
    statusDot.setAttribute("aria-hidden", "true");
    statusText.textContent = isOffline ? "Offline" : (player.ready ? "Bereit" : "Nicht bereit");
    statusWrap.appendChild(statusDot);
    statusWrap.appendChild(statusText);
    row.appendChild(name);
    row.appendChild(statusWrap);
    var leaveBtn = document.createElement("button");
    leaveBtn.type = "button";
    leaveBtn.className = "btn-leave";
    leaveBtn.textContent = "Austreten";
    leaveBtn.title = "Lobby verlassen und Controller freigeben (auch Hardware-Controller)";
    leaveBtn.addEventListener("click", function (u) {
      return function () {
        leaveBtn.disabled = true;
        fetch("/api/lobby/leave", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: u })
        })
          .then(function (r) { return r.text().then(function (t) { return { ok: r.ok, text: t }; }); })
          .then(function (result) {
            if (result.ok) {
              if (window.lobbyUsername === u) window.lobbyUsername = "";
              loadLobbyPlayers();
              loadAvailableControllers();
            } else {
              leaveBtn.disabled = false;
              alert(result.text || "Fehler beim Verlassen.");
            }
          })
          .catch(function () {
            leaveBtn.disabled = false;
            alert("Verbindungsfehler.");
          });
      };
    }(player.username));
    row.appendChild(leaveBtn);
    list.appendChild(row);
  });
  if (window.updateSpielStartenState) window.updateSpielStartenState();
}

function loadLobbyPlayers() {
  fetch("/api/lobby/status")
    .then(function (response) { return response.json(); })
    .then(function (data) {
      applyLobbyPlayersData(data.players || []);
    })
    .catch(function (error) {
      console.error("Error:", error);
      window.lobbyPlayers = [];
    });
}

function loadHighscores(mode) {
  var tbody = document.getElementById("highscoresBody");
  if (!tbody) return;

  mode = mode || 5;
  tbody.innerHTML = "<tr id=\"highscoresEmpty\"><td colspan=\"4\" class=\"muted\">Lade Highscores…</td></tr>";

  fetch("/api/highscores/" + mode + "?t=" + Date.now(), { cache: "no-store" })
    .then(function (response) { return response.json(); })
    .then(function (data) {
      var list = Array.isArray(data) ? data : (data.highscores || data.entries || []);

      tbody.innerHTML = "";
      if (list.length === 0) {
        tbody.innerHTML = "<tr id=\"highscoresEmpty\"><td colspan=\"4\" class=\"muted\">Keine Einträge für " + mode + " Fragen.</td></tr>";
        return;
      }

      list.forEach(function (entry, index) {
        var rank = index + 1;
        var tr = document.createElement("tr");
        var score = entry.score != null ? Number(entry.score) : 0;
        var date = entry.created_at || "";

        if (date.length > 19) date = date.substring(0, 19).replace("T", " ");
        if (rank === 1) tr.className = "rank-1";
        if (rank === 2) tr.className = "rank-2";
        if (rank === 3) tr.className = "rank-3";

        tr.innerHTML = "<td>" + rank + "</td><td>" + (entry.username || "?") + "</td><td>" + score + "</td><td>" + date + "</td>";
        tbody.appendChild(tr);
      });
    })
    .catch(function (error) {
      console.error("Error:", error);
    });
}

function createWebController(callback) {
  fetch("/api/controllers/create-web", {
    method: "POST",
    headers: { "Content-Type": "application/json" }
  })
    .then(function (response) {
      if (!response.ok) return response.text().then(function (text) { throw new Error(text || "Web-Controller konnte nicht erstellt werden."); });
      return response.json();
    })
    .then(function (data) {
      if (callback) callback(true, data);
    })
    .catch(function (error) {
      console.error("Error:", error);
      if (callback) callback(false, error.message);
    });
}

window.applyLobbyPlayersData = applyLobbyPlayersData;
window.loadAvailableControllers = loadAvailableControllers;
window.loadLobbyPlayers = loadLobbyPlayers;
window.loadHighscores = loadHighscores;
window.createWebController = createWebController;

// --- Login ---
document.getElementById("loginBtn").addEventListener("click", function (event) {
  event.preventDefault();
  showInlineMessage("loginError", "", false);

  var username = document.getElementById("login-username").value.trim();
  var password = document.getElementById("login-password").value;

  if (!username || !password) {
    showInlineMessage("loginError", "Bitte Benutzername und Passwort eingeben.", false);
    return;
  }
  if (username.length < 3) {
    showInlineMessage("loginError", "Benutzername muss mindestens 3 Zeichen haben.", false);
    return;
  }
  if (password.length < 4) {
    showInlineMessage("loginError", "Passwort muss mindestens 4 Zeichen haben.", false);
    return;
  }

  fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username: username, password: password })
  })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: text };
      });
    })
    .then(function (result) {
      if (result.ok) {
        window.currentUsername = username;
        document.body.classList.remove("state-initial", "state-register");
        document.body.classList.add("state-controller");
        loadAvailableControllers();
        loadUserRfid();
        showInlineMessage("loginError", "Anmeldung erfolgreich.", true);
      } else {
        showInlineMessage("loginError", mapApiErrorToGerman(result), false);
      }
    })
    .catch(function (error) {
      console.error("Error:", error);
      showInlineMessage("loginError", "Verbindungsfehler. Bitte erneut versuchen.", false);
    });
});

// --- Register ---
document.getElementById("register-form").addEventListener("submit", function (event) {
  event.preventDefault();
  showInlineMessage("registerError", "", false);

  var username = document.getElementById("register-username").value.trim();
  var password = document.getElementById("register-password").value;
  var passwordRepeat = document.getElementById("register-password-repeat").value;
  var rfidRaw = document.getElementById("register-rfid").value.trim();
  var rfidUid = rfidRaw ? rfidRaw.replace(/\s+/g, "").toUpperCase() : "";

  if (!username || !password) {
    showInlineMessage("registerError", "Bitte Benutzername und Passwort eingeben.", false);
    return;
  }
  if (username.length < 3) {
    showInlineMessage("registerError", "Benutzername muss mindestens 3 Zeichen haben.", false);
    return;
  }
  if (password.length < 4) {
    showInlineMessage("registerError", "Passwort muss mindestens 4 Zeichen haben.", false);
    return;
  }

  if (password !== passwordRepeat) {
    showInlineMessage("registerError", "Die Passwörter stimmen nicht überein.", false);
    return;
  }

  var body = { username: username, password: password, passwordRepeat: passwordRepeat };
  if (rfidUid) body.rfidUid = rfidUid;

  fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: text };
      });
    })
    .then(function (result) {
      if (result.ok) {
        showInlineMessage("registerError", "Benutzer erstellt.", true);
      } else {
        showInlineMessage("registerError", mapApiErrorToGerman(result), false);
      }
    })
    .catch(function (error) {
      console.error("Error:", error);
      showInlineMessage("registerError", "Verbindungsfehler. Bitte erneut versuchen.", false);
    });
});

// --- Controller-Auswahl ---
document.getElementById("createControllerBtn").addEventListener("click", function () {
  var button = this;
  button.disabled = true;
  showInlineMessage("controllerError", "", false);
  window.open(getWebControllerUrl(), "_blank");
  showInlineMessage("controllerError", "Controller geöffnet.", true);
  setTimeout(function () {
    loadAvailableControllers();
    button.disabled = false;
  }, 1500);
});

document.getElementById("refreshControllersBtn").addEventListener("click", function () {
  loadAvailableControllers();
});

document.getElementById("rfidSaveBtn").addEventListener("click", function () {
  saveUserRfid();
});

// --- Weiter zur Lobby (Controller binden + Lobby beitreten) ---
document.getElementById("weiterZurLobby").addEventListener("click", function (event) {
  event.preventDefault();
  showInlineMessage("controllerError", "", false);

  var selected = document.querySelector(".controller-card.is-available.is-selected");
  if (!selected) {
    showInlineMessage("controllerError", "Bitte wähle einen Controller aus.", false);
    return;
  }

  fetch("/api/players/bind", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username: window.currentUsername,
      controllerId: selected.getAttribute("data-controller-id"),
      controllerType: selected.getAttribute("data-controller-type") || "WEB"
    })
  })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: text };
      });
    })
    .then(function (bindResult) {
      if (!bindResult.ok) {
        showInlineMessage("controllerError", mapApiErrorToGerman(bindResult), false);
        return null;
      }
      return fetch("/api/lobby/join", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: window.currentUsername })
      });
    })
    .then(function (lobbyResponse) {
      if (lobbyResponse === null) return null;
      return lobbyResponse.text().then(function (text) {
        return { ok: lobbyResponse.ok, text: text };
      });
    })
    .then(function (lobbyResult) {
      if (!lobbyResult) return;
      if (lobbyResult.ok) {
        window.lobbyUsername = window.currentUsername;
        loadAvailableControllers();
        loadLobbyPlayers();
        if (window.showMainSection) window.showMainSection("lobby");
        showInlineMessage("controllerError", "", false);
        if (window.showLogin) window.showLogin();
        window.currentUsername = "";
        showInlineMessage("loginError", "In der Lobby. Gerät bereit für den nächsten Spieler.", true);
      } else {
        showInlineMessage("controllerError", mapApiErrorToGerman(lobbyResult), false);
      }
    })
    .catch(function (error) {
      console.error("Error:", error);
      showInlineMessage("controllerError", "Verbindungsfehler. Bitte erneut versuchen.", false);
    });
});

document.getElementById("refreshLobby").addEventListener("click", function () {
  loadLobbyPlayers();
});

// --- Spiel-Konfiguration und Start ---
document.getElementById("configGameBtnWrap").addEventListener("click", function () {
  var config = readGameConfig();
  var feedbackEl = document.getElementById("configGameError");
  var button = document.getElementById("configGameBtn");

  showInlineMessage("configGameError", "", false);

  if (config.categories.length === 0 || config.difficulties.length === 0) {
    showInlineMessage("configGameError", "Bitte Kategorien und Schwierigkeiten auswählen.", false);
    return;
  }

  var players = window.lobbyPlayers || [];
  if (players.length === 0) {
    showInlineMessage("configGameError", "Keine Teilnehmer in der Lobby.", false);
    return;
  }
  if (!players.every(function (p) { return !!p.ready; })) {
    showInlineMessage("configGameError", "Nicht alle Spieler sind bereit.", false);
    return;
  }
  if (window.canStartGame && !window.canStartGame()) {
    showInlineMessage("configGameError", "Verfügbare Fragen und Modus müssen übereinstimmen.", false);
    return;
  }

  button.disabled = true;

  fetch("/api/game/config", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(config)
  })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: text };
      });
    })
    .then(function (result) {
      if (!result.ok) throw new Error(result.text || "Game-Konfiguration fehlgeschlagen.");
      return fetch("/api/game/start", {
        method: "POST",
        headers: { "Content-Type": "application/json" }
      });
    })
    .then(function (response) {
      return response.text().then(function (text) {
        return { ok: response.ok, text: text };
      });
    })
    .then(function (result) {
      button.disabled = false;
      if (window.updateSpielStartenState) window.updateSpielStartenState();
      if (!result.ok) throw new Error(result.text || "Spiel konnte nicht gestartet werden.");
      showInlineMessage("configGameError", "Spiel gestartet!", true);
    })
    .catch(function (error) {
      button.disabled = false;
      if (window.updateSpielStartenState) window.updateSpielStartenState();
      showInlineMessage("configGameError", mapApiErrorToGerman({ text: error && error.message }), false);
      console.error("Error:", error);
    });
});
