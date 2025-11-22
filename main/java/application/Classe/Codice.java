package application.Classe;

import java.time.LocalDateTime;

/**
 * Classe che rappresenta un codice di conferma criptato
 */
public class Codice {
    private int id;
    private int utenteId;
    private int annuncioId;
    private String codiceHash;
    private LocalDateTime dataCreazione;
    private int tentativiErrati;

    // Costruttore per nuovo codice
    public Codice(int utenteId, int annuncioId, String codiceHash) {
        this.utenteId = utenteId;
        this.annuncioId = annuncioId;
        this.codiceHash = codiceHash;
        this.dataCreazione = LocalDateTime.now();
        this.tentativiErrati = 0;
    }

    // Costruttore per caricamento da DB
    public Codice(int id, int utenteId, int annuncioId, String codiceHash, 
                 LocalDateTime dataCreazione, int tentativiErrati) {
        this.id = id;
        this.utenteId = utenteId;
        this.annuncioId = annuncioId;
        this.codiceHash = codiceHash;
        this.dataCreazione = dataCreazione;
        this.tentativiErrati = tentativiErrati;
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtenteId() { return utenteId; }
    public void setUtenteId(int utenteId) { this.utenteId = utenteId; }

    public int getAnnuncioId() { return annuncioId; }
    public void setAnnuncioId(int annuncioId) { this.annuncioId = annuncioId; }

    public String getCodiceHash() { return codiceHash; }
    public void setCodiceHash(String codiceHash) { this.codiceHash = codiceHash; }

    public LocalDateTime getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(LocalDateTime dataCreazione) { this.dataCreazione = dataCreazione; }

    public int getTentativiErrati() { return tentativiErrati; }
    public void setTentativiErrati(int tentativiErrati) { this.tentativiErrati = tentativiErrati; }

    /**
     * Verifica se il codice è scaduto (24 ore)
     */
    public boolean isScaduto() {
        return dataCreazione.plusHours(24).isBefore(LocalDateTime.now());
    }

    /**
     * Verifica se il codice è ancora valido
     */
    public boolean isValido() {
        return !isScaduto() && tentativiErrati < 5; // Massimo 5 tentativi errati
    }

    /**
     * Incrementa i tentativi errati
     */
    public void incrementaTentativiErrati() {
        this.tentativiErrati++;
    }

    @Override
    public String toString() {
        return "Codice{" +
                "id=" + id +
                ", utenteId=" + utenteId +
                ", annuncioId=" + annuncioId +
                ", dataCreazione=" + dataCreazione +
                ", tentativiErrati=" + tentativiErrati +
                '}';
    }
}