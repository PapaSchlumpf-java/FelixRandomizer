# FelixRandomizer

Serverseitiges Paper-Plugin für Minecraft Java 26.2. Sobald der Randomizer aktiv
ist, erhält jeder aktuell verbundene Spieler regelmäßig ein zufälliges Item.

- 75 % Schwerter, Äxte oder Rüstung
- 25 % sonstige Items
- Bevorzugte Ausrüstung ist mit 60 % Wahrscheinlichkeit passend verzaubert
- Standardintervall: 60 Sekunden
- Nach jedem Server- oder Weltstart zunächst deaktiviert

## Befehle

```text
/randomitems start
/randomitems stop
/randomitems interval 60
/randomitems status
```

Das Intervall kann zwischen 1 und 86.400 Sekunden liegen. Die Einstellung gilt
bis zum nächsten Server- oder Weltstart. Ist das Inventar voll, wird das Item
neben dem Spieler fallengelassen.

## Bauen

Voraussetzung ist eine Internetverbindung. Java 25 wird bei Bedarf automatisch
über die Gradle-Toolchain bezogen.

- macOS/Linux: `./gradlew build`
- Windows: `gradlew.bat build`

Die nutzbare Datei liegt danach unter
`build/libs/felix-randomizer-1.0.0.jar`.

Die JAR wird in das `plugins`-Verzeichnis eines Paper-Servers kopiert.
