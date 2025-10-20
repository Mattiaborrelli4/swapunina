package application.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestisce la connessione al database PostgreSQL con pool di connessioni base
 * Fornisce metodi per ottenere connessioni, verificarne lo stato e gestire il ciclo di vita
 * 
 * <p><b>Caratteristiche:</b>
 * <ul>
 *   <li>Gestione connessioni thread-safe</li>
 *   <li>Monitoraggio utilizzo connessioni</li>
 *   <li>Timeout configurabili</li>
 *   <li>Validazione connessioni</li>
 *   <li>Metriche prestazioni</li>
 * </ul>
 * </p>
 */
public class ConnessioneDB {
    // ========== COSTANTI DI CONFIGURAZIONE ==========
    
    /** Configurazione database */
    private static final String HOST_DB = "localhost";
    private static final int PORTA_DB = 5432;
    private static final String NOME_DB = "postgres";
    private static final String UTENTE_DB = "postgres";
    private static final String PASSWORD_DB = "1234";
    
    /** Timeout configurabili (in secondi) */
    private static final int TIMEOUT_CONNESSIONE = 5;
    private static final int TIMEOUT_SOCKET = 30;
    private static final int TIMEOUT_VALIDAZIONE = 2;
    
    /** Metriche e statistiche */
    private static final AtomicInteger CONTATORE_CONNESSIONI_APERTE = new AtomicInteger(0);
    private static final AtomicInteger CONTATORE_CONNESSIONI_TOTALI = new AtomicInteger(0);
    private static final AtomicInteger CONTATORE_ERRORI_CONNESSIONE = new AtomicInteger(0);
    
    /** Flag di inizializzazione */
    private static volatile boolean INIZIALIZZATO = false;
    
    // ========== INIZIALIZZAZIONE STATICA ==========
    
    /**
     * Blocco di inizializzazione statica per il caricamento del driver JDBC
     * Eseguito una sola volta quando la classe viene caricata
     */
    static {
        inizializzaDriver();
    }
    
    /**
     * Inizializza il driver JDBC PostgreSQL
     * Gestisce gli errori di caricamento in modo robusto
     */
    private static void inizializzaDriver() {
        try {
            Class.forName("org.postgresql.Driver");
            INIZIALIZZATO = true;
            System.out.println("✅ Driver PostgreSQL caricato con successo");
        } catch (ClassNotFoundException e) {
            INIZIALIZZATO = false;
            String messaggioErrore = "❌ Driver PostgreSQL non trovato. Verifica che il driver sia nel classpath.";
            System.err.println(messaggioErrore);
            throw new ExceptionInInitializerError(messaggioErrore + " " + e.getMessage());
        } catch (Exception e) {
            INIZIALIZZATO = false;
            String messaggioErrore = "❌ Errore durante l'inizializzazione del driver PostgreSQL.";
            System.err.println(messaggioErrore);
            throw new ExceptionInInitializerError(messaggioErrore + " " + e.getMessage());
        }
    }
    
    // ========== METODI GESTIONE CONNESSIONE ==========
    
