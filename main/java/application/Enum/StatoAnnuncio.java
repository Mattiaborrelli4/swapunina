package application.Enum;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public enum StatoAnnuncio {
    ATTIVO("Attivo", "🟢", "success", "#28a745", true),
    VENDUTO("Venduto", "💰", "sold", "#17a2b8", false),
    RITIRATO("Ritirato", "⏹️", "withdrawn", "#6c757d", false),
    SCADUTO("Scaduto", "⏰", "expired", "#ffc107", false);

    private final String displayName;
    private final String emoji;
    private final String styleClass;
    private final String color;
    private final boolean attivo;
    
    // Cache per lookup performance
    private static final Map<String, StatoAnnuncio> DISPLAY_NAME_MAP = new ConcurrentHashMap<>();
    private static final Map<String, StatoAnnuncio> NAME_MAP = new ConcurrentHashMap<>();
    private static final String[] DISPLAY_NAMES;
    private static final StatoAnnuncio[] VALUES = values();
    private static final StatoAnnuncio[] STATI_ATTIVI = {ATTIVO};
    private static final StatoAnnuncio[] STATI_NON_ATTIVI = {VENDUTO, RITIRATO, SCADUTO};
    
    static {
        // Pre-popola le mappe per lookup veloci
        for (StatoAnnuncio stato : VALUES) {
            NAME_MAP.put(stato.name().toLowerCase(), stato);
            DISPLAY_NAME_MAP.put(stato.displayName.toLowerCase(), stato);
            // Aggiungi varianti comuni
            DISPLAY_NAME_MAP.put(stato.name().toLowerCase(), stato);
        }
        
        // Pre-calcola i display names
        DISPLAY_NAMES = Arrays.stream(VALUES)
                .map(StatoAnnuncio::getDisplayName)
                .toArray(String[]::new);
    }

    StatoAnnuncio(String displayName, String emoji, String styleClass, String color, boolean attivo) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.styleClass = styleClass;
        this.color = color;
        this.attivo = attivo;
    }

    StatoAnnuncio(String displayName) {
        this(displayName, "", "default", "#6c757d", false);
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
     * Restituisce true se lo stato è attivo.
     * @return true se attivo
     */
    public boolean isAttivo() {
        return attivo;
    }
    
    /**
     * Restituisce il display name con emoji (se presente)
     * @return stringa formattata con emoji e nome
     */
    public String getDisplayNameWithEmoji() {
        return emoji.isEmpty() ? displayName : emoji + " " + displayName;
    }

    /**
     * Parsa una stringa in StatoAnnuncio con cache per performance
     * @param input stringa da parsare
     * @return StatoAnnuncio corrispondente, ATTIVO se non trovato
     */
    public static StatoAnnuncio parseStato(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ATTIVO;
        }
        
        String normalizedInput = input.trim().toLowerCase();
        
        // Lookup veloce nelle mappe cache
        StatoAnnuncio fromDisplayName = DISPLAY_NAME_MAP.get(normalizedInput);
        if (fromDisplayName != null) {
            return fromDisplayName;
        }
        
        StatoAnnuncio fromName = NAME_MAP.get(normalizedInput);
        if (fromName != null) {
            return fromName;
        }
        
        // Fallback: cerca corrispondenze parziali
        for (StatoAnnuncio stato : VALUES) {
            if (stato.displayName.toLowerCase().contains(normalizedInput) ||
                stato.name().toLowerCase().contains(normalizedInput)) {
                return stato;
            }
        }
        
        return ATTIVO;
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
     * @return StatoAnnuncio corrispondente, ATTIVO se non valido
     */
    public static StatoAnnuncio fromDbValue(int dbValue) {
        int index = dbValue - 1;
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
        return ATTIVO;
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
                .map(StatoAnnuncio::getDisplayNameWithEmoji)
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
                .map(StatoAnnuncio::getDisplayNameWithEmoji)
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
     * Verifica se questo stato è "Venduto"
     * @return true se è venduto
     */
    public boolean isVenduto() {
        return this == VENDUTO;
    }

    /**
     * Verifica se questo stato è "Ritirato"
     * @return true se è ritirato
     */
    public boolean isRitirato() {
        return this == RITIRATO;
    }

    /**
     * Verifica se questo stato è "Scaduto"
     * @return true se è scaduto
     */
    public boolean isScaduto() {
        return this == SCADUTO;
    }
    
    /**
     * Verifica se l'annuncio è attivo e visibile
     * @return true se l'annuncio è attivo
     */
    public boolean isVisibile() {
        return this == ATTIVO;
    }
    
    /**
     * Verifica se l'annuncio è concluso (non più attivo)
     * @return true se l'annuncio è concluso
     */
    public boolean isConcluso() {
        return this == VENDUTO || this == RITIRATO || this == SCADUTO;
    }
    
    /**
     * Verifica se l'annuncio può essere modificato
     * @return true se l'annuncio è modificabile
     */
    public boolean isModificabile() {
        return this == ATTIVO;
    }
    
    /**
     * Verifica se l'annuncio può ricevere offerte
     * @return true se l'annuncio può ricevere offerte
     */
    public boolean puoRicevereOfferte() {
        return this == ATTIVO;
    }
    
    /**
     * Verifica se l'annuncio è disponibile per la vendita
     * @return true se l'annuncio è disponibile
     */
    public boolean isDisponibile() {
        return this == ATTIVO;
    }

    /**
     * Restituisce gli stati attivi
     * @return array di stati attivi
     */
    public static StatoAnnuncio[] getStatiAttivi() {
        return STATI_ATTIVI.clone();
    }
    
    /**
     * Restituisce gli stati non attivi
     * @return array di stati non attivi
     */
    public static StatoAnnuncio[] getStatiNonAttivi() {
        return STATI_NON_ATTIVI.clone();
    }
    
    /**
     * Restituisce gli stati che possono essere visualizzati pubblicamente
     * @return array di stati visibili
     */
    public static StatoAnnuncio[] getStatiVisibili() {
        return new StatoAnnuncio[] { ATTIVO };
    }
    
    /**
     * Restituisce gli stati che indicano conclusione dell'annuncio
     * @return array di stati conclusi
     */
    public static StatoAnnuncio[] getStatiConclusi() {
        return new StatoAnnuncio[] { VENDUTO, RITIRATO, SCADUTO };
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
        return String.format("StatoAnnuncio{name=%s, displayName='%s', dbValue=%d, attivo=%s, emoji='%s', color='%s'}",
                name(), displayName, toDbValue(), attivo, emoji, color);
    }
    
    /**
     * Ottiene la descrizione estesa dello stato
     * @return descrizione dettagliata
     */
    public String getDescrizioneEstesa() {
        switch (this) {
            case ATTIVO: return "L'annuncio è attivo e visibile a tutti gli utenti";
            case VENDUTO: return "L'annuncio è stato venduto con successo";
            case RITIRATO: return "L'annuncio è stato ritirato dal venditore";
            case SCADUTO: return "L'annuncio è scaduto naturalmente";
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
        return "stato-annuncio-" + styleClass;
    }
    
    /**
     * Transizione allo stato successivo (per flussi di lavoro)
     * @param nuovoStato nuovo stato desiderato
     * @return true se la transizione è valida
     */
    public boolean puòTransitareA(StatoAnnuncio nuovoStato) {
        if (nuovoStato == null) return false;
        
        // Da ATTIVO si può andare a qualsiasi stato
        if (this == ATTIVO) {
            return true;
        }
        
        // Gli stati conclusi non possono cambiare
        return false;
    }
    
    /**
     * Verifica se lo stato può essere impostato manualmente
     * @return true se lo stato può essere impostato manualmente
     */
    public boolean puòEssereImpostatoManualmente() {
        // Solo alcuni stati possono essere impostati manualmente
        return this == ATTIVO || this == RITIRATO;
    }
    
    /**
     * Verifica se lo stato è automatico (non manuale)
     * @return true se lo stato è automatico
     */
    public boolean isAutomatico() {
        return this == VENDUTO || this == SCADUTO;
    }
}