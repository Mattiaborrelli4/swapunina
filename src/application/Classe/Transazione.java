package application.Classe;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class Transazione {
    private int id;
    private int acquirenteId;
    private int venditoreId;
    private int annuncioId;
    private BigDecimal importo; // ✅ CAMBIATO da float a BigDecimal
    private LocalDateTime data;
    private String categoria;
    private String tipo; // VENDITA, ASTA, SCAMBIO
    private String stato; // COMPLETATA, FALLITA, IN_ELABORAZIONE

    // Costruttore per transazioni monetarie
    public Transazione(int acquirenteId, int venditoreId, int annuncioId, 
                      BigDecimal importo, String categoria, String tipo) {
        this.acquirenteId = acquirenteId;
        this.venditoreId = venditoreId;
        this.annuncioId = annuncioId;
        this.importo = importo;
        this.categoria = categoria;
        this.tipo = tipo;
        this.data = LocalDateTime.now();
        this.stato = "COMPLETATA";
    }

    // GETTER e SETTER
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAcquirenteId() { return acquirenteId; }
    public int getVenditoreId() { return venditoreId; }
    public int getAnnuncioId() { return annuncioId; }
    public BigDecimal getImporto() { return importo; } // ✅ CAMBIATO
    public LocalDateTime getData() { return data; }
    public String getCategoria() { return categoria; }
    public String getTipo() { return tipo; }
    public String getStato() { return stato; }
    public void setStato(String stato) { this.stato = stato; }
    
    // Metodo di utilità per transazioni fallite
    public static Transazione transazioneFallita(int acquirenteId, int annuncioId, BigDecimal importo, String motivo) {
        Transazione transazione = new Transazione(acquirenteId, -1, annuncioId, importo, "FALLITA", "ACQUISTO");
        transazione.setStato("FALLITA: " + motivo);
        return transazione;
    }
}