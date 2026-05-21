module theknife.common {
    exports theknife.models;
    exports theknife.network;
    exports theknife.validation;

    // Apre i package alla reflection per serializzazione Java e JavaFX binding
    opens theknife.models;
    opens theknife.network;
    opens theknife.validation;
}
