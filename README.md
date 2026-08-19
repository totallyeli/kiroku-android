# Kiroku

Kiroku ist eine private, vollständig offline arbeitende Android-App für Notizen und tägliche Gewohnheiten. Es gibt keine Konten, Werbung, Analyse, Serververbindung oder Internet-Berechtigung. Alle Inhalte bleiben in einer lokalen Room-Datenbank auf dem Gerät; Android-Cloud-Backups sind deaktiviert.

## Funktionen

- Notizen erstellen, automatisch speichern, bearbeiten, anheften, farblich markieren, durchsuchen und bestätigt löschen
- Getrennter täglicher Gewohnheitentracker mit Tagesfortschritt und sofortigem Abhaken
- Gewohnheiten mit Name, Beschreibung, Symbol, Farbe und Erstellungsdatum verwalten
- Optionale tägliche Uhrzeit mit lokaler Benachrichtigung unter „Weitere Optionen“
- Aktuelle und längste Serie sowie Gesamtzahl erledigter Tage
- Monatskalender mit Navigation und Korrektur vergangener bzw. heutiger Einträge
- Helles, dunkles oder systemgesteuertes Design sowie dynamische Farben ab Android 12
- Adaptive Oberfläche mit Edge-to-edge-Darstellung und persistentem Tab-Zustand

## Technik und Struktur

Das Projekt verwendet Kotlin 2.4.10, Jetpack Compose/Material 3 (BOM 2026.06.01), Navigation Compose, ViewModel, Coroutines/Flow, Room 3.0.1, DataStore, Android Gradle Plugin 9.2.1 und Gradle 9.4.1. `minSdk` ist 26, `targetSdk`/`compileSdk` ist 37.0; Java- und Kotlin-Bytecode zielen auf JDK 17.

```text
app/src/main/java/dev/bugiel/kiroku/
├── data/       Room, DAOs und lokale Repositories
├── di/         kleiner manueller AppContainer
├── domain/     Modelle, Suche, Serien- und Datumslogik
├── reminder/   Planung und Anzeige lokaler Gewohnheitserinnerungen
└── ui/         Compose-Screens, ViewModels, Theme und UI-Helfer
```

## Tageswechsel und Serien

Erledigungen werden mit einem zusammengesetzten Primärschlüssel aus Gewohnheits-ID und lokalem `epochDay` gespeichert. Ein Tageswechsel löscht keine Daten: Die Oberfläche fragt einfach den neuen lokalen Kalendertag ab, wenn die App fortgesetzt wird und zusätzlich einmal pro Minute während sie geöffnet ist.

Eine aktuelle Serie endet heute oder – solange heute noch offen ist – gestern. Sind weder heute noch gestern erledigt, ist die aktuelle Serie null. Die längste Serie ist die längste lückenlose Folge eindeutiger lokaler Kalendertage. Änderungen im Kalender berechnen alle Werte sofort neu.

Eine optionale Erinnerungszeit verändert weder Tagesstatus noch Serien. Vor einer Benachrichtigung prüft Kiroku, ob die Gewohnheit am aktuellen lokalen Kalendertag bereits erledigt wurde. Erinnerungen werden nach Neustarts, Zeitzonenänderungen und App-Updates erneut geplant. Ab Android 13 muss der Benutzer Benachrichtigungen erlauben.

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

- Notizen unterstützen in Version 1 nur Klartext, keine Anhänge oder Formatierung.
- Alle Gewohnheiten sind täglich; eigene Wochenpläne sind noch nicht verfügbar.
- Android kann Erinnerungen durch Energiesparmaßnahmen geringfügig verzögert zustellen.
- Es gibt bewusst keine Synchronisierung, Freigabe oder Exportfunktion.
- Automatisierte Logiktests laufen lokal. Für visuelle Geräte-Tests wird ein eigener Emulator oder ein per ADB verbundenes Gerät benötigt.
