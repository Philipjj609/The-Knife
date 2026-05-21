package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Ristorante;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la vista dettagliata di un ristorante.
 * Usa ClientTK per recensioni e preferiti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */
/**
 * Controller JavaFX per la vista di dettaglio di un ristorante.
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Gestisce la visualizzazione di tutte le informazioni dettagliate di un ristorante
 * (nome, cucina, stelle Michelin, fascia di prezzo, contatti, descrizione, servizi aggiuntivi),
 * le quali sono integrate in un header "sticky" (fisso) in alto per ottimizzare lo spazio
 * visivo ed evitare card duplicate all'interno dell'area di scorrimento. Permette inoltre
 * l'apertura del sito web o della mappa geografica tramite browser esterno, l'inserimento
 * e rimozione del ristorante dai preferiti, e l'elenco delle recensioni con relative risposte.
 * Comunica con il server in modo non bloccante per l'interfaccia grafica tramite {@link Task}
 * o thread dedicati e aggiorna gli elementi UI in modo asincrono sul JavaFX Application Thread.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.1
 */
public class DettaglioRistoranteController implements Initializable {

    /** Label per mostrare il nome del ristorante. */
    @FXML private Text nameLabel;

    /** Label per mostrare le specialità culinarie/cucine. */
    @FXML private Text cuisineLabel;

    /** Label per mostrare la valutazione o stelle Michelin assegnate. */
    @FXML private Text starsLabel;

    /** Label per mostrare la fascia di prezzo del ristorante. */
    @FXML private Text priceLabel;

    /** Label per mostrare l'indirizzo civico. */
    @FXML private Text addressLabel;

    /** Label per mostrare la città/nazione. */
    @FXML private Text locationLabel;

    /** Label per mostrare il numero di telefono. */
    @FXML private Text phoneLabel;

    /** Collegamento ipertestuale per navigare al sito internet ufficiale del ristorante. */
    @FXML private Hyperlink websiteLink;

    /** Area di testo non modificabile per leggere la descrizione del ristorante. */
    @FXML private TextArea descriptionArea;

    /** Contenitore principale per la card dei premi e riconoscimenti. */
    @FXML private VBox premiCard;

    /** Contenitore grafico per i dettagli del premio Michelin (Bib Gourmand, stelle). */
    @FXML private VBox awardBox;

    /** Label che mostra il testo del premio Michelin ottenuto. */
    @FXML private Text awardLabel;

    /** Contenitore grafico per il badge "Stella Verde Michelin". */
    @FXML private VBox greenStarBox;

    /** Label che descrive la Stella Verde. */
    @FXML private Text greenStarLabel;

    /** Label per mostrare la media e la visualizzazione testuale delle stelle di recensione. */
    @FXML private Text mediaRecensioniLabel;

    /** Pulsante per consentire l'inserimento di una recensione. */
    @FXML private Button aggiungiRecensione;

    /** ListView contenente tutte le recensioni lasciate dagli utenti per questo ristorante. */
    @FXML private ListView<Recensione> recensioniListView;

    /** Pulsante per inserire o rimuovere il ristorante dall'elenco preferiti dell'utente. */
    @FXML private Button aggiungiPreferiti;

    /** Pulsante per aprire la finestra di dialogo con la mappa. */
    @FXML private Button visualizzaMappa;

    /** Pulsante per mostrare il telefono ed avviare la chiamata. */
    @FXML private Button chiamaRistorante;

    /** Label indicante se è attivo il servizio di consegna a domicilio (delivery). */
    @FXML private Label deliveryStatusLabel;

    /** Label indicante se è supportata la prenotazione online dei tavoli. */
    @FXML private Label prenotazioneOnlineStatusLabel;

    /** Area di testo per mostrare i servizi e le infrastrutture del ristorante. */
    @FXML private TextArea facilitiesArea;

    /** Il ristorante correntemente visualizzato. */
    private Ristorante ristorante;

    /** Lo username dell'utente attualmente loggato. */
    private String currentUser;

