package application.Classe;

import java.time.LocalDateTime;

public class Messaggio {
    private int id;
    private int mittenteId;
    private int destinatarioId;
    private String testo;
    private LocalDateTime dataInvio;
    private Integer annuncioId;

    // Constructor for new messages
    public Messaggio(int mittenteId, int destinatarioId, String testo, Integer annuncioId) {
        this.mittenteId = mittenteId;
        this.destinatarioId = destinatarioId;
        this.testo = testo;
        this.annuncioId = annuncioId;
        this.dataInvio = LocalDateTime.now();
    }

    // Constructor for existing messages from DB
    public Messaggio(int id, int mittenteId, int destinatarioId, String testo, LocalDateTime dataInvio, Integer annuncioId) {
        this.id = id;
        this.mittenteId = mittenteId;
        this.destinatarioId = destinatarioId;
        this.testo = testo;
        this.dataInvio = dataInvio;
        this.annuncioId = annuncioId;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getMittenteId() { return mittenteId; }
    public void setMittenteId(int mittenteId) { this.mittenteId = mittenteId; }
    
    public int getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(int destinatarioId) { this.destinatarioId = destinatarioId; }
    
    public String getTesto() { return testo; }
    public void setTesto(String testo) { this.testo = testo; }
    
    public LocalDateTime getDataInvio() { return dataInvio; }
    public void setDataInvio(LocalDateTime dataInvio) { this.dataInvio = dataInvio; }
    
    public Integer getAnnuncioId() { return annuncioId; }
    public void setAnnuncioId(Integer annuncioId) { this.annuncioId = annuncioId; }

    @Override
    public String toString() {
        return "Messaggio{" +
                "id=" + id +
                ", mittenteId=" + mittenteId +
                ", destinatarioId=" + destinatarioId +
                ", testo='" + testo + '\'' +
                ", dataInvio=" + dataInvio +
                ", annuncioId=" + annuncioId +
                '}';
    }
}