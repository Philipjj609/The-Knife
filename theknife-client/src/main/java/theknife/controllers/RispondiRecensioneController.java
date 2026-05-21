package theknife.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Risposta;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller JavaFX per la vista di inserimento di una risposta a una recensione.
 * Consente ad un ristoratore di rispondere a una recensione lasciata da un cliente.
 *
 * <p>Fa parte del pattern <b>MVC (Model-View-Controller)</b> come Controller.
 * L'invio della risposta al server avviene asincronamente in un thread di background
 * tramite {@link Task} per non bloccare l'interfaccia grafica.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class RispondiRecensioneController implements Initializable {

    /** Label che mostra informazioni riassuntive del ristorante e della recensione (nome, stelle, data). */
    @FXML private Label recensioneInfoLabel;

    /** Label per visualizzare il titolo della recensione. */
    @FXML private Label recensioneTitoloLabel;

    /** Label per visualizzare il commento testuale del cliente. */
    @FXML private Label recensioneCommentoLabel;

    /** Area di testo dove inserire la risposta del ristoratore. */
    @FXML private TextArea rispostaArea;

    /** Label per visualizzare eventuali messaggi di errore. */
    @FXML private Label errorLabel;

    /** L'oggetto recensione a cui si sta rispondendo. */
    private Recensione recensione;

    /** Lo username del ristoratore corrente che risponde. */
    private String currentUser;

    /** Riferimento al controller padre del ristoratore per aggiornare la dashboard in tempo reale. */
    private DashboardRistoratoreController parentController;

    /**
     * Metodo di inizializzazione JavaFX.
     *
     * @param location l'URL FXML
     * @param resources il ResourceBundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {}

    /**
     * Associa la recensione di riferimento e popola dinamicamente i campi descrittivi nella vista.
     *
     * @param recensione l'oggetto {@link Recensione} a cui rispondere
     */
    public void setRecensione(Recensione recensione) {
        this.recensione = recensione;
        popolaCampiRecensione();
    }

    /**
     * Imposta lo username dell'utente correntemente loggato.
     *
     * @param username lo username dell'utente
     */
    public void setCurrentUser(String username) { this.currentUser = username; }

    /**
     * Imposta il controller della dashboard del ristoratore da aggiornare dopo l'invio.
     *
     * @param parent il controller {@link DashboardRistoratoreController}
     */
    public void setParentController(DashboardRistoratoreController parent) { this.parentController = parent; }

    /**
     * Popola l'interfaccia con i dati relativi alla recensione selezionata.
     */
    private void popolaCampiRecensione() {
        if (recensione == null) return;
        recensioneInfoLabel.setText(String.format("%s - %s (%s)",
                recensione.getNomeRistorante(), recensione.getStelle(), recensione.getDataRecensioneFormatted()));
        recensioneTitoloLabel.setText(recensione.getTitolo());
        recensioneCommentoLabel.setText(recensione.getCommento());
    }

    /**
     * Gestisce l'invio della risposta.
     * Valida la lunghezza e la presenza del testo della risposta,
     * quindi effettua una chiamata di rete asincrona tramite {@link ClientTK#rispondiRecensione(Risposta)}.
     * All'avvenuta risposta riuscita, rinfresca la dashboard del proprietario e chiude la finestra.
     */
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
            AppNavigator.goBackOrClose(rispostaArea);
        });

        task.setOnFailed(e -> errorLabel.setText("Errore nell'inviare la risposta: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    /**
     * Gestisce l'annullamento dell'operazione e il ritorno alla schermata precedente.
     */
    @FXML
    private void handleAnnulla() {
        AppNavigator.goBackOrClose(rispostaArea);
    }
}
