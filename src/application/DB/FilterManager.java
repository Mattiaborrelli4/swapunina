package application.DB;

import application.Classe.Annuncio;
import application.Enum.Categoria;
import application.Enum.Tipologia;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilterManager {
    
    // Interfaccia per i trigger
    public interface FilterTrigger {
        void beforeFilter(List<Annuncio> annunci);
        void afterFilter(List<Annuncio> annunciFiltrati);
    }
    
    // Lista di trigger registrati
    private static List<FilterTrigger> triggers = new java.util.ArrayList<>();
    
    // Registra un nuovo trigger
    public static void registraTrigger(FilterTrigger trigger) {
        triggers.add(trigger);
    }
    
    // Rimuovi un trigger
    public static void rimuoviTrigger(FilterTrigger trigger) {
        triggers.remove(trigger);
    }
    
    public static List<Annuncio> applicaFiltri(List<Annuncio> annunci, 
                                              Categoria categoria, 
                                              Tipologia tipologia, 
                                              String queryRicerca, 
                                              String ordinamento) {
        
        // Esegui trigger prima del filtraggio
        eseguiTriggerBefore(annunci);
        
        List<Annuncio> annunciFiltrati = annunci.stream()
                .filter(filtroPerCategoria(categoria))
                .filter(filtroPerTipologia(tipologia))
                .filter(filtroPerRicerca(queryRicerca))
                .sorted((a, b) -> ordinaAnnunci(a, b, ordinamento))
                .collect(Collectors.toList());
        
        // Esegui trigger dopo il filtraggio
        eseguiTriggerAfter(annunciFiltrati);
        
        return annunciFiltrati;
    }
    
    // Esegui tutti i trigger "before"
    private static void eseguiTriggerBefore(List<Annuncio> annunci) {
        for (FilterTrigger trigger : triggers) {
            trigger.beforeFilter(annunci);
        }
    }
    
    // Esegui tutti i trigger "after"
    private static void eseguiTriggerAfter(List<Annuncio> annunciFiltrati) {
        for (FilterTrigger trigger : triggers) {
            trigger.afterFilter(annunciFiltrati);
        }
    }
    
    // ========== TRIGGER PREDEFINITI ==========
    
    // 1. Trigger per il logging
    public static class LoggingTrigger implements FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            System.out.println("[TRIGGER] Inizio filtraggio su " + annunci.size() + " annunci");
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            System.out.println("[TRIGGER] Filtraggio completato. " + 
                             annunciFiltrati.size() + " annunci filtrati");
        }
    }
    
    // 2. Trigger per validazione prezzi
    public static class ValidazionePrezzoTrigger implements FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Rimuovi annunci con prezzi non validi
            annunci.removeIf(annuncio -> annuncio.getPrezzo() < 0);
            System.out.println("[TRIGGER] Validazione prezzi completata");
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Nessuna azione dopo il filtraggio
        }
    }
    
    // 3. Trigger per statistiche
    public static class StatisticheTrigger implements FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Nessuna azione prima del filtraggio
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            if (!annunciFiltrati.isEmpty()) {
                double prezzoMedio = annunciFiltrati.stream()
                        .mapToDouble(Annuncio::getPrezzo)
                        .average()
                        .orElse(0);
                
                System.out.println("[TRIGGER] Statistiche: " +
                                 annunciFiltrati.size() + " annunci, " +
                                 "Prezzo medio: €" + String.format("%.2f", prezzoMedio));
            }
        }
    }
    
    // 4. Trigger per evidenziare offerte speciali
    public static class OfferteSpecialiTrigger implements FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Marca gli annunci in evidenza
            annunci.forEach(annuncio -> {
                if (annuncio.getPrezzo() < 10) {
                    annuncio.setInEvidenza(true);
                }
            });
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            long offerteSpeciali = annunciFiltrati.stream()
                    .filter(Annuncio::isInEvidenza)
                    .count();
            
            if (offerteSpeciali > 0) {
                System.out.println("[TRIGGER] " + offerteSpeciali + " offerte speciali trovate!");
            }
        }
    }
    
    // ========== METODI DI FILTRAGGIO (rimangono invariati) ==========
    
    private static Predicate<Annuncio> filtroPerCategoria(Categoria categoria) {
        return annuncio -> categoria == null || 
               (annuncio.getOggetto() != null && annuncio.getOggetto().getCategoria() == categoria);
    }
    
    private static Predicate<Annuncio> filtroPerTipologia(Tipologia tipologia) {
        return annuncio -> tipologia == null || annuncio.getTipologia() == tipologia;
    }
    
    private static Predicate<Annuncio> filtroPerRicerca(String queryRicerca) {
        return annuncio -> {
            if (queryRicerca == null || queryRicerca.isBlank()) {
                return true;
            }
            
            String searchLower = queryRicerca.toLowerCase();
            
            return (annuncio.getTitolo() != null && annuncio.getTitolo().toLowerCase().contains(searchLower)) ||
                   (annuncio.getOggetto() != null && annuncio.getOggetto().getDescrizione() != null &&
                    annuncio.getOggetto().getDescrizione().toLowerCase().contains(searchLower)) ||
                   (annuncio.getOggetto() != null && annuncio.getOggetto().getNome() != null &&
                    annuncio.getOggetto().getNome().toLowerCase().contains(searchLower));
        };
    }
    
    private static int ordinaAnnunci(Annuncio a, Annuncio b, String ordinamento) {
        if (ordinamento == null) {
            ordinamento = "recent";
        }
        
        switch (ordinamento) {
            case "price_asc": 
                return Double.compare(a.getPrezzo(), b.getPrezzo());
            case "price_desc": 
                return Double.compare(b.getPrezzo(), a.getPrezzo());
            case "recent":
            default: 
                if (a.getDataPubblicazione() == null && b.getDataPubblicazione() == null) {
                    return 0;
                } else if (a.getDataPubblicazione() == null) {
                    return 1;
                } else if (b.getDataPubblicazione() == null) {
                    return -1;
                }
                return b.getDataPubblicazione().compareTo(a.getDataPubblicazione());
        }
    }
    
    public static int contaAnnunciFiltrati(List<Annuncio> annunci, 
                                          Categoria categoria, 
                                          Tipologia tipologia, 
                                          String queryRicerca) {
        
        return (int) annunci.stream()
                .filter(filtroPerCategoria(categoria))
                .filter(filtroPerTipologia(tipologia))
                .filter(filtroPerRicerca(queryRicerca))
                .count();
    }
    
    // ========== INIZIALIZZAZIONE DEI TRIGGER ==========
    
    static {
        // Registra i trigger predefiniti all'avvio
        registraTrigger(new LoggingTrigger());
        registraTrigger(new ValidazionePrezzoTrigger());
        registraTrigger(new StatisticheTrigger());
        registraTrigger(new OfferteSpecialiTrigger());
        
        System.out.println("[TRIGGER] " + triggers.size() + " trigger registrati");
    }
}