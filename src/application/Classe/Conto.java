package application.Classe;

public class Conto {
    private float saldo;

    public Conto() {
        this.saldo = 0;
    }

    public float getSaldo() {
        return saldo;
    }

    public void accredita(float importo) {
        if (importo > 0) saldo += importo;
    }

    public boolean addebita(float importo) {
        if (importo > 0 && saldo >= importo) {
            saldo -= importo;
            return true;
        }
        return false;
    }
}