
# Dokumentation: Rollstuhl Sturzerkennung

## 1. Projektübersicht
* **Team:** Tim, Henrik, Johann
* **Organisationsmethode:** Wasserfall
* **Problemstellung:** Sicherheit von Rollstuhlfahrern bei schlechten Lichtverhältnissen verbessern
* **Zielgruppe:** Rollstuhlfahrer*innen

## 2. Projektplanung
### 2.1 Anforderungen (Must/Should/Could)
| Priorität | Anforderung | Status |
| :--- | :--- | :--- |
| Must-have | Abstandmessung mit Warntönen ab einer      bestimmten Entfernung
| Must-have | Neigungsensor mit automatischer Benachrichtigung, falls eine bestimmte Neigung überschritten ist
| Must-have | Backend, welches eine E-Mail an eine Kontaktperson schicken kann
| Should-have | Login im Frontend
| Should-have | Pflege von Notfallkontaktinformationen im Frontend
| Should-have | Pflege von eigenen Informationen im Frontend
| Chould-have | Kopplungslogik, mit der ein Gerät dynamisch mit einem Useraccount verbunden werden kann
| Could-have | Anbringung des 3D-gedruckten Gehäuses unter der Fußleiste 

### 2.2 Zeitplanung
* **Gesamtstunden:** 27std.
* **Wichtigste Meilensteine:**
    1. Fertigstellung des Hardware-Setups mit allen nötigen Komponenten
    2. Fertigstellung des Frontends mit Login für beliebig viele Nutzer
    3. Fertigstellung des Backends mitsamt aller Endpunkte
    4. Erster erfolgreicher Testdurchlauf

## 3. Technisches Design (CPS)
### 3.1 Hardware & Sensorik
* **Komponenten:** Esp32, MPU6050, HC-SR04, Passiver Buzzer, TM1637, Powerbank
* **Messwerterfassung:** Mit dem HC-SR04 wird sekündlich ein Abstand, bis zu 2m, gemessen, ab einer Entfernung von 50cm wird mit dem Buzzer ein Ton ausgegeben. Neben dem Abstand wird auch die Neigung sekündlich mit dem MPU6050 gemessen.
* **Schaltplan:** `![Schaltplan hier einfügen/verlinken]`

### 3.2 Software & Kommunikation
* **Logik (Programmablauf):** [Beschreibung oder Flowchart-Link]
* **Kommunikationswege:** [z. B. WLAN/MQTT/HTTP zu einem Server]
* **Datenverarbeitung:** [Wie werden die Daten gespeichert und visualisiert?]

## 4. Rechtliches & Nachhaltigkeit
### 4.1 Datenschutz & Urheberrecht
* **Datenschutz:** [Welche personenbezogenen Daten fallen an? Wie werden sie geschützt?]
* **Urheberrecht:** [Welche externen Bibliotheken/Lizenzen werden genutzt?]

### 4.2 Nachhaltigkeit
* **Hardware:** [Aspekte der Langlebigkeit oder Energieeffizienz]
* **Software:** [Ressourcenschonende Programmierung, z. B. Deep Sleep Modi]

## 5. Evaluation & Reflexion
* **Testergebnisse:** Abstand wird mit dem HC-SR04 gemessen, ab 50cm wird mit dem Passiven Buzzer ein Warnton ausgegeben. Der Ton wird lauter je geringer der Abstand wird. Die Neigung mit dem MPU6050 wird gemessen. Mit der grafischen Oberfläche können Notfallkontakte gespeichert werden und getriggert werden. 
* **Erkenntnisse:** [Was haben wir im Prozess gelernt?]
* **Ausblick:** [Was könnte in einer Version 2.0 verbessert werden?]

---

### Tipps für die Schüler zur Arbeit mit Markdown:
* **Bilder:** Legt eure Diagramme (Export aus Fritzing, Draw.io etc.) in einen Ordner `img` und verlinkt sie mit `![Titel](img/bildname.png)`.
* **Code:** Kurze Code-Snippets könnt ihr direkt einbinden:
    ```cpp
    if (sensorValue > threshold) {
      triggerAction();
    }
    ```
* **Tabellen:** Achtet auf die Trennstriche (`|` und `-`), damit die Tabellen korrekt gerendert werden.