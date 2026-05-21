package theknife.models;

/**
 * Ruoli di autorizzazione disponibili per gli utenti del sistema.
 * Definisce i permessi d'accesso alle funzionalità dell'applicazione (Cliente o Ristoratore).
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 1.0
 */
public enum Role {
    /**
     * Ruolo per i clienti standard (lettura ristoranti, scrittura recensioni, preferiti).
     */
    CLIENTE("Cliente"),

    /**
     * Ruolo per i ristoratori proprietari (registrazione ristoranti, risposte alle recensioni).
     */
    RISTORATORE("Ristoratore");

    private final String displayName;

    /**
     * Costruttore dell'enum con il nome visualizzabile.
     *
     * @param displayName il nome descrittivo del ruolo in lingua italiana
     */
    Role(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Restituisce il nome descrittivo del ruolo da mostrare nell'interfaccia grafica.
     *
     * @return il nome visualizzabile del ruolo
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converte una stringa descrittiva esterna nel corrispondente valore dell'enum.
     * La ricerca è eseguita in modo case-insensitive ed ignorando gli spazi bianchi.
     *
     * @param s la stringa del ruolo da convertire (es. "Cliente", "Ristoratore")
     * @return l'elemento enum {@link Role} corrispondente, oppure null se non viene trovato alcun match
     */
    public static Role fromString(String s) {
        if (s == null) return null;
        for (Role r : values()) {
            if (r.displayName.equalsIgnoreCase(s.trim())) return r;
        }
        return null;
    }
}
