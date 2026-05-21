package theknife;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import theknife.client.ClientTK;

import java.io.IOException;
import java.io.InputStream;

/**
 * Entry point dell'applicazione client TheKnife.
 *
 * All'avvio mostra un dialogo per configurare host e porta del server.
 * Una volta connesso, carica la home view (home.fxml) e l'utente può
 * interagire con il sistema tramite ClientTK (facade di rete).
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */

public class Main extends Application {

    /** Istanza globale del client di rete — accessibile da tutti i controller. */
    private static ClientTK client;

    @Override
    public void start(Stage primaryStage) {
        mostraDialogoConnessione(primaryStage);
    }

    /**
     * Mostra il dialogo di connessione al server.
     * Consente all'utente di inserire l'host e la porta del server.
     * All'avvenuto collegamento riuscito (eseguito asincronamente fuori dal thread della UI),
     * chiude il dialogo ed esegue il caricamento dell'interfaccia principale tramite {@link #caricaHome(Stage)}.
     *
     * @param primaryStage lo Stage principale dell'applicazione
     */
    private void mostraDialogoConnessione(Stage primaryStage) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Connessione al Server — The Knife");
        setApplicationIcon(dialogStage);

        Label titleLabel = new Label("TheKnife — Connessione al Server");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        TextField hostField = new TextField("localhost");
        hostField.setPromptText("Indirizzo IP o hostname");

        TextField portField = new TextField("9090");
        portField.setPromptText("Porta");
        // Solo numeri nella porta
        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) portField.setText(oldVal);
        });

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setWrapText(true);

        Button connectButton = new Button("Connetti");
        connectButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                "-fx-font-size: 14; -fx-padding: 8 24;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setVisible(false);
        spinner.setMaxSize(24, 24);

        HBox buttonBox = new HBox(10, connectButton, spinner);
        buttonBox.setAlignment(Pos.CENTER);

        connectButton.setOnAction(e -> {
            String host = hostField.getText().trim();
            String portText = portField.getText().trim();
            if (host.isEmpty() || portText.isEmpty()) {
                errorLabel.setText("Inserisci host e porta.");
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portText);
            } catch (NumberFormatException ex) {
                errorLabel.setText("La porta deve essere un numero valido.");
                return;
            }

            connectButton.setDisable(true);
            spinner.setVisible(true);
            errorLabel.setText("");

            // Tenta la connessione in un thread separato per non bloccare la UI
            new Thread(() -> {
                try {
                    ClientTK nuovoClient = new ClientTK(host, port);
                    Platform.runLater(() -> {
                        client = nuovoClient;
                        dialogStage.close();
                        caricaHome(primaryStage);
                    });
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        errorLabel.setText("Impossibile connettersi a " + host + ":" + port +
                                "\n" + ex.getMessage());
                        connectButton.setDisable(false);
                        spinner.setVisible(false);
                    });
                }
            }).start();
        });

        VBox layout = new VBox(15, titleLabel,
                new Label("Host:"), hostField,
                new Label("Porta:"), portField,
                buttonBox, errorLabel);
        layout.setPadding(new Insets(30));
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setStyle("-fx-background-color: #f8f9fa;");

        Scene scene = new Scene(layout, 400, 350);
        dialogStage.setScene(scene);
        dialogStage.setResizable(false);
        dialogStage.setOnCloseRequest(e -> Platform.exit());
        dialogStage.show();
    }

    /**
     * Carica e visualizza la schermata principale dell'applicazione (Home) caricandola
     * da file FXML ed associando i relativi fogli di stile CSS.
     *
     * @param primaryStage lo Stage principale su cui caricare la scena
     */
    private void caricaHome(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/home.fxml"));
            Parent root = loader.load();

            setApplicationIcon(primaryStage);
            primaryStage.setTitle("The Knife — Guida ai Ristoranti");

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.setOnCloseRequest(e -> {
                try { if (client != null) client.close(); } catch (IOException ignored) {}
                Platform.exit();
            });
            primaryStage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setContentText("Impossibile caricare l'interfaccia grafica: " + e.getMessage());
            alert.showAndWait();
            Platform.exit();
        }
    }

    // -------------------------------------------------------------------------
    // API statica per i controller
    // -------------------------------------------------------------------------

    /**
     * Restituisce l'istanza singleton del client di rete attiva e connessa.
     *
     * @return l'istanza corrente di {@link ClientTK}
     */
    public static ClientTK getClient() {
        return client;
    }

    /**
     * Imposta l'icona dell'applicazione su uno Stage caricandola dalle risorse.
     *
     * @param stage lo Stage a cui applicare l'icona
     */
    public static void setApplicationIcon(Stage stage) {
        try (InputStream is = Main.class.getResourceAsStream("/images/icon.png")) {
            if (is != null) {
                stage.getIcons().add(new Image(is));
            }
        } catch (IOException ignored) {}
    }

    /**
     * Main method dell'applicazione JavaFX.
     *
     * @param args argomenti a riga di comando
     */
    public static void main(String[] args) {
        launch(args);
    }
}
