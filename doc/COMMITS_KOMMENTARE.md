# Vorschläge für Commit-Nachrichten (deutsche Kommentare)

Diese Commits entsprechen den hinzugefügten deutschen Kommentaren in den jeweiligen Dateien. Keine Funktionalität wurde geändert.

---

## Frontend

```
docs(frontend): Wichtige Kommentare auf Deutsch hinzugefügt

- index.html, index_test.html: Struktur-Beschreibungen, Bereichskommentare
- app.js, app_http.js, app_mqtt.js: Modul-Header, Zweckbeschreibungen
- app_test.js, mqtt-testing.js: Test-Seiten dokumentiert
- styles.css, style_test.css: Stylesheet-Header
- env.js: Umgebungsvariablen kommentiert
```

---

## Web-Controller

```
docs(web-controller): Wichtige Kommentare auf Deutsch hinzugefügt

- controller.html, controllermobile.html: Zweck und Aufbau
- controller.js: MQTT-Topics und Controller-ID
- controller.css: Design-Beschreibung
- env.js: MQTT-Konfiguration kommentiert
```

---

## Java-Backend

```
docs(java-backend): Javadoc-Kommentare auf Deutsch hinzugefügt

- MainVerticle: Startablauf, Verticle-Registrierung
- auth: AuthController, AuthService, AuthRepository
- controllers: ControllersController, ControllersService, ControllersRepository
- lobby: LobbyController, LobbyService, LobbyRepository
- player: PlayerController, PlayerService, PlayerRepository
- game: GameController, GameService, GameRepository, GameStateManager, GameModel
- highscores: HighscoresController, HighscoresService, HighscoresRepository
- object: ObjectController, ObjectService, ObjectRepository
- mqtt: MqttVerticle, MqttController, MqttService
- http: HttpServerVerticle, HttpController
- database: DatabaseClient
```

---

## Arduino

```
docs(arduino): Wichtige Kommentare auf Deutsch hinzugefügt

- main.cpp: Hauptprogramm, Button-Logik, RFID, OLED
- hardware/buttons.cpp, rfid.cpp, neopixel.cpp, oled.cpp
- net/wifi_mqtt.cpp
- config.h, hardware/*.h, net/wifi_mqtt.h
```

---

## Ein einzelner Commit für alles

Falls Sie alle Änderungen in einem Commit zusammenfassen möchten:

```
docs: Deutsche Kommentare in allen Modulen hinzugefügt

Frontend, Web-Controller, Java-Backend und Arduino:
Struktur- und Zweckbeschreibungen ergänzt, keine Funktionsänderungen.
```