    /** True se chi visualizza è il ristoratore proprietario: nasconde recensioni e preferiti. */
    private boolean isProprietario = false;

    /** Riferimento opzionale al controller padre della dashboard cliente per favorire l'aggiornamento reciproco. */
    private DashboardClienteController dashboardClienteParentController;

    /**
     * Inizializza il controller JavaFX. Imposta la cella personalizzata della ListView delle recensioni.
     * Metodo richiamato automaticamente dopo il caricamento del file FXML.
     *
     * @param location l'URL di localizzazione della risorsa FXML
     * @param resources il bundle delle risorse localizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        recensioniListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Recensione r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRecensioneCard(r));
            }
        });
    }

    /**
     * Associa il ristorante da mostrare ed avvia il popolamento dei campi visivi,
     * il caricamento asincrono delle recensioni e lo stato del pulsante preferiti.
     *
     * @param ristorante il ristorante {@link Ristorante} da visualizzare
     */
    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
        populateFields();
        loadRecensioni();
        updateFavoritesButton();
    }

    /**
     * Associa lo username dell'utente loggato per determinare la visibilità
     * dei pulsanti interattivi (recensione e preferito).
     *
     * @param username il nome utente autenticato
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateRecensioneButton();
        updateFavoritesButton();
    }

    public void setIsProprietario(boolean isProprietario) {
        this.isProprietario = isProprietario;
        updateRecensioneButton();
        updateFavoritesButton();
    }

    /**
     * Associa il controller genitore della dashboard cliente.
     *
     * @param parentController il {@link DashboardClienteController} padre
     */
    public void setDashboardClienteParentController(DashboardClienteController parentController) {
        this.dashboardClienteParentController = parentController;
    }

    /**
     * Mostra o nasconde i pulsanti "Aggiungi Recensione" e "Aggiungi ai Preferiti"
     * a seconda che l'utente sia registrato e autenticato (username non nullo).
     */
    private void updateRecensioneButton() {
        boolean show = currentUser != null && !isProprietario;
        aggiungiRecensione.setVisible(show);
        aggiungiRecensione.setManaged(show);
        aggiungiPreferiti.setVisible(show);
        aggiungiPreferiti.setManaged(show);
    }

    /**
     * Avvia un thread secondario che richiede al server (tramite {@link ClientTK}) l'elenco delle recensioni
     * associate al ristorante. All'ottenimento, riempie la ListView e calcola la media stelle nella UI.
     */
    private void loadRecensioni() {
        if (ristorante == null) return;

        Task<List<Recensione>> task = new Task<>() {
            @Override
            protected List<Recensione> call() {
                return Main.getClient().getRecensioniRistorante(ristorante.getId());
            }
        };

        task.setOnSucceeded(e -> {
            List<Recensione> recensioni = task.getValue();
            recensioniListView.setItems(FXCollections.observableArrayList(recensioni));
            recensioniListView.setVisible(!recensioni.isEmpty());

            if (!recensioni.isEmpty()) {
                double media = recensioni.stream().mapToInt(Recensione::getValutazione).average().orElse(0.0);
                String stelle = "★".repeat((int) Math.round(media)) + "☆".repeat(5 - (int) Math.round(media));
                mediaRecensioniLabel.setText(String.format("%s (%.1f/5)", stelle, media));
            } else {
                mediaRecensioniLabel.setText("Nessuna valutazione");
            }
        });

        new Thread(task).start();
    }

    /**
     * Crea graficamente la card (VBox) di visualizzazione di una recensione lasciata da un utente.
     * Mostra stelle, autore, data, titolo, corpo del commento ed eventuale risposta del ristoratore.
     *
     * @param recensione l'oggetto {@link Recensione} da formattare
     * @return il contenitore grafico {@link VBox} della recensione
     */
    private VBox createRecensioneCard(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text stelle = new Text(recensione.getStelle());
        stelle.setStyle("-fx-font-size: 16; -fx-fill: #f39c12;");

        Text utente = new Text("di " + recensione.getUsernameCliente());
        utente.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        Text data = new Text(recensione.getDataRecensioneFormatted());
        data.setStyle("-fx-font-size: 10; -fx-fill: #6c757d;");

        header.getChildren().addAll(stelle, utente, data);

        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Text commento = new Text(recensione.getCommento());
        commento.setStyle("-fx-font-size: 12;");
        commento.setWrappingWidth(400);

        card.getChildren().addAll(header, titolo, commento);

        // Se l'utente correntemente loggato coincide con l'autore della recensione, mostra i bottoni di modifica/eliminazione
        if (currentUser != null && currentUser.equalsIgnoreCase(recensione.getUsernameCliente())) {
            HBox azioni = new HBox(8);
            Button modificaBtn = new Button("✏ Modifica");
            modificaBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10;");
            modificaBtn.setOnAction(e -> apriModificaRecensione(recensione));

            Button eliminaBtn = new Button("🗑 Elimina");
            eliminaBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10;");
            eliminaBtn.setOnAction(e -> confermaEliminaRecensione(recensione));

            azioni.getChildren().addAll(modificaBtn, eliminaBtn);
            card.getChildren().add(azioni);
        }

        if (recensione.getRisposta() != null) {
            VBox rispostaBox = new VBox(5);
            rispostaBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-border-radius: 5;");

            Text rispostaHeader = new Text("Risposta del ristoratore (" +
                    recensione.getRisposta().getDataRispostaFormatted() + "):");
            rispostaHeader.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-fill: #495057;");

            Text rispostaTesto = new Text(recensione.getRisposta().getTesto());
            rispostaTesto.setStyle("-fx-font-size: 11; -fx-fill: #495057;");
            rispostaTesto.setWrappingWidth(380);

            rispostaBox.getChildren().addAll(rispostaHeader, rispostaTesto);
            card.getChildren().add(rispostaBox);
        }

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
            javafx.application.Platform.runLater(() -> {
                try {
                    AppNavigator.show("/views/aggiungiRecensione.fxml", (AggiungiRecensioneController controller) -> {
                        controller.setRistorante(ristorante);
                        controller.setCurrentUser(currentUser);
                        controller.setRecensioneEsistente(recensione);
                        controller.setParentController(this);
                        if (dashboardClienteParentController != null) {
                            controller.setParentController(dashboardClienteParentController);
                        }
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
        confirm.setHeaderText("Elimina la tua recensione?");
        confirm.setContentText("Questa azione non può essere annullata.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    Main.getClient().eliminaRecensione(recensione.getId(), currentUser);
                    javafx.application.Platform.runLater(() -> {
                        loadRecensioni();
                        if (dashboardClienteParentController != null) {
                            dashboardClienteParentController.refreshData();
                        }
                    });
                }).start();
            }
        });
    }

    /**
     * Gestisce l'evento di visualizzazione della mappa stradale di Google Maps in base alle coordinate.
     */
    @FXML private void handleOpenMap() {
        if (ristorante != null && (ristorante.getLatitudine() != 0.0 || ristorante.getLongitudine() != 0.0)) {
            String mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    ristorante.getLatitudine(), ristorante.getLongitudine());
            openUrl(mapUrl);
        }
    }

    /**
     * Gestisce l'apertura del sito internet ufficiale del ristorante.
     */
    @FXML private void handleOpenWebsite() {
        if (ristorante != null && ristorante.getSitoWeb() != null
                && !ristorante.getSitoWeb().trim().isEmpty()
                && !ristorante.getSitoWeb().equalsIgnoreCase("N/A")) {
            openUrl(ristorante.getSitoWeb());
        }
    }

    /**
     * Chiude la schermata corrente di dettaglio tornando a quella precedente.
     */
    @FXML private void handleClose() {
        AppNavigator.goBackOrClose(nameLabel);
    }

    /**
     * Listener helper per aprire l'URL del sito web all'attivazione del link ipertestuale.
     *
     * @param event l'evento d'azione associato
     */
    private void openWebsite(ActionEvent event) {
        if (ristorante != null && ristorante.getSitoWeb() != null) openUrl(ristorante.getSitoWeb());
    }

    /**
     * Tenta l'apertura di un URL esterno delegando la navigazione al browser web predefinito del sistema.
     *
     * @param url la stringa dell'URL da aprire
     */
    private void openUrl(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (IOException | URISyntaxException e) {
            showAlert("Errore", "Impossibile aprire l'URL", e.getMessage());
        }
    }

    /**
     * Gestisce l'evento di click sul pulsante recensione. Richiede l'autenticazione ed apre
     * la finestra di composizione recensione {@link AggiungiRecensioneController}.
     */
    @FXML
    private void handleAggiungiRecensione() {
        if (currentUser == null) {
            showAlert("Accesso Richiesto", "Devi essere autenticato",
                    "Per lasciare una recensione devi prima effettuare l'accesso.");
            return;
        }

        try {
            AppNavigator.show("/views/aggiungiRecensione.fxml", (AggiungiRecensioneController controller) -> {
                controller.setRistorante(ristorante);
                controller.setCurrentUser(currentUser);
                controller.setParentController(this);

                if (dashboardClienteParentController != null) {
                    controller.setParentController(dashboardClienteParentController);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gestisce l'aggiunta o la rimozione del ristorante dall'elenco preferiti dell'utente corrente.
     * L'operazione avviene in background con callback di aggiornamento del bottone preferito
     * nel JavaFX Thread.
     */
    @FXML
    private void handleAggiungiPreferiti() {
        if (currentUser == null) {
            showAlert("Accesso Richiesto", "Devi essere autenticato",
                    "Per aggiungere ai preferiti devi prima effettuare l'accesso.");
            return;
        }
        if (ristorante == null) return;

        new Thread(() -> {
            boolean isFav = Main.getClient().isPreferito(currentUser, ristorante.getId());
            if (isFav) {
                Main.getClient().rimuoviPreferito(currentUser, ristorante.getId());
                javafx.application.Platform.runLater(() -> {
                    aggiungiPreferiti.setText("❤️ Aggiungi ai Preferiti");
                    showAlert("Rimosso", "Ristorante rimosso dai preferiti",
                            ristorante.getNome() + " è stato rimosso dai tuoi preferiti.");
                });
            } else {
                Main.getClient().aggiungiPreferito(currentUser, ristorante.getId());
                javafx.application.Platform.runLater(() -> {
                    aggiungiPreferiti.setText("💔 Rimuovi dai Preferiti");
                    showAlert("Aggiunto", "Ristorante aggiunto ai preferiti",
                            ristorante.getNome() + " è stato aggiunto ai tuoi preferiti.");
                });
            }
        }).start();
    }

    /**
     * Gestisce l'evento di visualizzazione della mappa FXML localmente all'applicazione.
     */
    @FXML
    private void handleVisualizzaMappa() {
        if (ristorante != null && (ristorante.getLatitudine() != 0.0 || ristorante.getLongitudine() != 0.0)) {
            try {
                AppNavigator.show("/views/mapDialog.fxml", (MapDialogController controller) ->
                        controller.setRestaurant(ristorante));
            } catch (IOException e) {
                showAlert("Errore", "Impossibile caricare la mappa", e.getMessage());
            }
        } else {
            showAlert("Informazione", "Posizione non disponibile",
                    "Le coordinate GPS per questo ristorante non sono disponibili.");
        }
    }

    /**
     * Mostra una finestra di avviso con il numero telefonico del ristorante per l'utente.
     */
    @FXML
    private void handleChiamaRistorante() {
        if (ristorante != null && ristorante.getTelefono() != null
                && !ristorante.getTelefono().trim().isEmpty()
                && !ristorante.getTelefono().equalsIgnoreCase("N/A")) {
            showAlert("Numero di Telefono", "Chiama il ristorante",
                    "Numero: " + ristorante.getTelefono());
        } else {
            showAlert("Informazione", "Numero non disponibile",
                    "Il numero di telefono per questo ristorante non è disponibile.");
        }
    }

    /**
     * Forza il ricaricamento asincrono dell'elenco recensioni aggiornando la vista.
     */
    public void refreshRecensioni() { loadRecensioni(); }

    /**
     * Interroga in asincrono lo stato del database per capire se il ristorante è segnato preferito dell'utente,
     * aggiornando il testo del pulsante associato.
     */
    private void updateFavoritesButton() {
        if (currentUser != null && ristorante != null) {
            new Thread(() -> {
                boolean isFav = Main.getClient().isPreferito(currentUser, ristorante.getId());
                javafx.application.Platform.runLater(() ->
                        aggiungiPreferiti.setText(isFav ? "💔 Rimuovi dai Preferiti" : "❤️ Aggiungi ai Preferiti"));
            }).start();
        }
    }

    /**
     * Popola tutti i componenti grafici testuali, hyperlink,badge e aree di testo
     * caricando i dati del ristorante correntemente associato.
     */
    private void populateFields() {
        if (ristorante == null) return;

        nameLabel.setText(ristorante.getNome());
        cuisineLabel.setText(String.join(", ", ristorante.getCucine()));

        int stars = ristorante.getStarCount();
        starsLabel.setText(stars > 0 ? "★".repeat(stars) + " Michelin" : "Non classificato");

        priceLabel.setText(ristorante.getPrezzoStringa());
        addressLabel.setText(ristorante.getIndirizzo());
        locationLabel.setText(ristorante.getCitta() + (ristorante.getNazione() != null ? ", " + ristorante.getNazione() : ""));
        phoneLabel.setText(ristorante.getTelefono() != null ? ristorante.getTelefono() : "N/A");

        if (ristorante.getSitoWeb() != null && !ristorante.getSitoWeb().trim().isEmpty()
                && !ristorante.getSitoWeb().equalsIgnoreCase("N/A")) {
            websiteLink.setText(ristorante.getSitoWeb());
            websiteLink.setOnAction(this::openWebsite);
        } else {
            websiteLink.setText("Non disponibile");
            websiteLink.setDisable(true);
        }

        descriptionArea.setText(ristorante.getDescrizione());

        List<String> servizi = ristorante.getServizi();
        facilitiesArea.setText(servizi != null && !servizi.isEmpty()
                ? String.join(", ", servizi)
                : "Nessuna informazione sui servizi disponibile.");

        String rico = ristorante.getRiconoscimento();
        if (rico != null && !rico.trim().isEmpty() && !rico.equalsIgnoreCase("N/A")) {
            awardLabel.setText(rico);
            awardBox.setVisible(true);
            awardBox.setManaged(true);
        } else {
            awardBox.setVisible(false);
            awardBox.setManaged(false);
        }

        if (ristorante.isGreenStar()) {
            greenStarLabel.setText("Green Star");
            greenStarBox.setVisible(true);
            greenStarBox.setManaged(true);
        } else {
            greenStarBox.setVisible(false);
            greenStarBox.setManaged(false);
        }

        boolean hasAwards = awardBox.isVisible() || greenStarBox.isVisible();
        premiCard.setVisible(hasAwards);
        premiCard.setManaged(hasAwards);

        updateServicesDisplay();
    }

    /**
     * Aggiorna lo stile cromatico e testuale per i badge asporto/consegna (delivery)
     * e prenotazione online.
     */
    private void updateServicesDisplay() {
        if (ristorante == null) return;

        deliveryStatusLabel.setText(ristorante.isDelivery() ? "Sì" : "No");
        deliveryStatusLabel.setStyle(ristorante.isDelivery()
                ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
                : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        prenotazioneOnlineStatusLabel.setText(ristorante.isPrenotazioneOnline() ? "Sì" : "No");
        prenotazioneOnlineStatusLabel.setStyle(ristorante.isPrenotazioneOnline()
                ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
                : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
    }

    /**
     * Helper per mostrare messaggi informativi a schermo tramite finestre di dialogo JavaFX.
     *
     * @param title titolo del dialogo
     * @param header testata del dialogo
     * @param message corpo del messaggio
     */
    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
