package application.Enum;

public enum StatoAnnuncio {
    ATTIVO("Attivo"),
    VENDUTO("Venduto"),
    RITIRATO("Ritirato"),
    SCADUTO("Scaduto");

    private final String displayName;

    StatoAnnuncio(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static StatoAnnuncio parseStato(String input) {
        if (input == null) return ATTIVO;

        input = input.trim();

        for (StatoAnnuncio stato : values()) {
            if (stato.name().equalsIgnoreCase(input) || 
                stato.getDisplayName().equalsIgnoreCase(input)) {
                return stato;
            }
        }

        return ATTIVO;
    }
}