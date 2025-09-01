package theknife;

/* * Main.java
 *
 * Classe principale per avviare l'applicazione "The Knife" che gestisce un sistema di ristoranti.
 * Inizializza i dati dei ristoranti da un file CSV e carica la schermata di login.
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import theknife.models.Ristorante;
import theknife.utils.FileManager;
import java.util.List;

/**
 * Classe principale dell'applicazione JavaFX "The Knife".
 * <p>
 * Inizializza i dati dei ristoranti e avvia la finestra principale dell'applicazione.
 * Contiene inoltre un helper per impostare l'icona dell'applicazione su uno Stage.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class Main extends Application {
    /**
     * Lista pubblica di ristoranti caricata all'avvio dell'applicazione.
     * <p>
     * Può essere utilizzata dai controller e dai servizi per ottenere i dati dei ristoranti.
     * </p>
     * @since 1.0
     */
    public static List<Ristorante> ristoranti;

    /**
     * Imposta l'icona dell'applicazione sullo stage fornito.
     *
     * @param stage Stage su cui impostare l'icona, must be non-null.
     * @since 1.0
     */
    public static void setApplicationIcon(Stage stage) {
        try {
            stage.getIcons().add(new javafx.scene.image.Image(
                    Main.class.getResourceAsStream("/images/icon.png")));
        } catch (Exception e) {
            System.out.println("Icona non trovata: " + e.getMessage());
        }
    }

    /**
     * Avvia l'applicazione JavaFX, caricando i ristoranti e mostrando la view principale.
     *
     * @param primaryStage Stage principale fornito da JavaFX.
     * @throws Exception se non è possibile caricare le risorse o la view.
     * @since 1.0
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carica dati iniziali
        ristoranti = FileManager.caricaRistorantiDaCSV("src/main/resources/data/michelin_my_maps.csv");

        // Carica la schermata principale
        Parent root = FXMLLoader.load(getClass().getResource("/views/home.fxml"));

        // Crea la scena e applica il CSS
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

        primaryStage.setTitle("The Knife - Ristoranti Michelin");

        // Imposta l'icona della finestra
        setApplicationIcon(primaryStage);

        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    /**
     * Punto d'ingresso principale per l'applicazione quando eseguita come jar.
     *
     * @param args Argomenti della riga di comando (ignorati).
     * @since 1.0
     */
    public static void main(String[] args) {
        launch(args);
    }
}