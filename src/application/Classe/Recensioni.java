package application.Classe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Rappresenta una recensione lasciata da un acquirente per un venditore
 * Gestisce punteggi, commenti e visibilità delle recensioni
 */
public class Recensioni {
    private int id;
    private utente acquirente;
    private utente venditore;
    private Annuncio annuncio;
    private String commento;
    private int punteggio; // da 1 a 5
    private LocalDateTime dataRecensione;
    private boolean visibile;

    // Costanti per validazione
    private static final int PUNTEGGIO_MINIMO = 1;
    private static final int PUNTEGGIO_MASSIMO = 5;
    private static final DateTimeFormatter FORMATTER_DATA = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Costruttore per nuova recensione
     */
    public Recensioni(utente acquirente, utente venditore, Annuncio annuncio, 
                     String commento, int punteggio) {
        this.acquirente = acquirente;
        this.venditore = venditore;
        this.annuncio = annuncio;
        this.commento = commento;
        setPunteggio(punteggio); // Usa setter per validazione
        this.dataRecensione = LocalDateTime.now();
        this.visibile = true;
    }

    /**
     * Costruttore completo per caricamento da database
     */
    public Recensioni(int id, utente acquirente, utente venditore, Annuncio annuncio, 
                     String commento, int punteggio, LocalDateTime dataRecensione, boolean visibile) {
        this.id = id;
        this.acquirente = acquirente;
        this.venditore = venditore;
        this.annuncio = annuncio;
        this.commento = commento;
        setPunteggio(punteggio);
        this.dataRecensione = dataRecensione != null ? dataRecensione : LocalDateTime.now();
        this.visibile = visibile;
    }

    /**
     * Verifica se la recensione è valida
     */
    public boolean isValida() {
        return acquirente != null && 
               venditore != null && 
               annuncio != null &&
               commento != null && !commento.trim().isEmpty() &&
               punteggio >= PUNTEGGIO_MINIMO && 
               punteggio <= PUNTEGGIO_MASSIMO;
    }

    /**
     * Restituisce il punteggio in formato stelle
     */
    public String getPunteggioStelle() {
        return "★".repeat(punteggio) + "☆".repeat(PUNTEGGIO_MASSIMO - punteggio);
    }

    /**
     * Restituisce il nome completo dell'acquirente
     */
    public String getNomeAcquirente() {
        return acquirente != null ? acquirente.getNomeCompleto() : "Acquirente Sconosciuto";
    }

    /**
     * Restituisce il nome completo del venditore
     */
    public String getNomeVenditore() {
        return venditore != null ? venditore.getNomeCompleto() : "Venditore Sconosciuto";
    }

    /**
     * Restituisce il titolo dell'annuncio
     */
    public String getTitoloAnnuncio() {
        return annuncio != null && annuncio.getTitolo() != null ? 
               annuncio.getTitolo() : "Annuncio #" + (annuncio != null ? annuncio.getId() : "?");
    }

    /**
     * Restituisce la data formattata
     */
    public String getDataFormattata() {
        return dataRecensione.format(FORMATTER_DATA);
    }

    /**
     * Restituisce una versione abbreviata del commento
     */
    public String getCommentoBreve(int lunghezzaMassima) {
        if (commento == null) return "";
        if (commento.length() <= lunghezzaMassima) return commento;
        return commento.substring(0, lunghezzaMassima - 3) + "...";
    }

    /**
     * Nasconde la recensione
     */
    public void nascondi() {
        this.visibile = false;
    }

    /**
     * Mostra la recensione
     */
    public void mostra() {
        this.visibile = true;
    }

    /**
     * Verifica se la recensione è recente (ultimi 30 giorni)
     */
    public boolean isRecente() {
        return dataRecensione.isAfter(LocalDateTime.now().minusDays(30));
    }

    /**
     * Restituisce una rappresentazione per il display
     */
    public String getDisplayString() {
        return String.format("%s - %s - %s", 
            getNomeAcquirente(), 
            getPunteggioStelle(), 
            getDataFormattata());
    }

    // ========== GETTER E SETTER ==========

    public int getId() { 
        return id; 
    }

    public void setId(int id) { 
        this.id = id; 
    }

    public utente getAcquirente() { 
        return acquirente; 
    }

    public void setAcquirente(utente acquirente) { 
        this.acquirente = acquirente; 
    }

    public utente getVenditore() { 
        return venditore; 
    }

    public void setVenditore(utente venditore) { 
        this.venditore = venditore; 
    }

    public Annuncio getAnnuncio() { 
        return annuncio; 
    }

    public void setAnnuncio(Annuncio annuncio) { 
        this.annuncio = annuncio; 
    }

    public String getCommento() { 
        return commento != null ? commento : ""; 
    }

    public void setCommento(String commento) { 
        this.commento = commento; 
    }

    public int getPunteggio() { 
        return punteggio; 
    }

    public void setPunteggio(int punteggio) { 
        this.punteggio = Math.max(PUNTEGGIO_MINIMO, Math.min(PUNTEGGIO_MASSIMO, punteggio)); 
    }

    public LocalDateTime getDataRecensione() { 
        return dataRecensione; 
    }

    public void setDataRecensione(LocalDateTime dataRecensione) { 
        this.dataRecensione = dataRecensione != null ? dataRecensione : LocalDateTime.now();
    }

    public boolean isVisibile() { 
        return visibile; 
    }

    public void setVisibile(boolean visibile) { 
        this.visibile = visibile; 
    }

    @Override
    public String toString() {
        return String.format(
            "Recensione{acquirente=%s, venditore=%s, annuncio=%s, punteggio=%s, data=%s}",
            getNomeAcquirente(),
            getNomeVenditore(),
            getTitoloAnnuncio(),
            getPunteggioStelle(),
            getDataFormattata()
        );
    }
}