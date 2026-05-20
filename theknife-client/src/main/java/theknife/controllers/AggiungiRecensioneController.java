package theknife.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Ristorante;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AggiungiRecensioneController implements Initializable {

    @FXML private Text ristoranteLabel;
    @FXML private ToggleButton star1, star2, star3, star4, star5;
    @FXML private TextField titoloField;
    @FXML private TextArea commentoArea;
    @FXML private Label errorLabel;
    @FXML private Button pubblicaButton;

    private List<ToggleButton> stars;
    private Ristorante ristorante;
    private String currentUser;
    private Recensione recensioneEsistente; // null = nuova, non-null = modifica
    private DettaglioRistoranteController dettaglioParentController;
    private EsploraRistorantiController esploraParentController;
    private DashboardClienteController dashboardClienteParentController;
    private int selectedRating = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        stars = List.of(star1, star2, star3, star4, star5);
        setupStarRating();
    }

    private void setupStarRating() {
        for (int i = 0; i < stars.size(); i++) {
            final int rating = i + 1;
            ToggleButton star = stars.get(i);

            star.setOnAction(event -> { selectedRating = rating; updateStarDisplay(); });

            star.setOnMouseEntered(event -> {
                for (int j = 0; j < rating; j++)
                    stars.get(j).setStyle("-fx-font-size: 20; -fx-text-fill: #f39c12; -fx-background-color: transparent; -fx-border-color: transparent;");
                for (int j = rating; j < stars.size(); j++)
                    stars.get(j).setStyle("-fx-font-size: 20; -fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-border-color: transparent;");
            });
            star.setOnMouseExited(event -> updateStarDisplay());
        }
    }

    private void updateStarDisplay() {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setStyle(i < selectedRating
                    ? "-fx-font-size: 20; -fx-text-fill: #f39c12; -fx-background-color: transparent; -fx-border-color: transparent;"
                    : "-fx-font-size: 20; -fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-border-color: transparent;");
        }
    }

    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
        ristoranteLabel.setText("Recensione per: " + ristorante.getNome());
    }

    public void setCurrentUser(String username) { this.currentUser = username; }
    public void setParentController(DettaglioRistoranteController c) { this.dettaglioParentController = c; }
    public void setParentController(EsploraRistorantiController c) { this.esploraParentController = c; }
    public void setParentController(DashboardClienteController c) { this.dashboardClienteParentController = c; }

    /** Pre-compila il form con i dati di una recensione esistente per la modifica. */
    public void setRecensioneEsistente(Recensione r) {
        this.recensioneEsistente = r;
        selectedRating = r.getValutazione();
        updateStarDisplay();
        titoloField.setText(r.getTitolo() != null ? r.getTitolo() : "");
        commentoArea.setText(r.getCommento() != null ? r.getCommento() : "");
        if (pubblicaButton != null) pubblicaButton.setText("Salva Modifiche");
    }

    @FXML
    private void handlePubblica() {
        if (selectedRating == 0) { errorLabel.setText("Seleziona una valutazione con le stelle!"); return; }
        if (titoloField.getText().trim().isEmpty()) { errorLabel.setText("Inserisci un titolo per la recensione!"); return; }
        if (commentoArea.getText().trim().isEmpty()) { errorLabel.setText("Scrivi un commento per la recensione!"); return; }

        if (recensioneEsistente != null) {
            // Modalità modifica
            recensioneEsistente.setValutazione(selectedRating);
            recensioneEsistente.setTitolo(titoloField.getText().trim());
            recensioneEsistente.setCommento(commentoArea.getText().trim());

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() {
                    return Main.getClient().modificaRecensione(recensioneEsistente);
                }
            };
            task.setOnSucceeded(e -> {
                notificaParent();
                AppNavigator.goBackOrClose(titoloField);
            });
            task.setOnFailed(e -> errorLabel.setText("Errore nel modificare la recensione: " + task.getException().getMessage()));
            new Thread(task).start();
        } else {
            // Modalità nuova recensione
            Recensione nuova = new Recensione(
                    currentUser,
                    ristorante.getId(),
                    selectedRating,
                    titoloField.getText().trim(),
                    commentoArea.getText().trim());

            Task<Recensione> task = new Task<>() {
                @Override protected Recensione call() {
                    return Main.getClient().aggiungiRecensione(nuova);
                }
            };
            task.setOnSucceeded(e -> {
                notificaParent();
                AppNavigator.goBackOrClose(titoloField);
            });
            task.setOnFailed(e -> errorLabel.setText("Errore nel salvare la recensione: " + task.getException().getMessage()));
            new Thread(task).start();
        }
    }

    private void notificaParent() {
        if (dettaglioParentController != null) dettaglioParentController.refreshRecensioni();
        if (esploraParentController != null) esploraParentController.refreshView();
        if (dashboardClienteParentController != null) dashboardClienteParentController.refreshData();
    }

    @FXML
    private void handleAnnulla() {
        AppNavigator.goBackOrClose(titoloField);
    }
}
