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
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Ristorante;
import theknife.models.Utente;
import theknife.services.RecensioniManager;
import theknife.services.PreferitiManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per il dashboard utente (Cliente).
 * Gestisce la visualizzazione delle recensioni personali, dei preferiti
 * e delle statistiche legate all'account cliente. Espone metodi per
 * aprire dettagli ristorante ed esplorare la lista completa.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class DashboardClienteController implements Initializable {

    @FXML
    private Text benvenutoLabel;
    @FXML
    private Text numRecensioniLabel;
    @FXML
    private Text mediaValutazioniLabel;
    @FXML
    private Text preferitiLabel;
    @FXML
    private ListView<Recensione> recensioniListView;
    @FXML
    private ListView<Ristorante> preferitiListView;
    @FXML
    private Label nessueRecensioniLabel;
    @FXML
    private Label nessunPreferitoLabel;

    private Utente currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRecensioniListView();
        setupPreferitiListView();
    }

    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        loadUserData();
    }

    /**
     * Configura la ListView delle recensioni con celle personalizzate.
     * @since 1.0
     */
    private void setupRecensioniListView() {
        // Configura la ListView per le recensioni
        recensioniListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Recensione recensione, boolean empty) {
                super.updateItem(recensione, empty);
                if (empty || recensione == null) {
                    setGraphic(null);
                } else {
                    VBox card = createRecensioneCard(recensione);
                    card.setOnMouseClicked(event -> apriDettaglioRistorante(recensione.getNomeRistorante()));
                    setGraphic(card);
                }
            }
        });

        // Configura la ListView per i preferiti
        preferitiListView.setCellFactory(listView -> new ListCell<>() {
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
     * Configura la ListView dei preferiti con celle personalizzate.
     * @since 1.0
     */
    private void setupPreferitiListView() {
        // Configura la ListView per i preferiti
        preferitiListView.setCellFactory(listView2 -> new ListCell<Ristorante>() {
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
     * Carica i dati dell'utente corrente: recensioni, preferiti e statistiche.
     * @since 1.0
     */
    private void loadUserData() {
        if (currentUser == null)
            return;

        benvenutoLabel.setText("Benvenuto, " + currentUser.getNome() + "!");

        // Carica recensioni dell'utente
        List<Recensione> recensioni = RecensioniManager.getRecensioniPerCliente(currentUser.getUsername());
        ObservableList<Recensione> recensioniList = FXCollections.observableArrayList(recensioni);
        recensioniListView.setItems(recensioniList);

        // Carica preferiti dell'utente
        List<Ristorante> preferiti = PreferitiManager.getPreferitiPerUtente(currentUser.getUsername());
        ObservableList<Ristorante> preferitiObsList = FXCollections.observableArrayList(preferiti);
        preferitiListView.setItems(preferitiObsList);

        // Aggiorna statistiche
        updateStatistiche(recensioni, preferiti);

        // Mostra/nascondi labels per liste vuote
        nessueRecensioniLabel.setVisible(recensioni.isEmpty());
        nessunPreferitoLabel.setVisible(preferiti.isEmpty());
    }

    /**
     * Aggiorna le statistiche (numero recensioni, media valutazioni, preferiti).
     * @param recensioni Lista delle recensioni dell'utente.
     * @param preferiti Lista dei ristoranti preferiti.
     * @since 1.0
     */
    private void updateStatistiche(List<Recensione> recensioni, List<Ristorante> preferiti) {
        numRecensioniLabel.setText(String.valueOf(recensioni.size()));
        preferitiLabel.setText(String.valueOf(preferiti.size()));

        if (recensioni.isEmpty()) {
            mediaValutazioniLabel.setText("N/A");
        } else {
            double media = recensioni.stream()
                    .mapToInt(Recensione::getValutazione)
                    .average()
                    .orElse(0.0);
            String stelle = "★".repeat((int) Math.round(media)) +
                    "☆".repeat(5 - (int) Math.round(media));
            mediaValutazioniLabel.setText(String.format("%.1f %s", media, stelle));
        }
    }

    /**
     * Crea la card grafica per una recensione da mostrare nella ListView.
     * @param recensione Oggetto Recensione da visualizzare.
     * @return VBox contenente la rappresentazione grafica della recensione.
     * @since 1.0
     */
    private VBox createRecensioneCard(Recensione recensione) {
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

        Text data = new Text(recensione.getDataRecensioneFormatted());
        data.setStyle("-fx-font-size: 10; -fx-fill: #6c757d;");

        header.getChildren().addAll(ristorante, stelle, data);

        // Titolo
        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        // Commento (troncato)
        String commentoTroncato = recensione.getCommento().length() > 100
                ? recensione.getCommento().substring(0, 100) + "..."
                : recensione.getCommento();
        Text commento = new Text(commentoTroncato);
        commento.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        // Risposta del ristoratore se presente
        if (recensione.getRisposta() != null) {
            Text risposta = new Text("💬 Il ristoratore ha risposto");
            risposta.setStyle("-fx-font-size: 11; -fx-fill: #28a745; -fx-font-style: italic;");
            card.getChildren().addAll(header, titolo, commento, risposta);
        } else {
            card.getChildren().addAll(header, titolo, commento);
        } // Click per aprire dettagli
        card.setOnMouseClicked(event -> apriDettaglioRistorante(recensione.getNomeRistorante()));
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");

        return card;
    }

    /**
     * Crea la card grafica per un ristorante da mostrare nella lista dei preferiti.
     * @param ristorante Oggetto Ristorante da visualizzare.
     * @return VBox contenente la rappresentazione grafica del ristorante.
     * @since 1.0
     */
    private VBox createRistoranteCard(Ristorante ristorante) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        // Nome e cucina
        Text nome = new Text(ristorante.getName());
        nome.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text cucina = new Text(ristorante.getCuisine());
        cucina.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        // Stelle Michelin
        HBox stelleBox = new HBox(5);
        if (ristorante.getStars() > 0) {
            Text stelle = new Text("★".repeat(ristorante.getStars()) + " Michelin");
            stelle.setStyle("-fx-font-size: 12; -fx-fill: #f39c12;");
            stelleBox.getChildren().add(stelle);
        }

        if (ristorante.getGreenStar() != null && !ristorante.getGreenStar().trim().isEmpty()
                && !ristorante.getGreenStar().equalsIgnoreCase("N/A")) {
            Text greenStar = new Text("🌟 Green Star");
            greenStar.setStyle("-fx-font-size: 12; -fx-fill: #27ae60;");
            stelleBox.getChildren().add(greenStar);
        }

        // Località e prezzo
        Text localita = new Text(ristorante.getLocation());
        localita.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        Text prezzo = new Text(ristorante.getPrice() != null ? ristorante.getPrice() : "N/A");
        prezzo.setStyle("-fx-font-size: 12; -fx-fill: #e74c3c; -fx-font-weight: bold;");

        Button rimuoviBtn = new Button("❤️ Rimuovi");
        rimuoviBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10;");
        rimuoviBtn.setOnAction(event -> {
            PreferitiManager.rimuoviPreferito(currentUser.getUsername(), ristorante.getName());
            loadUserData(); // Ricarica i dati
        });

        card.getChildren().addAll(nome, cucina, stelleBox, localita, prezzo, rimuoviBtn); // Click per aprire dettagli
        card.setOnMouseClicked(event -> apriDettaglioRistorante(ristorante.getName()));
        card.setStyle(card.getStyle() + "-fx-cursor: hand;");

        return card;
    }

    /**
     * Apre la finestra di esplorazione ristoranti passando l'utente corrente.
     * @since 1.0
     */
    @FXML
    private void handleEsploraRistoranti() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/esploraRistoranti.fxml"));
            Parent root = loader.load();

            EsploraRistorantiController controller = loader.getController();
            controller.setCurrentUser(currentUser);
            controller.setParentController(this);

            Stage stage = new Stage();
            stage.setTitle("Esplora Ristoranti - The Knife");

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            stage.setScene(new Scene(root, 1000, 800));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Apre la vista dettaglio per il ristorante con nome specificato.
     * @param nomeRistorante Nome del ristorante da aprire.
     * @since 1.0
     */
    private void apriDettaglioRistorante(String nomeRistorante) {
        // Trova il ristorante
        Ristorante ristorante = Main.ristoranti.stream()
                .filter(r -> r.getName().equals(nomeRistorante))
                .findFirst()
                .orElse(null);

        if (ristorante != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dettaglioRistorante.fxml"));
                Parent root = loader.load();
                DettaglioRistoranteController controller = loader.getController();
                controller.setRistorante(ristorante);
                controller.setCurrentUser(currentUser.getUsername());
                controller.setDashboardClienteParentController(this);

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
    }

    /**
     * Ricarica i dati del dashboard (usato come callback dai child controllers).
     * @since 1.0
     */
    public void refreshData() {
        loadUserData();
    }
}
