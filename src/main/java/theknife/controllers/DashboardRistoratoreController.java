package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import theknife.models.Utente;
import theknife.services.RecensioniManager;
import theknife.services.RistorantiManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller per il dashboard del ristoratore.
 * Gestisce la visualizzazione e la gestione dei ristoranti del proprietario,
 * delle recensioni ricevute e delle operazioni correlate come rispondere alle
 * recensioni o aggiungere nuovi ristoranti.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class DashboardRistoratoreController implements Initializable {

    @FXML
    private Text benvenutoLabel;
    @FXML
    private Text numRistorantiLabel;
    @FXML
    private Text totalRecensioniLabel;
    @FXML
    private Text mediaGeneraleLabel;
    @FXML
    private Text risposteDaInviareLabel;
    @FXML
    private ListView<Ristorante> ristorantiListView;
    @FXML
    private ListView<Recensione> recensioniListView;
    @FXML
    private ComboBox<String> filtroRistoranteCombo;
    @FXML
    private Label nessunRistoranteLabel;
    @FXML
    private Label nessueRecensioniRistoratoreLabel;
    @FXML
    private TabPane tabPane;

    private Utente currentUser;
    private List<Recensione> tutteRecensioni;

    /**
     * Inizializza il controller impostando le ListView e le configurazioni iniziali.
     *
     * @param location URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRistorantiListView();
        setupRecensioniListView();
    }

    /**
     * Imposta l'utente corrente (ristoratore) e carica i suoi dati.
     *
     * @param user Utente corrente di tipo Ristoratore, must be non-null.
     * @since 1.0
     */
    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        loadUserData();
    }

    /**
     * Configura la ListView dei ristoranti con celle personalizzate.
     * @since 1.0
     */
    private void setupRistorantiListView() {
        ristorantiListView.setCellFactory(listView -> new ListCell<Ristorante>() {
            @Override
            protected void updateItem(Ristorante ristorante, boolean empty) {
                super.updateItem(ristorante, empty);
                if (empty || ristorante == null) {
                    setGraphic(null);
                } else {
                    VBox card = createRistoranteCard(ristorante);
                    setGraphic(card);
                }
            }
        });
    }

    /**
     * Configura la ListView delle recensioni con celle personalizzate.
     * @since 1.0
     */
    private void setupRecensioniListView() {
        recensioniListView.setCellFactory(listView -> new ListCell<Recensione>() {
            @Override
            protected void updateItem(Recensione recensione, boolean empty) {
                super.updateItem(recensione, empty);
                if (empty || recensione == null) {
                    setGraphic(null);
                } else {
                    VBox card = createRecensioneCardRistoratore(recensione);
                    setGraphic(card);
                }
            }
        });
    }

    /**
     * Carica i dati (ristoranti e recensioni) per l'utente corrente e aggiorna la UI.
     * @since 1.0
     */
    private void loadUserData() {
        if (currentUser == null)
            return;

        benvenutoLabel.setText("Benvenuto, " + currentUser.getNome() + "!");

        // Carica ristoranti del ristoratore
        List<Ristorante> ristoranti = RistorantiManager.getRistorantiPerProprietario(currentUser.getUsername());
        ObservableList<Ristorante> ristorantiList = FXCollections.observableArrayList(ristoranti);
        ristorantiListView.setItems(ristorantiList);

        // Carica recensioni per tutti i ristoranti del ristoratore
        tutteRecensioni = RecensioniManager.getRecensioniPerRistoratore(currentUser.getUsername());
        ObservableList<Recensione> recensioniList = FXCollections.observableArrayList(tutteRecensioni);
        recensioniListView.setItems(recensioniList);

        // Popola il filtro ristorante
        List<String> nomiRistoranti = ristoranti.stream()
                .map(Ristorante::getName)
                .collect(Collectors.toList());
        filtroRistoranteCombo.setItems(FXCollections.observableArrayList(nomiRistoranti));

        // Aggiorna statistiche
        updateStatistiche(ristoranti, tutteRecensioni);

        // Mostra/nascondi labels per liste vuote
        nessunRistoranteLabel.setVisible(ristoranti.isEmpty());
        nessueRecensioniRistoratoreLabel.setVisible(tutteRecensioni.isEmpty());
    }

    /**
     * Aggiorna le statistiche del ristoratore: numero ristoranti, recensioni, media e risposte mancanti.
     *
     * @param ristoranti Lista dei ristoranti del proprietario.
     * @param recensioni Lista di recensioni relative ai suoi ristoranti.
     * @since 1.0
     */
    private void updateStatistiche(List<Ristorante> ristoranti, List<Recensione> recensioni) {
        numRistorantiLabel.setText(String.valueOf(ristoranti.size()));
        totalRecensioniLabel.setText(String.valueOf(recensioni.size()));

        // Calcola media generale
        if (recensioni.isEmpty()) {
            mediaGeneraleLabel.setText("N/A");
        } else {
            double media = recensioni.stream()
                    .mapToInt(Recensione::getValutazione)
                    .average()
                    .orElse(0.0);
            String stelle = "★".repeat((int) Math.round(media)) +
                    "☆".repeat(5 - (int) Math.round(media));
            mediaGeneraleLabel.setText(String.format("%.1f %s", media, stelle));
        }

        // Conta recensioni senza risposta
        long risposteDaInviare = recensioni.stream()
                .filter(r -> r.getRisposta() == null)
                .count();
        risposteDaInviareLabel.setText(String.valueOf(risposteDaInviare));
    }

    /**
     * Crea la card grafica per un ristorante.
     * @param ristorante Ristorante da visualizzare.
     * @return VBox con la rappresentazione grafica.
     * @since 1.0
     */
    private VBox createRistoranteCard(Ristorante ristorante) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text nome = new Text(ristorante.getName());
        nome.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Text cucina = new Text(ristorante.getCuisine());
        cucina.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        header.getChildren().addAll(nome, cucina);

        // Info ristorante
        Text localita = new Text("📍 " + ristorante.getLocation());
        localita.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        Text telefono = new Text("📞 " + ristorante.getPhoneNumber());
        telefono.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        // Stelle Michelin
        HBox stelleBox = new HBox(10);
        if (ristorante.getStars() > 0) {
            Text stelle = new Text("★".repeat(ristorante.getStars()) + " Michelin");
            stelle.setStyle("-fx-font-size: 12; -fx-fill: #f39c12; -fx-font-weight: bold;");
            stelleBox.getChildren().add(stelle);
        }

        if (ristorante.hasGreenStar()) {
            Text greenStar = new Text("🌟 Green Star");
            greenStar.setStyle("-fx-font-size: 12; -fx-fill: #27ae60; -fx-font-weight: bold;");
            stelleBox.getChildren().add(greenStar);
        }

        // Statistiche recensioni per questo ristorante
        List<Recensione> recensioniRistorante = RecensioniManager.getRecensioniPerRistorante(ristorante.getName());
        double media = RecensioniManager.getMediaValutazioni(ristorante.getName());

        Text statsRecensioni = new Text(String.format("📊 %d recensioni (Media: %.1f)",
                recensioniRistorante.size(), media));
        statsRecensioni.setStyle("-fx-font-size: 11; -fx-fill: #28a745;");

        // Pulsanti azione
        HBox buttonsBox = new HBox(10);
        Button visualizzaBtn = new Button("Visualizza Dettagli");
        visualizzaBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10;");
        visualizzaBtn.setOnAction(event -> apriDettaglioRistorante(ristorante));

        Button recensioniBtn = new Button("Vedi Recensioni");
        recensioniBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 10;");
        recensioniBtn.setOnAction(event -> {
            // Cambia alla tab delle recensioni (indice 1)
            tabPane.getSelectionModel().select(1);
            // Filtra per il ristorante specifico
            filtroRistoranteCombo.setValue(ristorante.getName());
            handleFiltroRistorante();
        });

        buttonsBox.getChildren().addAll(visualizzaBtn, recensioniBtn);

        card.getChildren().addAll(header, localita, telefono, stelleBox, statsRecensioni, buttonsBox);
        return card;
    }

    /**
     * Crea la card grafica per una recensione destinata al ristoratore.
     * @param recensione Recensione da visualizzare.
     * @return VBox con la rappresentazione grafica.
     * @since 1.0
     */
    private VBox createRecensioneCardRistoratore(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text ristorante = new Text(recensione.getNomeRistorante());
        ristorante.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text stelle = new Text(recensione.getStelle());
        stelle.setStyle("-fx-font-size: 14; -fx-fill: #f39c12;");

        Text cliente = new Text("di " + recensione.getUsernameCliente());
        cliente.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        Text data = new Text(recensione.getDataRecensioneFormatted());
        data.setStyle("-fx-font-size: 10; -fx-fill: #6c757d;");

        header.getChildren().addAll(ristorante, stelle, cliente, data);

        // Titolo
        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // Commento
        Text commento = new Text(recensione.getCommento());
        commento.setStyle("-fx-font-size: 12; -fx-fill: #495057;");
        commento.setWrappingWidth(500);

        card.getChildren().addAll(header, titolo, commento);

        // Risposta o pulsante per rispondere
        if (recensione.getRisposta() != null) {
            VBox rispostaBox = new VBox(5);
            rispostaBox.setStyle("-fx-background-color: #e8f5e8; -fx-padding: 10; -fx-border-radius: 5;");

            Text rispostaHeader = new Text("✅ La tua risposta (" +
                    recensione.getRisposta().getDataRispostaFormatted() + "):");
            rispostaHeader.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-fill: #2e7d32;");

            Text rispostaTesto = new Text(recensione.getRisposta().getTesto());
            rispostaTesto.setStyle("-fx-font-size: 11; -fx-fill: #2e7d32;");
            rispostaTesto.setWrappingWidth(480);

            rispostaBox.getChildren().addAll(rispostaHeader, rispostaTesto);
            card.getChildren().add(rispostaBox);
        } else {
            Button rispondiBtn = new Button("💬 Rispondi alla Recensione");
            rispondiBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11;");
            rispondiBtn.setOnAction(event -> apriFinstraRisposta(recensione));
            card.getChildren().add(rispondiBtn);
        }

        return card;
    }

    /**
     * Apre la finestra per aggiungere un nuovo ristorante.
     * @since 1.0
     */
    @FXML
    private void handleAggiungiRistorante() {
        try {
            // Carica la finestra per aggiungere un nuovo ristorante
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/aggiungiRistorante.fxml"));
            Parent root = loader.load();

            // Ottieni il controller e imposta i dati necessari
            AggiungiRistoranteController controller = loader.getController();
            controller.setCurrentUser(currentUser.getUsername());
            controller.setParentController(this);

            // Crea e mostra la finestra modale
            Stage stage = new Stage();
            stage.setTitle("Aggiungi Nuovo Ristorante");
            stage.setScene(new Scene(root, 800, 900));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(ristorantiListView.getScene().getWindow());
            stage.setResizable(false);

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire la finestra",
                    "Si è verificato un errore nell'apertura della finestra per aggiungere il ristorante.");
        }
    }

    /**
     * Filtra le recensioni per il ristorante selezionato nel ComboBox.
     * @since 1.0
     */
    @FXML
    private void handleFiltroRistorante() {
        String ristoranteSelezionato = filtroRistoranteCombo.getValue();
        if (ristoranteSelezionato == null || ristoranteSelezionato.isEmpty()) {
            return;
        }

        List<Recensione> recensioniFiltrate = tutteRecensioni.stream()
                .filter(r -> r.getNomeRistorante().equals(ristoranteSelezionato))
                .collect(Collectors.toList());

        ObservableList<Recensione> recensioniList = FXCollections.observableArrayList(recensioniFiltrate);
        recensioniListView.setItems(recensioniList);
    }

    /**
     * Mostra tutte le recensioni senza filtri.
     * @since 1.0
     */
    @FXML
    private void handleMostraTutte() {
        filtroRistoranteCombo.setValue(null);
        ObservableList<Recensione> recensioniList = FXCollections.observableArrayList(tutteRecensioni);
        recensioniListView.setItems(recensioniList);
    }

    /**
     * Apre la vista dettaglio per il ristorante dato.
     * @param ristorante Ristorante da aprire.
     * @since 1.0
     */
    private void apriDettaglioRistorante(Ristorante ristorante) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dettaglioRistorante.fxml"));
            Parent root = loader.load();

            DettaglioRistoranteController controller = loader.getController();
            controller.setRistorante(ristorante);
            controller.setCurrentUser(currentUser.getUsername());

            Stage stage = new Stage();
            stage.setTitle("Dettaglio Ristorante - " + ristorante.getName());

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Apre la finestra per rispondere ad una recensione.
     * @param recensione Recensione a cui rispondere.
     * @since 1.0
     */
    private void apriFinstraRisposta(Recensione recensione) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/rispondiRecensione.fxml"));
            Parent root = loader.load();

            RispondiRecensioneController controller = loader.getController();
            controller.setRecensione(recensione);
            controller.setCurrentUser(currentUser.getUsername());
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle("Rispondi alla Recensione");

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            stage.setScene(new Scene(root, 850, 800));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.showAndWait();

            // Ricarica i dati dopo aver chiuso la finestra
            loadUserData();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire la finestra",
                    "Si è verificato un errore nell'apertura della finestra per rispondere.");
        }
    }

    /**
     * Mostra un alert informativo.
     *
     * @param title Titolo dell'alert.
     * @param header Header dell'alert.
     * @param message Messaggio dell'alert.
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
     * Aggiorna i dati del dashboard dopo modifiche.
     * @since 1.0
     */
    public void refreshData() {
        if (currentUser != null) {
            loadUserData();
        }
    }
}

