# MauliWinterEvent

Winter-/Weihnachts-Event-Plugin für MauliCraft.

## Anforderungen
- Java 20
- Spigot/Paper 1.21.x (getestet gegen Paper API 1.21.1)
- Max. ~30 gleichzeitige Spieler problemlos

## Befehle & Rechte
- `/advent` — Öffnet den Adventskalender (`mauliwinter.advent`)
- `/winterevent reload` — Reload (`mauliwinter.admin`)

## Features
- Zeitraum 01.12. – 31.12., Nachholen erlaubt
- Pro Tag 1 Claim (Reset 00:00 Serverzeit)
- SQLite-Speicherung (`plugins/MauliWinterEvent/winterevent.db`)
- ZNPCs: NPC führt einfach `/advent` aus
- Rewards per Konsole (config.yml)
- Bossbar/Titles, deutsche Texte

## Build
```bash
mvn -q -e -DskipTests package
```
Jar: `target/MauliWinterEvent-1.0.0.jar`

## Konfiguration
Siehe `config.yml`. Beispiel nutzt:
- 24.12 & 25.12: Legendary Key
- 31.12: Vault Key
- Sonst: Rare Key

Passe die Key-Befehle an dein Crate-Plugin an.
