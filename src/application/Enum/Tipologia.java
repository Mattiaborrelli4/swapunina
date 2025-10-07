package application.Enum;

public enum Tipologia {
    VENDITA("Vendita diretta"),
    SCAMBIO("Scambio"),
    REGALO("In regalo"),
    ASTA("Asta");

    private final String displayName;

    Tipologia(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Tipologia fromDisplayName(String displayName) {
        if (displayName == null) return null;
        
        // Normalizza la stringa per gestire diversi formati
        String normalized = displayName.trim().toUpperCase();
        
        for (Tipologia tipo : values()) {
            if (tipo.name().equals(normalized) || 
                tipo.getDisplayName().toUpperCase().equals(normalized)) {
                return tipo;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // 🔥 NUOVO: Metodo per verificare se richiede prezzo
    public boolean richiedePrezzo() {
        return this == VENDITA || this == ASTA;
    }

    // 🔥 NUOVO: Metodo per verificare se è asta
    public boolean isAsta() {
        return this == ASTA;
    }

    // 🔥 NUOVO: Metodo per verificare se è scambio
    public boolean isScambio() {
        return this == SCAMBIO;
    }

    // 🔥 NUOVO: Metodo per verificare se è regalo
    public boolean isRegalo() {
        return this == REGALO;
    }

    // 🔥 NUOVO: Metodo per verificare se è vendita diretta
    public boolean isVenditaDiretta() {
        return this == VENDITA;
    }
}