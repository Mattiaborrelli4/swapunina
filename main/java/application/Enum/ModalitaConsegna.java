package application.Enum;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum ModalitaConsegna {
    RITIRO_IN_PERSONA("Ritiro in persona", "🏠", "pickup", "#28a745"),
    SPEDIZIONE("Spedizione", "📦", "shipping", "#17a2b8"),
    ENTROAMBI("Ritiro o spedizione", "🔄", "both", "#ffc107");

    private final String displayName;
    private final String emoji;
    private final String styleClass;
    private final String color;
    
    // Cache per lookup performance
    private static final Map<String, ModalitaConsegna> DISPLAY_NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, ModalitaConsegna> NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] DISPLAY_NAMES;
    private static final ModalitaConsegna[] VALUES = values();
    
    static {
        // Pre-popola le mappe per lookup veloci
        for (ModalitaConsegna modalita : VALUES) {
            NAME_MAP.put(modalita.name().toLowerCase(), modalita);
            DISPLAY_NAME_MAP.put(modalita.displayName.toLowerCase(), modalita);
            // Aggiungi varianti comuni
            DISPLAY_NAME_MAP.put(modalita.name().toLowerCase(), modalita);
            DISPLAY_NAME_MAP.put("ritiro", RITIRO_IN_PERSONA);
            DISPLAY_NAME_MAP.put("spedizione", SPEDIZIONE);
            DISPLAY_NAME_MAP.put("entrambi", ENTROAMBI);
            DISPLAY_NAME_MAP.put("both", ENTROAMBI);
        }
        
        // Pre-calcola i display names
        DISPLAY_NAMES = Arrays.stream(VALUES)
                .map(ModalitaConsegna::getDisplayName)
                .toArray(String[]::new);
    }

    ModalitaConsegna(String displayName, String emoji, String styleClass, String color) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.styleClass = styleClass;
        this.color = color;
    }

    ModalitaConsegna(String displayName) {
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
     * Parsa una stringa in ModalitaConsegna con cache per performance
     * @param input stringa da parsare
     * @return ModalitaConsegna corrispondente, RITIRO_IN_PERSONA se non trovato
     */
    public static ModalitaConsegna parseModalita(String input) {
        if (input == null || input.trim().isEmpty()) {
            return RITIRO_IN_PERSONA;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        
        // Lookup veloce nelle mappe cache
        ModalitaConsegna fromDisplayName = DISPLAY_NAME_MAP.get(normalizedInput);
        if (fromDisplayName != null) {
            return fromDisplayName;
        }
        
        ModalitaConsegna fromName = NAME_MAP.get(normalizedInput);
        if (fromName != null) {
            return fromName;
        }
        
        // Fallback: cerca corrispondenze parziali
        for (ModalitaConsegna modalita : VALUES) {
            if (modalita.displayName.toLowerCase().contains(normalizedInput) ||
                modalita.name().toLowerCase().contains(normalizedInput)) {
                return modalita;
            }
        }
        
        return RITIRO_IN_PERSONA;
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
     * @return ModalitaConsegna corrispondente, RITIRO_IN_PERSONA se non valido
     */
    public static ModalitaConsegna fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
        return RITIRO_IN_PERSONA;
    }

    /**
     * Restituisce tutte le modalità come array di stringhe (pre-calcolato)
     * @return array di display names
     */
    public static String[] getAllDisplayNames() {
        return DISPLAY_NAMES.clone(); // Clone per sicurezza
    }
    
    /**
     * Restituisce tutte le modalità come array di stringhe con emoji
     * @return array di display names con emoji
     */
    public static String[] getAllDisplayNamesWithEmoji() {
        return Arrays.stream(VALUES)
                .map(ModalitaConsegna::getDisplayNameWithEmoji)
                .toArray(String[]::new);
    }
    
    /**
     * Restituisce tutte le modalità come lista per ComboBox
     * @return lista di modalità
     */
    public static java.util.List<String> getDisplayNamesList() {
        return Arrays.asList(DISPLAY_NAMES);
    }
    
    /**
     * Restituisce tutte le modalità con emoji come lista
     * @return lista di modalità con emoji
     */
    public static java.util.List<String> getDisplayNamesWithEmojiList() {
        return Arrays.stream(VALUES)
                .map(ModalitaConsegna::getDisplayNameWithEmoji)
                .collect(Collectors.toList());
    }

    /**
     * Verifica se una stringa rappresenta una modalità valida
     * @param input stringa da verificare
     * @return true se la modalità è valida
     */
    public static boolean isValidModalita(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        return DISPLAY_NAME_MAP.containsKey(normalizedInput) || 
               NAME_MAP.containsKey(normalizedInput);
    }

    // METODI DI BUSINESS LOGIC

    /**
     * Verifica se questa modalità è "Ritiro in persona"
     * @return true se è ritiro in persona
     */
    public boolean isRitiroInPersona() {
        return this == RITIRO_IN_PERSONA;
    }

    /**
     * Verifica se questa modalità è "Spedizione"
     * @return true se è spedizione
     */
    public boolean isSpedizione() {
        return this == SPEDIZIONE;
    }

    /**
     * Verifica se questa modalità è "Entrambi"
     * @return true se sono entrambe le modalità
     */
    public boolean isEntrambi() {
        return this == ENTROAMBI;
    }
    
    /**
     * Verifica se include il ritiro in persona
     * @return true se include ritiro
     */
    public boolean includeRitiro() {
        return this == RITIRO_IN_PERSONA || this == ENTROAMBI;
    }
    
    /**
     * Verifica se include la spedizione
     * @return true se include spedizione
     */
    public boolean includeSpedizione() {
        return this == SPEDIZIONE || this == ENTROAMBI;
    }
    
    /**
     * Verifica se la modalità è flessibile (entrambe le opzioni)
     * @return true se è flessibile
     */
    public boolean isFlessibile() {
        return this == ENTROAMBI;
    }
    
    /**
     * Verifica se la modalità richiede incontro fisico
     * @return true se richiede incontro
     */
    public boolean richiedeIncontro() {
        return this.includeRitiro();
    }
    
    /**
     * Verifica se la modalità richiede imballaggio
     * @return true se richiede imballaggio
     */
    public boolean richiedeImballaggio() {
        return this.includeSpedizione();
    }

    /**
     * Restituisce le modalità che includono ritiro
     * @return array di modalità con ritiro
     */
    public static ModalitaConsegna[] getModalitaConRitiro() {
        return new ModalitaConsegna[] { RITIRO_IN_PERSONA, ENTROAMBI };
    }
    
    /**
     * Restituisce le modalità che includono spedizione
     * @return array di modalità con spedizione
     */
    public static ModalitaConsegna[] getModalitaConSpedizione() {
        return new ModalitaConsegna[] { SPEDIZIONE, ENTROAMBI };
    }
    
    /**
     * Restituisce le modalità flessibili
     * @return array di modalità flessibili
     */
    public static ModalitaConsegna[] getModalitaFlessibili() {
        return new ModalitaConsegna[] { ENTROAMBI };
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
        return String.format("ModalitaConsegna{name=%s, displayName='%s', dbValue=%d, emoji='%s', color='%s'}",
                name(), displayName, toDbValue(), emoji, color);
    }
    
    /**
     * Ottiene la descrizione estesa della modalità
     * @return descrizione dettagliata
     */
    public String getDescrizioneEstesa() {
        switch (this) {
            case RITIRO_IN_PERSONA: return "L'acquirente ritira l'oggetto di persona";
            case SPEDIZIONE: return "L'oggetto viene spedito all'acquirente";
            case ENTROAMBI: return "Sia ritiro che spedizione disponibili";
            default: return displayName;
        }
    }
    
    /**
     * Ottiene il colore CSS per la modalità
     * @return colore CSS in formato esadecimale
     */
    public String getCssColor() {
        return color;
    }
    
    /**
     * Ottiene la classe CSS per la modalità
     * @return nome della classe CSS
     */
    public String getCssClass() {
        return "modalita-consegna-" + styleClass;
    }
    
    /**
     * Ottiene i costi stimati per la modalità
     * @return descrizione dei costi
     */
    public String getInfoCosti() {
        switch (this) {
            case RITIRO_IN_PERSONA: return "Gratuito - nessun costo aggiuntivo";
            case SPEDIZIONE: return "A carico dell'acquirente/venditore";
            case ENTROAMBI: return "Spedizione a pagamento, ritiro gratuito";
            default: return "Costi da definire";
        }
    }
    
    /**
     * Ottiene i tempi stimati per la modalità
     * @return descrizione dei tempi
     */
    public String getInfoTempi() {
        switch (this) {
            case RITIRO_IN_PERSONA: return "Immediato - all'accordo delle parti";
            case SPEDIZIONE: return "2-5 giorni lavorativi";
            case ENTROAMBI: return "Immediato per ritiro, 2-5 giorni per spedizione";
            default: return "Tempi da definire";
        }
    }
    
    /**
     * Verifica se la modalità è compatibile con una distanza
     * @param distanzaKm distanza in km
     * @return true se compatibile
     */
    public boolean isCompatibileConDistanza(double distanzaKm) {
        switch (this) {
            case RITIRO_IN_PERSONA:
                return distanzaKm <= 50; // Ritiro entro 50km
            case SPEDIZIONE:
                return true; // Spedizione disponibile ovunque
            case ENTROAMBI:
                return true; // Sempre compatibile
            default:
                return false;
        }
    }
}