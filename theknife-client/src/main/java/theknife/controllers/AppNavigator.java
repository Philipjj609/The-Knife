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

final class AppNavigator {

    private static StackPane contentPane;
    private static final Deque<Node> history = new ArrayDeque<>();

    private AppNavigator() {}

    static void initialize(StackPane pane, HomeController controller) {
        contentPane = pane;
        history.clear();
    }

    static void clearHistory() {
        history.clear();
    }

    static <T> T show(String fxmlPath, Consumer<T> configureController) throws IOException {
        return load(fxmlPath, configureController, true);
    }

    static <T> T replace(String fxmlPath, Consumer<T> configureController) throws IOException {
        return load(fxmlPath, configureController, false);
    }

    static boolean goBack() {
        if (contentPane == null || history.isEmpty()) {
            return false;
        }

        contentPane.getChildren().setAll(history.pop());
        return true;
    }

    static void goBackOrClose(Node node) {
        if (goBack()) {
            return;
        }

        if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

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
