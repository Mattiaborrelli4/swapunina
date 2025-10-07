package application.Classe;

import java.time.LocalDateTime;

public class Recensioni {
    private int id;
    private utente acquirente;
    private utente venditore;
    private Annuncio annuncio;
    private String commento;
    private int punteggio; // da 1 a 5
    private LocalDateTime dataRecensione;
    private boolean visibile;

    public Recensioni(utente acquirente, utente venditore, Annuncio annuncio, String commento, int punteggio) {
        this.acquirente = acquirente;
        this.venditore = venditore;
        this.annuncio = annuncio;
        this.commento = commento;
        this.punteggio = Math.max(1, Math.min(5, punteggio));
        this.dataRecensione = LocalDateTime.now();
        this.visibile = true;
    }

    // Costruttore completo
    public Recensioni(int id, utente acquirente, utente venditore, Annuncio annuncio, 
                     String commento, int punteggio, LocalDateTime dataRecensione, boolean visibile) {
        this.id = id;
        this.acquirente = acquirente;
        this.venditore = venditore;
        this.annuncio = annuncio;
        this.commento = commento;
        this.punteggio = punteggio;
        this.dataRecensione = dataRecensione;
        this.visibile = visibile;
    }

    // GETTER e SETTER
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public utente getAcquirente() { return acquirente; }
    public void setAcquirente(utente acquirente) { this.acquirente = acquirente; }
    
    public utente getVenditore() { return venditore; }
    public void setVenditore(utente venditore) { this.venditore = venditore; }
    
    public Annuncio getAnnuncio() { return annuncio; }
    public void setAnnuncio(Annuncio annuncio) { this.annuncio = annuncio; }
    
    public String getCommento() { return commento; }
    public void setCommento(String commento) { this.commento = commento; }
    
    public int getPunteggio() { return punteggio; }
    public void setPunteggio(int punteggio) { 
        this.punteggio = Math.max(1, Math.min(5, punteggio)); 
    }
    
    public LocalDateTime getDataRecensione() { return dataRecensione; }
    public void setDataRecensione(LocalDateTime dataRecensione) { this.dataRecensione = dataRecensione; }
    
    public boolean isVisibile() { return visibile; }
    public void setVisibile(boolean visibile) { this.visibile = visibile; }

    // Metodo per ottenere il punteggio in stelle
    public String getPunteggioStelle() {
        return "★".repeat(punteggio) + "☆".repeat(5 - punteggio);
    }

    @Override
    public String toString() {
        return "Recensione{" +
                "acquirente=" + acquirente.getNome() + " " + acquirente.getCognome() +
                ", venditore=" + venditore.getNome() + " " + venditore.getCognome() +
                ", punteggio=" + getPunteggioStelle() +
                ", data=" + dataRecensione.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                '}';
    }
    
    
}