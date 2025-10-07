package application.Enum;

public enum StatoOfferta {
    InAttesa("In attesa"),
    Accettata("Accettata"),
    Rifiutata("Rifiutata");

    private final String displayName;

    StatoOfferta(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // 🔥 NUOVO: Metodo per parsare lo stato
    public static StatoOfferta parseStato(String input) {
        if (input == null) return InAttesa;

        input = input.trim();

        for (StatoOfferta stato : values()) {
            if (stato.name().equalsIgnoreCase(input) || 
                stato.getDisplayName().equalsIgnoreCase(input)) {
                return stato;
            }
        }

        return InAttesa;
    }

    // 🔥 NUOVO: Metodo per verificare se è in attesa
    public boolean isInAttesa() {
        return this == InAttesa;
    }

    // 🔥 NUOVO: Metodo per verificare se è accettata
    public boolean isAccettata() {
        return this == Accettata;
    }

    // 🔥 NUOVO: Metodo per verificare se è rifiutata
    public boolean isRifiutata() {
        return this == Rifiutata;
    }

    @Override
    public String toString() {
        return displayName;
    }
}