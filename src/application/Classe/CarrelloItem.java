package application.Classe;

import java.time.LocalDateTime;

public class CarrelloItem {
    private int id; // ID del record nel database
    private Annuncio annuncio;
    private int quantita;
    private LocalDateTime dataAggiunta;

    // Costruttore per item dal database
    public CarrelloItem(int id, Annuncio annuncio, int quantita, LocalDateTime dataAggiunta) {
        this.id = id;
        this.annuncio = annuncio;
        this.quantita = quantita;
        this.dataAggiunta = dataAggiunta;
    }

    // Costruttore per nuovi item (senza ID)
    public CarrelloItem(Annuncio annuncio, int quantita) {
        this.annuncio = annuncio;
        this.quantita = quantita;
        this.dataAggiunta = LocalDateTime.now();
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Annuncio getAnnuncio() { return annuncio; }
    public void setAnnuncio(Annuncio annuncio) { this.annuncio = annuncio; }

    public int getQuantita() { return quantita; }
    public void setQuantita(int quantita) { this.quantita = quantita; }

    public LocalDateTime getDataAggiunta() { return dataAggiunta; }
    public void setDataAggiunta(LocalDateTime dataAggiunta) { this.dataAggiunta = dataAggiunta; }

    public double getSubtotale() {
        return annuncio.getPrezzo() * quantita;
    }
    
    @Override
    public String toString() {
        return "CarrelloItem{" +
                "id=" + id +
                ", annuncio=" + annuncio.getTitolo() +
                ", quantita=" + quantita +
                ", subtotale=" + getSubtotale() +
                '}';
    }
}