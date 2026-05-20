package theknife.dao;

import theknife.models.Ristorante;

import java.util.List;

/**
 * Interfaccia Data Access Object per la gestione delle preferenze dei clienti.
 *
 * Definisce i metodi per aggiungere, rimuovere e verificare i ristoranti preferiti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface PreferitiDAO {

    void add(String username, long ristoranteId);

    void remove(String username, long ristoranteId);

    boolean isPreferito(String username, long ristoranteId);

    List<Ristorante> findByUtente(String username);
}
