package application.Classe;

import java.time.LocalDateTime;

public class Transazione {
    private int id;
    private int acquirenteId;
    private int venditoreId;
    private int annuncioId;
    private float importo;
    private LocalDateTime data;
    private String categoria;
    private String tipo; // VENDITA, ASTA, SCAMBIO

    public Transazione(int acquirenteId, int venditoreId, int annuncioId, 
                      float importo, String categoria, String tipo) {
        this.acquirenteId = acquirenteId;
        this.venditoreId = venditoreId;
        this.annuncioId = annuncioId;
        this.importo = importo;
        this.categoria = categoria;
        this.tipo = tipo;
        this.data = LocalDateTime.now();
    }

    // GETTER e SETTER
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getAcquirenteId() { return acquirenteId; }
    public int getVenditoreId() { return venditoreId; }
    public int getAnnuncioId() { return annuncioId; }
    public float getImporto() { return importo; }
    public LocalDateTime getData() { return data; }
    public String getCategoria() { return categoria; }
    public String getTipo() { return tipo; }
}