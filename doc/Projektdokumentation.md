# Projektdokumentation (API, MQTT, Architektur)

## 1) Zweck und Systemueberblick

Dieses Projekt ist ein Multiplayer-Quiz mit drei aktiven Schichten:

- **Frontend** (`frontend/`): Browser-UI fuer Login, Lobby, Spielsteuerung, Highscores.
- **Web-Controller** (`web-controller/`): Browserbasierter Controller pro Spieler/Geraet.
- **Backend** (`java-backend/`): Vert.x REST-API + Spiellogik + MQTT-Bruecke + MariaDB.
- **Hardware-Controller** (`arduino/`): ESP32/Arduino Controller mit RFID, Buttons, OLED und MQTT.

Kommunikation:

- HTTP/REST fuer klassische Requests (Login, Lobby, Spielstart, Highscores).
- MQTT fuer Echtzeit-Events (Spielzustand, Frage, Antworten, Ready-Status, Controller-Status).

Standard-Praefix fuer MQTT-Topics:

- `group-16/` (konfigurierbar ueber `MQTT_MESSAGE_PREFIX`)

---

## 2) Laufzeit-Komponenten und Ports

- Frontend: `http://localhost` (Port 80)
- Web-Controller: `http://localhost:81`
- Backend (REST): `http://localhost:8080`
- MQTT Broker:
  - TCP: `1883`
  - WebSocket: `9001`
- MariaDB: `3306`
- phpMyAdmin: `http://localhost:8081`

---

## 3) Backend-Architektur (kurz)

Zentrale Klassen:

- `MainVerticle`: startet DB, MQTT-Verticle und HTTP-Verticle; registriert Controller.
- `HttpServerVerticle`: REST-Router/HTTP-Server.
- `MqttVerticle`: MQTT-Verbindung.
- `MqttController`: verarbeitet MQTT-Subscriptions und EventBus-Weiterleitung.
- `MqttService`: veroeffentlicht MQTT-Nachrichten.
- Fachcontroller:
  - `AuthController`
  - `ControllersController`
  - `LobbyController`
  - `PlayerController`
  - `GameController`
  - `HighscoresController`

---

## 4) HTTP-API (vollstaendige Routen)

## Auth

- `POST /api/auth/register`
  - Body: `{ "username", "password", "passwordRepeat", "rfidUid?" }`
  - Erfolg: `201`
- `POST /api/auth/login`
  - Body: `{ "username", "password" }`
  - Erfolg: `200`, Fehler z. B. `401`, `409`
- `GET /api/auth/rfid/:uid`
  - RFID-UID -> Username Lookup
  - Erfolg: `200`, nicht gefunden: `404`
- `GET /api/auth/rfid?username=<name>`
  - Liefert gespeicherte RFID fuer User
  - Erfolg: JSON `{ "rfidUid": "..." }`
- `PUT /api/auth/rfid`
  - Body: `{ "username", "rfidUid" }` (leerer Wert entfernt RFID)
  - Erfolg: `200`, Konflikt: `409`

## Controller

- `GET /api/controllers/available`
  - Erfolg: JSON `{ "controllers": [...] }`
- `POST /api/controllers/create-web`
  - Erstellt Web-Controller
  - Erfolg: `201`
- `POST /api/controllers/disconnect`
  - Body: `{ "controllerId" }`
  - Wird beim Schliessen des Web-Controllers verwendet
- `GET /api/controllers/:controllerId/player-status`
  - Liefert Player-Bindung/Ready fuer einen Controller

## Lobby

- `POST /api/lobby/create`
  - Erstellt aktuelle Lobby-Session
- `GET /api/lobby/status`
  - Liefert Spielerliste + Status
- `POST /api/lobby/join`
  - Body: `{ "username" }`
- `POST /api/lobby/leave`
  - Body: `{ "username" }`
  - Triggert auch `lobby.updated` intern

## Player

- `POST /api/players/bind`
  - Body: `{ "username", "controllerId", "controllerType?" }`
  - `controllerType` default: `WEB`

## Game

- `POST /api/game/config`
  - Body: Spielkonfiguration (Modus, Kategorien, Schwierigkeit)
- `POST /api/game/start`
  - Startet Countdown/Spielablauf

## Highscores

- `GET /api/highscores/:mode?limit=<n>`
  - `mode` typischerweise `5`, `10`, `20`
  - Rueckgabe: Highscore-Liste als JSON

## 5) MQTT-Kanaele (vollstaendige Uebersicht)

Hinweis: Alle Topics sind relativ zum Praefix `group-16/`.

## 5.1 Topics, die das Backend **abonniert**

