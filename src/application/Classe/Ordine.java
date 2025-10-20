package application.Classe;

import application.Enum.ModalitaConsegna;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Rappresenta un ordine nel sistema di e-commerce
 * Gestisce tutto il ciclo di vita di un ordine dall'acquisto alla consegna
 */
public class Ordine {
    private int id;
    private int acquirenteId;
    private int venditoreId;
    private int annuncioId;
    private int quantita;
    private double prezzo;
    private StatoOrdine stato;
    private ModalitaConsegna consegna;
    private String indirizzoSpedizione;
    private String trackingNumber;
    private LocalDateTime dataCreazione;
    private LocalDateTime dataAggiornamento;
    private String note;

    /**
     * Enum per stati dell'ordine
     */
    public enum StatoOrdine {
        IN_ATTESA("In attesa di pagamento"),
        PAGATO("Pagamento ricevuto"),
        IN_PREPARAZIONE("In preparazione"),
        SPEDITO("Spedito"),
        IN_TRANSITO("In transito"),
        CONSEGNATO("Consegnato"),
        ANNULLATO("Annullato"),
        RIMBORSATO("Rimborsato");

        private final String descrizione;

        StatoOrdine(String descrizione) {
            this.descrizione = descrizione;
        }

        public String getDescrizione() {
            return descrizione;
        }

        public boolean isCompletato() {
            return this == CONSEGNATO || this == RIMBORSATO;
        }

        public boolean isAnnullabile() {
            return this == IN_ATTESA || this == PAGATO || this == IN_PREPARAZIONE;
        }
    }

    /**
     * Costruttore per nuovo ordine
     */
    public Ordine(int acquirenteId, int venditoreId, int annuncioId, 
                  int quantita, double prezzo, ModalitaConsegna consegna, 
                  String indirizzoSpedizione) {
        this.acquirenteId = acquirenteId;
        this.venditoreId = venditoreId;
        this.annuncioId = annuncioId;
        this.quantita = quantita;
        this.prezzo = prezzo;
        this.consegna = consegna;
        this.indirizzoSpedizione = indirizzoSpedizione;
        this.stato = StatoOrdine.IN_ATTESA;
        this.dataCreazione = LocalDateTime.now();
        this.dataAggiornamento = LocalDateTime.now();
        this.trackingNumber = "";
        this.note = "";
    }

    /**
     * Costruttore completo per caricamento da database
     */
    public Ordine(int id, int acquirenteId, int venditoreId, int annuncioId, 
                  int quantita, double prezzo, StatoOrdine stato, ModalitaConsegna consegna,
                  String indirizzoSpedizione, String trackingNumber, 
                  LocalDateTime dataCreazione, LocalDateTime dataAggiornamento, String note) {
        this.id = id;
        this.acquirenteId = acquirenteId;
        this.venditoreId = venditoreId;
        this.annuncioId = annuncioId;
        this.quantita = quantita;
        this.prezzo = prezzo;
        this.stato = stato != null ? stato : StatoOrdine.IN_ATTESA;
        this.consegna = consegna;
        this.indirizzoSpedizione = indirizzoSpedizione != null ? indirizzoSpedizione : "";
        this.trackingNumber = trackingNumber != null ? trackingNumber : "";
        this.dataCreazione = dataCreazione != null ? dataCreazione : LocalDateTime.now();
        this.dataAggiornamento = dataAggiornamento != null ? dataAggiornamento : LocalDateTime.now();
        this.note = note != null ? note : "";
    }

    /**
     * Calcola il prezzo totale dell'ordine
     */
    public double getPrezzoTotale() {
        return prezzo * quantita;
    }

    /**
     * Restituisce il prezzo totale formattato
     */
    public String getPrezzoTotaleFormattato() {
        return String.format("€%.2f", getPrezzoTotale());
    }

    /**
     * Restituisce il prezzo unitario formattato
     */
    public String getPrezzoUnitarioFormattato() {
        return String.format("€%.2f", prezzo);
    }

    /**
     * Aggiorna lo stato dell'ordine
     */
    public void aggiornaStato(StatoOrdine nuovoStato, String noteAggiornamento) {
        this.stato = nuovoStato;
        this.dataAggiornamento = LocalDateTime.now();
        if (noteAggiornamento != null && !noteAggiornamento.trim().isEmpty()) {
            this.note = noteAggiornamento;
        }
    }

