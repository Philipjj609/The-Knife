package theknife.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Navigatore delle schermate JavaFX all'interno del pannello principale dell'applicazione.
 * Gestisce una pila (history) di nodi per supportare l'operazione di "torna indietro".
 *
 * Mantiene lo stato di navigazione centralizzato (simile al pattern Controller di navigazione).
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
final class AppNavigator {

    /** Il pannello contenitore principale delle viste. */
    private static StackPane contentPane;

    /** La cronologia dei nodi visualizzati (pila LIFO). */
    private static final Deque<Node> history = new ArrayDeque<>();

    /**
     * Costruttore privato per impedire l'istanziazione di questa classe di utility.
     */
    private AppNavigator() {}

    /**
     * Inizializza il navigatore associando il pannello contenitore e azzerando la cronologia.
     *
     * @param pane il {@link StackPane} su cui caricare e alternare le viste
     * @param controller il controller principale della home
     */
    static void initialize(StackPane pane, HomeController controller) {
        contentPane = pane;
        history.clear();
    }

    /**
     * Svuota completamente la cronologia delle schermate visitate.
     */
    static void clearHistory() {
        history.clear();
    }

    /**
     * Mostra una nuova vista caricandola da FXML e inserendo la vista precedente nella cronologia.
     *
     * @param <T> il tipo generico del controller associato alla nuova vista
     * @param fxmlPath il percorso del file FXML relativo alla risorsa
     * @param configureController una lambda expression per configurare il controller prima della visualizzazione
     * @return il controller istanziato ed eventualmente configurato
     * @throws IOException se si verifica un errore durante il caricamento del file FXML
     */
    static <T> T show(String fxmlPath, Consumer<T> configureController) throws IOException {
        return load(fxmlPath, configureController, true);
    }

    /**
     * Sostituisce la vista corrente con una nuova senza salvare la schermata precedente nella cronologia.
     *
     * @param <T> il tipo generico del controller della nuova vista
     * @param fxmlPath il percorso del file FXML relativo alla risorsa
     * @param configureController una lambda expression per configurare il controller
     * @return il controller istanziato
     * @throws IOException se si verifica un errore durante il caricamento del file FXML
     */
    static <T> T replace(String fxmlPath, Consumer<T> configureController) throws IOException {
        return load(fxmlPath, configureController, false);
    }

    /**
     * Ritorna alla schermata precedente se presente nella cronologia.
     *
     * @return true se il ripristino ha successo (cronologia non vuota), false altrimenti
     */
    static boolean goBack() {
        if (contentPane == null || history.isEmpty()) {
            return false;
        }

        contentPane.getChildren().setAll(history.pop());
        return true;
    }

    /**
     * Torna indietro se possibile; se la cronologia è vuota, tenta di chiudere la finestra (Stage) corrente.
     *
     * @param node un nodo grafico appartenente alla finestra corrente da chiudere
     */
    static void goBackOrClose(Node node) {
        if (goBack()) {
            return;
        }

        if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    /**
     * Esegue il caricamento effettivo del file FXML, istanzia il relativo controller
     * ed effettua la transizione della vista sul pannello contenitore.
     *
     * @param <T> il tipo generico del controller
     * @param fxmlPath il percorso del file FXML da caricare
     * @param configureController azione di configurazione sul controller
     * @param pushCurrent true se si vuole salvare la schermata corrente nella pila di cronologia
     * @return il controller della nuova vista
     * @throws IOException se il file FXML non può essere letto o caricato
     * @throws IllegalStateException se il navigatore non è stato inizializzato
     */
    private static <T> T load(String fxmlPath, Consumer<T> configureController, boolean pushCurrent)
            throws IOException {
        if (contentPane == null) {
            throw new IllegalStateException("Navigatore non inizializzato");
        }

        FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(fxmlPath));
        Parent root = loader.load();
        T controller = loader.getController();

        if (configureController != null && controller != null) {
            configureController.accept(controller);
        }

        if (pushCurrent && !contentPane.getChildren().isEmpty()) {
            history.push(contentPane.getChildren().get(0));
        }

        contentPane.getChildren().setAll(root);
        return controller;
    }
}
