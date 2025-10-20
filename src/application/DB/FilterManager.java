package application.DB;

import application.Classe.Annuncio;
import application.Enum.Categoria;
import application.Enum.Tipologia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Gestisce il filtraggio degli annunci con sistema a trigger per estendibilità
 */
public class FilterManager {
    
    public interface FilterTrigger {
        void beforeFilter(List<Annuncio> annunci);
        void afterFilter(List<Annuncio> annunciFiltrati);
    }
    
    private static final List<FilterTrigger> triggers = new ArrayList<>();
    
    public static void registraTrigger(FilterTrigger trigger) {
        triggers.add(trigger);
    }
    
    public static void rimuoviTrigger(FilterTrigger trigger) {
        triggers.remove(trigger);
    }
    
    /**
     * Applica filtri multipli agli annunci con supporto trigger
     */
    public static List<Annuncio> applicaFiltri(List<Annuncio> annunci, 
                                              Categoria categoria, 
                                              Tipologia tipologia, 
                                              String queryRicerca, 
                                              String ordinamento) {
        
        eseguiTriggerBefore(annunci);
        
        List<Annuncio> annunciFiltrati = annunci.stream()
                .filter(filtroPerCategoria(categoria))
                .filter(filtroPerTipologia(tipologia))
                .filter(filtroPerRicerca(queryRicerca))
                .sorted(creaComparatore(ordinamento))
                .collect(Collectors.toList());
        
        eseguiTriggerAfter(annunciFiltrati);
        
        return annunciFiltrati;
    }
    
    private static void eseguiTriggerBefore(List<Annuncio> annunci) {
        triggers.forEach(trigger -> trigger.beforeFilter(annunci));
    }
    
    private static void eseguiTriggerAfter(List<Annuncio> annunciFiltrati) {
        triggers.forEach(trigger -> trigger.afterFilter(annunciFiltrati));
    }
    
    // ========== TRIGGER PREDEFINITI ==========
    
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
    
    public static class ValidazionePrezzoTrigger implements FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            annunci.removeIf(annuncio -> annuncio.getPrezzo() < 0);
        }
        
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Nessuna azione dopo il filtraggio
        }
    }
    
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
    
    public static class OfferteSpecialiTrigger implements FilterTrigger {
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
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
    
    // ========== METODI DI FILTRAGGIO OTTIMIZZATI ==========
    
    private static Predicate<Annuncio> filtroPerCategoria(Categoria categoria) {
        return annuncio -> categoria == null || 
               (annuncio.getOggetto() != null && annuncio.getOggetto().getCategoria() == categoria);
    }
    
    private static Predicate<Annuncio> filtroPerTipologia(Tipologia tipologia) {
        return annuncio -> tipologia == null || annuncio.getTipologia() == tipologia;
    }
    
    private static Predicate<Annuncio> filtroPerRicerca(String queryRicerca) {
        if (queryRicerca == null || queryRicerca.isBlank()) {
            return annuncio -> true;
        }
        
        final String searchLower = queryRicerca.toLowerCase();
        return annuncio -> {
            boolean titoloMatch = annuncio.getTitolo() != null && 
                                annuncio.getTitolo().toLowerCase().contains(searchLower);
            
            if (titoloMatch) return true;
            
            if (annuncio.getOggetto() != null) {
                boolean descrizioneMatch = annuncio.getOggetto().getDescrizione() != null &&
                                         annuncio.getOggetto().getDescrizione().toLowerCase().contains(searchLower);
                boolean nomeMatch = annuncio.getOggetto().getNome() != null &&
                                  annuncio.getOggetto().getNome().toLowerCase().contains(searchLower);
                
                return descrizioneMatch || nomeMatch;
            }
            
            return false;
        };
    }
    
    private static Comparator<Annuncio> creaComparatore(String ordinamento) {
        if (ordinamento == null) {
            ordinamento = "recent";
        }
        
        switch (ordinamento) {
            case "price_asc": 
                return Comparator.comparingDouble(Annuncio::getPrezzo);
            case "price_desc": 
                return Comparator.comparingDouble(Annuncio::getPrezzo).reversed();
            case "recent":
            default: 
                return (a, b) -> {
                    if (a.getDataPubblicazione() == null && b.getDataPubblicazione() == null) return 0;
                    if (a.getDataPubblicazione() == null) return 1;
                    if (b.getDataPubblicazione() == null) return -1;
                    return b.getDataPubblicazione().compareTo(a.getDataPubblicazione());
                };
        }
    }
    
    /**
     * Conta gli annunci che corrispondono ai filtri senza applicarli
     */
    public static long contaAnnunciFiltrati(List<Annuncio> annunci, 
                                          Categoria categoria, 
                                          Tipologia tipologia, 
                                          String queryRicerca) {
        
        return annunci.stream()
                .filter(filtroPerCategoria(categoria))
                .filter(filtroPerTipologia(tipologia))
                .filter(filtroPerRicerca(queryRicerca))
                .count();
    }
    
    /**
     * Versione alternativa che restituisce int per compatibilità
     */
    public static int contaAnnunciFiltratiInt(List<Annuncio> annunci, 
                                            Categoria categoria, 
                                            Tipologia tipologia, 
                                            String queryRicerca) {
        
        long countLong = contaAnnunciFiltrati(annunci, categoria, tipologia, queryRicerca);
        
        // Conversione sicura da long a int
        if (countLong > Integer.MAX_VALUE) {
            System.err.println("Warning: numero di annunci eccede Integer.MAX_VALUE");
            return Integer.MAX_VALUE;
        }
        return (int) countLong;
    }
    
    /**
     * Applica filtro rapido solo per testo (più veloce per ricerche semplici)
     */
    public static List<Annuncio> filtroRapidoTesto(List<Annuncio> annunci, String queryRicerca) {
        if (queryRicerca == null || queryRicerca.isBlank()) {
            return new ArrayList<>(annunci);
        }
        
        final String searchLower = queryRicerca.toLowerCase();
        return annunci.stream()
                .filter(annuncio -> annuncio.getTitolo() != null && 
                                  annuncio.getTitolo().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
    }
    
    /**
     * Filtraggio batch per multiple categorie
     */
    public static List<Annuncio> filtraPerCategorie(List<Annuncio> annunci, List<Categoria> categorie) {
        if (categorie == null || categorie.isEmpty()) {
            return new ArrayList<>(annunci);
        }
        
        return annunci.stream()
                .filter(annuncio -> annuncio.getOggetto() != null && 
                                  categorie.contains(annuncio.getOggetto().getCategoria()))
                .collect(Collectors.toList());
    }
    
    // Inizializzazione trigger predefiniti
    static {
        registraTrigger(new LoggingTrigger());
        registraTrigger(new ValidazionePrezzoTrigger());
        registraTrigger(new StatisticheTrigger());
        registraTrigger(new OfferteSpecialiTrigger());
    }
}