package application.Enum;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum OrigineOggetto {
    NUOVO("Nuovo", "🆕", "new", "#28a745"),
    USATO("Usato", "🔧", "used", "#6c757d"),
    RICONDIZIONATO("Ricondizionato", "🔄", "refurbished", "#17a2b8"),
    REGALO("Ricevuto in regalo", "🎁", "gift", "#e83e8c"),
    SCAMBIO("Ottenuto per scambio", "🤝", "exchange", "#ffc107");
    
    private final String displayName;
    private final String emoji;
    private final String styleClass;
    private final String color;
    
    // Cache per lookup performance
    private static final Map<String, OrigineOggetto> DISPLAY_NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, OrigineOggetto> NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] DISPLAY_NAMES;
    private static final OrigineOggetto[] VALUES = values();
    
    static {
        // Pre-popola le mappe per lookup veloci
        for (OrigineOggetto origine : VALUES) {
            NAME_MAP.put(origine.name().toLowerCase(), origine);
            DISPLAY_NAME_MAP.put(origine.displayName.toLowerCase(), origine);
            // Aggiungi varianti comuni
            DISPLAY_NAME_MAP.put(origine.name().toLowerCase(), origine);
        }
        
        // Pre-calcola i display names
        DISPLAY_NAMES = Arrays.stream(VALUES)
                .map(OrigineOggetto::getDisplayName)
                .toArray(String[]::new);
    }
    
    OrigineOggetto(String displayName, String emoji, String styleClass, String color) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.styleClass = styleClass;
        this.color = color;
    }
    
    OrigineOggetto(String displayName) {
        this(displayName, "", "default", "#6c757d");
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public String getStyleClass() {
        return styleClass;
    }
    
    public String getColor() {
        return color;
    }
    
    /**
     * Restituisce il display name con emoji (se presente)
     * @return stringa formattata con emoji e nome
     */
    public String getDisplayNameWithEmoji() {
        return emoji.isEmpty() ? displayName : emoji + " " + displayName;
    }

    /**
     * Parsa una stringa in OrigineOggetto con cache per performance
     * @param input stringa da parsare
     * @return OrigineOggetto corrispondente, USATO se non trovato
     */
    public static OrigineOggetto parseOrigine(String input) {
        if (input == null || input.trim().isEmpty()) {
            return USATO; // Default più comune
        }
        
        String normalizedInput = input.trim().toLowerCase();
        
        // Lookup veloce nelle mappe cache
        OrigineOggetto fromDisplayName = DISPLAY_NAME_MAP.get(normalizedInput);
        if (fromDisplayName != null) {
            return fromDisplayName;
        }
        
        OrigineOggetto fromName = NAME_MAP.get(normalizedInput);
        if (fromName != null) {
            return fromName;
        }
        
        // Fallback: cerca corrispondenze parziali
        for (OrigineOggetto origine : VALUES) {
            if (origine.displayName.toLowerCase().contains(normalizedInput) ||
                origine.name().toLowerCase().contains(normalizedInput)) {
                return origine;
            }
        }
        
        return USATO; // fallback a usato
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
     * @return OrigineOggetto corrispondente, USATO se non valido
     */
    public static OrigineOggetto fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
        return USATO;
    }

    /**
     * Restituisce tutte le origini come array di stringhe (pre-calcolato)
     * @return array di display names
     */
    public static String[] getAllDisplayNames() {
        return DISPLAY_NAMES.clone(); // Clone per sicurezza
    }
    
    /**
     * Restituisce tutte le origini come array di stringhe con emoji
     * @return array di display names con emoji
     */
    public static String[] getAllDisplayNamesWithEmoji() {
        return Arrays.stream(VALUES)
                .map(OrigineOggetto::getDisplayNameWithEmoji)
                .toArray(String[]::new);
    }
    
    /**
     * Restituisce tutte le origini come lista per ComboBox
     * @return lista di origini
     */
    public static java.util.List<String> getDisplayNamesList() {
        return Arrays.asList(DISPLAY_NAMES);
    }
    
    /**
     * Restituisce tutte le origini con emoji come lista
     * @return lista di origini con emoji
     */
    public static java.util.List<String> getDisplayNamesWithEmojiList() {
        return Arrays.stream(VALUES)
                .map(OrigineOggetto::getDisplayNameWithEmoji)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se una stringa rappresenta un'origine valida
     * @param input stringa da verificare
     * @return true se l'origine è valida
     */
    public static boolean isValidOrigine(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        return DISPLAY_NAME_MAP.containsKey(normalizedInput) || 
               NAME_MAP.containsKey(normalizedInput);
    }

    // METODI DI BUSINESS LOGIC

    /**
     * Verifica se questa origine è "Nuovo"
     * @return true se è nuovo
     */
    public boolean isNuovo() {
        return this == NUOVO;
    }

    /**
     * Verifica se questa origine è "Usato"
     * @return true se è usato
     */
    public boolean isUsato() {
        return this == USATO;
    }
    
    /**
     * Verifica se questa origine è "Ricondizionato"
     * @return true se è ricondizionato
     */
    public boolean isRicondizionato() {
        return this == RICONDIZIONATO;
    }
    
    /**
     * Verifica se questa origine è "Regalo"
     * @return true se è regalo
     */
    public boolean isRegalo() {
        return this == REGALO;
    }
    
    /**
     * Verifica se questa origine è "Scambio"
     * @return true se è scambio
     */
    public boolean isScambio() {
        return this == SCAMBIO;
    }
    
    /**
     * Verifica se l'oggetto è nuovo o ricondizionato (condizioni migliori)
     * @return true se è in ottime condizioni
     */
    public boolean isComeNuovo() {
        return this == NUOVO || this == RICONDIZIONATO;
    }
    
    /**
     * Verifica se l'oggetto è di seconda mano
     * @return true se è usato/scambiato/regalato
     */
    public boolean isSecondaMano() {
        return this == USATO || this == REGALO || this == SCAMBIO;
    }
    
    /**
     * Verifica se l'origine indica che l'oggetto è gratuito
     * @return true se è gratuito
     */
    public boolean isGratuito() {
        return this == REGALO;
    }

    /**
     * Restituisce le origini per oggetti in ottime condizioni
     * @return array di origini come nuovi
     */
    public static OrigineOggetto[] getOriginiComeNuove() {
        return new OrigineOggetto[] { NUOVO, RICONDIZIONATO };
    }
    
    /**
     * Restituisce le origini per oggetti di seconda mano
     * @return array di origini seconda mano
     */
    public static OrigineOggetto[] getOriginiSecondaMano() {
        return new OrigineOggetto[] { USATO, REGALO, SCAMBIO };
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
        return String.format("OrigineOggetto{name=%s, displayName='%s', dbValue=%d, emoji='%s', color='%s'}",
                name(), displayName, toDbValue(), emoji, color);
    }
    
    /**
     * Ottiene la descrizione estesa dell'origine
     * @return descrizione dettagliata
     */
    public String getDescrizioneEstesa() {
        switch (this) {
            case NUOVO: return "Prodotto nuovo, mai utilizzato";
            case USATO: return "Prodotto usato, in condizioni variabili";
            case RICONDIZIONATO: return "Prodotto usato ricondizionato dal produttore";
            case REGALO: return "Prodotto ricevuto in regalo";
            case SCAMBIO: return "Prodotto ottenuto tramite scambio";
            default: return displayName;
        }
    }
    
    /**
     * Ottiene il colore CSS per l'origine
     * @return colore CSS in formato esadecimale
     */
    public String getCssColor() {
        return color;
    }
    
    /**
     * Ottiene la classe CSS per l'origine
     * @return nome della classe CSS
     */
    public String getCssClass() {
        return "origine-oggetto-" + styleClass;
    }
    
    /**
     * Ottiene un suggerimento per il prezzo in base all'origine
     * @return suggerimento per il prezzo
     */
    public String getSuggerimentoPrezzo() {
        switch (this) {
            case NUOVO: return "Prezzo di listino o leggermente scontato";
            case USATO: return "Prezzo ridotto in base alle condizioni";
            case RICONDIZIONATO: return "Prezzo inferiore al nuovo con garanzia";
            case REGALO: return "Generalmente gratuito o prezzo simbolico";
            case SCAMBIO: return "Valore equivalente all'oggetto scambiato";
            default: return "Prezzo in base al mercato";
        }
    }
    
    // Metodo isVendibile
    public boolean isVendibile() {
        return this == NUOVO || this == USATO || this == RICONDIZIONATO;
    }
}