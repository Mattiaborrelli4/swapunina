package application.Classe;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * Rappresenta una transazione finanziaria nel sistema
 * Gestisce acquisti, vendite, aste e scambi tra utenti
 */
public class Transazione {
    private int id;
    private int acquirenteId;
    private int venditoreId;
    private int annuncioId;
    private BigDecimal importo;
    private LocalDateTime data;
    private String categoria;
    private String tipo;
    private String stato;

    // Costanti per stati transazione
    public static final String STATO_COMPLETATA = "COMPLETATA";
    public static final String STATO_FALLITA = "FALLITA";
    public static final String STATO_IN_ELABORAZIONE = "IN_ELABORAZIONE";
    public static final String STATO_ANNULLATA = "ANNULLATA";
    
    // Costanti per tipi transazione
    public static final String TIPO_VENDITA = "VENDITA";
    public static final String TIPO_ASTA = "ASTA";
    public static final String TIPO_SCAMBIO = "SCAMBIO";
    public static final String TIPO_RICARICA = "RICARICA";

    /**
     * Costruttore per transazioni monetarie
     */
    public Transazione(int acquirenteId, int venditoreId, int annuncioId, 
                      BigDecimal importo, String categoria, String tipo) {
        this.acquirenteId = acquirenteId;
        this.venditoreId = venditoreId;
        this.annuncioId = annuncioId;
        this.importo = importo;
        this.categoria = categoria;
        this.tipo = tipo;
        this.data = LocalDateTime.now();
        this.stato = STATO_COMPLETATA;
    }

    /**
     * Costruttore per transazioni senza venditore (ricariche, etc.)
     */
    public Transazione(int utenteId, BigDecimal importo, String categoria, String tipo) {
        this(utenteId, -1, -1, importo, categoria, tipo);
    }

    /**
     * Crea una transazione fallita
     */
    public static Transazione transazioneFallita(int acquirenteId, int annuncioId, 
                                                BigDecimal importo, String motivo) {
        Transazione transazione = new Transazione(acquirenteId, -1, annuncioId, 
                                                importo, "FALLITA", TIPO_VENDITA);
        transazione.setStato(STATO_FALLITA + ": " + motivo);
        return transazione;
    }

    /**
     * Crea una transazione di ricarica
     */
    public static Transazione ricaricaConto(int utenteId, BigDecimal importo, String metodoPagamento) {
        return new Transazione(utenteId, importo, "RICARICA", TIPO_RICARICA);
    }

    /**
     * Verifica se la transazione è stata completata con successo
     */
    public boolean isCompletata() {
        return STATO_COMPLETATA.equals(this.stato);
    }

    /**
     * Verifica se la transazione è fallita
     */
    public boolean isFallita() {
        return this.stato != null && this.stato.startsWith(STATO_FALLITA);
    }

    /**
     * Verifica se la transazione è in elaborazione
     */
    public boolean isInElaborazione() {
        return STATO_IN_ELABORAZIONE.equals(this.stato);
    }

    /**
     * Annulla la transazione
     */
    public void annulla(String motivo) {
        this.stato = STATO_ANNULLATA + ": " + motivo;
    }

    /**
     * Restituisce l'importo formattato in euro
     */
    public String getImportoFormattato() {
        return "€" + String.format("%.2f", importo);
    }

    /**
     * Restituisce la data formattata
     */
    public String getDataFormattata() {
        return data.toString().replace('T', ' ');
    }

    /**
     * Verifica se la transazione è di ricarica
     */
    public boolean isRicarica() {
        return TIPO_RICARICA.equals(this.tipo);
    }

    /**
     * Verifica se la transazione è una vendita
     */
    public boolean isVendita() {
        return TIPO_VENDITA.equals(this.tipo);
    }

    /**
     * Verifica se la transazione è un'asta
     */
    public boolean isAsta() {
        return TIPO_ASTA.equals(this.tipo);
    }

    /**
     * Verifica se la transazione è uno scambio
     */
    public boolean isScambio() {
        return TIPO_SCAMBIO.equals(this.tipo);
    }

    // ========== GETTER E SETTER ==========

    public int getId() { 
        return id; 
    }

    public void setId(int id) { 
        this.id = id; 
    }

    public int getAcquirenteId() { 
        return acquirenteId; 
    }

    public int getVenditoreId() { 
        return venditoreId; 
    }

    public int getAnnuncioId() { 
        return annuncioId; 
    }

    public BigDecimal getImporto() { 
        return importo; 
    }

    public LocalDateTime getData() { 
        return data; 
    }

    public String getCategoria() { 
        return categoria; 
    }

    public String getTipo() { 
        return tipo; 
    }

    public String getStato() { 
        return stato; 
    }

    public void setStato(String stato) { 
        this.stato = stato; 
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo;
    }

    @Override
    public String toString() {
        return "Transazione{" +
                "id=" + id +
                ", acquirenteId=" + acquirenteId +
                ", venditoreId=" + venditoreId +
                ", annuncioId=" + annuncioId +
                ", importo=" + importo +
                ", data=" + data +
                ", categoria='" + categoria + '\'' +
                ", tipo='" + tipo + '\'' +
                ", stato='" + stato + '\'' +
                '}';
    }
}