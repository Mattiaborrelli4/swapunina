package application.Enum;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum Tipologia {
    VENDITA("Vendita diretta", "💰", true),
    SCAMBIO("Scambio", "🔄", false),
    REGALO("In regalo", "🎁", false),
    ASTA("Asta", "🔨", true);

    private final String displayName;
    private final String emoji;
    private final boolean richiedePrezzo;
    
    // Cache per lookup performance
    private static final Map<String, Tipologia> DISPLAY_NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, Tipologia> NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] DISPLAY_NAMES;
    private static final Tipologia[] VALUES = values();
    
    static {
        // Pre-popola le mappe per lookup veloci
        for (Tipologia tipologia : VALUES) {
            NAME_MAP.put(tipologia.name().toLowerCase(), tipologia);
            DISPLAY_NAME_MAP.put(tipologia.displayName.toLowerCase(), tipologia);
            // Aggiungi varianti comuni
            DISPLAY_NAME_MAP.put(tipologia.name().toLowerCase(), tipologia);
        }
        
        // Pre-calcola i display names
        DISPLAY_NAMES = Arrays.stream(VALUES)
                .map(Tipologia::getDisplayName)
                .toArray(String[]::new);
    }

    Tipologia(String displayName, String emoji, boolean richiedePrezzo) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.richiedePrezzo = richiedePrezzo;
    }

    Tipologia(String displayName) {
        this(displayName, "", false);
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
     * Converte da display name a Tipologia con cache per performance
     * @param displayName display name da convertire
     * @return Tipologia corrispondente, null se non trovata
     */
    public static Tipologia fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }
        
        String normalized = displayName.trim().toLowerCase();
        return DISPLAY_NAME_MAP.get(normalized);
    }

    /**
     * Parsa una stringa in Tipologia con cache per performance
     * @param input stringa da parsare
     * @return Tipologia corrispondente, null se non trovata
     */
    public static Tipologia parseTipologia(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        
        // Lookup veloce nelle mappe cache
        Tipologia fromDisplayName = DISPLAY_NAME_MAP.get(normalizedInput);
        if (fromDisplayName != null) {
            return fromDisplayName;
        }
        
        Tipologia fromName = NAME_MAP.get(normalizedInput);
        if (fromName != null) {
            return fromName;
        }
        
        // Fallback: cerca corrispondenze parziali
        for (Tipologia tipo : VALUES) {
            if (tipo.displayName.toLowerCase().contains(normalizedInput) ||
                tipo.name().toLowerCase().contains(normalizedInput)) {
                return tipo;
            }
        }
        
        return null;
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
     * @return Tipologia corrispondente, null se non valido
     */
    public static Tipologia fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
        return null;
    }

    /**
     * Restituisce tutte le tipologie come array di stringhe (pre-calcolato)
     * @return array di display names
     */
    public static String[] getAllDisplayNames() {
        return DISPLAY_NAMES.clone(); // Clone per sicurezza
    }
    
    /**
     * Restituisce tutte le tipologie come array di stringhe con emoji
     * @return array di display names con emoji
     */
    public static String[] getAllDisplayNamesWithEmoji() {
        return Arrays.stream(VALUES)
                .map(Tipologia::getDisplayNameWithEmoji)
                .toArray(String[]::new);
    }
    
    /**
     * Restituisce tutte le tipologie come lista per ComboBox
     * @return lista di tipologie
     */
    public static java.util.List<String> getDisplayNamesList() {
        return Arrays.asList(DISPLAY_NAMES);
    }
    
    /**
     * Restituisce tutte le tipologie con emoji come lista
     * @return lista di tipologie con emoji
     */
    public static java.util.List<String> getDisplayNamesWithEmojiList() {
        return Arrays.stream(VALUES)
                .map(Tipologia::getDisplayNameWithEmoji)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se una stringa rappresenta una tipologia valida
     * @param input stringa da verificare
     * @return true se la tipologia è valida
     */
    public static boolean isValidTipologia(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        return DISPLAY_NAME_MAP.containsKey(normalizedInput) || 
               NAME_MAP.containsKey(normalizedInput);
    }

    // METODI DI BUSINESS LOGIC

    /**
     * Verifica se questa tipologia richiede un prezzo
     * @return true se richiede prezzo
     */
    public boolean richiedePrezzo() {
        return richiedePrezzo;
    }

    /**
     * Verifica se questa tipologia è un'asta
     * @return true se è asta
     */
    public boolean isAsta() {
        return this == ASTA;
    }

    /**
     * Verifica se questa tipologia è uno scambio
     * @return true se è scambio
     */
    public boolean isScambio() {
        return this == SCAMBIO;
    }

    /**
     * Verifica se questa tipologia è un regalo
     * @return true se è regalo
     */
    public boolean isRegalo() {
        return this == REGALO;
    }

    /**
     * Verifica se questa tipologia è una vendita diretta
     * @return true se è vendita diretta
     */
    public boolean isVenditaDiretta() {
        return this == VENDITA;
    }

    /**
     * Verifica se questa tipologia permette offerte
     * @return true se permette offerte
     */
    public boolean permetteOfferte() {
        return this == ASTA || this == SCAMBIO;
    }

    /**
     * Verifica se questa tipologia è gratuita
     * @return true se è gratuita
     */
    public boolean isGratuita() {
        return this == REGALO;
    }

    /**
     * Restituisce le tipologie che richiedono prezzo
     * @return array di tipologie con prezzo
     */
    public static Tipologia[] getTipologieConPrezzo() {
        return Arrays.stream(VALUES)
                .filter(Tipologia::richiedePrezzo)
                .toArray(Tipologia[]::new);
    }

    /**
     * Restituisce le tipologie senza prezzo
     * @return array di tipologie senza prezzo
     */
    public static Tipologia[] getTipologieSenzaPrezzo() {
        return Arrays.stream(VALUES)
                .filter(tipo -> !tipo.richiedePrezzo())
                .toArray(Tipologia[]::new);
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
        return String.format("Tipologia{name=%s, displayName='%s', richiedePrezzo=%s, emoji='%s'}",
                name(), displayName, richiedePrezzo, emoji);
    }
    
    /**
     * Ottiene la descrizione estesa della tipologia
     * @return descrizione dettagliata
     */
    public String getDescrizioneEstesa() {
        switch (this) {
            case VENDITA: return "Vendita diretta a prezzo fisso";
            case SCAMBIO: return "Scambio con altri oggetti";
            case REGALO: return "Regalo completamente gratuito";
            case ASTA: return "Asta con offerte competitive";
            default: return displayName;
        }
    }
}