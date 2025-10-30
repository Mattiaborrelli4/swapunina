package application.Classe;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestisce le statistiche economiche per transazioni e vendite
 * Fornisce calcoli per min, max, medio, totale e statistiche per categoria
 */
public class StatisticheEconomiche {
    private final BigDecimal valoreMin;
    private final BigDecimal valoreMax;
    private final BigDecimal valoreMedio;
    private final BigDecimal totaleVendite;
    private final int numeroTransazioni;
    private final LocalDate periodoInizio;
    private final LocalDate periodoFine;

    /**
     * Costruttore principale per lista di BigDecimal
     */
    public StatisticheEconomiche(List<BigDecimal> valori, LocalDate periodoInizio, LocalDate periodoFine) {
        this.periodoInizio = periodoInizio;
        this.periodoFine = periodoFine;
        
        if (valori == null || valori.isEmpty()) {
            this.valoreMin = BigDecimal.ZERO;
            this.valoreMax = BigDecimal.ZERO;
            this.valoreMedio = BigDecimal.ZERO;
            this.totaleVendite = BigDecimal.ZERO;
            this.numeroTransazioni = 0;
        } else {
            this.valoreMin = valori.stream()
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            this.valoreMax = valori.stream()
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            this.totaleVendite = valori.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            this.numeroTransazioni = valori.size();
            
            this.valoreMedio = numeroTransazioni > 0 ? 
                    totaleVendite.divide(BigDecimal.valueOf(numeroTransazioni), 2, RoundingMode.HALF_UP) : 
                    BigDecimal.ZERO;
        }
    }

    /**
     * Factory method per creare statistiche da lista di Float
     */
    public static StatisticheEconomiche daFloat(List<Float> valori, LocalDate periodoInizio, LocalDate periodoFine) {
        if (valori == null || valori.isEmpty()) {
            return new StatisticheEconomiche(List.of(), periodoInizio, periodoFine);
        }
        
        List<BigDecimal> valoriDecimali = valori.stream()
                .map(BigDecimal::valueOf)
                .collect(Collectors.toList());
        
        return new StatisticheEconomiche(valoriDecimali, periodoInizio, periodoFine);
    }

    /**
     * Factory method per creare statistiche da lista di BigDecimal
     */
    public static StatisticheEconomiche daBigDecimal(List<BigDecimal> valori, LocalDate periodoInizio, LocalDate periodoFine) {
        return new StatisticheEconomiche(valori, periodoInizio, periodoFine);
    }

    /**
     * Factory method per statistiche vuote
     */
    public static StatisticheEconomiche statisticheVuote(LocalDate periodoInizio, LocalDate periodoFine) {
        return new StatisticheEconomiche(List.of(), periodoInizio, periodoFine);
    }

    /**
     * Calcola statistiche aggregate per categoria
     */
    public static Map<String, StatisticheEconomiche> statistichePerCategoria(
            List<Transazione> transazioni, LocalDate inizio, LocalDate fine) {
        
        return transazioni.stream()
            .filter(t -> !t.getData().toLocalDate().isBefore(inizio) && 
                        !t.getData().toLocalDate().isAfter(fine))
            .collect(Collectors.groupingBy(
                Transazione::getCategoria,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> daBigDecimal(
                        list.stream()
                            .map(Transazione::getImporto)
                            .collect(Collectors.toList()),
                        inizio, fine
                    )
                )
            ));
    }

    /**
     * Calcola statistiche per venditore
     */
    public static Map<Integer, StatisticheEconomiche> statistichePerVenditore(
            List<Transazione> transazioni, LocalDate inizio, LocalDate fine) {
        
        return transazioni.stream()
            .filter(t -> !t.getData().toLocalDate().isBefore(inizio) && 
                        !t.getData().toLocalDate().isAfter(fine))
            .collect(Collectors.groupingBy(
                Transazione::getVenditoreId,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> daBigDecimal(
                        list.stream()
                            .map(Transazione::getImporto)
                            .collect(Collectors.toList()),
                        inizio, fine
                    )
                )
            ));
    }

    /**
     * Verifica se ci sono dati statistici disponibili
     */
    public boolean hasDati() {
        return numeroTransazioni > 0;
    }

    /**
     * Restituisce il valore medio formattato in euro
     */
    public String getValoreMedioFormattato() {
        return "€" + String.format("%.2f", valoreMedio);
    }

    /**
     * Restituisce il totale formattato in euro
     */
    public String getTotaleFormattato() {
        return "€" + String.format("%.2f", totaleVendite);
    }

    /**
     * Restituisce il range di valori formattato
     */
    public String getRangeFormattato() {
        return "€" + String.format("%.2f", valoreMin) + " - €" + String.format("%.2f", valoreMax);
    }

    /**
     * Restituisce il periodo formattato
     */
    public String getPeriodoFormattato() {
        return periodoInizio + " - " + periodoFine;
    }

    // ========== GETTER ==========

    public BigDecimal getValoreMin() { 
        return valoreMin; 
    }

    public BigDecimal getValoreMax() { 
        return valoreMax; 
    }

    public BigDecimal getValoreMedio() { 
        return valoreMedio; 
    }

    public BigDecimal getTotaleVendite() { 
        return totaleVendite; 
    }

    public int getNumeroTransazioni() { 
        return numeroTransazioni; 
    }

    public LocalDate getPeriodoInizio() { 
        return periodoInizio; 
    }

    public LocalDate getPeriodoFine() { 
        return periodoFine; 
    }

    /**
     * Metodi di convenienza per compatibilità con float
     */
    public float getValoreMinFloat() { 
        return valoreMin.floatValue(); 
    }

    public float getValoreMaxFloat() { 
        return valoreMax.floatValue(); 
    }

    public float getValoreMedioFloat() { 
        return valoreMedio.floatValue(); 
    }

    public float getTotaleVenditeFloat() { 
        return totaleVendite.floatValue(); 
    }

    @Override
    public String toString() {
        return String.format(
            "Statistiche [%s - %s]: Min: €%.2f, Max: €%.2f, Media: €%.2f, Totale: €%.2f, Transazioni: %d",
            periodoInizio, 
            periodoFine, 
            valoreMin, 
            valoreMax, 
            valoreMedio, 
            totaleVendite, 
            numeroTransazioni
        );
    }
}