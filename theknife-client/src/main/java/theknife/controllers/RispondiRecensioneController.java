package theknife.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Risposta;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller per rispondere a una recensione.
 * Usa ClientTK per salvare la risposta sul server.
 *
 * @author Philip Jon Ji Ciuca
 * @version 2.0
 */
public class RispondiRecensioneController implements Initializable {

    @FXML private Label recensioneInfoLabel;
    @FXML private Label recensioneTitoloLabel;
    @FXML private Label recensioneCommentoLabel;
    @FXML private TextArea rispostaArea;
    @FXML private Label errorLabel;

    private Recensione recensione;
    private String currentUser;
    private DashboardRistoratoreController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    public void setRecensione(Recensione recensione) {
        this.recensione = recensione;
        popolaCampiRecensione();
    }

    public void setCurrentUser(String username) { this.currentUser = username; }
    public void setParentController(DashboardRistoratoreController parent) { this.parentController = parent; }

    private void popolaCampiRecensione() {
        if (recensione == null) return;
        recensioneInfoLabel.setText(String.format("%s - %s (%s)",
                recensione.getNomeRistorante(), recensione.getStelle(), recensione.getDataRecensioneFormatted()));
        recensioneTitoloLabel.setText(recensione.getTitolo());
        recensioneCommentoLabel.setText(recensione.getCommento());
    }

    @FXML
    private void handleInviaRisposta() {
        if (rispostaArea.getText().trim().isEmpty()) {
            errorLabel.setText("Scrivi una risposta prima di inviarla!");
            return;
        }
        if (rispostaArea.getText().trim().length() < 10) {
            errorLabel.setText("La risposta deve essere di almeno 10 caratteri!");
            return;
        }

        Risposta nuova = new Risposta(currentUser, recensione.getId(), rispostaArea.getText().trim());

        Task<Risposta> task = new Task<>() {
            @Override
            protected Risposta call() {
                return Main.getClient().rispondiRecensione(nuova);
            }
        };

        task.setOnSucceeded(e -> {
            if (parentController != null) parentController.refreshData();
            ((Stage) rispostaArea.getScene().getWindow()).close();
        });

        task.setOnFailed(e -> errorLabel.setText("Errore nell'inviare la risposta: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    @FXML
    private void handleAnnulla() {
        ((Stage) rispostaArea.getScene().getWindow()).close();
    }
}
