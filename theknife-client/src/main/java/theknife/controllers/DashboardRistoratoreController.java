package theknife.controllers;

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
import java.util.stream.Collectors;

/**
 * Controller per il dashboard del ristoratore.
 * Usa ClientTK per tutti i dati.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */
public class DashboardRistoratoreController implements Initializable {

    @FXML private Text benvenutoLabel;
    @FXML private Text numRistorantiLabel;
    @FXML private Text totalRecensioniLabel;
    @FXML private Text mediaGeneraleLabel;
    @FXML private Text risposteDaInviareLabel;
    @FXML private ListView<Ristorante> ristorantiListView;
    @FXML private ListView<Recensione> recensioniListView;
    @FXML private ComboBox<String> filtroRistoranteCombo;
    @FXML private Label nessunRistoranteLabel;
    @FXML private Label nessueRecensioniRistoratoreLabel;
    @FXML private TabPane tabPane;

    private Utente currentUser;
    private List<Ristorante> ristorantiUtente;
    private List<Recensione> tutteRecensioni;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRistorantiListView();
        setupRecensioniListView();
    }

    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        loadUserData();
    }

    private void setupRistorantiListView() {
        ristorantiListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRistoranteCard(r));
            }
        });
    }

    private void setupRecensioniListView() {
        recensioniListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Recensione r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRecensioneCardRistoratore(r));
            }
        });
    }

    private void loadUserData() {
        if (currentUser == null) return;

        benvenutoLabel.setText("Benvenuto, " + currentUser.getNome() + "!");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                ristorantiUtente = Main.getClient().getRistorantiProprietario(currentUser.getId());

                List<Long> ristIds = ristorantiUtente.stream()
                        .map(Ristorante::getId)
                        .collect(Collectors.toList());

                tutteRecensioni = ristIds.isEmpty()
                        ? List.of()
                        : Main.getClient().getRecensioniRistoratori(ristIds);
                return null;
            }

            @Override
            protected void succeeded() {
                ristorantiListView.setItems(FXCollections.observableArrayList(ristorantiUtente));
                recensioniListView.setItems(FXCollections.observableArrayList(tutteRecensioni));

                List<String> nomi = ristorantiUtente.stream()
                        .map(Ristorante::getNome)
                        .collect(Collectors.toList());
                filtroRistoranteCombo.setItems(FXCollections.observableArrayList(nomi));

                updateStatistiche(ristorantiUtente, tutteRecensioni);
                nessunRistoranteLabel.setVisible(ristorantiUtente.isEmpty());
                nessueRecensioniRistoratoreLabel.setVisible(tutteRecensioni.isEmpty());
            }
        };
        new Thread(task).start();
    }

    private void updateStatistiche(List<Ristorante> ristoranti, List<Recensione> recensioni) {
        numRistorantiLabel.setText(String.valueOf(ristoranti.size()));
        totalRecensioniLabel.setText(String.valueOf(recensioni.size()));

        if (recensioni.isEmpty()) {
            mediaGeneraleLabel.setText("N/A");
        } else {
            double media = recensioni.stream().mapToInt(Recensione::getValutazione).average().orElse(0.0);
            String stelle = "★".repeat((int) Math.round(media)) + "☆".repeat(5 - (int) Math.round(media));
            mediaGeneraleLabel.setText(String.format("%.1f %s", media, stelle));
        }

        long risposteDaInviare = recensioni.stream().filter(r -> r.getRisposta() == null).count();
        risposteDaInviareLabel.setText(String.valueOf(risposteDaInviare));
    }

    private VBox createRistoranteCard(Ristorante ristorante) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text nome = new Text(ristorante.getNome());
        nome.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Text cucina = new Text(String.join(", ", ristorante.getCucine()));
        cucina.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        header.getChildren().addAll(nome, cucina);

        Text localita = new Text("📍 " + ristorante.getCitta());
        localita.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        Text telefono = new Text("📞 " + (ristorante.getTelefono() != null ? ristorante.getTelefono() : "N/A"));
        telefono.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        HBox stelleBox = new HBox(10);
        if (ristorante.getStarCount() > 0) {
            Text s = new Text("★".repeat(ristorante.getStarCount()) + " Michelin");
            s.setStyle("-fx-font-size: 12; -fx-fill: #f39c12; -fx-font-weight: bold;");
            stelleBox.getChildren().add(s);
        }
        if (ristorante.isGreenStar()) {
            Text gs = new Text("🌟 Green Star");
            gs.setStyle("-fx-font-size: 12; -fx-fill: #27ae60; -fx-font-weight: bold;");
            stelleBox.getChildren().add(gs);
        }

        HBox buttonsBox = new HBox(10);
        Button visualizzaBtn = new Button("Visualizza Dettagli");
        visualizzaBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10;");
        visualizzaBtn.setOnAction(event -> apriDettaglioRistorante(ristorante));

        Button recensioniBtn = new Button("Vedi Recensioni");
        recensioniBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 10;");
        recensioniBtn.setOnAction(event -> {
            tabPane.getSelectionModel().select(1);
            filtroRistoranteCombo.setValue(ristorante.getNome());
            handleFiltroRistorante();
        });

        buttonsBox.getChildren().addAll(visualizzaBtn, recensioniBtn);
        card.getChildren().addAll(header, localita, telefono, stelleBox, buttonsBox);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                apriDettaglioRistorante(ristorante);
            }
        });
        return card;
    }

    private VBox createRecensioneCardRistoratore(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

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

        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Text commento = new Text(recensione.getCommento());
        commento.setStyle("-fx-font-size: 12; -fx-fill: #495057;");
        commento.setWrappingWidth(500);

        card.getChildren().addAll(header, titolo, commento);

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

    @FXML
    private void handleAggiungiRistorante() {
        try {
            AppNavigator.show("/views/aggiungiRistorante.fxml", (AggiungiRistoranteController controller) -> {
                controller.setCurrentUser(currentUser);
                controller.setParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFiltroRistorante() {
        String ristoranteSelezionato = filtroRistoranteCombo.getValue();
        if (ristoranteSelezionato == null || ristoranteSelezionato.isEmpty()) return;

        List<Recensione> filtrate = tutteRecensioni.stream()
                .filter(r -> r.getNomeRistorante().equals(ristoranteSelezionato))
                .collect(Collectors.toList());
        recensioniListView.setItems(FXCollections.observableArrayList(filtrate));
    }

    @FXML
    private void handleMostraTutte() {
        filtroRistoranteCombo.setValue(null);
        recensioniListView.setItems(FXCollections.observableArrayList(tutteRecensioni));
    }

    private void apriDettaglioRistorante(Ristorante ristorante) {
        try {
            AppNavigator.show("/views/dettaglioRistorante.fxml", (DettaglioRistoranteController controller) -> {
                controller.setRistorante(ristorante);
                controller.setCurrentUser(currentUser.getUsername());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void apriFinstraRisposta(Recensione recensione) {
        try {
            AppNavigator.show("/views/rispondiRecensione.fxml", (RispondiRecensioneController controller) -> {
                controller.setRecensione(recensione);
                controller.setCurrentUser(currentUser.getUsername());
                controller.setParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshData() {
        if (currentUser != null) loadUserData();
    }
}
