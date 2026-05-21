package theknife.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import theknife.models.Ristorante;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller JavaFX per il dialogo di visualizzazione geografica del ristorante (Mappa).
 * Mostra le coordinate geografiche (latitudine, longitudine) e un URL cliccabile/copiabile
 * per localizzare il ristorante su Google Maps.
 *
 * <p>Fa parte del pattern <b>MVC (Model-View-Controller)</b> come Controller.
 * Le interazioni grafiche, la gestione della clipboard di sistema e l'avvio del browser predefinito
 * avvengono in modo sincrono sul thread JavaFX.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class MapDialogController implements Initializable {

    /**
     * Costruttore di default per {@link MapDialogController}.
     * Necessario per l'inizializzazione tramite FXML loader.
     */
    public MapDialogController() {}

    /** Label per il titolo del dialogo che riporta il nome del ristorante. */
    @FXML private Text titleLabel;

    /** Campo di testo di sola lettura per la latitudine del ristorante. */
    @FXML private TextField latitudeField;

    /** Campo di testo di sola lettura per la longitudine del ristorante. */
    @FXML private TextField longitudeField;

    /** Campo di testo di sola lettura per l'URL di Google Maps del ristorante. */
    @FXML private TextField urlField;

    /** Il ristorante correntemente mostrato nella dialog. */
    private Ristorante restaurant;

    /** L'URL formattato di Google Maps corrispondente alle coordinate del ristorante. */
    private String mapUrl;

    /**
     * Metodo di inizializzazione JavaFX.
     *
     * @param location l'URL FXML
     * @param resources il ResourceBundle
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializzazione se necessaria
    }

    /**
     * Associa il ristorante a questa dialog ed esegue il popolamento dei relativi campi.
     *
     * @param restaurant il {@link Ristorante} da visualizzare
     */
    public void setRestaurant(Ristorante restaurant) {
        this.restaurant = restaurant;
        updateFields();
    }

    /**
     * Aggiorna i campi testuali della UI formattando le coordinate e calcolando l'URL di Google Maps.
     */
    private void updateFields() {
        if (restaurant != null) {
            titleLabel.setText("Posizione - " + restaurant.getNome());
            latitudeField.setText(String.valueOf(restaurant.getLatitudine()));
            longitudeField.setText(String.valueOf(restaurant.getLongitudine()));

            mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    restaurant.getLatitudine(), restaurant.getLongitudine());
            urlField.setText(mapUrl);
        }
    }

    /**
     * Copia il valore della latitudine negli appunti (Clipboard) del sistema operativo.
     */
    @FXML
    private void copyLatitude() {
        copyToClipboard(latitudeField.getText());
    }

    /**
     * Copia il valore della longitudine negli appunti (Clipboard) del sistema operativo.
     */
    @FXML
    private void copyLongitude() {
        copyToClipboard(longitudeField.getText());
    }

    /**
     * Copia l'URL di Google Maps generato negli appunti (Clipboard) del sistema operativo.
     */
    @FXML
    private void copyUrl() {
        copyToClipboard(urlField.getText());
    }



    /**
     * Chiude la finestra di dialogo ritornando alla vista principale.
     */
    @FXML
    private void close() {
        AppNavigator.goBackOrClose(titleLabel);
    }

    /**
     * Metodo di supporto per inserire una stringa testuale nella clipboard di sistema.
     *
     * @param text la stringa da copiare
     */
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}
