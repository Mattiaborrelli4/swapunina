package application.Classe;

import application.Enum.Tipologia;
import java.time.LocalDate;
import java.util.*;

public class Report {
    private int totaleOfferte;
    private Map<Tipologia, Integer> offerteAccettate;
    private StatisticheEconomiche statsVendite;
    private LocalDate dataGenerazione;

    public Report() {
        this.totaleOfferte = 0;
        this.offerteAccettate = new HashMap<>();
        this.statsVendite = null;
        this.dataGenerazione = LocalDate.now();
    }

    public void incrementaOfferte() {
        totaleOfferte++;
    }

    public void aggiungiOffertaAccettata(Tipologia tipologia) {
        offerteAccettate.put(tipologia, offerteAccettate.getOrDefault(tipologia, 0) + 1);
    }

    public void setStatistiche(StatisticheEconomiche stats) {
        this.statsVendite = stats;
    }

    public int getTotaleOfferte() {
        return totaleOfferte;
    }

    public Map<Tipologia, Integer> getOfferteAccettate() {
        return offerteAccettate;
    }

    public StatisticheEconomiche getStatsVendite() {
        return statsVendite;
    }

    public LocalDate getDataGenerazione() {
        return dataGenerazione;
    }
}