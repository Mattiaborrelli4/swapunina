package application.DB;

import application.Classe.Annuncio;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Sistema di trigger per la gestione automatica degli annunci
 * 
 * <p>I trigger si attivano durante le operazioni di filtraggio per applicare
 * regole di business, validazione e ottimizzazioni in modo automatico e trasparente.</p>
 * 
 * <p><b>Caratteristiche principali:</b>
 * <ul>
 *   <li>Logging dettagliato delle operazioni</li>
 *   <li>Validazione automatica dei dati</li>
 *   <li>Raccolta statistiche in tempo reale</li>
 *   <li>Gestione offerte speciali</li>
 *   <li>Controlli di sicurezza e anomalie</li>
 *   <li>Ottimizzazioni delle performance</li>
 * </ul>
 * </p>
 */
public class AnnuncioTrigger {
    private static final Logger LOGGER = Logger.getLogger(AnnuncioTrigger.class.getName());
    
    // ========== COSTANTI DI CONFIGURAZIONE ==========
    
    /** Soglia di prezzo per le super offerte */
    private static final double PREZZO_SUPER_OFFERTA = 5.0;
    
    /** Soglia di prezzo per le offerte speciali */
    private static final double PREZZO_OFFERTA_SPECIALE = 10.0;
    
    /** Soglia di prezzo per rilevamento anomalie (prezzi sospettosamente alti) */
    private static final double PREZZO_SOSPETTO = 10000.0;
    
    /** Dimensione massima della cache per prevenire memory leak */
    private static final int MAX_CACHE_SIZE = 1000;
    
    /** Soglia per log dettagliato (batch grandi) */
    private static final int SOGLIA_BATCH_GRANDE = 100;
    
    /** Soglia per log performance (operazioni lente in ms) */
    private static final int SOGLIA_PERFORMANCE_LENTA = 100;
    
    // Cache per statistiche e metriche di performance
    private static final ConcurrentHashMap<String, AtomicInteger> STATISTICHE_GLOBALI = new ConcurrentHashMap<>();
    
    // ========== TRIGGER DI LOGGING AVANZATO ==========
    
    /**
     * Trigger per il monitoraggio e logging delle operazioni di filtraggio
     * 
     * <p><b>Funzionalità:</b>
     * <ul>
     *   <li>Tracciamento metriche di efficienza</li>
     *   <li>Log condizionale per batch di grandi dimensioni</li>
     *   <li>Aggregazione statistiche globali</li>
     *   <li>Monitoraggio performance in tempo reale</li>
     * </ul>
     * </p>
     */
    public static class TriggerLogging implements FilterManager.FilterTrigger {
        private final AtomicInteger contatoreTotaleProcessati = new AtomicInteger(0);
        private final AtomicInteger dimensioneBatchCorrente = new AtomicInteger(0);
        
        /**
         * Metodo eseguito prima dell'operazione di filtraggio
         * 
         * @param annunci Lista degli annunci da filtrare
         */
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            dimensioneBatchCorrente.set(annunci.size());
            contatoreTotaleProcessati.addAndGet(annunci.size());
            
            // Log dettagliato solo per batch di grandi dimensioni (ottimizzazione performance)
            if (annunci.size() > SOGLIA_BATCH_GRANDE) {
                LOGGER.log(Level.FINE, 
                    "🔄 Inizio filtraggio di {0} annunci - Totale processati: {1}", 
                    new Object[]{annunci.size(), contatoreTotaleProcessati.get()}
                );
            }
        }
        
        /**
         * Metodo eseguito dopo l'operazione di filtraggio
         * 
         * @param annunciFiltrati Lista degli annunci dopo il filtraggio
         */
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            int conteggioFiltrati = annunciFiltrati.size();
            int conteggioOriginale = dimensioneBatchCorrente.get();
            
            // Calcolo metriche di efficienza del filtro
            double efficienza = conteggioOriginale > 0 ? 
                (conteggioFiltrati * 100.0) / conteggioOriginale : 0;
                
