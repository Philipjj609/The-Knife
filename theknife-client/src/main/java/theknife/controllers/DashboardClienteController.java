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
 * Controller per il dashboard utente (Cliente).
 * Usa ClientTK per ottenere i dati dal server.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */
public class DashboardClienteController implements Initializable {

    @FXML private Text benvenutoLabel;
    @FXML private Text numRecensioniLabel;
    @FXML private Text mediaValutazioniLabel;
    @FXML private Text preferitiLabel;
    @FXML private ListView<Recensione> recensioniListView;
    @FXML private ListView<Ristorante> preferitiListView;
    @FXML private Label nessueRecensioniLabel;
    @FXML private Label nessunPreferitoLabel;

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

    private void setupRecensioniListView() {
        recensioniListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Recensione r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRecensioneCard(r));
            }
        });
    }

    private void setupPreferitiListView() {
        preferitiListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRistoranteCard(r));
            }
        });
    }

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

    @FXML
    private void handleEsploraRistoranti() {
        try {
            AppNavigator.show("/views/esploraRistoranti.fxml", (EsploraRistorantiController controller) -> {
                controller.setCurrentUser(currentUser);
                controller.setParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public void refreshData() {
        loadUserData();
    }
}
