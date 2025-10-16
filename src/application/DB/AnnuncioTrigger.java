package application.DB;

import application.Classe.Annuncio;
import java.util.List;

/**
 * Classe che contiene tutti i trigger per la gestione degli annunci
 * I trigger si attivano automaticamente durante le operazioni di filtraggio
 */
public class AnnuncioTrigger {
    
    // ========== TRIGGER DI LOGGING ==========
    
    /**
     * Trigger per il logging delle operazioni di filtraggio
     */
    public static class LoggingTrigger implements FilterManager.FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Logging silenzioso - nessun output
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Logging silenzioso - nessun output
        }
    }
    
    // ========== TRIGGER DI VALIDAZIONE ==========
    
    /**
     * Trigger per la validazione dei dati degli annunci
     */
    public static class ValidazioneTrigger implements FilterManager.FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            int annunciInvalidi = 0;
            
            for (Annuncio annuncio : annunci) {
                // Validazione prezzo
                if (annuncio.getPrezzo() < 0) {
                    annunciInvalidi++;
                }
                
                // Validazione oggetto null
                if (annuncio.getOggetto() == null) {
                    annunciInvalidi++;
                }
            }
            
            if (annunciInvalidi > 0) {
                annunci.removeIf(annuncio -> 
                    annuncio.getPrezzo() < 0 || annuncio.getOggetto() == null
                );
            }
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Nessuna azione dopo il filtraggio
        }
    }
    
    // ========== TRIGGER DI STATISTICHE ==========
    
    /**
     * Trigger per la generazione di statistiche
     */
    public static class StatisticheTrigger implements FilterManager.FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Calcola statistiche prima del filtraggio (silenzioso)
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Calcola statistiche dopo il filtraggio (silenzioso)
        }
    }
    
    // ========== TRIGGER DI OFFERTE SPECIALI ==========
    
    /**
     * Trigger per gestire le offerte speciali
     */
    public static class OfferteSpecialiTrigger implements FilterManager.FilterTrigger {
        private static final double PREZZO_OFFERTA_SPECIALE = 10.0;
        private static final double PREZZO_SUPER_OFFERTA = 5.0;
        
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Marca gli annunci in base al prezzo
            for (Annuncio annuncio : annunci) {
                if (annuncio.getPrezzo() <= PREZZO_SUPER_OFFERTA) {
                    annuncio.setInEvidenza(true);
                    annuncio.setTitolo("[SUPER OFFERTA] " + annuncio.getTitolo());
                } else if (annuncio.getPrezzo() <= PREZZO_OFFERTA_SPECIALE) {
                    annuncio.setInEvidenza(true);
                    annuncio.setTitolo("[OFFERTA] " + annuncio.getTitolo());
                }
            }
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Nessun output per le offerte speciali
        }
    }
    
    // ========== TRIGGER DI SICUREZZA ==========
    
    /**
     * Trigger per controlli di sicurezza
     */
    public static class SicurezzaTrigger implements FilterManager.FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Controlla annunci con prezzi sospetti (silenzioso)
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Verifica che non ci siano annunci duplicati (silenzioso)
        }
    }
    
    // ========== METODI DI SUPPORTO ==========
    
    /**
     * Metodo per loggare le operazioni (silenzioso)
     */
    private static void logOperazione(String tipo, String messaggio, int quantita) {
        // Logging silenzioso - nessun output
    }
    
    /**
     * Registra tutti i trigger predefiniti
     */
    public static void registraTuttiITrigger() {
        FilterManager.registraTrigger(new LoggingTrigger());
        FilterManager.registraTrigger(new ValidazioneTrigger());
        FilterManager.registraTrigger(new StatisticheTrigger());
        FilterManager.registraTrigger(new OfferteSpecialiTrigger());
        FilterManager.registraTrigger(new SicurezzaTrigger());
        
        // Registrazione silenziosa - nessun output
    }
    
    /**
     * Rimuovi tutti i trigger
     */
    public static void rimuoviTuttiITrigger() {
        FilterManager.registraTrigger(new FilterManager.FilterTrigger() {
            public void beforeFilter(List<Annuncio> annunci) {}
            public void afterFilter(List<Annuncio> annunciFiltrati) {}
        });
        
        // Rimozione silenziosa - nessun output
    }
}