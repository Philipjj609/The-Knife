# The Knife

Applicazione JavaFX client-server per consultare e gestire ristoranti,
recensioni e preferiti. Il catalogo di partenza proviene dalla Guida Michelin.

Il progetto è organizzato come build Maven multi-modulo:

| Modulo | Contenuto |
|--------|-----------|
| `theknife-common` | Modelli di dominio, DTO di rete, enum, validatore condiviso |
| `theknife-server` | Server TCP multi-thread, DAO JDBC, persistenza PostgreSQL |
| `theknife-client` | GUI JavaFX; comunica col server esclusivamente tramite `ClientTK` |

## Funzionalità

- Dialogo di connessione all'avvio: l'utente specifica host e porta del server.
- **Ospite**: ricerca con filtri avanzati (selezione multipla), scheda dettaglio ristorante, mappa, recensioni.
- **Cliente registrato**: preferiti, scrittura/modifica/cancellazione recensioni, dashboard personale.
- **Ristoratore registrato**: inserimento locali, risposta/modifica/cancellazione risposte, dashboard aggregata.
- Password hashate con BCrypt lato server; hash mai trasmesso al client.
- Autorizzazione per ruolo verificata inline nel server a ogni richiesta.
- Ping monitor ogni 2 secondi: riconnessione automatica in caso di caduta del server.

## Requisiti

- JDK 17 o superiore
- Maven 3.9 o superiore
- PostgreSQL 13 o superiore (solo sulla macchina server)

## Database

Il server applica lo schema e importa il CSV automaticamente al primo avvio.
È necessario solo creare il database prima di avviare il server:

```sql
CREATE DATABASE dbtk;
```

Le credenziali di connessione si configurano tramite variabili d'ambiente
(hanno la precedenza) oppure modificando il file di properties:

```
theknife-server/src/main/resources/db.properties
```

Variabili d'ambiente supportate:

```
THEKNIFE_DB_URL
THEKNIFE_DB_USER
THEKNIFE_DB_PASSWORD
```

Il file CSV Michelin deve trovarsi nel percorso atteso dalla directory di
lavoro al momento dell'avvio del server (configurabile in `db.properties`).

## Build

```bash
mvn clean package
```

## Esecuzione

Avviare prima il server:

```bash
java -jar theknife-server/target/theknife-server-shaded.jar
```

Poi avviare il client:

```bash
java -jar theknife-client/target/theknife-client-shaded.jar
```

All'avvio il client mostra un dialogo grafico che chiede host e porta.
Valori predefiniti: `localhost` e `9090`.

## Note architetturali

- Il client non accede mai al database e non legge file CSV: tutte le
  operazioni passano dal server tramite oggetti `Richiesta` / `Esito`
  serializzati su TCP.
- Il server usa DAO stateless con connection pool HikariCP (max 10
  connessioni). Ogni client connesso è gestito da un thread del pool
  fisso (max 20 thread).
- I filtri di ricerca sono applicati client-side su dati caricati
  all'apertura della schermata, senza ulteriori richieste al server.
- `MultiSelectComboBox` è un controllo JavaFX custom che estende
  `Control` e permette la selezione multipla con filtro testuale.
