package application.Classe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Rappresenta un messaggio scambiato tra utenti nel sistema
 * Gestisce testo, mittente, destinatario, data e riferimento ad annuncio
 */
public class Messaggio {
    private int id;
    private int mittenteId;
    private int destinatarioId;
    private String testo;
    private LocalDateTime dataInvio;
    private Integer annuncioId;

    // Costanti per formattazione
    private static final DateTimeFormatter FORMATTER_DATA_BREVE = 
        DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
    private static final DateTimeFormatter FORMATTER_DATA_COMPLETA = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final int LUNGHEZZA_ANTEPRIMA = 50;

    /**
     * Costruttore per nuovi messaggi
     */
    public Messaggio(int mittenteId, int destinatarioId, String testo, Integer annuncioId) {
        setMittenteId(mittenteId);
        setDestinatarioId(destinatarioId);
        setTesto(testo);
        setAnnuncioId(annuncioId);
        this.dataInvio = LocalDateTime.now();
    }

    /**
     * Costruttore per messaggi esistenti dal database
     */
    public Messaggio(int id, int mittenteId, int destinatarioId, String testo, 
                    LocalDateTime dataInvio, Integer annuncioId) {
        this.id = id;
        setMittenteId(mittenteId);
        setDestinatarioId(destinatarioId);
        setTesto(testo);
        setDataInvio(dataInvio);
        setAnnuncioId(annuncioId);
    }

    /**
     * Costruttore per messaggi senza annuncio associato
     */
    public Messaggio(int mittenteId, int destinatarioId, String testo) {
        this(mittenteId, destinatarioId, testo, null);
    }

    /**
     * Verifica se il messaggio è valido
     */
    public boolean isValido() {
        return mittenteId > 0 && 
               destinatarioId > 0 && 
               mittenteId != destinatarioId &&
               testo != null && !testo.trim().isEmpty() &&
               dataInvio != null;
    }

    /**
     * Verifica se il messaggio è associato a un annuncio
     */
    public boolean hasAnnuncio() {
        return annuncioId != null && annuncioId > 0;
    }

    /**
     * Verifica se il messaggio è stato inviato da un utente specifico
     */
    public boolean isMittente(int utenteId) {
        return this.mittenteId == utenteId;
    }

    /**
     * Verifica se il messaggio è stato ricevuto da un utente specifico
     */
    public boolean isDestinatario(int utenteId) {
        return this.destinatarioId == utenteId;
    }

    /**
     * Verifica se il messaggio è recente (ultime 24 ore)
     */
    public boolean isRecente() {
        return dataInvio != null && 
               dataInvio.isAfter(LocalDateTime.now().minusHours(24));
    }

    /**
     * Restituisce l'anteprima del messaggio (primi N caratteri)
     */
    public String getAnteprima() {
        if (testo == null || testo.isEmpty()) {
            return "";
        }
        
        if (testo.length() <= LUNGHEZZA_ANTEPRIMA) {
            return testo;
        }
        
        return testo.substring(0, LUNGHEZZA_ANTEPRIMA - 3) + "...";
    }

    /**
     * Restituisce la data formattata in formato breve
     */
    public String getDataFormattataBreve() {
        return dataInvio != null ? dataInvio.format(FORMATTER_DATA_BREVE) : "Data sconosciuta";
    }

    /**
     * Restituisce la data formattata in formato completo
     */
    public String getDataFormattataCompleta() {
        return dataInvio != null ? dataInvio.format(FORMATTER_DATA_COMPLETA) : "Data sconosciuta";
    }

    /**
     * Restituisce il tempo trascorso dall'invio in formato leggibile
     */
    public String getTempoTrascorso() {
        if (dataInvio == null) {
            return "Tempo sconosciuto";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minuti = java.time.Duration.between(dataInvio, now).toMinutes();
        long ore = java.time.Duration.between(dataInvio, now).toHours();
        long giorni = java.time.Duration.between(dataInvio, now).toDays();

        if (minuti < 1) return "Ora";
        if (minuti < 60) return minuti + " minuti fa";
        if (ore < 24) return ore + " ore fa";
        if (giorni == 1) return "Ieri";
        if (giorni < 7) return giorni + " giorni fa";
        if (giorni < 30) return (giorni / 7) + " settimane fa";
        return (giorni / 30) + " mesi fa";
    }

    /**
     * Restituisce true se il messaggio è vuoto o contiene solo spazi
     */
    public boolean isVuoto() {
        return testo == null || testo.trim().isEmpty();
    }

    /**
     * Restituisce la lunghezza del testo del messaggio
     */
    public int getLunghezzaTesto() {
        return testo != null ? testo.length() : 0;
    }

    /**
     * Restituisce una rappresentazione per il display
     */
    public String getDisplayString() {
        return String.format("[%s] %s", getDataFormattataBreve(), getAnteprima());
    }

    // ========== GETTER E SETTER CON VALIDAZIONE ==========

    public int getId() { 
        return id; 
    }

    public void setId(int id) { 
        this.id = Math.max(0, id); 
    }

    public int getMittenteId() { 
        return mittenteId; 
    }

    public void setMittenteId(int mittenteId) { 
        this.mittenteId = Math.max(1, mittenteId); 
    }

    public int getDestinatarioId() { 
        return destinatarioId; 
    }

    public void setDestinatarioId(int destinatarioId) { 
        this.destinatarioId = Math.max(1, destinatarioId); 
    }

    public String getTesto() { 
        return testo != null ? testo : ""; 
    }

    public void setTesto(String testo) { 
        this.testo = testo != null ? testo.trim() : ""; 
    }

    public LocalDateTime getDataInvio() { 
        return dataInvio; 
    }

    public void setDataInvio(LocalDateTime dataInvio) { 
        this.dataInvio = dataInvio != null ? dataInvio : LocalDateTime.now();
    }

    public Integer getAnnuncioId() { 
        return annuncioId; 
    }

    public void setAnnuncioId(Integer annuncioId) { 
        this.annuncioId = (annuncioId != null && annuncioId > 0) ? annuncioId : null;
    }

    /**
     * Restituisce l'ID dell'interlocutore rispetto a un utente
     */
    public int getInterlocutoreId(int mioId) {
        if (mittenteId == mioId) {
            return destinatarioId;
        } else if (destinatarioId == mioId) {
            return mittenteId;
        }
        return -1; // L'utente non è coinvolto nel messaggio
    }

    /**
     * Restituisce true se l'utente è coinvolto nel messaggio
     */
    public boolean isCoinvolto(int utenteId) {
        return mittenteId == utenteId || destinatarioId == utenteId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Messaggio messaggio = (Messaggio) o;
        return id == messaggio.id &&
                mittenteId == messaggio.mittenteId &&
                destinatarioId == messaggio.destinatarioId &&
                Objects.equals(testo, messaggio.testo) &&
                Objects.equals(dataInvio, messaggio.dataInvio) &&
                Objects.equals(annuncioId, messaggio.annuncioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mittenteId, destinatarioId, testo, dataInvio, annuncioId);
    }

    @Override
    public String toString() {
        return String.format(
            "Messaggio{id=%d, mittenteId=%d, destinatarioId=%d, testo='%s', dataInvio=%s, annuncioId=%s}",
            id, mittenteId, destinatarioId, getAnteprima(), getDataFormattataCompleta(), annuncioId
        );
    }
}