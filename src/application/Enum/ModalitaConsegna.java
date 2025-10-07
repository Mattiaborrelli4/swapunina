package application.Enum;

public enum ModalitaConsegna {
    RITIRO_IN_PERSONA("Ritiro in persona"),
    SPEDIZIONE("Spedizione"),
    ENTROAMBI("Ritiro o spedizione");

    private final String displayName;

    ModalitaConsegna(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ModalitaConsegna parseModalita(String input) {
        if (input == null) return RITIRO_IN_PERSONA;

        input = input.trim();

        for (ModalitaConsegna modalita : values()) {
            if (modalita.name().equalsIgnoreCase(input) || 
                modalita.getDisplayName().equalsIgnoreCase(input)) {
                return modalita;
            }
        }

        return RITIRO_IN_PERSONA;
    }
}