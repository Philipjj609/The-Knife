# The Knife - Applicazione per la Gestione di Ristoranti

The Knife è un'applicazione desktop sviluppata in Java con JavaFX per la gestione e consultazione di informazioni su ristoranti ispirati alla guida Michelin.

## 🚀 Funzionalità

- **Esplorazione ristoranti**: Interfaccia intuitiva per scoprire ristoranti con filtri avanzati
- **Sistema di recensioni**: Valutazioni e commenti per ogni ristorante
- **Gestione preferiti**: Salva i tuoi ristoranti preferiti
- **Dashboard personalizzate**: Diverse funzionalità per clienti e ristoratori
- **Mappe integrate**: Visualizzazione posizione ristoranti con integrazione Google Maps

## 👥 Tipologie di Utenti

- **Ospiti**: Esplorazione ristoranti senza registrazione
- **Clienti registrati**: Recensioni, preferiti e funzionalità personali
- **Ristoratori registrati**: Gestione ristoranti e risposte alle recensioni

## 🛠️ Tecnologie Utilizzate

- **Java 17**: Linguaggio di programmazione principale
- **JavaFX**: Framework per l'interfaccia grafica
- **JBCrypt**: Libreria per la crittografia delle password
- **CSV**: Persistenza dati attraverso file strutturati

## 📦 Installazione

1. Clona il repository:
```bash
git clone https://github.com/tuoutente/the-knife.git
```
Importa il progetto nel tuo IDE preferito

Assicurati di avere Java 17 e JavaFX configurati

Esegui la classe Main per avviare l'applicazione

🏗️ Struttura del Progetto
```text
the-knife/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── theknife/
│   │   │       ├── controllers/      # Controller JavaFX
│   │   │       ├── models/           # Modelli dati
│   │   │       ├── services/         # Gestione business logic
│   │   │       ├── utils/            # Utility e helpers
│   │   │       ├── Main.java         # Classe principale
│   │   │       └── esegui.java       # Entry point alternativo
│   │   └── resources/
│   │       ├── data/                 # File CSV per persistenza
│   │       ├── images/               # Immagini e icone
│   │       ├── styles/               # Fogli di stile CSS
│   │       └── views/                # File FXML per le viste
├── target/                           # Cartella output build
└── pom.xml                          # Configurazione Maven
📋 Requisiti di Sistema
Java Runtime Environment (JRE) 17 o superiore
```
4 GB di RAM minimo

200 MB di spazio libero su disco

Sistema operativo: Windows 10/11, macOS 10.14+, o Linux

🚦 Esecuzione
Da IDE
Esegui la classe Main nel package theknife

Da terminale
```bash
mvn clean javafx:run
```
Come JAR eseguibile
```bash
mvn clean package
java -jar TheKnife.jar
```
📊 Funzionalità Tecniche
Architettura MVC (Model-View-Controller)

Persistenza dati con file CSV

Autenticazione sicura con hash BCrypt

Interfaccia responsive con JavaFX

Gestione errori e validazione input

🤝 Contribuire
Le pull request sono benvenute. Per cambiamenti importanti, apri prima una issue per discutere cosa vorresti cambiare.

📄 Licenza
Questo progetto è concesso in licenza con la Licenza MIT. Vedi il file LICENSE per maggiori dettagli.

📞 Supporto
Per problemi o domande, apri una issue sulla repository GitHub o contatta [il tuo indirizzo email].

The Knife - Scopri, recensisci, condividi l'esperienza culinaria
