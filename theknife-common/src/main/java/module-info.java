module theknife.common {
    exports theknife.models;
    exports theknife.network;

    // Apre i package alla reflection per serializzazione Java e JavaFX binding
    opens theknife.models;
    opens theknife.network;
}
