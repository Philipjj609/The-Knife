package theknife.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import theknife.models.Recensione;
import theknife.models.Ristorante;
import theknife.services.RecensioniManager;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la finestra di inserimento di una nuova recensione.
 * Gestisce la selezione delle stelle, la validazione dei campi e la
 * creazione/salvataggio di una Recensione. Viene utilizzato sia dal
 * dettaglio ristorante che dalla dashboard cliente/esplora.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class AggiungiRecensioneController implements Initializable {

    @FXML
    private Text ristoranteLabel;
    @FXML
    private ToggleButton star1, star2, star3, star4, star5;
    @FXML
    private TextField titoloField;
    @FXML
    private TextArea commentoArea;
    @FXML
    private Label errorLabel;
    private List<ToggleButton> stars;
    private Ristorante ristorante;
    private String currentUser;
    private DettaglioRistoranteController dettaglioParentController;
    private EsploraRistorantiController esploraParentController;
    private DashboardClienteController dashboardClienteParentController;
    private int selectedRating = 0;

    /**
     * Inizializza il controller e setta il comportamento delle stelle.
     *
     * @param location URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        stars = List.of(star1, star2, star3, star4, star5);
        setupStarRating();
    }

    /**
     * Imposta il comportamento di selezione e hover per il rating a stelle.
     * Questo metodo aggiorna la variabile selectedRating in risposta agli eventi.
     * @since 1.0
     */
    private void setupStarRating() {
        for (int i = 0; i < stars.size(); i++) {
            final int rating = i + 1;
            ToggleButton star = stars.get(i);

            star.setOnAction(_e -> {
                selectedRating = rating;
                updateStarDisplay();
            });

            // Hover effects
            star.setOnMouseEntered(_e -> {
                for (int j = 0; j < rating; j++) {
                    stars.get(j).setStyle(
                            "-fx-font-size: 20; -fx-text-fill: #f39c12; -fx-background-color: transparent; -fx-border-color: transparent;");
                }
                for (int j = rating; j < stars.size(); j++) {
                    stars.get(j).setStyle(
                            "-fx-font-size: 20; -fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-border-color: transparent;");
                }
            });

            star.setOnMouseExited(_e -> updateStarDisplay());
        }
    }

    /**
     * Aggiorna lo stile delle stelle in base alla valutazione selezionata.
     * @since 1.0
     */
    private void updateStarDisplay() {
        for (int i = 0; i < stars.size(); i++) {
            if (i < selectedRating) {
                stars.get(i).setStyle(
                        "-fx-font-size: 20; -fx-text-fill: #f39c12; -fx-background-color: transparent; -fx-border-color: transparent;");
            } else {
                stars.get(i).setStyle(
                        "-fx-font-size: 20; -fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-border-color: transparent;");
            }
        }
    }

    /**
     * Imposta il ristorante per il quale si sta creando la recensione.
     * @param ristorante Oggetto Ristorante, must be non-null.
     * @since 1.0
     */
    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
        ristoranteLabel.setText("Recensione per: " + ristorante.getName());
    }

    /**
     * Imposta l'username corrente dell'utente che scrive la recensione.
     * @param username Username corrente, può essere null.
     * @since 1.0
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    /**
     * Imposta il controller genitore di tipo DettaglioRistoranteController.
     * @param parentController Controller genitore.
     * @since 1.0
     */
    public void setParentController(DettaglioRistoranteController parentController) {
        this.dettaglioParentController = parentController;
    }

    /**
     * Imposta il controller genitore di tipo EsploraRistorantiController.
     * @param parentController Controller genitore.
     * @since 1.0
     */
    public void setParentController(EsploraRistorantiController parentController) {
        this.esploraParentController = parentController;
    }

    /**
     * Imposta il controller genitore di tipo DashboardClienteController.
     * @param parentController Controller genitore.
     * @since 1.0
     */
    public void setParentController(DashboardClienteController parentController) {
        this.dashboardClienteParentController = parentController;
    }

    /**
     * Gestisce la pubblicazione della recensione: valida i campi, crea
     * l'oggetto Recensione, lo salva e aggiorna i parent controller.
     * Mostra messaggi di errore in caso di problemi.
     *
     * @since 1.0
     */
    @FXML
    private void handlePubblica() {
        // Validazione
        if (selectedRating == 0) {
            errorLabel.setText("Seleziona una valutazione con le stelle!");
            return;
        }

        if (titoloField.getText().trim().isEmpty()) {
            errorLabel.setText("Inserisci un titolo per la recensione!");
            return;
        }

        if (commentoArea.getText().trim().isEmpty()) {
            errorLabel.setText("Scrivi un commento per la recensione!");
            return;
        }

        try {
            // Crea la nuova recensione
            Recensione nuovaRecensione = new Recensione(
                    currentUser,
                    ristorante.getName(),
                    selectedRating,
                    titoloField.getText().trim(),
                    commentoArea.getText().trim());

            // Salva la recensione
            RecensioniManager.aggiungiRecensione(nuovaRecensione); // Aggiorna il parent controller appropriato
            if (dettaglioParentController != null) {
                dettaglioParentController.refreshRecensioni();
            }
            if (esploraParentController != null) {
                esploraParentController.refreshView();
            }
            if (dashboardClienteParentController != null) {
                dashboardClienteParentController.refreshData();
            }

            // Chiudi la finestra
            Stage stage = (Stage) titoloField.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            errorLabel.setText("Errore nel salvare la recensione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Chiude la finestra di inserimento della recensione senza salvare.
     * @since 1.0
     */
    @FXML
    private void handleAnnulla() {
        Stage stage = (Stage) titoloField.getScene().getWindow();
        stage.close();
    }
}