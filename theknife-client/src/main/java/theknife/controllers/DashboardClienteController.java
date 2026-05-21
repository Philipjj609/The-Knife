package theknife.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Ristorante;
import theknife.models.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller JavaFX per la dashboard riservata all'utente con ruolo "Cliente".
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Gestisce la visualizzazione delle informazioni sul profilo dell'utente loggato,
 * il riepilogo delle sue statistiche personali (numero recensioni, media valutazioni, numero preferiti),
 * e gli elenchi personali di recensioni scritte e ristoranti preferiti tramite componenti {@link ListView}.
 * Interagisce asincronamente con la Facade {@link ClientTK} per l'ottenimento dei dati e la gestione delle
 * operazioni di modifica ed eliminazione recensioni o rimozione dai preferiti, impiegando thread in background
 * e {@link Platform#runLater(Runnable)} per garantire l'aggiornamento sicuro e reattivo dell'interfaccia utente.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */
public class DashboardClienteController implements Initializable {

    /** Testo di benvenuto personalizzato con il nome dell'utente. */
    @FXML private Text benvenutoLabel;

    /** Contatore delle recensioni inserite dall'utente. */
    @FXML private Text numRecensioniLabel;

    /** Valore medio delle valutazioni date dall'utente con rappresentazione a stelle. */
    @FXML private Text mediaValutazioniLabel;

    /** Contatore dei ristoranti salvati nei preferiti dell'utente. */
    @FXML private Text preferitiLabel;

    /** Lista grafica per la visualizzazione delle recensioni scritte dall'utente. */
    @FXML private ListView<Recensione> recensioniListView;

    /** Lista grafica per la visualizzazione dei ristoranti preferiti salvati dall'utente. */
    @FXML private ListView<Ristorante> preferitiListView;

    /** Messaggio mostrato qualora la lista delle recensioni sia vuota. */
    @FXML private Label nessueRecensioniLabel;

    /** Messaggio mostrato qualora la lista dei preferiti sia vuota. */
    @FXML private Label nessunPreferitoLabel;

    /** L'utente cliente correntemente loggato. */
    private Utente currentUser;

    /**
     * Inizializza il controller JavaFX. Imposta i custom cell factory per le ListView
     * al fine di rendere le schede personalizzate (card) per recensioni e preferiti.
     * Metodo richiamato automaticamente dopo il caricamento del file FXML.
     *
     * @param location l'URL di localizzazione del file FXML
     * @param resources il bundle di risorse localizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRecensioniListView();
        setupPreferitiListView();
    }

    /**
     * Associa l'utente cliente correntemente loggato ed avvia il caricamento dei suoi dati dal server.
     *
     * @param user l'utente {@link Utente} loggato
     */
    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        loadUserData();
    }

    /**
     * Configura la visualizzazione personalizzata degli elementi della ListView delle recensioni.
     */
    private void setupRecensioniListView() {
        recensioniListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Recensione r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRecensioneCard(r));
            }
        });
    }

    /**
     * Configura la visualizzazione personalizzata degli elementi della ListView dei preferiti.
     */
    private void setupPreferitiListView() {
        preferitiListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRistoranteCard(r));
            }
        });
    }

    /**
     * Avvia un thread in background per scaricare in modo asincrono dal server (tramite {@link ClientTK})
     * l'elenco delle recensioni e dei preferiti associati all'utente corrente.
     * I dati ottenuti vengono poi impostati sulle rispettive ListView e utilizzati per calcolare
     * le statistiche nel JavaFX Application Thread.
     */
    private void loadUserData() {
        if (currentUser == null) return;

        benvenutoLabel.setText("Benvenuto, " + currentUser.getNome() + "!");

        Task<Void> task = new Task<>() {
            private List<Recensione> recensioni = List.of();
            private List<Ristorante> preferiti  = List.of();

            @Override
            protected Void call() {
                try {
                    recensioni = Main.getClient().getRecensioniCliente(currentUser.getUsername());
                } catch (Exception e) {
                    System.err.println("[Dashboard] Errore caricamento recensioni: " + e.getMessage());
                }
                try {
                    preferiti = Main.getClient().getPreferiti(currentUser.getUsername());
                } catch (Exception e) {
                    System.err.println("[Dashboard] Errore caricamento preferiti: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void succeeded() {
                recensioniListView.setItems(FXCollections.observableArrayList(recensioni));
                preferitiListView.setItems(FXCollections.observableArrayList(preferiti));
                updateStatistiche(recensioni, preferiti);
                nessueRecensioniLabel.setVisible(recensioni.isEmpty());
                nessunPreferitoLabel.setVisible(preferiti.isEmpty());
            }
        };
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex == null) {
                System.err.println("[Dashboard] Task fallito senza eccezione");
            } else {
                System.err.println("[Dashboard] Errore: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });
        new Thread(task).start();
    }

    /**
     * Calcola e aggiorna le informazioni statistiche dell'utente (numero recensioni, preferiti, media stelle)
     * mostrandole nei rispettivi nodi di testo della UI.
     *
     * @param recensioni la lista delle recensioni dell'utente
     * @param preferiti la lista dei ristoranti preferiti dell'utente
     */
    private void updateStatistiche(List<Recensione> recensioni, List<Ristorante> preferiti) {
        numRecensioniLabel.setText(String.valueOf(recensioni.size()));
        preferitiLabel.setText(String.valueOf(preferiti.size()));

        if (recensioni.isEmpty()) {
            mediaValutazioniLabel.setText("N/A");
        } else {
            double media = recensioni.stream().mapToInt(Recensione::getValutazione).average().orElse(0.0);
            String stelle = "★".repeat((int) Math.round(media)) + "☆".repeat(5 - (int) Math.round(media));
            mediaValutazioniLabel.setText(String.format("%.1f %s", media, stelle));
        }
    }

    /**
     * Crea dinamicamente un elemento grafico (VBox) che rappresenta la card di una recensione.
     * Include dettagli come nome ristorante, valutazione a stelle, data, titolo, corpo troncato,
     * l'indicazione di un'eventuale risposta del ristoratore, pulsanti di modifica/eliminazione
     * ed il supporto al doppio click per navigare al ristorante recensito.
     *
     * @param recensione l'oggetto {@link Recensione} da mostrare
     * @return il contenitore grafico {@link VBox} configurato
     */
    private VBox createRecensioneCard(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text ristorante = new Text(recensione.getNomeRistorante());
        ristorante.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text stelle = new Text(recensione.getStelle());
        stelle.setStyle("-fx-font-size: 14; -fx-fill: #f39c12;");

        Text data = new Text(recensione.getDataRecensioneFormatted());
        data.setStyle("-fx-font-size: 10; -fx-fill: #6c757d;");

        header.getChildren().addAll(ristorante, stelle, data);

        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        String commentoTroncato = recensione.getCommento().length() > 100
                ? recensione.getCommento().substring(0, 100) + "..."
                : recensione.getCommento();
        Text commento = new Text(commentoTroncato);
        commento.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        if (recensione.getRisposta() != null) {
            Text risposta = new Text("💬 Il ristoratore ha risposto");
            risposta.setStyle("-fx-font-size: 11; -fx-fill: #28a745; -fx-font-style: italic;");
            card.getChildren().addAll(header, titolo, commento, risposta);
        } else {
            card.getChildren().addAll(header, titolo, commento);
        }

        // Bottoni modifica / elimina
        HBox azioni = new HBox(8);
        Button modificaBtn = new Button("✏ Modifica");
        modificaBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10;");
        modificaBtn.setOnAction(e -> apriModificaRecensione(recensione));

        Button eliminaBtn = new Button("🗑 Elimina");
        eliminaBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10;");
        eliminaBtn.setOnAction(e -> confermaEliminaRecensione(recensione));

        azioni.getChildren().addAll(modificaBtn, eliminaBtn);
        card.getChildren().add(azioni);

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                apriDettaglioRistorante(recensione.getRistoranteId(), recensione.getNomeRistorante());
            }
        });
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");
        return card;
    }

    /**
     * Richiede asincronamente i dati completi del ristorante associato alla recensione dal server,
     * quindi apre in modo asincrono (su JavaFX Thread) la schermata per la modifica della recensione.
     *
     * @param recensione la recensione {@link Recensione} da modificare
     */
    private void apriModificaRecensione(Recensione recensione) {
        new Thread(() -> Main.getClient().getRistorante(recensione.getRistoranteId()).ifPresent(ristorante ->
            Platform.runLater(() -> {
                try {
                    AppNavigator.show("/views/aggiungiRecensione.fxml", (AggiungiRecensioneController controller) -> {
                        controller.setRistorante(ristorante);
                        controller.setCurrentUser(currentUser.getUsername());
                        controller.setRecensioneEsistente(recensione);
                        controller.setParentController(this);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            })
        )).start();
    }

    /**
     * Mostra una finestra di dialogo di conferma per procedere all'eliminazione di una recensione.
     * Se confermato, avvia un thread in background che notifica il server tramite la Facade di rete
     * e aggiorna la schermata principale al termine del processo.
     *
     * @param recensione la recensione {@link Recensione} da eliminare
     */
    private void confermaEliminaRecensione(Recensione recensione) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Elimina Recensione");
        confirm.setHeaderText("Elimina la recensione per " + recensione.getNomeRistorante() + "?");
        confirm.setContentText("Questa azione non può essere annullata.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    Main.getClient().eliminaRecensione(recensione.getId(), currentUser.getUsername());
                    Platform.runLater(this::loadUserData);
                }).start();
            }
        });
    }

    /**
     * Crea dinamicamente un elemento grafico (VBox) che rappresenta la card di un ristorante preferito.
     * Mostra dettagli come nome, tipo di cucina, stelle Michelin, città, fascia di prezzo
     * e un pulsante per rimuovere il ristorante dai preferiti, oltre al supporto al doppio click per navigare al dettaglio.
     *
     * @param ristorante il {@link Ristorante} preferito da rappresentare
     * @return la VBox contenente la scheda grafica del ristorante
     */
    private VBox createRistoranteCard(Ristorante ristorante) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        Text nome = new Text(ristorante.getNome());
        nome.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text cucina = new Text(String.join(", ", ristorante.getCucine()));
        cucina.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        HBox stelleBox = new HBox(5);
        if (ristorante.getStarCount() > 0) {
            Text s = new Text("★".repeat(ristorante.getStarCount()) + " Michelin");
            s.setStyle("-fx-font-size: 12; -fx-fill: #f39c12;");
            stelleBox.getChildren().add(s);
        }
        if (ristorante.isGreenStar()) {
            Text gs = new Text("🌟 Green Star");
            gs.setStyle("-fx-font-size: 12; -fx-fill: #27ae60;");
            stelleBox.getChildren().add(gs);
        }

        Text localita = new Text(ristorante.getCitta() + (ristorante.getNazione() != null ? ", " + ristorante.getNazione() : ""));
        localita.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        Text prezzo = new Text(ristorante.getPrezzoStringa());
        prezzo.setStyle("-fx-font-size: 12; -fx-fill: #e74c3c; -fx-font-weight: bold;");

        Button rimuoviBtn = new Button("❤️ Rimuovi");
        rimuoviBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10;");
        rimuoviBtn.setOnAction(event -> {
            new Thread(() -> {
                Main.getClient().rimuoviPreferito(currentUser.getUsername(), ristorante.getId());
                javafx.application.Platform.runLater(this::loadUserData);
            }).start();
        });

        card.getChildren().addAll(nome, cucina, stelleBox, localita, prezzo, rimuoviBtn);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                apriDettaglioRistorante(ristorante.getId(), ristorante.getNome());
            }
        });
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");
        return card;
    }

    /**
     * Gestisce l'evento di navigazione verso la schermata di esplorazione/ricerca dei ristoranti.
     */
    @FXML
    private void handleEsploraRistoranti() {
        try {
            AppNavigator.show("/views/esploraRistoranti.fxml", (EsploraRistorantiController controller) -> {
                controller.setSessionState(true, currentUser);
                controller.setParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Avvia un thread in background per richiedere al server i dati completi di un ristorante
     * e apre (su JavaFX Application Thread) la relativa schermata di dettaglio.
     *
     * @param ristoranteId l'identificativo univoco del ristorante
     * @param nomeRistorante il nome del ristorante
     */
    private void apriDettaglioRistorante(long ristoranteId, String nomeRistorante) {
        new Thread(() -> {
            Main.getClient().getRistorante(ristoranteId).ifPresent(ristorante ->
                javafx.application.Platform.runLater(() -> {
                    try {
                        AppNavigator.show("/views/dettaglioRistorante.fxml", (DettaglioRistoranteController controller) -> {
                            controller.setRistorante(ristorante);
                            controller.setCurrentUser(currentUser.getUsername());
                            controller.setDashboardClienteParentController(this);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
            );
        }).start();
    }

    /**
     * Ricarica e aggiorna tutti i dati dell'utente sincronizzandoli con lo stato attuale sul server.
     */
    public void refreshData() {
        loadUserData();
    }
}
