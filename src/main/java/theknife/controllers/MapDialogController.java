package theknife.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import theknife.models.Ristorante;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller per la finestra di dialogo mappa.
 * Mostra coordinate, URL Google Maps e fornisce comandi per copiare
 * e aprire la posizione nel browser.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class MapDialogController implements Initializable {
    @FXML
    private Text titleLabel;
    @FXML
    private TextField latitudeField;
    @FXML
    private TextField longitudeField;
    @FXML
    private TextField urlField;

    private Ristorante restaurant;
    private String mapUrl;

    /**
     * Inizializzazione del controller (JavaFX lifecycle).
     *
     * @param location  URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializzazione se necessaria
    }

    /**
     * Imposta il ristorante da mostrare nella dialog e aggiorna i campi.
     *
     * @param restaurant Oggetto Ristorante, must be non-null.
     * @since 1.0
     */
    public void setRestaurant(Ristorante restaurant) {
        this.restaurant = restaurant;
        updateFields();
    }

    /**
     * Aggiorna i campi della UI (coordinate e URL Google Maps) in base al ristorante impostato.
     * Non modifica lo stato esterno all'applicazione.
     *
     * @since 1.0
     */
    private void updateFields() {
        if (restaurant != null) {
            titleLabel.setText("Posizione - " + restaurant.getName());
            latitudeField.setText(String.valueOf(restaurant.getLatitude()));
            longitudeField.setText(String.valueOf(restaurant.getLongitude()));

            mapUrl = String.format(java.util.Locale.US, "https://www.google.com/maps?q=%.6f,%.6f",
                    restaurant.getLatitude(), restaurant.getLongitude());
            urlField.setText(mapUrl);
        }
    }

    /**
     * Copia la latitudine negli appunti di sistema.
     *
     * @since 1.0
     */
    @FXML
    private void copyLatitude() {
        copyToClipboard(latitudeField.getText());
    }

    /**
     * Copia la longitudine negli appunti di sistema.
     *
     * @since 1.0
     */
    @FXML
    private void copyLongitude() {
        copyToClipboard(longitudeField.getText());
    }

    /**
     * Copia l'URL di Google Maps negli appunti di sistema.
     *
     * @since 1.0
     */
    @FXML
    private void copyUrl() {
        copyToClipboard(urlField.getText());
    }

    /**
     * Apre la posizione corrente nel browser predefinito.
     * <p>
     * Se il desktop è supportato prova ad aprire l'URI, altrimenti non fa nulla.
     * Eventuali eccezioni vengono catturate internamente.
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void openInBrowser() {
        if (mapUrl != null && !mapUrl.isEmpty()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(mapUrl));
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Chiude la finestra di dialogo mappa.
     *
     * @since 1.0
     */
    @FXML
    private void close() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Copia il testo fornito negli appunti di sistema.
     *
     * @param text Testo da copiare, deve essere non-null.
     * @since 1.0
     */
    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }
}