package application.Enum;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum StatoOfferta {
    IN_ATTESA("In attesa", "⏳", "warning", "#FFA500"),
    ACCETTATA("Accettata", "✅", "success", "#28a745"),
    RIFIUTATA("Rifiutata", "❌", "error", "#dc3545");

    private final String displayName;
    private final String emoji;
    private final String styleClass;
    private final String color;
    
    // Cache per lookup performance
    private static final Map<String, StatoOfferta> DISPLAY_NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, StatoOfferta> NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] DISPLAY_NAMES;
    private static final StatoOfferta[] VALUES = values();
    
    static {
        // Pre-popola le mappe per lookup veloci
        for (StatoOfferta stato : VALUES) {
            NAME_MAP.put(stato.name().toLowerCase(), stato);
            DISPLAY_NAME_MAP.put(stato.displayName.toLowerCase(), stato);
            // Aggiungi varianti comuni
            DISPLAY_NAME_MAP.put(stato.name().toLowerCase().replace("_", ""), stato);
        }
        
        // Pre-calcola i display names
        DISPLAY_NAMES = Arrays.stream(VALUES)
                .map(StatoOfferta::getDisplayName)
                .toArray(String[]::new);
    }

    StatoOfferta(String displayName, String emoji, String styleClass, String color) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.styleClass = styleClass;
        this.color = color;
    }

    StatoOfferta(String displayName) {
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
     * Parsa una stringa in StatoOfferta con cache per performance
     * @param input stringa da parsare
     * @return StatoOfferta corrispondente, IN_ATTESA se non trovato
     */
    public static StatoOfferta parseStato(String input) {
        if (input == null || input.trim().isEmpty()) {
            return IN_ATTESA;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        
        // Lookup veloce nelle mappe cache
        StatoOfferta fromDisplayName = DISPLAY_NAME_MAP.get(normalizedInput);
        if (fromDisplayName != null) {
            return fromDisplayName;
        }
        
        StatoOfferta fromName = NAME_MAP.get(normalizedInput);
        if (fromName != null) {
            return fromName;
        }
        
        // Fallback: cerca corrispondenze parziali
        for (StatoOfferta stato : VALUES) {
            if (stato.displayName.toLowerCase().contains(normalizedInput) ||
                stato.name().toLowerCase().contains(normalizedInput)) {
                return stato;
            }
        }
        
        return IN_ATTESA;
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
     * @return StatoOfferta corrispondente, IN_ATTESA se non valido
     */
    public static StatoOfferta fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
        return IN_ATTESA;
    }

    /**
     * Restituisce tutti gli stati come array di stringhe (pre-calcolato)
     * @return array di display names
     */
    public static String[] getAllDisplayNames() {
        return DISPLAY_NAMES.clone(); // Clone per sicurezza
    }
    
    /**
     * Restituisce tutti gli stati come array di stringhe con emoji
     * @return array di display names con emoji
     */
    public static String[] getAllDisplayNamesWithEmoji() {
        return Arrays.stream(VALUES)
                .map(StatoOfferta::getDisplayNameWithEmoji)
                .toArray(String[]::new);
    }
    
    /**
     * Restituisce tutti gli stati come lista per ComboBox
     * @return lista di stati
     */
    public static java.util.List<String> getDisplayNamesList() {
        return Arrays.asList(DISPLAY_NAMES);
    }
    
    /**
     * Restituisce tutti gli stati con emoji come lista
     * @return lista di stati con emoji
     */
    public static java.util.List<String> getDisplayNamesWithEmojiList() {
        return Arrays.stream(VALUES)
                .map(StatoOfferta::getDisplayNameWithEmoji)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se una stringa rappresenta uno stato valido
     * @param input stringa da verificare
     * @return true se lo stato è valido
     */
    public static boolean isValidStato(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        return DISPLAY_NAME_MAP.containsKey(normalizedInput) || 
               NAME_MAP.containsKey(normalizedInput);
    }

    // METODI DI BUSINESS LOGIC

    /**
     * Verifica se questo stato è "In attesa"
     * @return true se è in attesa
     */
    public boolean isInAttesa() {
        return this == IN_ATTESA;
    }

    /**
     * Verifica se questo stato è "Accettata"
     * @return true se è accettata
     */
    public boolean isAccettata() {
        return this == ACCETTATA;
    }

    /**
     * Verifica se questo stato è "Rifiutata"
     * @return true se è rifiutata
     */
    public boolean isRifiutata() {
        return this == RIFIUTATA;
    }
    
    /**
     * Verifica se l'offerta è stata conclusa (accettata o rifiutata)
     * @return true se l'offerta è conclusa
     */
    public boolean isConclusa() {
        return this == ACCETTATA || this == RIFIUTATA;
    }
    
    /**
     * Verifica se l'offerta è ancora pendente
     * @return true se l'offerta è pendente
     */
    public boolean isPendente() {
        return this == IN_ATTESA;
    }
    
    /**
     * Verifica se l'offerta è stata accettata con successo
     * @return true se l'offerta è andata a buon fine
     */
    public boolean isSuccesso() {
        return this == ACCETTATA;
    }
    
    /**
     * Verifica se l'offerta è fallita
     * @return true se l'offerta è fallita
     */
    public boolean isFallita() {
        return this == RIFIUTATA;
    }

    /**
     * Restituisce gli stati conclusi (accettata/rifiutata)
     * @return array di stati conclusi
     */
    public static StatoOfferta[] getStatiConclusi() {
        return new StatoOfferta[] { ACCETTATA, RIFIUTATA };
    }
    
    /**
     * Restituisce gli stati pendenti
     * @return array di stati pendenti
     */
    public static StatoOfferta[] getStatiPendenti() {
        return new StatoOfferta[] { IN_ATTESA };
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
        return String.format("StatoOfferta{name=%s, displayName='%s', dbValue=%d, emoji='%s', color='%s'}",
                name(), displayName, toDbValue(), emoji, color);
    }
    
    /**
     * Ottiene la descrizione estesa dello stato
     * @return descrizione dettagliata
     */
    public String getDescrizioneEstesa() {
        switch (this) {
            case IN_ATTESA: return "L'offerta è in attesa di una risposta";
            case ACCETTATA: return "L'offerta è stata accettata";
            case RIFIUTATA: return "L'offerta è stata rifiutata";
            default: return displayName;
        }
    }
    
    /**
     * Ottiene il colore CSS per lo stato
     * @return colore CSS in formato esadecimale
     */
    public String getCssColor() {
        return color;
    }
    
    /**
     * Ottiene la classe CSS per lo stato
     * @return nome della classe CSS
     */
    public String getCssClass() {
        return "stato-offerta-" + styleClass;
    }
    
    /**
     * Transizione allo stato successivo (per flussi di lavoro)
     * @param nuovoStato nuovo stato desiderato
     * @return true se la transizione è valida
     */
    public boolean puòTransitareA(StatoOfferta nuovoStato) {
        if (nuovoStato == null) return false;
        
        switch (this) {
            case IN_ATTESA:
                return nuovoStato == ACCETTATA || nuovoStato == RIFIUTATA;
            case ACCETTATA:
            case RIFIUTATA:
                return false; // Stati finali, non si può cambiare
            default:
                return false;
        }
    }
}