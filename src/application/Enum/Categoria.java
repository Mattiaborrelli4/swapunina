package application.Enum;

public enum Categoria {
    LIBRI("Libri"),
    INFORMATICA("Informatica"),
    ABBIGLIAMENTO("Abbigliamento"),
    ELETTRONICA("Elettronica"),
    MUSICA("Musica"),
    CASA("Casa e arredamento"),
    SPORT("Sport"),
    GIOCATTOLI("Giocattoli"),
    ALTRO("Altro");

    private final String displayName;

    Categoria(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int toDbValue() {
        return this.ordinal() + 1;
    }

    public static Categoria fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index < 0 || index >= values().length) {
            return ALTRO;
        }
        return values()[index];
    }

    public static Categoria parseCategoria(String input) {
        if (input == null) return ALTRO;

        input = input.trim();

        // Cerca match con il nome dell'enum (es: LIBRI)
        for (Categoria c : values()) {
            if (c.name().equalsIgnoreCase(input)) {
                return c;
            }
        }

        // Cerca match con il displayName (es: "Libri", "Informatica")
        for (Categoria c : values()) {
            if (c.getDisplayName().equalsIgnoreCase(input)) {
                return c;
            }
        }

        return ALTRO;
    }

    // 🔥 NUOVO: Metodo per ottenere tutte le categorie come array di stringhe
    public static String[] getAllDisplayNames() {
        Categoria[] categories = values();
        String[] names = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            names[i] = categories[i].getDisplayName();
        }
        return names;
    }

    // 🔥 NUOVO: Metodo per verificare se è una categoria valida
    public static boolean isValidCategory(String input) {
        return !parseCategoria(input).equals(ALTRO) || ALTRO.name().equalsIgnoreCase(input);
    }
}