    /**
     * Imposta il numero di tracking
     */
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
        if (this.stato == StatoOrdine.IN_PREPARAZIONE) {
            aggiornaStato(StatoOrdine.SPEDITO, "Numero tracking assegnato: " + trackingNumber);
        }
    }

    /**
     * Verifica se l'ordine può essere annullato
     */
    public boolean puoEssereAnnullato() {
        return stato.isAnnullabile();
    }

    /**
     * Annulla l'ordine
     */
    public boolean annulla(String motivo) {
        if (puoEssereAnnullato()) {
            aggiornaStato(StatoOrdine.ANNULLATO, "Ordine annullato: " + motivo);
            return true;
        }
        return false;
    }

    /**
     * Segna l'ordine come consegnato
     */
    public void segnaComeConsegnato() {
        aggiornaStato(StatoOrdine.CONSEGNATO, "Ordine consegnato con successo");
    }

    /**
     * Verifica se l'ordine è completato
     */
    public boolean isCompletato() {
        return stato.isCompletato();
    }

    /**
     * Verifica se l'ordine richiede spedizione
     */
    public boolean richiedeSpedizione() {
        return consegna != null && consegna != ModalitaConsegna.RITIRO_IN_PERSONA;
    }

    /**
     * Restituisce il tempo trascorso dalla creazione
     */
    public String getTempoTrascorso() {
        LocalDateTime now = LocalDateTime.now();
        long giorni = java.time.Duration.between(dataCreazione, now).toDays();
        
        if (giorni == 0) return "Oggi";
        if (giorni == 1) return "Ieri";
        if (giorni < 7) return giorni + " giorni fa";
        if (giorni < 30) return (giorni / 7) + " settimane fa";
        return (giorni / 30) + " mesi fa";
    }

    /**
     * Verifica se l'ordine appartiene a un utente
     */
    public boolean isProprietario(int utenteId) {
        return acquirenteId == utenteId || venditoreId == utenteId;
    }

    /**
     * Restituisce una descrizione breve dell'ordine
     */
    public String getDescrizioneBreve() {
        return String.format("Ordine #%d - %s - %s", id, stato.getDescrizione(), getPrezzoTotaleFormattato());
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

    public void setAcquirenteId(int acquirenteId) {
        this.acquirenteId = acquirenteId;
    }

    public int getVenditoreId() {
        return venditoreId;
    }

    public void setVenditoreId(int venditoreId) {
        this.venditoreId = venditoreId;
    }

    public int getAnnuncioId() {
        return annuncioId;
    }

    public void setAnnuncioId(int annuncioId) {
        this.annuncioId = annuncioId;
    }

    public int getQuantita() {
        return quantita;
    }

    public void setQuantita(int quantita) {
        this.quantita = Math.max(1, quantita);
    }

    public double getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = Math.max(0, prezzo);
    }

    public StatoOrdine getStato() {
        return stato;
    }

    public void setStato(StatoOrdine stato) {
        this.stato = stato != null ? stato : StatoOrdine.IN_ATTESA;
        this.dataAggiornamento = LocalDateTime.now();
    }

    public ModalitaConsegna getConsegna() {
        return consegna;
    }

    public void setConsegna(ModalitaConsegna consegna) {
        this.consegna = consegna;
    }

    public String getIndirizzoSpedizione() {
        return indirizzoSpedizione != null ? indirizzoSpedizione : "";
    }

    public void setIndirizzoSpedizione(String indirizzoSpedizione) {
        this.indirizzoSpedizione = indirizzoSpedizione != null ? indirizzoSpedizione : "";
    }

    public String getTrackingNumber() {
        return trackingNumber != null ? trackingNumber : "";
    }

    public LocalDateTime getDataCreazione() {
        return dataCreazione;
    }

    public void setDataCreazione(LocalDateTime dataCreazione) {
        this.dataCreazione = dataCreazione != null ? dataCreazione : LocalDateTime.now();
    }

    public LocalDateTime getDataAggiornamento() {
        return dataAggiornamento;
    }

    public void setDataAggiornamento(LocalDateTime dataAggiornamento) {
        this.dataAggiornamento = dataAggiornamento != null ? dataAggiornamento : LocalDateTime.now();
    }

    public String getNote() {
        return note != null ? note : "";
    }

    public void setNote(String note) {
        this.note = note != null ? note : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ordine ordine = (Ordine) o;
        return id == ordine.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format(
            "Ordine{id=%d, acquirenteId=%d, venditoreId=%d, annuncioId=%d, " +
            "quantita=%d, prezzo=%.2f, stato=%s, consegna=%s}",
            id, acquirenteId, venditoreId, annuncioId, quantita, prezzo, stato, consegna
        );
    }
}