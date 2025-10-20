package application.Enum;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum Categoria {
    LIBRI("Libri", "📚"),
    INFORMATICA("Informatica", "💻"),
    ABBIGLIAMENTO("Abbigliamento", "👕"),
    ELETTRONICA("Elettronica", "📱"),
    MUSICA("Musica", "🎵"),
    CASA("Casa e arredamento", "🏠"),
    SPORT("Sport", "⚽"),
    GIOCATTOLI("Giocattoli", "🧸"),
    ALTRO("Altro", "📦");

    private final String displayName;
    private final String emoji;
    
    // Cache per lookup performance
    private static final Map<String, Categoria> DISPLAY_NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Categoria> NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] DISPLAY_NAMES;
    private static final Categoria[] VALUES = values();
    
    static {
        // Pre-popola le mappe per lookup veloci
        for (Categoria categoria : VALUES) {
            NAME_MAP.put(categoria.name().toLowerCase(), categoria);
            DISPLAY_NAME_MAP.put(categoria.displayName.toLowerCase(), categoria);
            // Aggiungi anche varianti comuni
            DISPLAY_NAME_MAP.put(categoria.name().toLowerCase(), categoria);
        }
        
        // Pre-calcola i display names
        DISPLAY_NAMES = Arrays.stream(VALUES)
                .map(Categoria::getDisplayName)
                .toArray(String[]::new);
    }

    Categoria(String displayName, String emoji) {
        this.displayName = displayName;
        this.emoji = emoji;
    }

    Categoria(String displayName) {
        this(displayName, "");
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    /**
     * Restituisce il display name con emoji (se presente)
     * @return stringa formattata con emoji e nome
     */
    public String getDisplayNameWithEmoji() {
        return emoji.isEmpty() ? displayName : emoji + " " + displayName;
    }

    /**
     * Converte l'enum in valore per database
     * @return valore numerico per il database
     */
    public int toDbValue() {
        return this.ordinal() + 1;
    }

    /**
     * Converte un valore dal database in enum
     * @param dbValue valore dal database
     * @return Categoria corrispondente, ALTRO se non valido
     */
    public static Categoria fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
        return ALTRO;
    }

    /**
     * Parsa una stringa in Categoria con cache per performance
     * @param input stringa da parsare
     * @return Categoria corrispondente, ALTRO se non trovata
     */
    public static Categoria parseCategoria(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ALTRO;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        
        // Lookup veloce nelle mappe cache
        Categoria fromDisplayName = DISPLAY_NAME_MAP.get(normalizedInput);
        if (fromDisplayName != null) {
            return fromDisplayName;
        }
        
        Categoria fromName = NAME_MAP.get(normalizedInput);
        if (fromName != null) {
            return fromName;
        }
        
        // Fallback: cerca corrispondenze parziali nei display names
        for (Categoria categoria : VALUES) {
            if (categoria.displayName.toLowerCase().contains(normalizedInput) ||
                categoria.name().toLowerCase().contains(normalizedInput)) {
                return categoria;
            }
        }
        
        return ALTRO;
    }

    /**
     * Restituisce tutte le categorie come array di stringhe (pre-calcolato)
     * @return array di display names
     */
    public static String[] getAllDisplayNames() {
        return DISPLAY_NAMES.clone(); // Clone per sicurezza
    }
    
    /**
     * Restituisce tutte le categorie come array di stringhe con emoji
     * @return array di display names con emoji
     */
    public static String[] getAllDisplayNamesWithEmoji() {
        return Arrays.stream(VALUES)
                .map(Categoria::getDisplayNameWithEmoji)
                .toArray(String[]::new);
    }
    
    /**
     * Restituisce tutte le categorie come lista per ComboBox
     * @return lista di categorie
     */
    public static java.util.List<String> getDisplayNamesList() {
        return Arrays.asList(DISPLAY_NAMES);
    }
    
    /**
     * Restituisce tutte le categorie con emoji come lista
     * @return lista di categorie con emoji
     */
    public static java.util.List<String> getDisplayNamesWithEmojiList() {
        return Arrays.stream(VALUES)
                .map(Categoria::getDisplayNameWithEmoji)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se una stringa rappresenta una categoria valida
     * @param input stringa da verificare
     * @return true se la categoria è valida
     */
    public static boolean isValidCategory(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        return DISPLAY_NAME_MAP.containsKey(normalizedInput) || 
               NAME_MAP.containsKey(normalizedInput) ||
               ALTRO.name().equalsIgnoreCase(input);
    }
    
    /**
     * Verifica se questa categoria è ALTRO
     * @return true se è la categoria ALTRO
     */
    public boolean isAltro() {
        return this == ALTRO;
    }
    
    /**
     * Restituisce le categorie più comuni per suggerimenti
     * @return array delle categorie principali (escludendo ALTRO)
     */
    public static Categoria[] getMainCategories() {
        return Arrays.copyOf(VALUES, VALUES.length - 1);
    }
    
    /**
     * Cerca categorie per parola chiave
     * @param keyword parola da cercare
     * @return array di categorie che corrispondono
     */
    public static Categoria[] searchCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getMainCategories();
        }
        
        String normalizedKeyword = keyword.trim().toLowerCase();
        return Arrays.stream(VALUES)
                .filter(cat -> !cat.isAltro())
                .filter(cat -> 
                    cat.displayName.toLowerCase().contains(normalizedKeyword) ||
                    cat.name().toLowerCase().contains(normalizedKeyword) ||
                    cat.emoji.contains(normalizedKeyword)
                )
                .toArray(Categoria[]::new);
    }
    
    @Override
    public String toString() {
        return displayName;
    }
    
    /**
     * Metodo utility per logging
     * @return rappresentazione dettagliata
     */
    public String toDebugString() {
        return String.format("Categoria{name=%s, displayName='%s', dbValue=%d, emoji='%s'}",
                name(), displayName, toDbValue(), emoji);
    }
}