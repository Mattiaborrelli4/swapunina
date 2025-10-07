package application.Classe;

import java.time.LocalDateTime;

public class Offerta {
    private double importo;
    private String offerente;
    private LocalDateTime dataOra;
    private boolean accettata;

    public Offerta(double importo, String offerente) {
        this.importo = importo;
        this.offerente = offerente;
        this.dataOra = LocalDateTime.now();
        this.accettata = false;
    }

    // GETTER
    public double getImporto() {
        return importo;
    }

    public String getOfferente() {
        return offerente;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public boolean isAccettata() {
        return accettata;
    }

    // SETTER
    public void setAccettata(boolean accettata) {
        this.accettata = accettata;
    }

    @Override
    public String toString() {
        return "Offerta{" +
                "importo=" + importo +
                ", offerente='" + offerente + '\'' +
                ", dataOra=" + dataOra +
                ", accettata=" + accettata +
                '}';
    }
}