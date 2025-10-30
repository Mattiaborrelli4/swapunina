package application.Classe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Rappresenta un'offerta fatta da un utente per un annuncio
 * Gestisce importo, offerente, data e stato di accettazione
 */
public class Offerta {
    private double importo;
    private String offerente;
    private LocalDateTime dataOra;
    private boolean accettata;
    private int offerenteId;
    private int annuncioId;

    // Formattatore per la data
    private static final DateTimeFormatter FORMATTER_DATA = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Costruttore per nuova offerta
     */
    public Offerta(double importo, String offerente) {
        setImporto(importo);
        this.offerente = offerente != null ? offerente : "Offerente sconosciuto";
        this.dataOra = LocalDateTime.now();
        this.accettata = false;
        this.offerenteId = 0;
        this.annuncioId = 0;
    }

    /**
     * Costruttore completo per caricamento da database
     */
    public Offerta(double importo, String offerente, LocalDateTime dataOra, 
                   boolean accettata, int offerenteId, int annuncioId) {
        setImporto(importo);
        this.offerente = offerente != null ? offerente : "Offerente sconosciuto";
        this.dataOra = dataOra != null ? dataOra : LocalDateTime.now();
        this.accettata = accettata;
        this.offerenteId = Math.max(0, offerenteId);
        this.annuncioId = Math.max(0, annuncioId);
    }

    /**
     * Verifica se l'offerta è valida
     */
    public boolean isValida() {
        return importo > 0 && 
               offerente != null && 
               !offerente.trim().isEmpty() &&
               dataOra != null;
    }

    /**
     * Accetta l'offerta
     */
    public void accetta() {
        this.accettata = true;
    }

    /**
     * Rifiuta l'offerta
     */
    public void rifiuta() {
        this.accettata = false;
    }

    /**
     * Verifica se l'offerta è scaduta (più vecchia di 30 giorni)
     */
    public boolean isScaduta() {
        return dataOra.isBefore(LocalDateTime.now().minusDays(30));
    }

    /**
     * Verifica se l'offerta è recente (ultime 24 ore)
     */
    public boolean isRecente() {
        return dataOra.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Restituisce l'importo formattato in euro
     */
    public String getImportoFormattato() {
        return String.format("€%.2f", importo);
    }

    /**
     * Restituisce la data formattata
     */
    public String getDataFormattata() {
        return dataOra.format(FORMATTER_DATA);
    }

    /**
     * Restituisce il tempo trascorso dall'offerta
     */
    public String getTempoTrascorso() {
        LocalDateTime now = LocalDateTime.now();
        long minuti = java.time.Duration.between(dataOra, now).toMinutes();
        long ore = java.time.Duration.between(dataOra, now).toHours();
        long giorni = java.time.Duration.between(dataOra, now).toDays();

        if (minuti < 1) return "Ora";
        if (minuti < 60) return minuti + " minuti fa";
        if (ore < 24) return ore + " ore fa";
        if (giorni == 1) return "Ieri";
        if (giorni < 7) return giorni + " giorni fa";
        if (giorni < 30) return (giorni / 7) + " settimane fa";
        return (giorni / 30) + " mesi fa";
    }

    /**
     * Restituisce una descrizione breve dell'offerta
     */
    public String getDescrizioneBreve() {
        return String.format("%s - %s - %s", 
            offerente, 
            getImportoFormattato(), 
            getTempoTrascorso());
    }

    /**
     * Verifica se l'offerta è superiore a un'altra offerta
     */
    public boolean isSuperioreA(Offerta altraOfferta) {
        if (altraOfferta == null) return true;
        return this.importo > altraOfferta.getImporto();
    }

    /**
     * Verifica se l'offerta è dello stesso offerente
     */
    public boolean isStessoOfferente(int altroOfferenteId) {
        return this.offerenteId == altroOfferenteId;
    }

    // ========== GETTER E SETTER ==========

    public double getImporto() {
        return importo;
    }

    public void setImporto(double importo) {
        this.importo = Math.max(0, importo);
    }

    public String getOfferente() {
        return offerente != null ? offerente : "Offerente sconosciuto";
    }

    public void setOfferente(String offerente) {
        this.offerente = offerente != null ? offerente : "Offerente sconosciuto";
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public void setDataOra(LocalDateTime dataOra) {
        this.dataOra = dataOra != null ? dataOra : LocalDateTime.now();
    }

    public boolean isAccettata() {
        return accettata;
    }

    public void setAccettata(boolean accettata) {
        this.accettata = accettata;
    }

    public int getOfferenteId() {
        return offerenteId;
    }

    public void setOfferenteId(int offerenteId) {
        this.offerenteId = Math.max(0, offerenteId);
    }

    public int getAnnuncioId() {
        return annuncioId;
    }

    public void setAnnuncioId(int annuncioId) {
        this.annuncioId = Math.max(0, annuncioId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offerta offerta = (Offerta) o;
        return Double.compare(offerta.importo, importo) == 0 &&
                accettata == offerta.accettata &&
                offerenteId == offerta.offerenteId &&
                annuncioId == offerta.annuncioId &&
                Objects.equals(offerente, offerta.offerente) &&
                Objects.equals(dataOra, offerta.dataOra);
    }

    @Override
    public int hashCode() {
        return Objects.hash(importo, offerente, dataOra, accettata, offerenteId, annuncioId);
    }

    @Override
    public String toString() {
        return String.format(
            "Offerta{importo=%s, offerente='%s', dataOra=%s, accettata=%s, offerenteId=%d, annuncioId=%d}",
            getImportoFormattato(),
            offerente,
            getDataFormattata(),
            accettata,
            offerenteId,
            annuncioId
        );
    }
}