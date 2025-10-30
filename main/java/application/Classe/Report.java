package application.Classe;

import application.Enum.Tipologia;
import java.time.LocalDate;
import java.util.*;

/**
 * Rappresenta un report delle attività di vendita e offerte
 * Fornisce statistiche aggregate su offerte, vendite e performance
 */
public class Report {
    private int totaleOfferte;
    private final Map<Tipologia, Integer> offerteAccettate;
    private StatisticheEconomiche statsVendite;
    private final LocalDate dataGenerazione;
    private int totaleAnnunci;
    private int annunciAttivi;
    private final Map<String, Integer> venditePerCategoria;

    public Report() {
        this.totaleOfferte = 0;
        this.offerteAccettate = new EnumMap<>(Tipologia.class);
        this.statsVendite = StatisticheEconomiche.statisticheVuote(LocalDate.now(), LocalDate.now());
        this.dataGenerazione = LocalDate.now();
        this.totaleAnnunci = 0;
        this.annunciAttivi = 0;
        this.venditePerCategoria = new HashMap<>();
    }

    /**
     * Incrementa il contatore delle offerte totali
     */
    public void incrementaOfferte() {
        totaleOfferte++;
    }

    /**
     * Aggiunge un'offerta accettata per una specifica tipologia
     */
    public void aggiungiOffertaAccettata(Tipologia tipologia) {
        offerteAccettate.put(tipologia, offerteAccettate.getOrDefault(tipologia, 0) + 1);
    }

    /**
     * Imposta le statistiche economiche delle vendite
     */
    public void setStatistiche(StatisticheEconomiche stats) {
        this.statsVendite = stats != null ? stats : 
            StatisticheEconomiche.statisticheVuote(LocalDate.now(), LocalDate.now());
    }

    /**
     * Imposta il conteggio degli annunci
     */
    public void setConteggioAnnunci(int totale, int attivi) {
        this.totaleAnnunci = Math.max(0, totale);
        this.annunciAttivi = Math.max(0, attivi);
    }

    /**
     * Aggiunge vendite per una categoria specifica
     */
    public void aggiungiVenditeCategoria(String categoria, int quantita) {
        if (categoria != null && !categoria.trim().isEmpty() && quantita > 0) {
            venditePerCategoria.put(categoria, venditePerCategoria.getOrDefault(categoria, 0) + quantita);
        }
    }

    /**
     * Calcola il tasso di successo delle offerte
     */
    public double getTassoSuccessoOfferte() {
        if (totaleOfferte == 0) return 0.0;
        int offerteAccettateTotali = getTotaleOfferteAccettate();
        return (double) offerteAccettateTotali / totaleOfferte * 100;
    }

    /**
     * Restituisce il numero totale di offerte accettate
     */
    public int getTotaleOfferteAccettate() {
        return offerteAccettate.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Restituisce la tipologia più popolare
     */
    public Tipologia getTipologiaPiuPopolare() {
        return offerteAccettate.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Restituisce il tasso di annunci attivi
     */
    public double getTassoAnnunciAttivi() {
        if (totaleAnnunci == 0) return 0.0;
        return (double) annunciAttivi / totaleAnnunci * 100;
    }

    /**
     * Restituisce la categoria con più vendite
     */
    public String getCategoriaPiuVenduta() {
        return venditePerCategoria.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Nessuna");
    }

    /**
     * Verifica se ci sono dati nel report
     */
    public boolean hasDati() {
        return totaleOfferte > 0 || 
               getTotaleOfferteAccettate() > 0 || 
               statsVendite.hasDati() ||
               totaleAnnunci > 0;
    }

    /**
     * Restituisce un riepilogo formattato del report
     */
    public String getRiepilogo() {
        return String.format(
            "Report del %s:\n" +
            "- Offerte totali: %d\n" +
            "- Offerte accettate: %d (%.1f%%)\n" +
            "- Annunci attivi: %d/%d (%.1f%%)\n" +
            "- Vendite totali: %s\n" +
            "- Categoria più venduta: %s",
            dataGenerazione,
            totaleOfferte,
            getTotaleOfferteAccettate(),
            getTassoSuccessoOfferte(),
            annunciAttivi,
            totaleAnnunci,
            getTassoAnnunciAttivi(),
            statsVendite.getTotaleFormattato(),
            getCategoriaPiuVenduta()
        );
    }

    // ========== GETTER ==========

    public int getTotaleOfferte() {
        return totaleOfferte;
    }

    public Map<Tipologia, Integer> getOfferteAccettate() {
        return new EnumMap<>(offerteAccettate);
    }

    public StatisticheEconomiche getStatsVendite() {
        return statsVendite;
    }

    public LocalDate getDataGenerazione() {
        return dataGenerazione;
    }

    public int getTotaleAnnunci() {
        return totaleAnnunci;
    }

    public int getAnnunciAttivi() {
        return annunciAttivi;
    }

    public Map<String, Integer> getVenditePerCategoria() {
        return new HashMap<>(venditePerCategoria);
    }

    /**
     * Restituisce le statistiche per una specifica tipologia
     */
    public int getOfferteAccettatePerTipologia(Tipologia tipologia) {
        return offerteAccettate.getOrDefault(tipologia, 0);
    }

    @Override
    public String toString() {
        return getRiepilogo();
    }
}