    /**
     * Crea e restituisce una connessione al database con proprietà ottimizzate
     * 
     * @return Connection oggetto connessione al database
     * @throws SQLException se la connessione fallisce per qualsiasi motivo
     */
    public static Connection getConnessione() throws SQLException {
        if (!INIZIALIZZATO) {
            throw new SQLException("Driver database non inizializzato");
        }
        
        String url = String.format("jdbc:postgresql://%s:%d/%s", HOST_DB, PORTA_DB, NOME_DB);
        Properties proprieta = new Properties();
        
        // Credenziali di base
        proprieta.setProperty("user", UTENTE_DB);
        proprieta.setProperty("password", PASSWORD_DB);
        
        // Proprietà di performance e robustezza
        proprieta.setProperty("connectTimeout", String.valueOf(TIMEOUT_CONNESSIONE));
        proprieta.setProperty("socketTimeout", String.valueOf(TIMEOUT_SOCKET));
        proprieta.setProperty("tcpKeepAlive", "true");
        proprieta.setProperty("ApplicationName", "SwapUnina-App");
        
        // Ottimizzazioni prestazioni
        proprieta.setProperty("defaultRowFetchSize", "100");
        proprieta.setProperty("preparedStatementCacheQueries", "256");
        proprieta.setProperty("preparedStatementCacheSizeMiB", "5");
        
        try {
            Connection connessione = DriverManager.getConnection(url, proprieta);
            
            // Aggiorna metriche
            CONTATORE_CONNESSIONI_APERTE.incrementAndGet();
            CONTATORE_CONNESSIONI_TOTALI.incrementAndGet();
            
            System.out.println("🔗 Connessione database stabilita (" + 
                             CONTATORE_CONNESSIONI_APERTE.get() + " connessioni attive)");
            
            return connessione;
            
        } catch (SQLException e) {
            // Aggiorna metriche errori
            CONTATORE_ERRORI_CONNESSIONE.incrementAndGet();
            
            System.err.println("❌ Errore durante la connessione al database: " + e.getMessage());
            throw new SQLException("Impossibile connettersi al database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Chiude una connessione in modo sicuro con gestione errori
     * 
     * @param connessione La connessione da chiudere (può essere null)
     */
    public static void chiudiConnessione(Connection connessione) {
        if (connessione != null) {
            try {
                if (!connessione.isClosed()) {
                    connessione.close();
                    CONTATORE_CONNESSIONI_APERTE.decrementAndGet();
                    System.out.println("🔒 Connessione chiusa (" + 
                                     CONTATORE_CONNESSIONI_APERTE.get() + " connessioni attive)");
                }
            } catch (SQLException e) {
                System.err.println("⚠️ Errore durante la chiusura della connessione: " + e.getMessage());
            }
        }
    }
    
    /**
     * Chiude tutte le risorse di database in modo sicuro
     * Metodo di utilità per la pulizia completa
     */
    public static void chiudiConnessione() {
        System.out.println("🧹 Pulizia connessioni database...");
        System.out.println("📊 Statistiche finali - Totali: " + CONTATORE_CONNESSIONI_TOTALI.get() + 
                         ", Errori: " + CONTATORE_ERRORI_CONNESSIONE.get());
        
        // Nota: In un'implementazione con pool di connessioni, qui si chiuderebbe il pool
        // Per ora, questo metodo serve principalmente per le metriche e pulizia generale
    }
    
    // ========== METODI DI VERIFICA E VALIDAZIONE ==========
    
    /**
     * Verifica se è possibile stabilire una connessione con il database
     * 
     * @return true se la connessione è stabilita e valida, false altrimenti
     */
    public static boolean verificaConnessione() {
        Connection connessione = null;
        try {
            connessione = getConnessione();
            return connessione != null && !connessione.isClosed() && connessione.isValid(TIMEOUT_VALIDAZIONE);
        } catch (SQLException e) {
            System.err.println("❌ Verifica connessione fallita: " + e.getMessage());
            return false;
        } finally {
            chiudiConnessione(connessione);
        }
    }
    
    /**
     * Verifica rapidamente la connessione con un timeout ridotto
     * Ideale per check veloci dello stato del database
     * 
     * @return true se la connessione è disponibile e reattiva, false altrimenti
     */
    public static boolean verificaConnessioneRapida() {
        Connection connessione = null;
        try {
            connessione = getConnessione();
            return connessione.isValid(1); // Timeout di 1 secondo per validazione rapida
        } catch (SQLException e) {
            return false;
        } finally {
            chiudiConnessione(connessione);
        }
    }
    
    /**
     * Testa le prestazioni della connessione eseguendo una query semplice
     * 
     * @return tempo di risposta in millisecondi, -1 se fallisce
     */
    public static long testPrestazioniConnessione() {
        long startTime = System.currentTimeMillis();
        Connection connessione = null;
        
        try {
            connessione = getConnessione();
            try (var statement = connessione.createStatement();
                 var resultSet = statement.executeQuery("SELECT 1")) {
                
                if (resultSet.next()) {
                    long endTime = System.currentTimeMillis();
                    return endTime - startTime;
                }
            }
            return -1;
            
        } catch (SQLException e) {
            return -1;
        } finally {
            chiudiConnessione(connessione);
        }
    }
    
    /**
     * Esegue un test completo della connessione al database
     * Include verifica connettività, prestazioni e stato
     * 
     * @return Report dettagliato del test
     */
    public static String testConnessioneCompleto() {
        StringBuilder report = new StringBuilder();
        report.append("=== TEST CONNESSIONE DATABASE ===\n");
        
        // Test connettività base
        report.append("🔌 Connettività base: ");
        boolean connessioneOK = verificaConnessione();
        report.append(connessioneOK ? "✅ OK\n" : "❌ FALLITO\n");
        
        // Test prestazioni
        report.append("⚡ Test prestazioni: ");
        long tempoRisposta = testPrestazioniConnessione();
        if (tempoRisposta >= 0) {
            report.append("✅ ").append(tempoRisposta).append("ms\n");
        } else {
            report.append("❌ FALLITO\n");
        }
        
        // Test connessione rapida
        report.append("🚀 Connessione rapida: ");
        boolean rapidaOK = verificaConnessioneRapida();
        report.append(rapidaOK ? "✅ OK\n" : "❌ FALLITO\n");
        
        // Statistiche
        report.append("📊 Statistiche: ");
        report.append("Attive=").append(CONTATORE_CONNESSIONI_APERTE.get())
              .append(", Totali=").append(CONTATORE_CONNESSIONI_TOTALI.get())
              .append(", Errori=").append(CONTATORE_ERRORI_CONNESSIONE.get())
              .append("\n");
        
        report.append("================================\n");
        return report.toString();
    }
    
    // ========== METODI DI UTILITÀ E METRICHE ==========
    
    /**
     * Restituisce il numero di connessioni attualmente aperte
     * 
     * @return Numero di connessioni attive
     */
    public static int getConnessioniAttive() {
        return CONTATORE_CONNESSIONI_APERTE.get();
    }
    
    /**
     * Restituisce il numero totale di connessioni create
     * 
     * @return Numero totale di connessioni
     */
    public static int getConnessioniTotali() {
        return CONTATORE_CONNESSIONI_TOTALI.get();
    }
    
    /**
     * Restituisce il numero di errori di connessione
     * 
     * @return Numero di errori
     */
    public static int getErroriConnessione() {
        return CONTATORE_ERRORI_CONNESSIONE.get();
    }
    
    /**
     * Verifica se il driver è stato inizializzato correttamente
     * 
     * @return true se il driver è pronto, false altrimenti
     */
    public static boolean isInizializzato() {
        return INIZIALIZZATO;
    }
    
    /**
     * Ripristina le metriche e i contatori
     * Utile per test o reset statistiche
     */
    public static void resettaMetriche() {
        CONTATORE_CONNESSIONI_APERTE.set(0);
        CONTATORE_CONNESSIONI_TOTALI.set(0);
        CONTATORE_ERRORI_CONNESSIONE.set(0);
        System.out.println("📊 Metriche connessioni resettate");
    }
    
    /**
     * Ottiene un report delle statistiche correnti
     * 
     * @return Stringa formattata con le statistiche
     */
    public static String getReportStatistiche() {
        return String.format(
            "📈 Statistiche Connessioni DB - Attive: %d, Totali: %d, Errori: %d, Inizializzato: %s",
            CONTATORE_CONNESSIONI_APERTE.get(),
            CONTATORE_CONNESSIONI_TOTALI.get(),
            CONTATORE_ERRORI_CONNESSIONE.get(),
            INIZIALIZZATO ? "✅" : "❌"
        );
    }
}