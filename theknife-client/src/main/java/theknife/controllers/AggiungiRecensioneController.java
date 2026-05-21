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

/**
 * Controller per la finestra di inserimento e modifica di una recensione.
 *
 * Gestisce la valutazione a stelle e l'invio dei dati al server.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class AggiungiRecensioneController implements Initializable {

    /** Etichetta di testo che mostra il nome del ristorante destinatario della recensione. */
    @FXML private Text ristoranteLabel;

    /** Pulsanti toggle per la valutazione a stelle (da 1 a 5). */
    @FXML private ToggleButton star1, star2, star3, star4, star5;

    /** Campo di testo per l'inserimento del titolo della recensione. */
    @FXML private TextField titoloField;

    /** Area di testo per la scrittura del commento esteso della recensione. */
    @FXML private TextArea commentoArea;

    /** Etichetta per la visualizzazione di messaggi di errore o validazione. */
    @FXML private Label errorLabel;

    /** Pulsante per confermare l'invio della recensione (nuova o modificata). */
    @FXML private Button pubblicaButton;

    /** Lista di pulsanti a stelle per facilitare la gestione cumulativa dell'interfaccia. */
    private List<ToggleButton> stars;

    /** Modello del ristorante a cui si riferisce la recensione. */
    private Ristorante ristorante;

    /** Username dell'utente corrente che scrive la recensione. */
    private String currentUser;

    /** Istanza della recensione esistente in caso di operazione di modifica (null se si tratta di una nuova recensione). */
    private Recensione recensioneEsistente;

    /** Riferimento al controller del dettaglio ristorante parent per l'aggiornamento dei dati. */
    private DettaglioRistoranteController dettaglioParentController;

    /** Riferimento al controller di esplorazione ristoranti parent per l'aggiornamento dei dati. */
    private EsploraRistorantiController esploraParentController;

    /** Riferimento al controller della dashboard cliente parent per l'aggiornamento dei dati. */
    private DashboardClienteController dashboardClienteParentController;

    /** Valutazione numerica selezionata dall'utente (da 1 a 5), inizialmente a 0. */
    private int selectedRating = 0;

    /**
     * Inizializza il controller JavaFX. Associa i pulsanti delle stelle e imposta
     * i listener per la selezione e per l'effetto hover.
     *
     * @param location  URL di localizzazione del file FXML
     * @param resources risorse localizzate utilizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        stars = List.of(star1, star2, star3, star4, star5);
        setupStarRating();
    }

    /**
     * Configura i listener di eventi sulle stelle per gestire il click e l'hover.
     */
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

    /**
     * Aggiorna lo stile grafico delle stelle in base al rating selezionato.
     */
    private void updateStarDisplay() {
        for (int i = 0; i < stars.size(); i++) {
            stars.get(i).setStyle(i < selectedRating
                    ? "-fx-font-size: 20; -fx-text-fill: #f39c12; -fx-background-color: transparent; -fx-border-color: transparent;"
                    : "-fx-font-size: 20; -fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-border-color: transparent;");
        }
    }

    /**
     * Imposta il ristorante da recensire e aggiorna l'interfaccia grafica.
     *
     * @param ristorante il ristorante associato alla recensione
     */
    public void setRistorante(Ristorante ristorante) {
        this.ristorante = ristorante;
        ristoranteLabel.setText("Recensione per: " + ristorante.getNome());
    }

    /**
     * Imposta l'utente che sta scrivendo la recensione.
     *
     * @param username lo username dell'utente
     */
    public void setCurrentUser(String username) { this.currentUser = username; }

    /**
     * Imposta il parent controller del dettaglio ristorante.
     *
     * @param c il controller di dettaglio ristorante
     */
    public void setParentController(DettaglioRistoranteController c) { this.dettaglioParentController = c; }

    /**
     * Imposta il parent controller dell'esplorazione dei ristoranti.
     *
     * @param c il controller di esplorazione
     */
    public void setParentController(EsploraRistorantiController c) { this.esploraParentController = c; }

    /**
     * Imposta il parent controller della dashboard cliente.
     *
     * @param c il controller della dashboard cliente
     */
    public void setParentController(DashboardClienteController c) { this.dashboardClienteParentController = c; }

    /**
     * Pre-compila il form con i dati di una recensione esistente per la modifica.
     *
     * @param r l'istanza della recensione esistente
     */
    public void setRecensioneEsistente(Recensione r) {
        this.recensioneEsistente = r;
        selectedRating = r.getValutazione();
        updateStarDisplay();
        titoloField.setText(r.getTitolo() != null ? r.getTitolo() : "");
        commentoArea.setText(r.getCommento() != null ? r.getCommento() : "");
        if (pubblicaButton != null) pubblicaButton.setText("Salva Modifiche");
    }

    /**
     * Gestisce l'azione di pubblicazione della recensione.
     * Valida l'input inserito dall'utente e avvia una richiesta di rete
     * asincrona tramite un JavaFX {@link Task} per comunicare con il server.
     * In caso di successo, notifica il controller parent per ricaricare le recensioni
     * e chiude la finestra. In caso di errore, visualizza il messaggio nella label di errore.
     */
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

    /**
     * Notifica i controller parent per aggiornare le viste con le ultime recensioni inserite o modificate.
     */
    private void notificaParent() {
        if (dettaglioParentController != null) dettaglioParentController.refreshRecensioni();
        if (esploraParentController != null) esploraParentController.refreshView();
        if (dashboardClienteParentController != null) dashboardClienteParentController.refreshData();
    }

    /**
     * Gestisce l'azione di annullamento dell'inserimento o della modifica della recensione,
     * chiudendo la finestra o tornando alla vista precedente.
     */
    @FXML
    private void handleAnnulla() {
        AppNavigator.goBackOrClose(titoloField);
    }
}
