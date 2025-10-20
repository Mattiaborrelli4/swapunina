package application.Classe;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Conto {
    private int id;
    private int utenteId;
    private BigDecimal saldo;
    private final List<Movimento> movimenti;

    public enum TipoMovimento {
        ACCREDITO, ADDEBITO, ACQUISTO, RICARICA
    }

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

    /**
     * Accredita un importo positivo sul conto
     */
    public void accredita(BigDecimal importo, String descrizione) {
        if (importo.compareTo(BigDecimal.ZERO) > 0) {
            saldo = saldo.add(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.ACCREDITO, descrizione));
        }
    }

    /**
     * Addebita un importo positivo dal conto se il saldo è sufficiente
     * @return true se l'operazione è riuscita, false altrimenti
     */
    public boolean addebita(BigDecimal importo, String descrizione) {
        if (importo.compareTo(BigDecimal.ZERO) > 0 && saldo.compareTo(importo) >= 0) {
            saldo = saldo.subtract(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.ADDEBITO, descrizione));
            return true;
        }
        return false;
    }
    
    /**
     * Effettua un acquisto specifico sottraendo l'importo dal saldo
     * @return true se l'acquisto è riuscito, false se saldo insufficiente
     */
    public boolean effettuaAcquisto(BigDecimal importo, String descrizione) {
        if (importo.compareTo(BigDecimal.ZERO) > 0 && saldo.compareTo(importo) >= 0) {
            saldo = saldo.subtract(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.ACQUISTO, descrizione));
            return true;
        }
        return false;
    }
    
    /**
     * Ricarica il conto con un importo positivo
     */
    public void ricarica(BigDecimal importo, String metodoPagamento) {
        if (importo.compareTo(BigDecimal.ZERO) > 0) {
            saldo = saldo.add(importo);
            movimenti.add(new Movimento(importo, TipoMovimento.RICARICA, 
                "Ricarica tramite " + metodoPagamento));
        }
    }

    /**
     * Restituisce una lista immodificabile dei movimenti per prevenire modifiche esterne
     */
    public List<Movimento> getMovimenti() {
        return Collections.unmodifiableList(movimenti);
    }

    /**
     * Filtra i movimenti per tipo utilizzando stream
     */
    public List<Movimento> getMovimentiPerTipo(TipoMovimento tipo) {
        return movimenti.stream()
                .filter(m -> m.getTipo() == tipo)
                .toList();
    }
    
    /**
     * Verifica se il saldo è sufficiente per un dato importo
     */
    public boolean saldoSufficiente(BigDecimal importo) {
        return saldo.compareTo(importo) >= 0;
    }
    
    @Override
    public String toString() {
        return String.format("Conto [Utente: %d, Saldo: €%.2f, Movimenti: %d]", 
            utenteId, saldo, movimenti.size());
    }
}