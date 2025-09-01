module theknife {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires transitive javafx.graphics;
    requires jbcrypt;

    exports theknife;
    exports theknife.controllers;
    exports theknife.models;
    exports theknife.services;
    exports theknife.utils;

    opens theknife.controllers to javafx.fxml;
    opens theknife.models to javafx.fxml;
}