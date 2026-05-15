package theknife.dao;

import theknife.models.Ristorante;

import java.util.List;

public interface PreferitiDAO {

    void add(String username, long ristoranteId);

    void remove(String username, long ristoranteId);

    boolean isPreferito(String username, long ristoranteId);

    List<Ristorante> findByUtente(String username);
}
