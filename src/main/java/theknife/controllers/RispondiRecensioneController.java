package theknife.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import theknife.models.Recensione;
import theknife.models.Risposta;
import theknife.services.RecensioniManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller per la finestra che permette al ristoratore di rispondere
 * a una recensione. Gestisce la validazione del testo di risposta,
 * la creazione dell'oggetto Risposta e l'aggiornamento tramite
 * RecensioniManager.
 *
 * Autore e versione mantenuti dal progetto.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class RispondiRecensioneController implements Initializable {

    @FXML
    private Label recensioneInfoLabel;
    @FXML
    private Label recensioneTitoloLabel;
    @FXML
    private Label recensioneCommentoLabel;
    @FXML
    private TextArea rispostaArea;
    @FXML
    private Label errorLabel;

    private Recensione recensione;
    private String currentUser;
    private DashboardRistoratoreController parentController;

    /**
     * Inizializzazione del controller (JavaFX lifecycle).
     *
     * @param location  URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup initial state
    }

    /**
     * Imposta la recensione da visualizzare nella finestra di risposta.
     *
     * @param recensione Recensione da rispondere, must be non-null.
     * @since 1.0
     */
    public void setRecensione(Recensione recensione) {
        this.recensione = recensione;
        popolaCampiRecensione();
    }

    /**
     * Imposta l'username corrente del ristoratore che risponde.
     *
     * @param username Username corrente, può essere null.
     * @since 1.0
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    /**
     * Imposta il controller genitore (DashboardRistoratoreController) per callback/aggiornamenti.
     *
     * @param parentController Controller genitore.
     * @since 1.0
     */
    public void setParentController(DashboardRistoratoreController parentController) {
        this.parentController = parentController;
    }

    /**
     * Popola i campi della UI con i dettagli della recensione impostata.
     * Non modifica lo stato dell'app tranne l'aggiornamento visivo.
     *
     * @since 1.0
     */
    private void popolaCampiRecensione() {
        if (recensione == null)
            return;

        recensioneInfoLabel.setText(String.format("%s - %s (%s)",
                recensione.getNomeRistorante(),
                recensione.getStelle(),
                recensione.getDataRecensioneFormatted()));

        recensioneTitoloLabel.setText(recensione.getTitolo());
        recensioneCommentoLabel.setText(recensione.getCommento());
        // Non serve più setWrappingWidth perché ora è un Label con wrapText="true" nel
        // FXML
    }

    /**
     * Valida e invia la risposta al manager delle recensioni.
     * <p>
     * Crea un oggetto Risposta e lo associa alla recensione corrente tramite RecensioniManager.
     * Aggiorna il controller genitore e chiude la finestra in caso di successo.
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void handleInviaRisposta() {
        // Validazione
        if (rispostaArea.getText().trim().isEmpty()) {
            errorLabel.setText("Scrivi una risposta prima di inviarla!");
            return;
        }

        if (rispostaArea.getText().trim().length() < 10) {
            errorLabel.setText("La risposta deve essere di almeno 10 caratteri!");
            return;
        }

        try {
            // Crea la risposta
            Risposta nuovaRisposta = new Risposta(
                    currentUser,
                    rispostaArea.getText().trim());

            // Aggiungi la risposta alla recensione
            RecensioniManager.aggiungiRisposta(recensione, nuovaRisposta);

            // Aggiorna il parent controller se presente
            if (parentController != null) {
                parentController.refreshData();
            }

            // Chiudi la finestra
            Stage stage = (Stage) rispostaArea.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            errorLabel.setText("Errore nell'inviare la risposta: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Chiude la finestra di risposta senza salvare.
     *
     * @since 1.0
     */
    @FXML
    private void handleAnnulla() {
        Stage stage = (Stage) rispostaArea.getScene().getWindow();
        stage.close();
    }
}