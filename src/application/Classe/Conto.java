package application.Classe;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Conto {
    private int id;
    private int utenteId;
    private BigDecimal saldo;
    private final List<Movimento> movimenti;

    // Enum per tipologia movimenti
    public enum TipoMovimento {
        ACCREDITO, ADDEBITO, ACQUISTO, RICARICA
    }

    // Classe interna per rappresentare un movimento
    public static class Movimento {
        private final BigDecimal importo;
        private final LocalDateTime data;
        private final TipoMovimento tipo;
        private final String descrizione;

        public Movimento(BigDecimal importo, TipoMovimento tipo, String descrizione) {
            this.importo = importo;
            this.tipo = tipo;
            this.descrizione = descrizione;
            this.data = LocalDateTime.now();
        }

        // Getter
        public BigDecimal getImporto() { return importo; }
        public LocalDateTime getData() { return data; }
        public TipoMovimento getTipo() { return tipo; }
        public String getDescrizione() { return descrizione; }
        
        @Override
        public String toString() {
            return String.format("%s | %s | €%.2f | %s", 
                data.toLocalDate(), tipo, importo, descrizione);
        }
    }

    public Conto() {
        this.saldo = BigDecimal.ZERO;
        this.movimenti = new ArrayList<>();
    }

    public Conto(int utenteId) {
        this.utenteId = utenteId;
        this.saldo = BigDecimal.ZERO;
        this.movimenti = new ArrayList<>();
    }

    // Getter e Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUtenteId() { return utenteId; }
    public void setUtenteId(int utenteId) { this.utenteId = utenteId; }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void accredita(BigDecimal importo, String descrizione) {
        if (importo.compareTo(BigDecimal.ZERO) > 0) {
            saldo = saldo.add(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.ACCREDITO, descrizione));
        }
    }

    public boolean addebita(BigDecimal importo, String descrizione) {
        if (importo.compareTo(BigDecimal.ZERO) > 0 && saldo.compareTo(importo) >= 0) {
            saldo = saldo.subtract(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.ADDEBITO, descrizione));
            return true;
        }
        return false;
    }
    
    // Metodo specifico per acquisti
    public boolean effettuaAcquisto(BigDecimal importo, String descrizione) {
        if (importo.compareTo(BigDecimal.ZERO) > 0 && saldo.compareTo(importo) >= 0) {
            saldo = saldo.subtract(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.ACQUISTO, descrizione));
            return true;
        }
        return false;
    }
    
    // Metodo specifico per ricariche
    public void ricarica(BigDecimal importo, String metodoPagamento) {
        if (importo.compareTo(BigDecimal.ZERO) > 0) {
            saldo = saldo.add(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.RICARICA, 
                "Ricarica tramite " + metodoPagamento));
        }
    }

    // Metodo per ottenere la lista dei movimenti
    public List<Movimento> getMovimenti() {
        return new ArrayList<>(movimenti);
    }

    // Metodo per filtrare i movimenti per tipo
    public List<Movimento> getMovimentiPerTipo(TipoMovimento tipo) {
        return movimenti.stream()
                .filter(m -> m.getTipo() == tipo)
                .toList();
    }
    
    // Verifica se il saldo è sufficiente per un acquisto
    public boolean saldoSufficiente(BigDecimal importo) {
        return saldo.compareTo(importo) >= 0;
    }
    
    @Override
    public String toString() {
        return String.format("Conto [Utente: %d, Saldo: €%.2f, Movimenti: %d]", 
            utenteId, saldo, movimenti.size());
    }
}