- `output`
- `auth/rfid/lookup`
- `controller/+/register`
- `controller/+/pong`
- `controller/+/request-status`
- `player/+/ready`
- `player/+/answer`

## 5.2 Topics, die das Backend **publisht**

- `controller/{controllerId}/status`
- `controller/{controllerId}/ping`
- `controller/{controllerMac}/rfid/reply`
- `player/{playerId}/status`
- `player/{playerId}/result`
- `game/state`
- `game/countdown`
- `game/question`
- `game/evaluation`
- `game/ended`
- `game/lobby/status`

## 5.3 Frontend (`frontend/js/mqtt/app_mqtt.js`) Subscriptions

- `game/state`
- `game/countdown`
- `game/question`
- `game/evaluation`
- `game/ended`
- `game/lobby/status`

## 5.4 Web-Controller (`web-controller/controller.js`)

Abonniert:

- `controller/{id}/ping`
- `controller/{id}/status`
- `game/state`
- `game/countdown`
- `game/question`
- `game/evaluation`
- `game/ended`
- dynamisch: `player/{playerId}/status`
- dynamisch: `player/{playerId}/result`

Publisht:

- `controller/{id}/register`
- `controller/{id}/pong`
- `controller/{id}/request-status`
- `player/{playerId}/ready`
- `player/{playerId}/answer`

## 5.5 Hardware-Controller (`arduino/src/net/wifi_mqtt.cpp`)

Abonniert:

- `controller/{mac}/ping`
- `controller/{mac}/status`
- `controller/{mac}/rfid/reply`
- `game/state`
- `game/question`
- `game/ended`
- `player/+/result`

Publisht:

- `controller/{mac}/register`
- `controller/{mac}/pong`
- `controller/{mac}/request-status`
- `auth/rfid/lookup`
- `player/{username}/ready`
- `player/{username}/answer`
- optional: MAC-Topic gemaess `MQTT_TOPIC_MAC`

---

## 6) Typische Datenfluesse

## A) Web-Controller Anmeldung und Bindung

1. Web-Controller startet mit `?id=...` (oder generiert `WEB-...`).
2. Publisht `controller/{id}/register`.
3. Fragt periodisch `controller/{id}/request-status`.
4. Backend antwortet auf `controller/{id}/status` mit `playerId` + `ready`.
5. Controller sendet Ready/Antworten auf `player/{playerId}/...`.

## B) RFID-Hardware Login

1. Hardware liest UID und publisht `auth/rfid/lookup` mit `{ uid, mac }`.
2. Backend loest UID zu Username auf.
3. Backend bindet Controller + fuegt User zur Lobby hinzu.
4. Backend antwortet auf `controller/{mac}/rfid/reply` mit Username (oder leer).

## C) Spielablauf

1. Frontend sendet `POST /api/game/config`.
2. Frontend sendet `POST /api/game/start`.
3. Backend publisht nacheinander:
   - `game/state` (z. B. COUNTDOWN/QUESTION/EVALUATION/ENDED)
   - `game/countdown`
   - `game/question`
   - `player/{id}/result`
   - `game/evaluation`
   - `game/ended`

## D) Lobby-Synchronisation

- Backend publisht bei Lobby-Aenderungen `game/lobby/status`.
- Frontend aktualisiert Liste und Ready-/Offline-Status live.

---

## 7) Beispiel-Payloads

Ready:

```json
{
  "ready": true,
  "action": "ready"
}
```

Antwort:

```json
{
  "questionId": 42,
  "selectedOption": "B"
}
```

Controller-Status:

```json
{
  "playerId": "alice",
  "ready": false,
  "ts": 1730000000000
}
```

RFID-Lookup Request:

```json
{
  "uid": "A1B2C3D4",
  "mac": "AA:BB:CC:DD:EE:FF"
}
```

RFID-Lookup Reply:

```json
{
  "username": "alice"
}
```

---

## 8) Konfiguration (relevante Variablen)

Backend:

- `MQTT_MESSAGE_PREFIX` (Default: `group-16/`)
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`

Frontend/Web-Controller (`window.__ENV__`):

- `MQTT_BROKER_URL`
- `MQTT_BROKER_PORT`
- `MQTT_USERNAME`
- `MQTT_PASSWORD`
- `MQTT_MESSAGE_PREFIX`

---

## 9) Hinweise fuer Betrieb und Erweiterung

- Topic-Praefix in allen Komponenten konsistent halten.
- Neue MQTT-Events immer in drei Schichten pruefen:
  - Publisher
  - Subscriber
  - UI/State-Handling
- Fuer neue REST-Routen:
  - Controller + Service + Frontend-Call synchron erweitern
  - Fehlercodes (`400/409/422/500`) konsistent dokumentieren.

