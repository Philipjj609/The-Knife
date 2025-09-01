package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import theknife.models.Recensione;
import theknife.models.Ristorante;
import theknife.services.PreferitiManager;
import theknife.services.RecensioniManager;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la vista dettagliata di un ristorante.
 * Popola i campi della UI con le informazioni del ristorante,
 * gestisce le azioni come aprire il sito, visualizzare la mappa,
 * aggiungere ai preferiti o lasciare una recensione.
 *
 * Fornisce anche metodi di aggiornamento per le recensioni e i servizi.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class DettaglioRistoranteController implements Initializable {

    @FXML
    private Text nameLabel;
    @FXML
    private Text cuisineLabel;
    @FXML
    private Text starsLabel;
    @FXML
    private Text priceLabel;
    @FXML
    private Text addressLabel;
    @FXML
    private Text locationLabel;
    @FXML
    private Text phoneLabel;
    @FXML
    private Hyperlink websiteLink;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private VBox awardBox;
    @FXML
    private Text awardLabel;
    @FXML
    private VBox greenStarBox;
    @FXML
    private Text greenStarLabel;
    @FXML
    private Text mediaRecensioniLabel;
    @FXML
    private Button aggiungiRecensione;
    @FXML
    private ListView<Recensione> recensioniListView;
    @FXML
    private Button aggiungiPreferiti;
    @FXML
    private Button visualizzaMappa;
    @FXML
    private Button chiamaRistorante;

    // Nuovi campi per i servizi semplificati
    @FXML
    private Label deliveryStatusLabel;
    @FXML
    private Label prenotazioneOnlineStatusLabel;
    @FXML
    private TextArea facilitiesArea;
    private Ristorante ristorante;
    private String currentUser; // Username dell'utente corrente
    private DashboardClienteController dashboardClienteParentController;

    /**
     * Inizializza il controller e configura la ListView delle recensioni.
     *
     * @param location URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Configura la ListView per le recensioni
        recensioniListView.setCellFactory(listView -> new ListCell<Recensione>() {
            @Override
            protected void updateItem(Recensione recensione, boolean empty) {
                super.updateItem(recensione, empty);
                if (empty || recensione == null) {
                    setGraphic(null);
                } else {
                    VBox card = createRecensioneCard(recensione);
                    setGraphic(card);
                }
            }
        });
    }

    /**
     * Imposta il ristorante di cui mostrare i dettagli e popola i campi.
     *
     * @param ristorante Oggetto Ristorante, must be non-null.
     * @since 1.0
     */
    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
        populateFields();
        loadRecensioni();
        updateFavoritesButton();
    }

    /**
     * Imposta l'username corrente e aggiorna le azioni disponibili.
     *
     * @param username Username corrente; se null l'utente è guest.
     * @since 1.0
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateRecensioneButton();
        updateFavoritesButton();
    }

    /**
     * Imposta il controller parent della dashboard cliente per notifiche.
     *
     * @param parentController DashboardClienteController che ha aperto questa vista.
     * @since 1.0
     */
    public void setDashboardClienteParentController(DashboardClienteController parentController) {
        this.dashboardClienteParentController = parentController;
    }

    /**
     * Aggiorna la visibilità dei pulsanti di recensione e preferiti in base allo stato di login.
     * @since 1.0
     */
    private void updateRecensioneButton() {
        // Solo i clienti possono lasciare recensioni
        aggiungiRecensione.setVisible(currentUser != null);
        // Solo gli utenti loggati possono aggiungere ai preferiti
        aggiungiPreferiti.setVisible(currentUser != null);
    }

    /**
     * Carica le recensioni relative al ristorante corrente e aggiorna la ListView.
     * @since 1.0
     */
    private void loadRecensioni() {
        if (ristorante == null)
            return;

        List<Recensione> recensioni = RecensioniManager.getRecensioniPerRistorante(ristorante.getName());
        ObservableList<Recensione> recensioniList = FXCollections.observableArrayList(recensioni);
        recensioniListView.setItems(recensioniList);

        // Mostra/nascondi il messaggio "nessuna recensione"
        boolean hasRecensioni = !recensioni.isEmpty();
        recensioniListView.setVisible(hasRecensioni);

        // Aggiorna la media delle valutazioni
        if (hasRecensioni) {
            double media = RecensioniManager.getMediaValutazioni(ristorante.getName());
            String stelle = "★".repeat((int) Math.round(media)) +
                    "☆".repeat(5 - (int) Math.round(media));
            mediaRecensioniLabel.setText(String.format("%s (%.1f/5)", stelle, media));
        } else {
            mediaRecensioniLabel.setText("Nessuna valutazione");
        }
    }

    /**
     * Crea la rappresentazione grafica di una recensione per la ListView.
     *
     * @param recensione Recensione da mostrare.
     * @return VBox contenente la card della recensione.
     * @since 1.0
     */
    private VBox createRecensioneCard(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Header con stelle e utente
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text stelle = new Text(recensione.getStelle());
        stelle.setStyle("-fx-font-size: 16; -fx-fill: #f39c12;");

        Text utente = new Text("di " + recensione.getUsernameCliente());
        utente.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        Text data = new Text(recensione.getDataRecensioneFormatted());
        data.setStyle("-fx-font-size: 10; -fx-fill: #6c757d;");

        header.getChildren().addAll(stelle, utente, data);

        // Titolo
        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // Commento
        Text commento = new Text(recensione.getCommento());
        commento.setStyle("-fx-font-size: 12;");
        commento.setWrappingWidth(400);

        card.getChildren().addAll(header, titolo, commento);

        // Aggiungi risposta del ristoratore se presente
        if (recensione.getRisposta() != null) {
            VBox rispostaBox = new VBox(5);
            rispostaBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 8; -fx-border-radius: 5; " +
                    "-fx-margin: 8 0 0 0;");

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
     * Apre la posizione del ristorante in Google Maps (browser esterno).
     * @since 1.0
     */
    @FXML
    private void handleOpenMap() {
        if (ristorante != null && (ristorante.getLatitude() != 0.0 || ristorante.getLongitude() != 0.0)) {
            String mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    ristorante.getLatitude(), ristorante.getLongitude());
            openUrl(mapUrl);
        }
    }

    /**
     * Apre il sito web del ristorante se disponibile.
     * @since 1.0
     */
    @FXML
    private void handleOpenWebsite() {
        if (ristorante != null && ristorante.getWebsiteUrl() != null
                && !ristorante.getWebsiteUrl().trim().isEmpty()
                && !ristorante.getWebsiteUrl().equalsIgnoreCase("N/A")) {
            openUrl(ristorante.getWebsiteUrl());
        }
    }

    /**
     * Chiude la finestra dettagli ristorante.
     * @since 1.0
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Handler per l'Hyperlink del sito web.
     *
     * @param event Evento di azione (ignored)
     * @since 1.0
     */
    private void openWebsite(ActionEvent event) {
        if (ristorante != null && ristorante.getWebsiteUrl() != null) {
            openUrl(ristorante.getWebsiteUrl());
        }
    }

    /**
     * Apre un URL nel browser predefinito, aggiungendo http se necessario.
     *
     * @param url URL da aprire.
     * @since 1.0
     */
    private void openUrl(String url) {
        try {
            // Ensure URL has protocol
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                showAlert("Errore", "Impossibile aprire il browser",
                        "Il sistema non supporta l'apertura automatica del browser.\nURL: " + url);
            }
        } catch (IOException | URISyntaxException e) {
            showAlert("Errore", "Impossibile aprire l'URL",
                    "Si è verificato un errore nell'apertura dell'URL: " + url + "\n" + e.getMessage());
        }
    }

    /**
     * Mostra una dialog informativa.
     *
     * @param title Titolo dell'alert.
     * @param header Header dell'alert.
     * @param message Messaggio di dettaglio.
     * @since 1.0
     */
    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Apre la finestra per lasciare una recensione; se l'utente non è autenticato mostra un alert.
     * @since 1.0
     */
    @FXML
    private void handleAggiungiRecensione() {
        if (currentUser == null) {
            showAlert("Accesso Richiesto", "Devi essere autenticato",
                    "Per lasciare una recensione devi prima effettuare l'accesso.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/aggiungiRecensione.fxml"));
            Parent root = loader.load();
            AggiungiRecensioneController controller = loader.getController();
            controller.setRistorante(ristorante);
            controller.setCurrentUser(currentUser);
            controller.setParentController(this);

            // Se abbiamo un riferimento alla dashboard cliente, passalo anche
            // all'AggiungiRecensioneController
            if (dashboardClienteParentController != null) {
                controller.setParentController(dashboardClienteParentController);
            }

            Stage stage = new Stage();
            stage.setTitle("Lascia una Recensione - " + ristorante.getName());

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            stage.setScene(new Scene(root, 500, 400));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Ricarica le recensioni dopo aver chiuso la finestra
            loadRecensioni();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire la finestra",
                    "Si è verificato un errore nell'apertura della finestra per aggiungere la recensione.");
        }
    }

    /**
     * Aggiunge o rimuove il ristorante dai preferiti dell'utente.
     * @since 1.0
     */
    @FXML
    private void handleAggiungiPreferiti() {
        if (currentUser == null) {
            showAlert("Accesso Richiesto", "Devi essere autenticato",
                    "Per aggiungere ai preferiti devi prima effettuare l'accesso.");
            return;
        }

        if (ristorante == null) {
            return;
        }

        boolean isFavorite = PreferitiManager.isPreferito(currentUser, ristorante.getName());

        if (isFavorite) {
            PreferitiManager.rimuoviPreferito(currentUser, ristorante.getName());
            aggiungiPreferiti.setText("❤️ Aggiungi ai Preferiti");
            showAlert("Rimosso", "Ristorante rimosso dai preferiti",
                    ristorante.getName() + " è stato rimosso dai tuoi preferiti.");
        } else {
            PreferitiManager.aggiungiPreferito(currentUser, ristorante.getName());
            aggiungiPreferiti.setText("💔 Rimuovi dai Preferiti");
            showAlert("Aggiunto", "Ristorante aggiunto ai preferiti",
                    ristorante.getName() + " è stato aggiunto ai tuoi preferiti.");
        }
    }

    /**
     * Apre la mappa del ristorante nel browser oppure mostra un alert se non disponibile.
     * @since 1.0
     */
    @FXML
    private void handleVisualizzaMappa() {
        if (ristorante != null && (ristorante.getLatitude() != 0.0 || ristorante.getLongitude() != 0.0)) {
            String mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    ristorante.getLatitude(), ristorante.getLongitude());
            openUrl(mapUrl);
        } else {
            showAlert("Informazione", "Posizione non disponibile",
                    "Le coordinate GPS per questo ristorante non sono disponibili.");
        }
    }

    /**
     * Tenta di avviare una chiamata al ristorante tramite il sistema; fallisce in modalità desktop.
     * @since 1.0
     */
    @FXML
    private void handleChiamaRistorante() {
        if (ristorante != null && ristorante.getPhoneNumber() != null
                && !ristorante.getPhoneNumber().trim().isEmpty()
                && !ristorante.getPhoneNumber().equalsIgnoreCase("N/A")) {
            try {
                String phoneUrl = "tel:" + ristorante.getPhoneNumber();
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(phoneUrl));
                } else {
                    showAlert("Numero di Telefono", "Chiama il ristorante",
                            "Numero: " + ristorante.getPhoneNumber());
                }
            } catch (Exception e) {
                showAlert("Numero di Telefono", "Chiama il ristorante",
                        "Numero: " + ristorante.getPhoneNumber());
            }
        } else {
            showAlert("Informazione", "Numero non disponibile",
                    "Il numero di telefono per questo ristorante non è disponibile.");
        }
    }

    /**
     * Ricarica le recensioni dal servizio.
     * @since 1.0
     */
    public void refreshRecensioni() {
        loadRecensioni();
    }

    /**
     * Aggiorna il testo del pulsante preferiti in base allo stato attuale.
     * @since 1.0
     */
    private void updateFavoritesButton() {
        if (currentUser != null && ristorante != null) {
            boolean isFavorite = PreferitiManager.isPreferito(currentUser, ristorante.getName());
            if (isFavorite) {
                aggiungiPreferiti.setText("💔 Rimuovi dai Preferiti");
            } else {
                aggiungiPreferiti.setText("❤️ Aggiungi ai Preferiti");
            }
        }
    }

    /**
     * Popola tutti i campi della UI con le informazioni del ristorante corrente.
     * @since 1.0
     */
    private void populateFields() {
        if (ristorante == null)
            return;

        nameLabel.setText(ristorante.getName());
        cuisineLabel.setText(ristorante.getCuisine());

        // Stelle Michelin
        int stars = ristorante.getStars();
        if (stars > 0) {
            starsLabel.setText("★".repeat(stars) + " Michelin");
        } else {
            starsLabel.setText("Non classificato");
        }

        priceLabel.setText(ristorante.getPrice());
        addressLabel.setText(ristorante.getAddress());
        locationLabel.setText(ristorante.getLocation());
        phoneLabel.setText(ristorante.getPhoneNumber());

        // Website
        if (ristorante.getWebsiteUrl() != null && !ristorante.getWebsiteUrl().trim().isEmpty()
                && !ristorante.getWebsiteUrl().equalsIgnoreCase("N/A")) {
            websiteLink.setText(ristorante.getWebsiteUrl());
            websiteLink.setOnAction(this::openWebsite);
        } else {
            websiteLink.setText("Non disponibile");
            websiteLink.setDisable(true);
        }

        descriptionArea.setText(ristorante.getDescription());

        // Popola la TextArea dei servizi e strutture
        if (ristorante.getFacilitiesAndServices() != null && !ristorante.getFacilitiesAndServices().trim().isEmpty()) {
            facilitiesArea.setText(ristorante.getFacilitiesAndServices());
        } else {
            facilitiesArea.setText("Nessuna informazione sui servizi disponibile.");
        }

        // Award
        if (ristorante.getAward() != null && !ristorante.getAward().trim().isEmpty()
                && !ristorante.getAward().equalsIgnoreCase("N/A")) {
            awardLabel.setText(ristorante.getAward());
            awardBox.setVisible(true);
        } else {
            awardBox.setVisible(false);
        }

        // Green Star
        if (ristorante.getGreenStar() != null && !ristorante.getGreenStar().trim().isEmpty()
                && !ristorante.getGreenStar().equalsIgnoreCase("N/A")) {
            greenStarLabel.setText(ristorante.getGreenStar());
            greenStarBox.setVisible(true);
        } else {
            greenStarBox.setVisible(false);
        }

        // Mostra/nascondi i servizi basandosi sul contenuto di facilitiesAndServices
        updateServicesDisplay();
    }

    /**
     * Aggiorna la visualizzazione dei servizi (delivery/prenotazione online) nella UI.
     * @since 1.0
     */
    private void updateServicesDisplay() {
        if (ristorante == null)
            return;

        // Imposta i valori Sì/No per delivery
        deliveryStatusLabel.setText(ristorante.isDeliveryAvailable() ? "Sì" : "No");
        deliveryStatusLabel.setStyle(ristorante.isDeliveryAvailable() ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
                : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        // Imposta i valori Sì/No per prenotazione online
        prenotazioneOnlineStatusLabel.setText(ristorante.isOnlineBookingAvailable() ? "Sì" : "No");
        prenotazioneOnlineStatusLabel
                .setStyle(ristorante.isOnlineBookingAvailable() ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
                        : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
    }
}
