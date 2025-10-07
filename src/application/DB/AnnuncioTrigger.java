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
            System.out.println("🔍 [TRIGGER] Inizio filtraggio: " + annunci.size() + " annunci da processare");
            logOperazione("FILTRO_INIZIO", "Inizio processo di filtraggio", annunci.size());
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            System.out.println("✅ [TRIGGER] Filtraggio completato: " + 
                             annunciFiltrati.size() + " annunci filtrati");
            logOperazione("FILTRO_FINE", "Filtraggio completato", annunciFiltrati.size());
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
                    System.out.println("⚠️ [TRIGGER] Annuncio " + annuncio.getId() + 
                                     " ha prezzo negativo: €" + annuncio.getPrezzo());
                    annunciInvalidi++;
                }
                
                // Validazione oggetto null
                if (annuncio.getOggetto() == null) {
                    System.out.println("⚠️ [TRIGGER] Annuncio " + annuncio.getId() + 
                                     " non ha oggetto associato");
                    annunciInvalidi++;
                }
            }
            
            if (annunciInvalidi > 0) {
                System.out.println("🚫 [TRIGGER] Rimossi " + annunciInvalidi + " annunci non validi");
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
            // Calcola statistiche prima del filtraggio
            if (!annunci.isEmpty()) {
                double prezzoMedioOriginale = annunci.stream()
                        .mapToDouble(Annuncio::getPrezzo)
                        .average()
                        .orElse(0);
                
                System.out.println("📊 [TRIGGER] Statistiche iniziali: " +
                                 annunci.size() + " annunci, " +
                                 "Prezzo medio: €" + String.format("%.2f", prezzoMedioOriginale));
            }
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Calcola statistiche dopo il filtraggio
            if (!annunciFiltrati.isEmpty()) {
                double prezzoMedio = annunciFiltrati.stream()
                        .mapToDouble(Annuncio::getPrezzo)
                        .average()
                        .orElse(0);
                
                double prezzoMin = annunciFiltrati.stream()
                        .mapToDouble(Annuncio::getPrezzo)
                        .min()
                        .orElse(0);
                
                double prezzoMax = annunciFiltrati.stream()
                        .mapToDouble(Annuncio::getPrezzo)
                        .max()
                        .orElse(0);
                
                System.out.println("📈 [TRIGGER] Statistiche finali: " +
                                 annunciFiltrati.size() + " annunci filtrati, " +
                                 "Prezzo medio: €" + String.format("%.2f", prezzoMedio) +
                                 ", Min: €" + String.format("%.2f", prezzoMin) +
                                 ", Max: €" + String.format("%.2f", prezzoMax));
            }
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
            long superOfferte = annunciFiltrati.stream()
                    .filter(a -> a.getPrezzo() <= PREZZO_SUPER_OFFERTA)
                    .count();
            
            long offerteSpeciali = annunciFiltrati.stream()
                    .filter(a -> a.getPrezzo() > PREZZO_SUPER_OFFERTA && 
                                a.getPrezzo() <= PREZZO_OFFERTA_SPECIALE)
                    .count();
            
            if (superOfferte > 0 || offerteSpeciali > 0) {
                System.out.println("🎯 [TRIGGER] Offerte speciali trovate: " +
                                 superOfferte + " SUPER offerte, " +
                                 offerteSpeciali + " offerte speciali");
            }
        }
    }
    
    // ========== TRIGGER DI SICUREZZA ==========
    
    /**
     * Trigger per controlli di sicurezza
     */
    public static class SicurezzaTrigger implements FilterManager.FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Controlla annunci con prezzi sospetti (troppo alti o troppo bassi)
            long annunciSospetti = annunci.stream()
                    .filter(a -> a.getPrezzo() > 10000 || 
                                (a.getPrezzo() < 1 && a.getPrezzo() > 0))
                    .count();
            
            if (annunciSospetti > 0) {
                System.out.println("🔒 [TRIGGER] Trovati " + annunciSospetti + 
                                 " annunci con prezzi sospetti");
            }
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Verifica che non ci siano annunci duplicati
            long annunciUnici = annunciFiltrati.stream()
                    .map(Annuncio::getId)
                    .distinct()
                    .count();
            
            if (annunciUnici != annunciFiltrati.size()) {
                System.out.println("⚠️ [TRIGGER] Attenzione: " + 
                                 (annunciFiltrati.size() - annunciUnici) + 
                                 " annunci duplicati trovati");
            }
        }
    }
    
    // ========== METODI DI SUPPORTO ==========
    
    /**
     * Metodo per loggare le operazioni (simulato)
     */
    private static void logOperazione(String tipo, String messaggio, int quantita) {
        // In una versione reale, qui salveremmo su database o file
        System.out.println("📝 [LOG] " + tipo + " - " + messaggio + " - Quantità: " + quantita);
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
        
        System.out.println("🚀 Registrati 5 trigger per la gestione annunci:");
        System.out.println("   1. 📋 LoggingTrigger - Traccia tutte le operazioni");
        System.out.println("   2. ✅ ValidazioneTrigger - Verifica dati annunci");
        System.out.println("   3. 📊 StatisticheTrigger - Genera statistiche");
        System.out.println("   4. 🎯 OfferteSpecialiTrigger - Gestisce offerte");
        System.out.println("   5. 🔒 SicurezzaTrigger - Controlli sicurezza");
    }
    
    /**
     * Rimuovi tutti i trigger
     */
    public static void rimuoviTuttiITrigger() {
        FilterManager.registraTrigger(new FilterManager.FilterTrigger() {
            public void beforeFilter(List<Annuncio> annunci) {}
            public void afterFilter(List<Annuncio> annunciFiltrati) {}
        });
        
        System.out.println("🗑️ Tutti i trigger sono stati rimossi");
    }
}