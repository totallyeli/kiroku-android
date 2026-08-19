# Kiroku

Kiroku ist eine private Android-App für Notizen und tägliche Gewohnheiten. Es gibt keine Konten, Werbung, Analyse oder Inhaltssynchronisierung. Notizen, Anlagen und Gewohnheiten bleiben lokal auf dem Gerät; Android-Cloud-Backups sind deaktiviert. Eine Internetverbindung wird ausschließlich für die vom Benutzer geöffnete Update-Prüfung über GitHub benötigt.

## Funktionen

- Notizen erstellen, automatisch speichern, bearbeiten, anheften, farblich markieren, durchsuchen und bestätigt löschen
- Markdown schreiben und als formatierte Vorschau anzeigen, einschließlich einer kompakten Formatleiste
- Notizen aus `.txt`- und `.md`-Dateien importieren
- Bilder, PDF- und Markdown-Dateien an Notizen anhängen, in Kiroku öffnen und über Android wieder exportieren
- Getrennter täglicher Gewohnheitentracker mit Tagesfortschritt und sofortigem Abhaken
- Gewohnheiten mit Name, Beschreibung, Farbe, Erstellungsdatum und einer erweiterten Auswahl aus 28 Symbolen verwalten
- Optionale tägliche Uhrzeit mit lokaler Benachrichtigung unter „Weitere Optionen“
- Aktuelle und längste Serie sowie Gesamtzahl erledigter Tage
- Monatskalender mit Navigation und Korrektur vergangener bzw. heutiger Einträge
- Helles, dunkles oder systemgesteuertes Design sowie dynamische Farben ab Android 12
- In den Einstellungen automatisch das neueste GitHub-Release prüfen, die APK laden und Androids Installer öffnen
- Adaptive Oberfläche mit Edge-to-edge-Darstellung und persistentem Tab-Zustand

## Technik und Struktur

Das Projekt verwendet Kotlin 2.4.10, Jetpack Compose/Material 3 (BOM 2026.06.01), Navigation Compose, ViewModel, Coroutines/Flow, Room 3.0.1, DataStore, Android Gradle Plugin 9.2.1 und Gradle 9.4.1. `minSdk` ist 26, `targetSdk`/`compileSdk` ist 37.0; Java- und Kotlin-Bytecode zielen auf JDK 17.

```text
app/src/main/java/dev/bugiel/kiroku/
├── data/       Room, DAOs, Dokumentzugriff und lokale Repositories
├── di/         kleiner manueller AppContainer
├── domain/     Modelle, Suche, Serien- und Datumslogik
├── reminder/   Planung und Anzeige lokaler Gewohnheitserinnerungen
├── update/     GitHub-Release-Prüfung und geprüfter APK-Download
└── ui/         Compose-Screens, Markdown, ViewModels, Theme und UI-Helfer
```

## Tageswechsel und Serien

Erledigungen werden mit einem zusammengesetzten Primärschlüssel aus Gewohnheits-ID und lokalem `epochDay` gespeichert. Ein Tageswechsel löscht keine Daten: Die Oberfläche fragt einfach den neuen lokalen Kalendertag ab, wenn die App fortgesetzt wird und zusätzlich einmal pro Minute während sie geöffnet ist.

Eine aktuelle Serie endet heute oder – solange heute noch offen ist – gestern. Sind weder heute noch gestern erledigt, ist die aktuelle Serie null. Die längste Serie ist die längste lückenlose Folge eindeutiger lokaler Kalendertage. Änderungen im Kalender berechnen alle Werte sofort neu.

Eine optionale Erinnerungszeit verändert weder Tagesstatus noch Serien. Vor einer Benachrichtigung prüft Kiroku, ob die Gewohnheit am aktuellen lokalen Kalendertag bereits erledigt wurde. Erinnerungen werden nach Neustarts, Zeitzonenänderungen und App-Updates erneut geplant. Ab Android 13 muss der Benutzer Benachrichtigungen erlauben.

## Updates und Datensicherheit

„Nach Updates suchen“ liest das neueste Release aus `totallyeli/kiroku-android`. Vor der Installation prüft Kiroku Paketname, höhere Versionsnummer, Signatur und – sofern von GitHub geliefert – die SHA-256-Prüfsumme der APK. Die eigentliche Installation bestätigt der Benutzer im Android-Systemdialog.

Ein App-Update verwendet denselben Paketnamen und Signierschlüssel. Die Datenbank wird von Version 2 auf 3 ausschließlich um die neue Anlagentabelle ergänzt; bestehende Notizen, Gewohnheiten, Erledigungen und Serien werden nicht ersetzt oder gelöscht. Anlagen werden in den privaten App-Dateien gespeichert und bleiben bei einem normalen Update ebenfalls erhalten. Eine Deinstallation der App entfernt dagegen weiterhin die lokalen App-Daten.

## Installation

Die aktuelle APK kann auf der GitHub-Seite unter **Releases** heruntergeladen und auf dem Android-Gerät geöffnet werden. Bei der ersten manuellen Installation muss Android die Installation aus der verwendeten Quelle erlauben. Bereits installierte Versionen können danach direkt über **Einstellungen → Updates** aktualisiert werden.

## Bauen und testen

Voraussetzungen: JDK 17 oder neuer sowie Android SDK Platform 37.0 mit Build Tools 36.0.0. Der lokale SDK-Pfad gehört ausschließlich in die ignorierte Datei `local.properties`.

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Unter Windows können dieselben Aufgaben mit `gradlew.bat` ausgeführt werden. Der erzeugte APK liegt exakt unter:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Installation auf einem verbundenen Gerät:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Bekannte Grenzen

- Alle Gewohnheiten sind täglich; eigene Wochenpläne sind noch nicht verfügbar.
- Android kann Erinnerungen durch Energiesparmaßnahmen geringfügig verzögert zustellen.
- Es gibt bewusst keine Konten oder Cloud-Synchronisierung. Anlagen lassen sich einzeln exportieren; ein vollständiges App-Backup ist noch nicht integriert.
- Automatisierte Logiktests laufen lokal. Für visuelle Geräte-Tests wird ein eigener Emulator oder ein per ADB verbundenes Gerät benötigt.
