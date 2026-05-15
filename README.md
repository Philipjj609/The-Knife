# The Knife - Laboratorio B

Applicazione JavaFX client-server per consultare e gestire ristoranti, recensioni e preferiti.

La Parte B separa la vecchia applicazione monolitica in tre moduli Maven:

- `theknife-common`: modelli condivisi e protocollo di rete serializzabile.
- `theknife-server`: server TCP multi-thread, DAO JDBC e persistenza PostgreSQL.
- `theknife-client`: GUI JavaFX che comunica solo con il server tramite `ClientTK`.

## Funzionalita

- Avvio client con connessione a un server configurabile.
- Schermata iniziale obbligatoria con scelta tra accesso e ingresso ospite.
- Ospite: ricerca, filtri, dettaglio ristorante, recensioni e mappa.
- Cliente: recensioni, preferiti e dashboard personale.
- Ristoratore: inserimento ristoranti, dashboard dei propri locali e risposta alle recensioni.
- Password hashate con BCrypt lato server.
- Accessi alle operazioni personali controllati dal server sulla sessione autenticata.

## Requisiti

- JDK 17 o superiore.
- Maven 3.9 o superiore.
- PostgreSQL 17 o compatibile.

## Database

Creare il database e applicare lo schema:

```powershell
createdb -U postgres theknife
psql -U postgres -d theknife -f database/schema.sql
```

Per popolare il catalogo Michelin dal CSV:

```powershell
powershell -ExecutionPolicy Bypass -File database/setup_database.ps1 -CsvPath "C:\Users\phili\Downloads\michelin_my_maps.csv"
```

Lo script crea il database se manca, applica `database/schema.sql` e importa il CSV normalizzando cucine e servizi nelle rispettive tabelle.

La configurazione predefinita del server e in:

```text
theknife-server/src/main/resources/db.properties
```

Si possono anche usare variabili d'ambiente, che hanno precedenza sul file:

- `THEKNIFE_DB_URL`
- `THEKNIFE_DB_USER`
- `THEKNIFE_DB_PASSWORD`

## Build

```powershell
mvn clean package
```

## Esecuzione

Avviare prima il server:

```powershell
java -jar theknife-server/target/theknife-server-1.0.0-shaded.jar
```

Poi avviare il client:

```powershell
mvn -pl theknife-client javafx:run
```

All'avvio il client chiede host e porta del server. I valori predefiniti sono:

- host: `localhost`
- porta: `9090`

## Note Architetturali

Il client non accede direttamente al database e non legge file CSV. Tutte le operazioni applicative passano dal server tramite richieste `Richiesta` e risposte `Esito`.

Il server usa DAO stateless e un connection pool HikariCP; ogni client connesso viene gestito da un thread del pool.
