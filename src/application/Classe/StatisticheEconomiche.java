package application.Classe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticheEconomiche {
    private BigDecimal valoreMin;
    private BigDecimal valoreMax;
    private BigDecimal valoreMedio;
    private BigDecimal totaleVendite;
    private int numeroTransazioni;
    private LocalDate periodoInizio;
    private LocalDate periodoFine;

    // Wrapper class per differenziare i costruttori
    public static class FloatValues {
        public final List<Float> valori;
        public FloatValues(List<Float> valori) {
            this.valori = valori;
        }
    }

    public static class BigDecimalValues {
        public final List<BigDecimal> valori;
        public BigDecimalValues(List<BigDecimal> valori) {
            this.valori = valori;
        }
    }

    // Costruttore per FloatValues
    public StatisticheEconomiche(FloatValues floatValues, LocalDate periodoInizio, LocalDate periodoFine) {
        this(convertiFloatABigDecimal(floatValues.valori), periodoInizio, periodoFine);
    }

    // Costruttore per BigDecimalValues
    public StatisticheEconomiche(BigDecimalValues bigDecimalValues, LocalDate periodoInizio, LocalDate periodoFine) {
        this(bigDecimalValues.valori, periodoInizio, periodoFine);
    }

    // Costruttore privato principale
    private StatisticheEconomiche(List<BigDecimal> valori, LocalDate periodoInizio, LocalDate periodoFine) {
        if (valori == null || valori.isEmpty()) {
            this.valoreMin = BigDecimal.ZERO;
            this.valoreMax = BigDecimal.ZERO;
            this.valoreMedio = BigDecimal.ZERO;
            this.totaleVendite = BigDecimal.ZERO;
            this.numeroTransazioni = 0;
        } else {
            this.valoreMin = valori.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            this.valoreMax = valori.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            this.totaleVendite = valori.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            this.numeroTransazioni = valori.size();
            
            if (numeroTransazioni > 0) {
                this.valoreMedio = totaleVendite.divide(BigDecimal.valueOf(numeroTransazioni), 2, BigDecimal.ROUND_HALF_UP);
            } else {
                this.valoreMedio = BigDecimal.ZERO;
            }
        }
        this.periodoInizio = periodoInizio;
        this.periodoFine = periodoFine;
    }

    private static List<BigDecimal> convertiFloatABigDecimal(List<Float> valoriFloat) {
        return valoriFloat.stream()
                .map(BigDecimal::valueOf)
                .collect(Collectors.toList());
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
                        new BigDecimalValues(
                            list.stream()
                                .map(Transazione::getImporto)
                                .collect(Collectors.toList())
                        ),
                        inizio, fine
                    )
                )
            ));
    }

    // GETTER (stessi della soluzione 1)
    public BigDecimal getValoreMin() { return valoreMin; }
    public BigDecimal getValoreMax() { return valoreMax; }
    public BigDecimal getValoreMedio() { return valoreMedio; }
    public BigDecimal getTotaleVendite() { return totaleVendite; }
    public int getNumeroTransazioni() { return numeroTransazioni; }
    public LocalDate getPeriodoInizio() { return periodoInizio; }
    public LocalDate getPeriodoFine() { return periodoFine; }

    public float getValoreMinFloat() { return valoreMin.floatValue(); }
    public float getValoreMaxFloat() { return valoreMax.floatValue(); }
    public float getValoreMedioFloat() { return valoreMedio.floatValue(); }
    public float getTotaleVenditeFloat() { return totaleVendite.floatValue(); }

    @Override
    public String toString() {
        return String.format("Statistiche [%s - %s]: Min: €%.2f, Max: €%.2f, Media: €%.2f, Totale: €%.2f, Transazioni: %d",
                periodoInizio, periodoFine, valoreMin, valoreMax, valoreMedio, totaleVendite, numeroTransazioni);
    }

 // Metodo factory per BigDecimal
    public static StatisticheEconomiche daBigDecimal(List<BigDecimal> valori, LocalDate periodoInizio, LocalDate periodoFine) {
        return new StatisticheEconomiche(valori, periodoInizio, periodoFine);
    }
}