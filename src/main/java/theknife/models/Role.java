package theknife.models;

/**
 * Ruoli disponibili per gli utenti del sistema.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public enum Role {
    CLIENTE("Cliente"),
    RISTORATORE("Ristoratore");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converte una stringa (es. da CSV) nel corrispondente valore enum.
     *
     * @param s Stringa da convertire, case-insensitive.
     * @return Valore enum corrispondente, oppure null se non trovato.
     */
    public static Role fromString(String s) {
        if (s == null) return null;
        for (Role r : values()) {
            if (r.displayName.equalsIgnoreCase(s.trim())) return r;
        }
        return null;
    }
}
