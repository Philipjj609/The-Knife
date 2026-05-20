package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
 * @author Philip Jon Ji Ciuca
 * @version 2.0
 */
public class DettaglioRistoranteController implements Initializable {

    @FXML private Text nameLabel;
    @FXML private Text cuisineLabel;
    @FXML private Text starsLabel;
    @FXML private Text priceLabel;
    @FXML private Text addressLabel;
    @FXML private Text locationLabel;
    @FXML private Text phoneLabel;
    @FXML private Hyperlink websiteLink;
    @FXML private TextArea descriptionArea;
    @FXML private VBox awardBox;
    @FXML private Text awardLabel;
    @FXML private VBox greenStarBox;
    @FXML private Text greenStarLabel;
    @FXML private Text mediaRecensioniLabel;
    @FXML private Button aggiungiRecensione;
    @FXML private ListView<Recensione> recensioniListView;
    @FXML private Button aggiungiPreferiti;
    @FXML private Button visualizzaMappa;
    @FXML private Button chiamaRistorante;
    @FXML private Label deliveryStatusLabel;
    @FXML private Label prenotazioneOnlineStatusLabel;
    @FXML private TextArea facilitiesArea;

    private Ristorante ristorante;
    private String currentUser;
    private DashboardClienteController dashboardClienteParentController;

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

    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
        populateFields();
        loadRecensioni();
        updateFavoritesButton();
    }

    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateRecensioneButton();
        updateFavoritesButton();
    }

    public void setDashboardClienteParentController(DashboardClienteController parentController) {
        this.dashboardClienteParentController = parentController;
    }

    private void updateRecensioneButton() {
        aggiungiRecensione.setVisible(currentUser != null);
        aggiungiPreferiti.setVisible(currentUser != null);
    }

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

    private VBox createRecensioneCard(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text stelle = new Text(recensione.getStelle());
        stelle.setStyle("-fx-font-size: 16; -fx-fill: #f39c12;");

        Text utente = new Text("di " + recensione.getAutoreDisplayName());
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

    @FXML private void handleOpenMap() {
        if (ristorante != null && (ristorante.getLatitudine() != 0.0 || ristorante.getLongitudine() != 0.0)) {
            String mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    ristorante.getLatitudine(), ristorante.getLongitudine());
            openUrl(mapUrl);
        }
    }

    @FXML private void handleOpenWebsite() {
        if (ristorante != null && ristorante.getSitoWeb() != null
                && !ristorante.getSitoWeb().trim().isEmpty()
                && !ristorante.getSitoWeb().equalsIgnoreCase("N/A")) {
            openUrl(ristorante.getSitoWeb());
        }
    }

    @FXML private void handleClose() {
        ((Stage) nameLabel.getScene().getWindow()).close();
    }

    private void openWebsite(ActionEvent event) {
        if (ristorante != null && ristorante.getSitoWeb() != null) openUrl(ristorante.getSitoWeb());
    }

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

            if (dashboardClienteParentController != null) {
                controller.setParentController(dashboardClienteParentController);
            }

            Stage stage = new Stage();
            stage.setTitle("Lascia una Recensione - " + ristorante.getNome());
            Main.setApplicationIcon(stage);
            stage.setScene(new Scene(root, 500, 400));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();
            loadRecensioni();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    @FXML
    private void handleVisualizzaMappa() {
        if (ristorante != null && (ristorante.getLatitudine() != 0.0 || ristorante.getLongitudine() != 0.0)) {
            String mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    ristorante.getLatitudine(), ristorante.getLongitudine());
            openUrl(mapUrl);
        } else {
            showAlert("Informazione", "Posizione non disponibile",
                    "Le coordinate GPS per questo ristorante non sono disponibili.");
        }
    }

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

    public void refreshRecensioni() { loadRecensioni(); }

    private void updateFavoritesButton() {
        if (currentUser != null && ristorante != null) {
            new Thread(() -> {
                boolean isFav = Main.getClient().isPreferito(currentUser, ristorante.getId());
                javafx.application.Platform.runLater(() ->
                        aggiungiPreferiti.setText(isFav ? "💔 Rimuovi dai Preferiti" : "❤️ Aggiungi ai Preferiti"));
            }).start();
        }
    }

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
        } else {
            awardBox.setVisible(false);
        }

        if (ristorante.isGreenStar()) {
            greenStarLabel.setText("Green Star");
            greenStarBox.setVisible(true);
        } else {
            greenStarBox.setVisible(false);
        }

        updateServicesDisplay();
    }

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

    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
