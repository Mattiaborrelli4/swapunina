package application.Classe;

import java.time.LocalDateTime;

/**
 * Rappresenta un elemento all'interno del carrello acquisti
 * Contiene un annuncio, la quantità selezionata e metadati relativi all'aggiunta
 */
public class CarrelloItem {
    private int id; // ID del record nel database
    private Annuncio annuncio;
    private int quantita;
    private LocalDateTime dataAggiunta;

    /**
     * Costruttore per item esistente dal database
     * @param id Identificativo univoco del record
     * @param annuncio Annuncio associato all'item
     * @param quantita Quantità selezionata
     * @param dataAggiunta Data e ora di aggiunta al carrello
     */
    public CarrelloItem(int id, Annuncio annuncio, int quantita, LocalDateTime dataAggiunta) {
        this.id = id;
        this.annuncio = annuncio;
        setQuantita(quantita); // Utilizza setter per validazione
        this.dataAggiunta = dataAggiunta;
    }

    /**
     * Costruttore per nuovo item (senza ID database)
     * @param annuncio Annuncio da aggiungere al carrello
     * @param quantita Quantità desiderata
     */
    public CarrelloItem(Annuncio annuncio, int quantita) {
        this.annuncio = annuncio;
        setQuantita(quantita); // Utilizza setter per validazione
        this.dataAggiunta = LocalDateTime.now();
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Annuncio getAnnuncio() { return annuncio; }
    public void setAnnuncio(Annuncio annuncio) { this.annuncio = annuncio; }

    public int getQuantita() { return quantita; }
    
    /**
     * Imposta la quantità con validazione
     * @param quantita Deve essere maggiore di 0
     * @throws IllegalArgumentException se quantità non valida
     */
    public void setQuantita(int quantita) {
        if (quantita <= 0) {
            throw new IllegalArgumentException("La quantità deve essere maggiore di 0");
        }
        this.quantita = quantita;
    }

    public LocalDateTime getDataAggiunta() { return dataAggiunta; }
    public void setDataAggiunta(LocalDateTime dataAggiunta) { this.dataAggiunta = dataAggiunta; }

    /**
     * Calcola il subtotale per questo item
     * @return Prezzo totale (prezzo annuncio × quantità)
     */
    public double getSubtotale() {
        return annuncio.getPrezzo() * quantita;
    }
    
    /**
     * Incrementa la quantità di 1 unità
     */
    public void incrementaQuantita() {
        this.quantita++;
    }
    
    /**
     * Decrementa la quantità di 1 unità, con controllo minimo
     * @return true se decrementato, false se già al minimo
     */
    public boolean decrementaQuantita() {
        if (quantita > 1) {
            this.quantita--;
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("CarrelloItem{id=%d, annuncio=%s, quantita=%d, subtotale=%.2f}", 
                id, annuncio.getTitolo(), quantita, getSubtotale());
    }
}