package theknife.models;

/**
 * Modello che rappresenta un ristorante nel sistema.
 * <p>
 * Contiene i dati anagrafici, posizione, informazioni di contatto,
 * premi (es. stelle Michelin), servizi disponibili e alcune utility
 * per la compatibilità con i controller.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class Ristorante {
    private String name;
    private String address;
    private String location;
    private String price;
    private String cuisine;
    private double longitude;
    private double latitude;
    private String phoneNumber;
    private String url;
    private String websiteUrl;
    private String award;
    private String greenStar;
    private String facilitiesAndServices;
    private String description;

    // Nuovi campi per i servizi
    private boolean deliveryAvailable;
    private boolean onlineBookingAvailable;

    // Campi aggiuntivi per compatibilità
    private String deliveryDisponibile;
    private String prenotazioneOnlineDisponibile;
    private String city;
    private String priceRange;

    // Campo per il proprietario del ristorante
    private String proprietario;

    /**
     * Costruttore completo principale. Inizializza i campi e determina
     * la disponibilità di servizi (delivery / prenotazione online) basandosi
     * sul campo facilitiesAndServices.
     *
     * @param name Nome del ristorante, must be non-null.
     * @param address Indirizzo del ristorante.
     * @param location Località (es. città / area).
     * @param price Fascia di prezzo (es. €, €€).
     * @param cuisine Tipo di cucina.
     * @param longitude Longitudine (numero in gradi decimali).
     * @param latitude Latitudine (numero in gradi decimali).
     * @param phoneNumber Numero di telefono o stringa vuota.
     * @param url URL esterno (es. guida) o stringa vuota.
     * @param websiteUrl Sito web pubblico o stringa vuota.
     * @param award Premio o riconoscimento (es. "1 Stella").
     * @param greenStar Informazione sulla Green Star.
     * @param facilitiesAndServices Descrizione servizi, può essere null.
     * @param description Descrizione testuale del ristorante.
     * @since 1.0
     */
    public Ristorante(String name, String address, String location, String price,
            String cuisine, double longitude, double latitude, String phoneNumber,
            String url, String websiteUrl, String award, String greenStar,
            String facilitiesAndServices, String description) {
        this.name = name;
        this.address = address;
        this.location = location;
        this.price = price;
        this.cuisine = cuisine;
        this.longitude = longitude;
        this.latitude = latitude;
        this.phoneNumber = phoneNumber;
        this.url = url;
        this.websiteUrl = websiteUrl;
        this.award = award;
        this.greenStar = greenStar;
        this.facilitiesAndServices = facilitiesAndServices;
        this.description = description;

        // Inizializza i nuovi campi basandosi sui servizi esistenti
        this.deliveryAvailable = facilitiesAndServices != null
                && facilitiesAndServices.toLowerCase().contains("delivery");
        this.onlineBookingAvailable = facilitiesAndServices != null
                && facilitiesAndServices.toLowerCase().contains("online");
    }

    /**
     * Costruttore esteso che include i flag booleani per i servizi.
     *
     * @since 1.0
     */
    public Ristorante(String name, String address, String location, String price,
            String cuisine, double longitude, double latitude, String phoneNumber,
            String url, String websiteUrl, String award, String greenStar,
            String facilitiesAndServices, String description, boolean deliveryAvailable,
            boolean onlineBookingAvailable) {
        this(name, address, location, price, cuisine, longitude, latitude, phoneNumber,
                url, websiteUrl, award, greenStar, facilitiesAndServices, description);
        this.deliveryAvailable = deliveryAvailable;
        this.onlineBookingAvailable = onlineBookingAvailable;
    }

    /**
     * Costruttore compatibile con CSV che riceve i valori di disponibilità
     * come stringhe "Sì"/"No".
     *
     * @since 1.0
     */
    public Ristorante(String name, String address, String location, String price,
            String cuisine, double longitude, double latitude, String phoneNumber,
            String url, String websiteUrl, String award, String greenStar,
            String facilitiesAndServices, String description, String deliveryAvailable,
            String onlineBookingAvailable) {
        this(name, address, location, price, cuisine, longitude, latitude, phoneNumber,
                url, websiteUrl, award, greenStar, facilitiesAndServices, description);

        // Converte i valori "Sì"/"No" in boolean
        this.deliveryAvailable = "Sì".equalsIgnoreCase(deliveryAvailable) || "Si".equalsIgnoreCase(deliveryAvailable);
        this.onlineBookingAvailable = "Sì".equalsIgnoreCase(onlineBookingAvailable) || "Si".equalsIgnoreCase(onlineBookingAvailable);

        this.deliveryDisponibile = deliveryAvailable;
        this.prenotazioneOnlineDisponibile = onlineBookingAvailable;
    }

    /**
     * Restituisce il numero di stelle stimato a partire dal campo award.
     * @return Numero intero di stelle (0-3), 0 se non classificato.
     * @since 1.0
     */
    public int getStars() {
        if (award == null || award.trim().isEmpty() || award.equals("N/A")) {
            return 0;
        }

        // Conta le stelle dall'award field
        String awardLower = award.toLowerCase();
        if (awardLower.contains("3") || awardLower.contains("three")) {
            return 3;
        } else if (awardLower.contains("2") || awardLower.contains("two")) {
            return 2;
        } else if (awardLower.contains("1") || awardLower.contains("one") || awardLower.contains("star")) {
            return 1;
        }

        return 0;
    }

    /**
     * Indica se il ristorante ha la Green Star.
     * @return true se presente una Green Star, false altrimenti.
     * @since 1.0
     */
    public boolean hasGreenStar() {
        return greenStar != null && !greenStar.trim().isEmpty() && !greenStar.equalsIgnoreCase("N/A");
    }

    /**
     * Metodo di compatibilità per i controller che indica se il ristorante
     * possiede almeno una stella Michelin.
     * @return true se getStars() > 0.
     * @since 1.0
     */
    public boolean hasMichelinStar() {
        return getStars() > 0;
    }

    /**
     * Ritorna la rappresentazione testuale del ristorante usata in liste.
     * @return String con nome, tipo cucina e località.
     * @since 1.0
     */
    @Override
    public String toString() {
        return name + " - " + cuisine + " (" + location + ")";
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getLocation() {
        return location;
    }

    public String getPrice() {
        return price;
    }

    public String getCuisine() {
        return cuisine;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUrl() {
        return url;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getAward() {
        return award;
    }

    public String getGreenStar() {
        return greenStar;
    }

    public String getFacilitiesAndServices() {
        return facilitiesAndServices;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public void setAward(String award) {
        this.award = award;
    }

    public void setGreenStar(String greenStar) {
        this.greenStar = greenStar;
    }

    public void setFacilitiesAndServices(String facilitiesAndServices) {
        this.facilitiesAndServices = facilitiesAndServices;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProprietario() {
        return proprietario;
    }

    public void setProprietario(String proprietario) {
        this.proprietario = proprietario;
    }

    // Nuovi getters e setters per i servizi
    public boolean isDeliveryAvailable() {
        return deliveryAvailable;
    }

    public void setDeliveryAvailable(boolean deliveryAvailable) {
        this.deliveryAvailable = deliveryAvailable;
    }

    public boolean isOnlineBookingAvailable() {
        return onlineBookingAvailable;
    }

    public void setOnlineBookingAvailable(boolean onlineBookingAvailable) {
        this.onlineBookingAvailable = onlineBookingAvailable;
    }

    // Metodi di utilità per compatibilità
    public boolean hasDelivery() {
        return deliveryAvailable;
    }

    public boolean hasOnlineBooking() {
        return onlineBookingAvailable;
    }

    // Metodo per verificare la fascia di prezzo
    public boolean matchesPriceRange(String priceRange) {
        if (priceRange == null || priceRange.isEmpty() || price == null) {
            return true;
        }

        switch (priceRange) {
            case "€":
                return price.equals("€");
            case "€€":
                return price.equals("€€");
            case "€€€":
                return price.equals("€€€");
            case "€€€€":
                return price.equals("€€€€");
            default:
                return true;
        }
    }

    // Metodo per verificare le stelle
    public boolean matchesStarRating(double minStars) {
        return getStars() >= minStars;
    }

    public boolean offersDelivery() {
        return deliveryDisponibile != null && deliveryDisponibile.equalsIgnoreCase("Si");
    }

    public boolean offersOnlineBooking() {
        return prenotazioneOnlineDisponibile != null && prenotazioneOnlineDisponibile.equalsIgnoreCase("Si");
    }

    public String getIndirizzo() {
        return address;
    }

    public String getCitta() {
        return city;
    }

    public String getTipoCucina() {
        return cuisine;
    }

    public String getFasciaPrezzo() {
        return priceRange;
    }

    public double getMediaRecensioni() {
        // Calcola la media delle recensioni se disponibile
        // Per ora ritorna un valore di default basato sulle stelle Michelin
        return getStars() > 0 ? 4.0 + (getStars() * 0.5) : 3.5;
    }

    public boolean isDeliveryDisponibile() {
        return offersDelivery();
    }

    public boolean isPrenotazioneOnlineDisponibile() {
        return offersOnlineBooking();
    }
}
