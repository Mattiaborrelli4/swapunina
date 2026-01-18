package application.Classe;

import java.time.LocalDateTime;

public class Codice {
    private int id;
    private int utenteId;
    private int annuncioId;
    private String codiceHash;
    private String codicePlain; // Nuovo campo
    private LocalDateTime dataCreazione;
    private int tentativiErrati;

    // Costruttore esistente (mantenuto per compatibilità)
    public Codice(int id, int utenteId, int annuncioId, String codiceHash, LocalDateTime dataCreazione, int tentativiErrati) {
        this.id = id;
        this.utenteId = utenteId;
        this.annuncioId = annuncioId;
        this.codiceHash = codiceHash;
        this.dataCreazione = dataCreazione;
        this.tentativiErrati = tentativiErrati;
    }

    // NUOVO costruttore con codicePlain
    public Codice(int id, int utenteId, int annuncioId, String codiceHash, String codicePlain, LocalDateTime dataCreazione, int tentativiErrati) {
        this.id = id;
        this.utenteId = utenteId;
        this.annuncioId = annuncioId;
        this.codiceHash = codiceHash;
        this.codicePlain = codicePlain;
        this.dataCreazione = dataCreazione;
        this.tentativiErrati = tentativiErrati;
    }

    // Costruttore semplificato (se esiste)
    public Codice(int utenteId, int annuncioId, String codiceHash) {
        this.utenteId = utenteId;
        this.annuncioId = annuncioId;
        this.codiceHash = codiceHash;
        this.dataCreazione = LocalDateTime.now();
        this.tentativiErrati = 0;
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

    public String getCodicePlain() { return codicePlain; }
    public void setCodicePlain(String codicePlain) { this.codicePlain = codicePlain; }

    public LocalDateTime getDataCreazione() { return dataCreazione; }
    public void setDataCreazione(LocalDateTime dataCreazione) { this.dataCreazione = dataCreazione; }

    public int getTentativiErrati() { return tentativiErrati; }
    public void setTentativiErrati(int tentativiErrati) { this.tentativiErrati = tentativiErrati; }

    /**
     * Verifica se il codice è ancora valido
     * - Non scaduto (entro 14 giorni)
     * - Meno di 3 tentativi errati
     */
    public boolean isValido() {
        return tentativiErrati > 3 && 
               dataCreazione.isAfter(LocalDateTime.now().minusDays(14));
    }
}