            // Log informativo solo quando c'è una riduzione significativa
            if (efficienza < 50.0 && conteggioOriginale > 10) {
                LOGGER.log(Level.INFO, 
                    "✅ Filtro applicato: {0} → {1} annunci (efficienza: {2}%)",
                    new Object[]{
                        conteggioOriginale, 
                        conteggioFiltrati, 
                        String.format("%.1f", efficienza)
                    }
                );
            }
            
            // Aggiornamento statistiche globali in modo thread-safe
            aggiornaStatisticaGlobale("annunci_processati_totali", conteggioOriginale);
            aggiornaStatisticaGlobale("annunci_filtrati_totali", conteggioFiltrati);
        }
    }
    
    // ========== TRIGGER DI VALIDAZIONE ROBUSTA ==========
    
    /**
     * Trigger per la validazione avanzata e la pulizia dei dati
     * 
     * <p><b>Controlli effettuati:</b>
     * <ul>
     *   <li>Validazione integrità dati obbligatori</li>
     *   <li>Controllo consistenza prezzi</li>
     *   <li>Verifica riferimenti oggetti</li>
     *   <li>Pulizia automatica dati non validi</li>
     * </ul>
     * </p>
     */
    public static class TriggerValidazione implements FilterManager.FilterTrigger {
        private final AtomicInteger contatoreNonValidi = new AtomicInteger(0);
        
        /**
         * Validazione e pulizia pre-filtro
         */
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            int dimensioneIniziale = annunci.size();
            
            // Rimozione efficiente degli annunci non validi usando predicate
            annunci.removeIf(annuncio -> !validaAnnuncioCompleto(annuncio));
            
            int rimossi = dimensioneIniziale - annunci.size();
            if (rimossi > 0) {
                contatoreNonValidi.addAndGet(rimossi);
                LOGGER.log(Level.WARNING, 
                    "🚫 Rimossi {0} annunci non validi durante la validazione pre-filtro", 
                    rimossi
                );
            }
        }
        
        /**
         * Validazione finale post-filtro
         */
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Validazione di sicurezza post-filtro
            int iniziale = annunciFiltrati.size();
            annunciFiltrati.removeIf(annuncio -> !validaAnnuncioCompleto(annuncio));
            int rimossi = iniziale - annunciFiltrati.size();
            
            if (rimossi > 0) {
                LOGGER.log(Level.WARNING, 
                    "🔍 Ulteriori {0} annunci non validi rimossi durante la validazione post-filtro", 
                    rimossi
                );
            }
        }
        
        /**
         * Validazione completa di un annuncio secondo le regole di business
         * 
         * @param annuncio L'annuncio da validare
         * @return true se l'annuncio è valido, false altrimenti
         */
        private boolean validaAnnuncioCompleto(Annuncio annuncio) {
            if (annuncio == null) {
                return false;
            }
            
            // Validazione campo prezzo (non negativo)
            if (annuncio.getPrezzo() < 0) {
                return false;
            }
            
            // Validazione oggetto associato (deve esistere)
            if (annuncio.getOggetto() == null) {
                return false;
            }
            
            // Validazione titolo (obbligatorio e lunghezza ragionevole)
            String titolo = annuncio.getTitolo();
            if (titolo == null || titolo.trim().isEmpty() || titolo.length() > 200) {
                return false;
            }
            
            // Validazione stato (deve essere definito)
            if (annuncio.getStato() == null) {
                return false;
            }
            
            // Validazione venditore (ID positivo)
            if (annuncio.getVenditoreId() <= 0) {
                return false;
            }
            
            // Validazione tipologia (deve essere definita)
            if (annuncio.getTipologia() == null) {
                return false;
            }
            
            // Validazione data di pubblicazione (non nel futuro)
            if (annuncio.getDataPubblicazione().isAfter(java.time.LocalDateTime.now())) {
                LOGGER.log(Level.WARNING, 
                    "⚠️ Annuncio {0} con data di pubblicazione nel futuro", 
                    annuncio.getId()
                );
                return false;
            }
            
            return true;
        }
    }
    
    // ========== TRIGGER DI STATISTICHE DETTAGLIATE ==========
    
    /**
     * Trigger per la raccolta di metriche e statistiche dettagliate
     * 
     * <p><b>Metriche raccolte:</b>
     * <ul>
     *   <li>Tempi di esecuzione operazioni</li>
     *   <li>Throughput di elaborazione</li>
     *   <li>Distribuzione prezzi</li>
     *   <li>Efficienza algoritmi</li>
     * </ul>
     * </p>
     */
    public static class TriggerStatistiche implements FilterManager.FilterTrigger {
        private long timestampInizio;
        private int conteggioIniziale;
        
        /**
         * Inizio raccolta metriche pre-filtro
         */
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            timestampInizio = System.nanoTime();
            conteggioIniziale = annunci.size();
            
            // Raccolta statistiche campionate pre-filtro
            raccogliStatistichePreFiltro(annunci);
        }
        
        /**
         * Analisi metriche post-filtro
         */
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            long timestampFine = System.nanoTime();
            long durataMs = (timestampFine - timestampInizio) / 1_000_000;
            
            // Calcolo metriche di performance
            double annunciPerMillisecondo = conteggioIniziale > 0 ? 
                (double) conteggioIniziale / Math.max(durataMs, 1) : 0;
                
            // Log performance per operazioni che superano la soglia di lentezza
            if (durataMs > SOGLIA_PERFORMANCE_LENTA) {
                LOGGER.log(Level.INFO,
                    "⏱️ Operazione di filtraggio completata in {0}ms (throughput: {1} annunci/ms)",
                    new Object[]{
                        durataMs, 
                        String.format("%.2f", annunciPerMillisecondo)
                    }
                );
            }
            
            // Raccolta statistiche post-filtro
            raccogliStatistichePostFiltro(annunciFiltrati);
            aggiornaStatisticaGlobale("operazioni_filtro_completate", 1);
        }
        
        /**
         * Raccolta statistiche campionate dalla lista pre-filtro
         */
        private void raccogliStatistichePreFiltro(List<Annuncio> annunci) {
            if (annunci.isEmpty()) return;
            
            // Campionamento statistico per ottimizzare le performance
            int dimensioneCampione = Math.min(annunci.size(), 1000);
            double sommaPrezzi = 0;
            
            for (int i = 0; i < dimensioneCampione; i++) {
                Annuncio annuncio = annunci.get(i);
                sommaPrezzi += annuncio.getPrezzo();
            }
            
            // Aggiornamento statistiche globali
            double prezzoMedio = sommaPrezzi / dimensioneCampione;
            aggiornaStatisticaGlobale("prezzo_medio_campionato", (int)(prezzoMedio * 100));
        }
        
        /**
         * Raccolta statistiche dalla lista post-filtro
         */
        private void raccogliStatistichePostFiltro(List<Annuncio> annunci) {
            // Statistiche di riepilogo operazione
            aggiornaStatisticaGlobale("annunci_filtrati_ultima_operazione", annunci.size());
        }
    }
    
    // ========== TRIGGER DI OFFERTE SPECIALI INTELLIGENTE ==========
    
    /**
     * Trigger per l'identificazione e gestione automatica delle offerte speciali
     * 
     * <p><b>Funzionalità:</b>
     * <ul>
     *   <li>Rilevamento automatico super offerte</li>
     *   <li>Evidenziazione offerte speciali</li>
     *   <li>Cache per evitare ri-processamento</li>
     *   <li>Gestione dinamica soglie prezzo</li>
     * </ul>
     * </p>
     */
    public static class TriggerOfferteSpeciali implements FilterManager.FilterTrigger {
        private final ConcurrentHashMap<Integer, Boolean> cacheAnnunciProcessati = new ConcurrentHashMap<>();
        
        /**
         * Analisi offerte speciali pre-filtro
         */
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            int contatoreEvidenziati = 0;
            
            for (Annuncio annuncio : annunci) {
                // Ottimizzazione: skip annunci già processati (cache)
                if (cacheAnnunciProcessati.containsKey(annuncio.getId())) {
                    continue;
                }
                
                boolean evidenziato = applicaRegoleOffertaSpeciale(annuncio);
                if (evidenziato) {
                    contatoreEvidenziati++;
                }
                
                // Inserimento in cache per evitare ri-processamento
                cacheAnnunciProcessati.put(annuncio.getId(), true);
            }
            
            if (contatoreEvidenziati > 0) {
                LOGGER.log(Level.INFO, 
                    "🎯 Evidenziati {0} annunci come offerte speciali", 
                    contatoreEvidenziati
                );
            }
        }
        
        /**
         * Pulizia periodica cache post-filtro
         */
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Pulizia cache periodica per prevenire memory leak
            if (cacheAnnunciProcessati.size() > MAX_CACHE_SIZE) {
                int dimensionePrecedente = cacheAnnunciProcessati.size();
                cacheAnnunciProcessati.clear();
                LOGGER.log(Level.FINE,
                    "🧹 Cache offerte speciali pulita: {0} → 0 elementi",
                    dimensionePrecedente
                );
            }
        }
        
        /**
         * Applica le regole di business per l'evidenziazione offerte speciali
         * 
         * @param annuncio L'annuncio da analizzare
         * @return true se l'annuncio è stato evidenziato, false altrimenti
         */
        private boolean applicaRegoleOffertaSpeciale(Annuncio annuncio) {
            double prezzo = annuncio.getPrezzo();
            boolean evidenziato = false;
            
            // Regola 1: Super offerta (prezzo molto basso)
            if (prezzo <= PREZZO_SUPER_OFFERTA && prezzo > 0) {
                annuncio.setInEvidenza(true);
                LOGGER.log(Level.FINE,
                    "🔥 Super offerta rilevata - Annuncio: {0}, Prezzo: €{1}",
                    new Object[]{annuncio.getId(), prezzo}
                );
                evidenziato = true;
            } 
            // Regola 2: Offerta speciale (prezzo competitivo)
            else if (prezzo <= PREZZO_OFFERTA_SPECIALE && prezzo > PREZZO_SUPER_OFFERTA) {
                annuncio.setInEvidenza(true);
                LOGGER.log(Level.FINE,
                    "⭐ Offerta speciale rilevata - Annuncio: {0}, Prezzo: €{1}",
                    new Object[]{annuncio.getId(), prezzo}
                );
                evidenziato = true;
            }
            // Regola 3: Rimuovi evidenziazione se prezzo troppo alto per offerta speciale
            else if (annuncio.isInEvidenza() && prezzo > PREZZO_OFFERTA_SPECIALE * 2) {
                annuncio.setInEvidenza(false);
                LOGGER.log(Level.FINE,
                    "🔻 Rimossa evidenziazione - Prezzo troppo alto: {0}, €{1}",
                    new Object[]{annuncio.getId(), prezzo}
                );
            }
            
            return evidenziato;
        }
    }
    
    // ========== TRIGGER DI SICUREZZA E ANOMALIE ==========
    
    /**
     * Trigger per il rilevamento di anomalie e controlli di sicurezza
     * 
     * <p><b>Controlli implementati:</b>
     * <ul>
     *   <li>Rilevamento prezzi sospetti</li>
     *   <li>Identificazione possibili duplicati</li>
     *   <li>Verifica consistenza stati</li>
     *   <li>Controllo qualità dati finali</li>
     * </ul>
     * </p>
     */
    public static class TriggerSicurezza implements FilterManager.FilterTrigger {
        private final AtomicInteger contatoreSospetti = new AtomicInteger(0);
        
        /**
         * Controlli sicurezza pre-filtro
         */
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            int sospettiIniziali = contatoreSospetti.get();
            
            // Esecuzione controlli di sicurezza
            annunci.forEach(this::controllaPrezziSospetti);
            rilevaDuplicati(annunci);
            
            int nuoviSospetti = contatoreSospetti.get() - sospettiIniziali;
            if (nuoviSospetti > 0) {
                LOGGER.log(Level.WARNING,
                    "🚨 Rilevati {0} annunci sospetti durante i controlli di sicurezza",
                    nuoviSospetti
                );
            }
        }
        
        /**
         * Verifica qualità finale post-filtro
         */
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Validazione qualità dati finale
            annunciFiltrati.forEach(this::validaQualitaFinale);
        }
        
        /**
         * Controlla prezzi anomali o sospetti
         */
        private void controllaPrezziSospetti(Annuncio annuncio) {
            double prezzo = annuncio.getPrezzo();
            
            // Controllo 1: Prezzo anormalmente alto
            if (prezzo > PREZZO_SOSPETTO) {
                contatoreSospetti.incrementAndGet();
                LOGGER.log(Level.WARNING,
                    "💎 Prezzo sospettosamente alto - Annuncio: {0}, Prezzo: €{1}",
                    new Object[]{annuncio.getId(), prezzo}
                );
            }
            
            // Controllo 2: Prezzo zero per annunci non regalo
            if (prezzo == 0 && !annuncio.isRegalo()) {
                contatoreSospetti.incrementAndGet();
                LOGGER.log(Level.WARNING,
                    "❓ Prezzo zero sospetto - Annuncio: {0}, Tipologia: {1}",
                    new Object[]{annuncio.getId(), annuncio.getTipologia()}
                );
            }
            
            // Controllo 3: Prezzo negativo (dovrebbe essere già gestito dalla validazione)
            if (prezzo < 0) {
                contatoreSospetti.incrementAndGet();
                LOGGER.log(Level.SEVERE,
                    "💀 Prezzo negativo rilevato - Annuncio: {0}, Prezzo: €{1}",
                    new Object[]{annuncio.getId(), prezzo}
                );
            }
        }
        
        /**
         * Rileva possibili annunci duplicati
         */
        private void rilevaDuplicati(List<Annuncio> annunci) {
            ConcurrentHashMap<String, Integer> conteggioTitoli = new ConcurrentHashMap<>();
            
            for (Annuncio annuncio : annunci) {
                String titoloNormalizzato = annuncio.getTitolo().toLowerCase().trim();
                int occorrenze = conteggioTitoli.merge(titoloNormalizzato, 1, Integer::sum);
                
                // Segnalazione duplicati potenziali
                if (occorrenze > 1) {
                    LOGGER.log(Level.INFO,
                        "🔍 Possibile duplicato rilevato - Titolo: '{0}', Occorrenze: {1}",
                        new Object[]{annuncio.getTitolo(), occorrenze}
                    );
                }
            }
        }
        
        /**
         * Validazione qualità finale degli annunci
         */
        private void validaQualitaFinale(Annuncio annuncio) {
            // Controllo 1: Annunci in evidenza con prezzo troppo alto
            if (annuncio.isInEvidenza() && annuncio.getPrezzo() > 50) {
                LOGGER.log(Level.INFO,
                    "💡 Annuncio in evidenza con prezzo alto - ID: {0}, Prezzo: €{1}",
                    new Object[]{annuncio.getId(), annuncio.getPrezzo()}
                );
            }
            
            // Controllo 2: Annunci scaduti ma ancora contrassegnati come disponibili
            if (annuncio.isScaduto() && annuncio.isDisponibile()) {
                LOGGER.log(Level.WARNING,
                    "⏰ Annuncio scaduto ma ancora disponibile - ID: {0}, Pubblicazione: {1}",
                    new Object[]{
                        annuncio.getId(), 
                        annuncio.getDataPubblicazioneFormattata()
                    }
                );
            }
            
            // Controllo 3: Annunci senza immagine ma in evidenza
            if (annuncio.isInEvidenza() && !annuncio.hasImmagine()) {
                LOGGER.log(Level.INFO,
                    "🖼️ Annuncio in evidenza senza immagine - ID: {0}, Titolo: {1}",
                    new Object[]{annuncio.getId(), annuncio.getTitolo()}
                );
            }
        }
    }
    
    // ========== TRIGGER DI PERFORMANCE E CACHE ==========
    
    /**
     * Trigger per l'ottimizzazione delle performance e gestione cache
     * 
     * <p><b>Ottimizzazioni:</b>
     * <ul>
     *   <li>Pre-caricamento cache</li>
     *   <li>Gestione memoria efficiente</li>
     *   <li>Cleanup automatico risorse</li>
     *   <li>Ottimizzazione accessi dati</li>
     * </ul>
     * </p>
     */
    public static class TriggerPerformance implements FilterManager.FilterTrigger {
        private final ConcurrentHashMap<Integer, Annuncio> cacheAnnunci = new ConcurrentHashMap<>();
        
        /**
         * Pre-caricamento cache pre-filtro
         */
        @Override
        public void beforeFilter(List<Annuncio> annunci) {
            // Pre-caricamento cache con limitazione per memoria
            annunci.stream()
                   .limit(MAX_CACHE_SIZE / 10) // Limita dimensione pre-caricamento
                   .forEach(annuncio -> cacheAnnunci.put(annuncio.getId(), annuncio));
            
            LOGGER.log(Level.FINEST, 
                "💾 Cache pre-caricata con {0} annunci", 
                cacheAnnunci.size()
            );
        }
        
        /**
         * Aggiornamento cache post-filtro
         */
        @Override
        public void afterFilter(List<Annuncio> annunciFiltrati) {
            // Aggiornamento cache con risultati filtrati
            cacheAnnunci.clear();
            annunciFiltrati.forEach(annuncio -> cacheAnnunci.put(annuncio.getId(), annuncio));
            
            // Pulizia preventiva cache se troppo grande
            if (cacheAnnunci.size() > MAX_CACHE_SIZE) {
                cacheAnnunci.clear();
                LOGGER.log(Level.INFO,
                    "🧹 Cache performance pulita per prevenire memory leak"
                );
            }
            
            LOGGER.log(Level.FINEST,
                "🔄 Cache aggiornata con {0} annunci filtrati",
                cacheAnnunci.size()
            );
        }
    }
    
    // ========== METODI DI UTILITÀ E GESTIONE GLOBALE ==========
    
    /**
     * Aggiorna una statistica globale in modo thread-safe
     * 
     * @param chiave La chiave della statistica
     * @param valore Il valore da aggiungere
     */
    private static void aggiornaStatisticaGlobale(String chiave, int valore) {
        STATISTICHE_GLOBALI.computeIfAbsent(chiave, k -> new AtomicInteger())
                          .addAndGet(valore);
    }
    
    /**
     * Restituisce una copia delle statistiche globali raccolte
     * 
     * @return Mappa con tutte le statistiche
     */
    public static ConcurrentHashMap<String, AtomicInteger> getStatisticheGlobali() {
        return new ConcurrentHashMap<>(STATISTICHE_GLOBALI);
    }
    
    /**
     * Resetta completamente tutte le statistiche e metriche
     */
    public static void resettaStatistiche() {
        STATISTICHE_GLOBALI.clear();
        LOGGER.info("📊 Statistiche globali resettate");
    }
    
    /**
     * Registra tutti i trigger predefiniti con configurazione ottimale
     * 
     * <p>Questo metodo inizializza l'intero sistema di trigger con
     * le configurazioni raccomandate per ambienti di produzione.</p>
     */
    public static void registraTuttiITrigger() {
        try {
            // Registrazione sequenziale di tutti i trigger
            FilterManager.registraTrigger(new TriggerLogging());
            FilterManager.registraTrigger(new TriggerValidazione());
            FilterManager.registraTrigger(new TriggerStatistiche());
            FilterManager.registraTrigger(new TriggerOfferteSpeciali());
            FilterManager.registraTrigger(new TriggerSicurezza());
            FilterManager.registraTrigger(new TriggerPerformance());
            
            LOGGER.info("✅ Tutti i trigger registrati con successo");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, 
                "❌ Errore critico durante la registrazione dei trigger", 
                e
            );
            throw new RuntimeException("Impossibile inizializzare i trigger", e);
        }
    }
    
    /**
     * Configura il livello di logging per il sistema di trigger
     * 
     * @param livello Il livello di logging desiderato (FINE, INFO, WARNING, ecc.)
     */
    public static void configuraLivelloLogging(Level livello) {
        LOGGER.setLevel(livello);
        LOGGER.log(Level.CONFIG, 
            "⚙️ Livello logging configurato a: {0}", 
            livello
        );
    }
    
    /**
     * Ottiene il numero totale di annunci processati dal sistema
     * 
     * @return Il conteggio totale degli annunci processati
     */
    public static int getTotaleAnnunciProcessati() {
        return STATISTICHE_GLOBALI
            .getOrDefault("annunci_processati_totali", new AtomicInteger(0))
            .get();
    }
    
    /**
     * Ottiene il numero totale di annunci filtrati dal sistema
     * 
     * @return Il conteggio totale degli annunci filtrati
     */
    public static int getTotaleAnnunciFiltrati() {
        return STATISTICHE_GLOBALI
            .getOrDefault("annunci_filtrati_totali", new AtomicInteger(0))
            .get();
    }
    
    /**
     * Verifica se il sistema di trigger è attivo e operativo
     * 
     * @return true se i trigger sono attivi, false altrimenti
     */
    public static boolean sonoTriggerAttivi() {
        return !STATISTICHE_GLOBALI.isEmpty() || 
               LOGGER.getLevel() != Level.OFF;
    }
    
    /**
     * Genera un report dettagliato delle statistiche e metriche
     * 
     * @return Stringa formattata con il report completo
     */
    public static String generaReportStatistiche() {
        StringBuilder report = new StringBuilder();
        report.append("📈 === REPORT DETTAGLIATO TRIGGER ANNUNCI ===\n");
        report.append(String.format("   Annunci totali processati: %,d\n", getTotaleAnnunciProcessati()));
        report.append(String.format("   Annunci totali filtrati: %,d\n", getTotaleAnnunciFiltrati()));
        report.append(String.format("   Operazioni di filtro completate: %,d\n", 
            STATISTICHE_GLOBALI.getOrDefault("operazioni_filtro_completate", new AtomicInteger(0)).get()));
        report.append(String.format("   Sistema trigger attivo: %s\n", sonoTriggerAttivi() ? "✅ SÌ" : "❌ NO"));
        report.append(String.format("   Livello logging: %s\n", LOGGER.getLevel()));
        report.append("============================================\n");
        
        // Statistiche aggiuntive
        STATISTICHE_GLOBALI.forEach((chiave, valore) -> {
            if (!chiave.startsWith("annunci_") && !chiave.equals("operazioni_filtro_completate")) {
                report.append(String.format("   %s: %,d\n", chiave, valore.get()));
            }
        });
        
        return report.toString();
    }
    
    /**
     * Metodo di utilità per il debugging del sistema
     * 
     * @return Stringa con lo stato corrente del sistema
     */
    public static String getStatoSistema() {
        return String.format(
            "🔧 Sistema Trigger - Processati: %,d, Filtrati: %,d, Attivo: %s",
            getTotaleAnnunciProcessati(),
            getTotaleAnnunciFiltrati(),
            sonoTriggerAttivi() ? "SÌ" : "NO"
        );
    }
}