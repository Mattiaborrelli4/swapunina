package application.Enum;

public enum OrigineOggetto {
    NUOVO("Nuovo"),
    USATO("Usato"),
    RICONDIZIONATO("Ricondizionato"),
    REGALO("Ricevuto in regalo"),
    SCAMBIO("Ottenuto per scambio");
    
    private final String displayName;
    
    OrigineOggetto(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    // 🔥 NUOVO: Metodo per parsare l'origine
    public static OrigineOggetto parseOrigine(String input) {
        if (input == null) return USATO; // Default più comune

        input = input.trim();

        for (OrigineOggetto origine : values()) {
            if (origine.name().equalsIgnoreCase(input) || 
                origine.getDisplayName().equalsIgnoreCase(input)) {
                return origine;
            }
        }

        return USATO; // fallback a usato
    }

    // 🔥 NUOVO: Metodo per verificare se è nuovo
    public boolean isNuovo() {
        return this == NUOVO;
    }

    // 🔥 NUOVO: Metodo per verificare se è usato
    public boolean isUsato() {
        return this == USATO;
    }

    // 🔥 NUOVO: Metodo per ottenere tutte le origini come array di stringhe
    public static String[] getAllDisplayNames() {
        OrigineOggetto[] origini = values();
        String[] names = new String[origini.length];
        for (int i = 0; i < origini.length; i++) {
            names[i] = origini[i].getDisplayName();
        }
        return names;
    }
}