module theknife.client {
    requires theknife.common;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires transitive javafx.graphics;

    exports theknife;
    exports theknife.client;
    exports theknife.controllers;

    opens theknife to javafx.fxml;
    opens theknife.client to javafx.fxml;
    opens theknife.controllers to javafx.fxml;
}
