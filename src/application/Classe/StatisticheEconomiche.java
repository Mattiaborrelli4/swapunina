package application.Classe;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticheEconomiche {
    private float valoreMin;
    private float valoreMax;
    private float valoreMedio;
    private float totaleVendite;
    private int numeroTransazioni;
    private LocalDate periodoInizio;
    private LocalDate periodoFine;

    public StatisticheEconomiche(List<Float> valori, LocalDate periodoInizio, LocalDate periodoFine) {
        if (valori == null || valori.isEmpty()) {
            this.valoreMin = 0;
            this.valoreMax = 0;
            this.valoreMedio = 0;
            this.totaleVendite = 0;
            this.numeroTransazioni = 0;
        } else {
            this.valoreMin = valori.stream().min(Float::compare).orElse(0f);
            this.valoreMax = valori.stream().max(Float::compare).orElse(0f);
            this.valoreMedio = (float) valori.stream().mapToDouble(Float::doubleValue).average().orElse(0);
            this.totaleVendite = (float) valori.stream().mapToDouble(Float::doubleValue).sum();
            this.numeroTransazioni = valori.size();
        }
        this.periodoInizio = periodoInizio;
        this.periodoFine = periodoFine;
    }

    // Metodo per statistiche per categoria
    public static Map<String, StatisticheEconomiche> statistichePerCategoria(
            List<Transazione> transazioni, LocalDate inizio, LocalDate fine) {
        
        return transazioni.stream()
            .filter(t -> !t.getData().toLocalDate().isBefore(inizio) && 
                        !t.getData().toLocalDate().isAfter(fine))
            .collect(Collectors.groupingBy(
                Transazione::getCategoria,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> new StatisticheEconomiche(
                        list.stream().map(Transazione::getImporto).collect(Collectors.toList()),
                        inizio, fine
                    )
                )
            ));
    }

    // GETTER
    public float getValoreMin() { return valoreMin; }
    public float getValoreMax() { return valoreMax; }
    public float getValoreMedio() { return valoreMedio; }
    public float getTotaleVendite() { return totaleVendite; }
    public int getNumeroTransazioni() { return numeroTransazioni; }
    public LocalDate getPeriodoInizio() { return periodoInizio; }
    public LocalDate getPeriodoFine() { return periodoFine; }

    @Override
    public String toString() {
        return String.format("Statistiche [%s - %s]: Min: €%.2f, Max: €%.2f, Media: €%.2f, Totale: €%.2f, Transazioni: %d",
                periodoInizio, periodoFine, valoreMin, valoreMax, valoreMedio, totaleVendite, numeroTransazioni);
    }
}