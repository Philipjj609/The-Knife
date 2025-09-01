package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import theknife.models.Ristorante;
import theknife.utils.FileManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller per la finestra di aggiunta di un nuovo ristorante.
 * Gestisce l'interfaccia di input, la validazione dei campi e la
 * creazione/salvataggio del Ristorante nel CSV e nella lista in memoria.
 * Utilizzato principalmente dal dashboard del ristoratore.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class AggiungiRistoranteController implements Initializable {

    @FXML
    private TextField nomeField;
    @FXML
    private ComboBox<String> cucinaCombo;
    @FXML
    private ComboBox<String> prezzoCombo;
    @FXML
    private TextField indirizzoField;
    @FXML
    private TextField localitaField;
    @FXML
    private TextField latitudineField;
    @FXML
    private TextField longitudineField;
    @FXML
    private TextField telefonoField;
    @FXML
    private TextField sitoWebField;
    @FXML
    private TextArea descrizioneArea;
    @FXML
    private TextArea serviziArea;
    @FXML
    private ComboBox<String> premioCombo;
    @FXML
    private CheckBox stellaVerdeCheck;
    @FXML
    private CheckBox deliveryCheck;
    @FXML
    private CheckBox prenotazioneOnlineCheck;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;

    private String currentUser; // Username del ristoratore
    private DashboardRistoratoreController parentController;

    /**
     * Inizializza i controlli UI e aggiunge listener per la validazione dei campi.
     *
     * @param location URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializza gli elementi dei ComboBox
        cucinaCombo.setItems(FXCollections.observableArrayList(
                "Italiana", "Mediterranea", "Francese", "Giapponese", "Cinese",
                "Indiana", "Messicana", "Americana", "Fusion", "Creative",
                "Contemporary", "Seafood", "Vegetariana", "Pizza", "Altro"));

        prezzoCombo.setItems(FXCollections.observableArrayList(
                "€", "€€", "€€€", "€€€€", "$$", "$$$", "$$$$"));

        premioCombo.setItems(FXCollections.observableArrayList(
                "Nessuno", "Bib Gourmand", "1 Stella", "2 Stelle", "3 Stelle",
                "Green Star", "Young Chef Award", "Service Award"));

        // Inizializza le ComboBox con valori predefiniti
        premioCombo.setValue("Nessuno"); // Aggiungi validazione per i campi numerici
        latitudineField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*\\.?\\d*")) {
                latitudineField.setText(oldValue);
            }
        });

        longitudineField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*\\.?\\d*")) {
                longitudineField.setText(oldValue);
            }
        });

        // Aggiungi listener per il telefono (solo numeri, spazi, +, -, ())
        telefonoField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\s\\+\\-\\(\\)]*")) {
                telefonoField.setText(oldValue);
            }
        });
    }

    /**
     * Imposta l'username corrente (proprietario) che aggiunge il ristorante.
     *
     * @param username Username del ristoratore, must be non-null.
     * @since 1.0
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    /**
     * Imposta il controller genitore (dashboard ristoratore) per aggiornamenti.
     *
     * @param parentController Controller genitore.
     * @since 1.0
     */
    public void setParentController(DashboardRistoratoreController parentController) {
        this.parentController = parentController;
    }

    /**
     * Gestisce il salvataggio del nuovo ristorante dopo la validazione dei campi.
     * Crea l'oggetto Ristorante, lo salva via FileManager e aggiorna il parent.
     * Mostra messaggi di errore in caso di problemi.
     *
     * @since 1.0
     */
    @FXML
    private void handleSalva() {
        clearMessages();

        // Validazione campi obbligatori
        if (!validaCampi()) {
            return;
        }

        try {
            // Crea nuovo ristorante con i nuovi servizi
            Ristorante nuovoRistorante = new Ristorante(
                    nomeField.getText().trim(),
                    indirizzoField.getText().trim(),
                    localitaField.getText().trim(),
                    prezzoCombo.getValue(),
                    cucinaCombo.getValue(),
                    Double.parseDouble(longitudineField.getText().trim()),
                    Double.parseDouble(latitudineField.getText().trim()),
                    telefonoField.getText().trim(),
                    generaUrlGuida(),
                    sitoWebField.getText().trim().isEmpty() ? "" : sitoWebField.getText().trim(),
                    premioCombo.getValue().equals("Nessuno") ? "" : premioCombo.getValue(),
                    stellaVerdeCheck.isSelected() ? "1" : "0",
                    serviziArea.getText().trim().isEmpty() ? "Standard restaurant services"
                            : serviziArea.getText().trim(),
                    descrizioneArea.getText().trim(),
                    deliveryCheck.isSelected(),
                    prenotazioneOnlineCheck.isSelected());

            // Aggiungi proprietario
            nuovoRistorante.setProprietario(currentUser);

            // Salva nel CSV
            boolean salvato = FileManager.aggiungiRistoranteAlCSV(nuovoRistorante);

            if (salvato) {
                // Aggiorna anche la lista in memoria dell'applicazione
                theknife.Main.ristoranti.add(nuovoRistorante);

                successLabel.setText("✅ Ristorante aggiunto con successo! Sarà visibile nel sistema.");

                // Aggiorna il dashboard del parent se presente
                if (parentController != null) {
                    parentController.refreshData();
                }

                // Chiudi la finestra dopo 2 secondi
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(() -> handleAnnulla());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } else {
                errorLabel.setText("❌ Errore durante il salvataggio. Riprova.");
            }

        } catch (NumberFormatException e) {
            errorLabel.setText("❌ Errore: Latitudine e Longitudine devono essere numeri validi.");
        } catch (Exception e) {
            errorLabel.setText("❌ Errore imprevisto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Valida i campi del form e imposta il messaggio di errore se necessario.
     *
     * @return true se tutti i campi obbligatori sono validi, false altrimenti.
     * @since 1.0
     */
    private boolean validaCampi() {
        StringBuilder errori = new StringBuilder();

        if (nomeField.getText().trim().isEmpty()) {
            errori.append("• Nome del ristorante è obbligatorio\n");
        }

        if (cucinaCombo.getValue() == null || cucinaCombo.getValue().isEmpty()) {
            errori.append("• Tipo di cucina è obbligatorio\n");
        }

        if (prezzoCombo.getValue() == null || prezzoCombo.getValue().isEmpty()) {
            errori.append("• Fascia di prezzo è obbligatoria\n");
        }

        if (indirizzoField.getText().trim().isEmpty()) {
            errori.append("• Indirizzo è obbligatorio\n");
        }

        if (localitaField.getText().trim().isEmpty()) {
            errori.append("• Località è obbligatoria\n");
        }

        if (latitudineField.getText().trim().isEmpty()) {
            errori.append("• Latitudine è obbligatoria\n");
        } else {
            try {
                double lat = Double.parseDouble(latitudineField.getText().trim());
                if (lat < -90 || lat > 90) {
                    errori.append("• Latitudine deve essere tra -90 e 90\n");
                }
            } catch (NumberFormatException e) {
                errori.append("• Latitudine deve essere un numero valido\n");
            }
        }

        if (longitudineField.getText().trim().isEmpty()) {
            errori.append("• Longitudine è obbligatoria\n");
        } else {
            try {
                double lon = Double.parseDouble(longitudineField.getText().trim());
                if (lon < -180 || lon > 180) {
                    errori.append("• Longitudine deve essere tra -180 e 180\n");
                }
            } catch (NumberFormatException e) {
                errori.append("• Longitudine deve essere un numero valido\n");
            }
        }

        if (telefonoField.getText().trim().isEmpty()) {
            errori.append("• Numero di telefono è obbligatorio\n");
        }

        if (descrizioneArea.getText().trim().isEmpty()) {
            errori.append("• Descrizione è obbligatoria\n");
        } else if (descrizioneArea.getText().trim().length() < 50) {
            errori.append("• Descrizione deve essere di almeno 50 caratteri\n");
        }

        // Verifica se il nome esiste già
        if (FileManager.esisteRistorante(nomeField.getText().trim())) {
            errori.append("• Esiste già un ristorante con questo nome\n");
        }

        if (errori.length() > 0) {
            errorLabel.setText("❌ Errori di validazione:\n" + errori.toString());
            return false;
        }

        return true;
    }

    /**
     * Genera un URL fittizio per la guida Michelin basato su nome e località.
     *
     * @return URL generato come String.
     * @since 1.0
     */
    private String generaUrlGuida() {
        // Genera un URL fittizio per la guida Michelin
        String nomeFormatted = nomeField.getText().trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");

        String locationFormatted = localitaField.getText().trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s,]", "")
                .replaceAll("\\s+", "-")
                .replaceAll(",", "");

        return "https://guide.michelin.com/en/" + locationFormatted + "/restaurant/" + nomeFormatted;
    }

    /**
     * Pulisce i messaggi di successo/errore nella UI.
     * @since 1.0
     */
    private void clearMessages() {
        errorLabel.setText("");
        successLabel.setText("");
    }

    /**
     * Chiude la finestra di aggiunta ristorante senza salvare.
     * @since 1.0
     */
    @FXML
    private void handleAnnulla() {
        ((Stage) nomeField.getScene().getWindow()).close();
    }
}

