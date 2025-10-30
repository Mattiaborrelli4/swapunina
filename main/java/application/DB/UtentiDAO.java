package application.DB;

import application.Classe.utente;
import application.DB.CloudinaryService;
import java.sql.*;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Data Access Object per la gestione degli utenti nel database
 * 
 * <p><b>Funzionalità principali:</b>
 * <ul>
 *   <li>Registrazione e autenticazione utenti</li>
 *   <li>Gestione password cifrate con BCrypt</li>
 *   <li>Cache per ottimizzare le performance</li>
 *   <li>Validazione dati e sicurezza</li>
 *   <li>Gestione stato attivo/inattivo utenti</li>
 *   <li>Gestione foto profilo con Cloudinary</li>
 *   <li>Avatar univoci per utenti senza foto</li>
 * </ul>
 * </p>
 */
public class UtentiDAO {
    private static final Logger LOGGER = Logger.getLogger(UtentiDAO.class.getName());
    
    // ========== CACHE PER PERFORMANCE ==========
    
    /** Cache per mapping email -> ID utente */
    private static final ConcurrentHashMap<String, Integer> CACHE_EMAIL_TO_ID = new ConcurrentHashMap<>();
    
    /** Cache per mapping ID -> nome utente */
    private static final ConcurrentHashMap<Integer, String> CACHE_ID_TO_NOME = new ConcurrentHashMap<>();
    
    /** Cache per mapping ID -> email utente */
    private static final ConcurrentHashMap<Integer, String> CACHE_ID_TO_EMAIL = new ConcurrentHashMap<>();
    
    /** Cache per verifica esistenza email */
    private static final ConcurrentHashMap<String, Boolean> CACHE_EMAIL_EXISTE = new ConcurrentHashMap<>();
    
    // ========== COSTANTI DI CONFIGURAZIONE ==========
    
    /** Costanti per BCrypt */
    private static final int BCRYPT_ROUNDS = 12;
    private static final int LUNGHEZZA_MINIMA_PASSWORD = 6;
    private static final int DIMENSIONE_MAX_CACHE = 1000;
    
    /** Nomi colonne database */
    private static final String COLONNA_ATTIVO = "attivo";
    private static final String COLONNA_EMAIL = "email";
    private static final String COLONNA_MATRICOLA = "matricola";
    
    // ========== SERVIZI ==========
    
    /** Servizio per la gestione delle immagini su Cloudinary */
    private final CloudinaryService cloudinaryService;
    
    // ========== STATISTICHE E METRICHE ==========
    
    /** Statistiche cache per monitoring */
    private final AtomicInteger contatoreCacheHit = new AtomicInteger(0);
    private final AtomicInteger contatoreCacheMiss = new AtomicInteger(0);
    
    // ========== COSTRUTTORE ==========
    
    /**
     * Costruttore - inizializza il database e verifica la struttura
     */
    public UtentiDAO() {
        this.cloudinaryService = new CloudinaryService();
        inizializzaDatabase();
    }

    // ========== INIZIALIZZAZIONE DATABASE ==========
    
    /**
     * Inizializza il database e verifica/crea la tabella utente
     * Gestisce anche l'aggiornamento dello schema se necessario
     */
    private void inizializzaDatabase() {
        String sqlVerificaTabella = 
            "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'utente')";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sqlVerificaTabella);
             ResultSet risultato = statement.executeQuery()) {
            
            if (risultato.next() && !risultato.getBoolean(1)) {
                creaTabellaUtente(connessione);
                LOGGER.log(Level.INFO, "✅ Tabella utente creata con successo");
            } else {
                LOGGER.log(Level.FINE, "🔍 Tabella utente già esistente - verifica struttura");
                verificaEAggiornaStrutturaTabella(connessione);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore critico nell'inizializzazione del database", e);
        }
    }
    
    /**
     * Verifica e aggiorna la struttura della tabella se necessario
     * Aggiunge colonne mancanti senza perdere dati esistenti
     */
    private void verificaEAggiornaStrutturaTabella(Connection connessione) {
        try {
            // Verifica se la colonna 'attivo' esiste
            if (!colonnaEsiste(connessione, COLONNA_ATTIVO)) {
                aggiungiColonnaAttivo(connessione);
            }
            
            // Verifica se la colonna 'ultimo_accesso' esiste
            if (!colonnaEsiste(connessione, "ultimo_accesso")) {
                aggiungiColonnaUltimoAccesso(connessione);
            }
            
            // Verifica se la colonna 'foto_profilo' esiste
            if (!colonnaEsiste(connessione, "foto_profilo")) {
                aggiungiColonnaFotoProfilo(connessione);
            }
            
            LOGGER.log(Level.INFO, "✅ Struttura tabella utente verificata e aggiornata");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nell'aggiornamento della struttura della tabella", e);
        }
    }
    
    /**
     * Verifica se una colonna specifica esiste nella tabella utente
     */
    private boolean colonnaEsiste(Connection connessione, String nomeColonna) throws SQLException {
        String sql = """
            SELECT EXISTS (
                SELECT FROM information_schema.columns 
                WHERE table_name = 'utente' AND column_name = ?
            )
            """;
        
        try (PreparedStatement statement = connessione.prepareStatement(sql)) {
            statement.setString(1, nomeColonna);
            try (ResultSet risultato = statement.executeQuery()) {
                return risultato.next() && risultato.getBoolean(1);
            }
        }
    }
    
    /**
     * Aggiunge la colonna 'attivo' alla tabella utente
     */
    private void aggiungiColonnaAttivo(Connection connessione) throws SQLException {
        String sql = "ALTER TABLE utente ADD COLUMN attivo BOOLEAN DEFAULT TRUE";
        try (PreparedStatement statement = connessione.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.log(Level.INFO, "✅ Colonna 'attivo' aggiunta alla tabella utente");
        }
    }
    
    /**
     * Aggiunge la colonna 'ultimo_accesso' alla tabella utente
     */
    private void aggiungiColonnaUltimoAccesso(Connection connessione) throws SQLException {
        String sql = "ALTER TABLE utente ADD COLUMN ultimo_accesso TIMESTAMP";
        try (PreparedStatement statement = connessione.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.log(Level.INFO, "✅ Colonna 'ultimo_accesso' aggiunta alla tabella utente");
        }
    }
    
    /**
     * Aggiunge la colonna 'foto_profilo' alla tabella utente
     */
    private void aggiungiColonnaFotoProfilo(Connection connessione) throws SQLException {
        String sql = "ALTER TABLE utente ADD COLUMN foto_profilo VARCHAR(500)";
        try (PreparedStatement statement = connessione.prepareStatement(sql)) {
            statement.executeUpdate();
            LOGGER.log(Level.INFO, "✅ Colonna 'foto_profilo' aggiunta alla tabella utente");
        }
    }
    
    /**
     * Crea la tabella utente con tutti i campi necessari
     */
    private void creaTabellaUtente(Connection connessione) {
        String sqlCreaTabella = """
            CREATE TABLE utente (
                id SERIAL PRIMARY KEY,
                matricola VARCHAR(20) UNIQUE NOT NULL,
                nome VARCHAR(100) NOT NULL,
                cognome VARCHAR(100) NOT NULL,
                email VARCHAR(255) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                foto_profilo VARCHAR(500),
                data_registrazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                ultimo_accesso TIMESTAMP,
                attivo BOOLEAN DEFAULT TRUE
            )
            """;
        
        try (PreparedStatement statement = connessione.prepareStatement(sqlCreaTabella)) {
            statement.executeUpdate();
            creaIndici(connessione);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nella creazione della tabella utente", e);
        }
    }
    
    /**
     * Crea indici per ottimizzare le query frequenti
     */
    private void creaIndici(Connection connessione) {
        String[] indici = {
            "CREATE INDEX idx_utente_email ON utente(email)",
            "CREATE INDEX idx_utente_matricola ON utente(matricola)",
            "CREATE INDEX idx_utente_nome_cognome ON utente(nome, cognome)",
            "CREATE INDEX idx_utente_attivo ON utente(attivo)"
        };
        
        for (String sqlIndice : indici) {
            try (PreparedStatement statement = connessione.prepareStatement(sqlIndice)) {
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "⚠️ Errore nella creazione dell'indice: " + sqlIndice, e);
            }
        }
        
        LOGGER.log(Level.INFO, "✅ Indici creati con successo");
    }
    
    // ========== METODI PRINCIPALI CRUD ==========
    
    /**
     * Verifica se un'email esiste già nel database
     * Utilizza cache per ottimizzare le performance
     * 
     * @param email L'email da verificare
     * @return true se l'email esiste, false altrimenti
     */
    public boolean emailEsiste(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailNormalizzata = email.trim().toLowerCase();
        
        // Controllo in cache
        Boolean esistenteInCache = CACHE_EMAIL_EXISTE.get(emailNormalizzata);
        if (esistenteInCache != null) {
            contatoreCacheHit.incrementAndGet();
            return esistenteInCache;
        }
        
        contatoreCacheMiss.incrementAndGet();
        boolean esiste = verificaEsistenzaCampo(COLONNA_EMAIL, emailNormalizzata);
        
        // Aggiorna cache
        CACHE_EMAIL_EXISTE.put(emailNormalizzata, esiste);
        pulisciCacheSeNecessario();
        
        return esiste;
    }
    
    /**
     * Verifica se una matricola esiste già nel database
     * 
     * @param matricola La matricola da verificare
     * @return true se la matricola esiste, false altrimenti
     */
    public boolean matricolaEsiste(String matricola) {
        return verificaEsistenzaCampo(COLONNA_MATRICOLA, matricola);
    }
    
    /**
     * Metodo generico per verificare l'esistenza di un campo
     */
    private boolean verificaEsistenzaCampo(String campo, String valore) {
        if (valore == null || valore.trim().isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM utente WHERE " + campo + " = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, valore.trim());
            
            try (ResultSet risultato = statement.executeQuery()) {
                return risultato.next() && risultato.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel verificare l'esistenza del campo: " + campo, e);
            return false;
        }
    }

    /**
     * Registra un nuovo utente nel sistema
     * 
     * @param utente L'utente da registrare
     * @return true se la registrazione è avvenuta con successo, false altrimenti
     */
    /**
     * Registra un nuovo utente nel sistema
     * 
     * @param utente L'utente da registrare
     * @return true se la registrazione è avvenuta con successo, false altrimenti
     */
    public boolean registraUtente(utente utente) {
        if (utente == null || !isUtenteValido(utente)) {
            LOGGER.log(Level.WARNING, "🚫 Tentativo di registrare un utente non valido");
            return false;
        }
        
        // Verifica duplicati
        if (emailEsiste(utente.getEmail())) {
            LOGGER.log(Level.WARNING, "📧 Email già registrata: " + utente.getEmail());
            return false;
        }
        
        if (matricolaEsiste(utente.getMatricola())) {
            LOGGER.log(Level.WARNING, "🎓 Matricola già registrata: " + utente.getMatricola());
            return false;
        }
        
        // QUERY CORRETTA - rimossa la colonna data_registrazione
        String sql = """
            INSERT INTO utente (matricola, nome, cognome, email, password) 
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            // Hash sicuro della password
            String passwordHash = BCrypt.hashpw(utente.getPassword(), BCrypt.gensalt(BCRYPT_ROUNDS));
            
            statement.setString(1, utente.getMatricola().trim());
            statement.setString(2, utente.getNome().trim());
            statement.setString(3, utente.getCognome().trim());
            statement.setString(4, utente.getEmail().trim().toLowerCase());
            statement.setString(5, passwordHash);
            
            int righeInserite = statement.executeUpdate();
            
            if (righeInserite > 0) {
                // Recupera ID generato
                try (ResultSet chiaviGenerate = statement.getGeneratedKeys()) {
                    if (chiaviGenerate.next()) {
                        int idUtente = chiaviGenerate.getInt(1);
                        utente.setId(idUtente);
                        
                        // Aggiorna cache
                        aggiornaCacheDopoRegistrazione(utente);
                    }
                }
                
                LOGGER.log(Level.INFO, "✅ Utente registrato con successo: " + utente.getEmail());
                return true;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nella registrazione dell'utente: " + utente.getEmail(), e);
        }
        
        return false;
    }
    
    /**
     * Verifica le credenziali di accesso di un utente
     * 
     * @param email L'email dell'utente
     * @param password La password da verificare
     * @return true se le credenziali sono corrette, false altrimenti
     */
    public boolean verificaCredenziali(String email, String password) {
        if (email == null || password == null || password.trim().isEmpty()) {
            return false;
        }
        
        String sql = "SELECT password FROM utente WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, email.trim().toLowerCase());
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    String passwordHash = risultato.getString("password");
                    return BCrypt.checkpw(password, passwordHash);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nella verifica delle credenziali per: " + email, e);
        }
        
        return false;
    }
    
    /**
     * Recupera un utente tramite email
     * 
     * @param email L'email dell'utente
     * @return Optional contenente l'utente se trovato, empty altrimenti
     */
    public Optional<utente> getUtenteByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String sql = "SELECT id, matricola, nome, cognome, email FROM utente WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, email.trim().toLowerCase());
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    utente utenteTrovato = new utente(
                        risultato.getString("matricola"),
                        risultato.getString("nome"),
                        risultato.getString("cognome"),
                        risultato.getString("email"),
                        ""  // Password non viene restituita per sicurezza
                    );
                    utenteTrovato.setId(risultato.getInt("id"));
                    
                    // Aggiorna cache
                    aggiornaCacheUtente(utenteTrovato);
                    
                    return Optional.of(utenteTrovato);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero utente per email: " + email, e);
        }
        
        return Optional.empty();
    }

    /**
     * Recupera ID utente tramite matricola
     * 
     * @param matricola La matricola dell'utente
     * @return L'ID utente o -1 se non trovato
     */
    public int getIdDaMatricola(String matricola) {
        if (matricola == null || matricola.trim().isEmpty()) {
            return -1;
        }
        
        String sql = "SELECT id FROM utente WHERE matricola = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {

            statement.setString(1, matricola.trim());
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    return risultato.getInt("id");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero ID per matricola: " + matricola, e);
        }
        
        return -1;
    }

    /**
     * Recupera ID utente tramite email
     * Utilizza cache per ottimizzare le performance
     * 
     * @param email L'email dell'utente
     * @return L'ID utente o -1 se non trovato
     */
    public int getIdByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return -1;
        }
        
        String emailNormalizzata = email.trim().toLowerCase();
        
        // Controllo in cache
        Integer idInCache = CACHE_EMAIL_TO_ID.get(emailNormalizzata);
        if (idInCache != null) {
            contatoreCacheHit.incrementAndGet();
            return idInCache;
        }
        
        contatoreCacheMiss.incrementAndGet();
        
        String sql = "SELECT id FROM utente WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, emailNormalizzata);
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    int idUtente = risultato.getInt("id");
                    
                    // Aggiorna cache
                    CACHE_EMAIL_TO_ID.put(emailNormalizzata, idUtente);
                    pulisciCacheSeNecessario();
                    
                    return idUtente;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero ID per email: " + email, e);
        }
        
        return -1;
    }
    
    /**
     * Recupera l'email di un utente dato il suo ID
     * Utilizza cache per ottimizzare le performance
     * 
     * @param userId ID dell'utente
     * @return Email dell'utente o null se non trovato
     */
    public String getEmailById(int userId) {
        if (userId <= 0) {
            return null;
        }
        
        // Controllo in cache
        String emailInCache = CACHE_ID_TO_EMAIL.get(userId);
        if (emailInCache != null) {
            contatoreCacheHit.incrementAndGet();
            return emailInCache;
        }
        
        contatoreCacheMiss.incrementAndGet();
        
        String sql = "SELECT email FROM utente WHERE id = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setInt(1, userId);
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    String email = risultato.getString("email");
                    
                    // Aggiorna cache
                    CACHE_ID_TO_EMAIL.put(userId, email);
                    CACHE_EMAIL_TO_ID.put(email.toLowerCase(), userId);
                    pulisciCacheSeNecessario();
                    
                    return email;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero email per ID: " + userId, e);
        }
        
        return null;
    }
    
    /**
     * Aggiorna la password di un utente
     * 
     * @param email L'email dell'utente
     * @param vecchiaPassword La password attuale
     * @param nuovaPassword La nuova password
     * @return true se l'aggiornamento è avvenuto con successo, false altrimenti
     */
    public boolean aggiornaPassword(String email, String vecchiaPassword, String nuovaPassword) {
        if (email == null || vecchiaPassword == null || nuovaPassword == null) {
            LOGGER.log(Level.WARNING, "🚫 Parametri null per aggiornamento password");
            return false;
        }
        
        String emailNormalizzata = email.trim().toLowerCase();
        
        LOGGER.log(Level.INFO, "🔐 Tentativo cambio password per: {0}", emailNormalizzata);
        
        // Verifica password attuale
        if (!verificaCredenziali(emailNormalizzata, vecchiaPassword)) {
            LOGGER.log(Level.WARNING, "❌ Password attuale non corretta per: {0}", emailNormalizzata);
            return false;
        }
        
        // Validazione nuova password
        if (!isPasswordValida(nuovaPassword, vecchiaPassword)) {
            return false;
        }
        
        String sql = "UPDATE utente SET password = ? WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {

            // Hash della nuova password
            String nuovaPasswordHash = BCrypt.hashpw(nuovaPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
            
            statement.setString(1, nuovaPasswordHash);
            statement.setString(2, emailNormalizzata);
            
            int righeAggiornate = statement.executeUpdate();
            boolean successo = righeAggiornate > 0;
            
            if (successo) {
                LOGGER.log(Level.INFO, "✅ Password aggiornata con successo per: {0}", emailNormalizzata);
                // Invalida cache
                invalidaCacheUtente(emailNormalizzata);
            } else {
                LOGGER.log(Level.WARNING, "⚠️ Nessuna riga aggiornata per: {0}", emailNormalizzata);
            }
            
            return successo;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore SQL durante aggiornamento password per: " + emailNormalizzata, e);
            return false;
        }
    }
    
    /**
     * Recupera il nome utente tramite ID
     * Utilizza cache per ottimizzare le performance
     * 
     * @param id L'ID dell'utente
     * @return Il nome dell'utente o "Utente Sconosciuto" se non trovato
     */
    public String getNomeUtenteById(int id) {
        if (id <= 0) {
            return "Utente Sconosciuto";
        }
        
        // Controllo in cache
        String nomeInCache = CACHE_ID_TO_NOME.get(id);
        if (nomeInCache != null) {
            contatoreCacheHit.incrementAndGet();
            return nomeInCache;
        }
        
        contatoreCacheMiss.incrementAndGet();
        
        String sql = "SELECT nome FROM utente WHERE id = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setInt(1, id);
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    String nome = risultato.getString("nome");
                    
                    // Aggiorna cache
                    CACHE_ID_TO_NOME.put(id, nome);
                    pulisciCacheSeNecessario();
                    
                    return nome;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero nome utente per ID: " + id, e);
        }
        
        return "Utente Sconosciuto";
    }
    
    // ========== GESTIONE FOTO PROFILO CON CLOUDINARY ==========

    /**
     * Recupera il percorso della foto profilo di un utente
     * Se non esiste, genera un avatar univoco basato sull'email
     */
    public String getFotoProfilo(String email) {
        if (email == null || email.trim().isEmpty()) {
            return generaAvatarUnivoco("default");
        }
        
        String sql = "SELECT foto_profilo FROM utente WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, email.trim().toLowerCase());
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    String fotoProfilo = risultato.getString("foto_profilo");
                    // Se non c'è foto profilo, genera un avatar univoco
                    if (fotoProfilo == null) {
                        return generaAvatarUnivoco(email);
                    }
                    return fotoProfilo;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero foto profilo per: " + email, e);
        }
        
        return generaAvatarUnivoco(email);
    }

    /**
     * Recupera un utente completo con foto profilo
     */
    public Optional<utente> getUtenteCompletoByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String sql = "SELECT id, matricola, nome, cognome, email, foto_profilo FROM utente WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, email.trim().toLowerCase());
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    utente utenteTrovato = new utente(
                        risultato.getString("matricola"),
                        risultato.getString("nome"),
                        risultato.getString("cognome"),
                        risultato.getString("email"),
                        ""  // Password non viene restituita per sicurezza
                    );
                    utenteTrovato.setId(risultato.getInt("id"));
                    utenteTrovato.setFotoProfilo(risultato.getString("foto_profilo"));
                    
                    // Aggiorna cache
                    aggiornaCacheUtente(utenteTrovato);
                    
                    return Optional.of(utenteTrovato);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel recupero utente completo per email: " + email, e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Elimina la foto profilo da Cloudinary e dal database
     */
    public boolean eliminaFotoProfilo(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Recupera l'URL corrente per ottenere il public_id
        String currentFoto = getFotoProfilo(email);
        if (currentFoto != null && currentFoto.contains("cloudinary")) {
            // Estrai il public_id dall'URL
            String publicId = extractPublicIdFromUrl(currentFoto);
            if (publicId != null) {
                cloudinaryService.deleteImage(publicId);
            }
        }
        
        // Aggiorna il database a NULL
        return aggiornaFotoProfilo(email, null);
    }
    
    /**
     * Estrae il public_id da un URL Cloudinary
     */
    private String extractPublicIdFromUrl(String cloudinaryUrl) {
        try {
            // L'URL Cloudinary ha il formato: https://res.cloudinary.com/<cloud_name>/image/upload/<public_id>.<format>
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length < 2) {
                return null;
            }
            String withVersion = parts[1];
            // Rimuovi la versione se presente (v1234567890/)
            if (withVersion.startsWith("v")) {
                int slashIndex = withVersion.indexOf('/');
                if (slashIndex != -1) {
                    withVersion = withVersion.substring(slashIndex + 1);
                }
            }
            // Rimuovi l'estensione del file
            int dotIndex = withVersion.lastIndexOf('.');
            if (dotIndex != -1) {
                withVersion = withVersion.substring(0, dotIndex);
            }
            return withVersion;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "❌ Impossibile estrarre public_id da URL: " + cloudinaryUrl, e);
            return null;
        }
    }
    
    // ========== GESTIONE AVATAR UNIVOCI ==========
    
    /**
     * Genera un avatar univoco basato sull'email dell'utente
     */
    private String generaAvatarUnivoco(String email) {
        if (email == null) email = "default";
        
        // Crea un hash univoco basato sull'email
        int hash = Math.abs(email.hashCode());
        
        // Lista di colori vibranti per avatar
        String[] colors = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", 
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
            "#F8C471", "#82E0AA", "#F1948A", "#85C1E9", "#D7BDE2"
        };
        
        // Seleziona colore basato sull'hash
        String color = colors[hash % colors.length];
        
        // Estrai le iniziali dall'email
        String iniziali = estraiIniziali(email);
        
        // Crea SVG univoco
        return creaAvatarSVG(iniziali, color);
    }

    /**
     * Estrae le iniziali dall'email (massimo 2 caratteri)
     */
    private String estraiIniziali(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "U";
        }
        
        // Prendi la parte prima della @
        String nomeParte = email.split("@")[0];
        
        // Rimuovi numeri e caratteri speciali, prendi solo lettere
        String soloLettere = nomeParte.replaceAll("[^a-zA-Z]", "");
        
        if (soloLettere.isEmpty()) {
            // Se non ci sono lettere, prendi i primi due caratteri
            return nomeParte.length() >= 2 ? 
                   nomeParte.substring(0, 2).toUpperCase() : 
                   nomeParte.toUpperCase();
        }
        
        // Prendi le prime due lettere
        if (soloLettere.length() >= 2) {
            return soloLettere.substring(0, 2).toUpperCase();
        } else {
            return soloLettere.toUpperCase();
        }
    }

    /**
     * Crea un avatar SVG come stringa base64
     */
    private String creaAvatarSVG(String iniziali, String color) {
        String svg = String.format(
            "<svg width=\"100\" height=\"100\" viewBox=\"0 0 100 100\" xmlns=\"http://www.w3.org/2000/svg\">" +
            "<rect width=\"100\" height=\"100\" fill=\"%s\" rx=\"50\"/>" +
            "<text x=\"50\" y=\"58\" text-anchor=\"middle\" dominant-baseline=\"middle\" " +
            "font-family=\"Arial, sans-serif\" font-size=\"38\" font-weight=\"bold\" fill=\"white\">%s</text>" +
            "</svg>", 
            color, iniziali
        );
        
        // Converti SVG in data URL
        String encoded = java.util.Base64.getEncoder().encodeToString(svg.getBytes());
        return "data:image/svg+xml;base64," + encoded;
    }
    
    /**
     * Recupera il percorso della foto profilo assicurandosi che sia univoco
     * Se non esiste, genera un avatar univoco
     */
    public String getFotoProfiloUnivoca(String email) {
        if (email == null || email.trim().isEmpty()) {
            return generaAvatarUnivoco("default");
        }
        
        String fotoProfilo = getFotoProfilo(email);
        
        // Se la foto profilo è condivisa con altri utenti, genera un avatar univoco
        if (fotoProfilo != null && !fotoProfilo.startsWith("data:image/svg+xml") && 
            !isFotoProfiloUnivoca(fotoProfilo, email)) {
            LOGGER.log(Level.WARNING, "🔄 Foto profilo condivisa rilevata per: {0}, generando avatar univoco", email);
            return generaAvatarUnivoco(email);
        }
        
        return fotoProfilo;
    }
    
    /**
     * Verifica se una foto profilo è già utilizzata da altri utenti
     */
    public boolean isFotoProfiloUnivoca(String percorsoFoto, String emailUtenteCorrente) {
        if (percorsoFoto == null || percorsoFoto.isEmpty()) {
            return true;
        }
        
        String sql = "SELECT COUNT(*) FROM utente WHERE foto_profilo = ? AND email != ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            statement.setString(1, percorsoFoto);
            statement.setString(2, emailUtenteCorrente.trim().toLowerCase());
            
            try (ResultSet risultato = statement.executeQuery()) {
                if (risultato.next()) {
                    return risultato.getInt(1) == 0;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel verificare univocità foto profilo", e);
        }
        
        return true;
    }
    
    // ========== MIGRAZIONE CLOUDINARY ==========
    
    /**
     * Migra tutte le foto profilo locali su Cloudinary
     */
    public void migraFotoProfiloSuCloudinary() {
        String sql = "SELECT id, email, foto_profilo FROM utente WHERE foto_profilo IS NOT NULL AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql);
             ResultSet risultato = statement.executeQuery()) {
            
            int migrateCount = 0;
            int errorCount = 0;
            
            while (risultato.next()) {
                int userId = risultato.getInt("id");
                String email = risultato.getString("email");
                String fotoProfilo = risultato.getString("foto_profilo");
                
                // Migra solo se è un percorso locale
                if (fotoProfilo != null && 
                    !fotoProfilo.startsWith("http") && 
                    !fotoProfilo.startsWith("cloudinary") &&
                    !fotoProfilo.startsWith("data:image")) {
                    
                    LOGGER.log(Level.INFO, "🔄 Migrazione foto per: {0}", email);
                    boolean success = aggiornaFotoProfilo(email, fotoProfilo);
                    
                    if (success) {
                        migrateCount++;
                    } else {
                        errorCount++;
                    }
                }
            }
            
            LOGGER.log(Level.INFO, "✅ Migrazione completata: {0} successi, {1} errori", 
                       new Object[]{migrateCount, errorCount});
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nella migrazione foto profilo", e);
        }
    }
    
    /**
     * Pulisce le foto profilo duplicate nel database
     */
    public void correggiFotoProfiloDuplicate() {
        String sql = """
            UPDATE utente u1 
            SET foto_profilo = NULL 
            WHERE foto_profilo IS NOT NULL 
            AND foto_profilo IN (
                SELECT foto_profilo 
                FROM utente u2 
                WHERE u2.id != u1.id 
                AND u2.foto_profilo = u1.foto_profilo
                AND u2.foto_profilo IS NOT NULL
            )
            """;
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            int righeAggiornate = statement.executeUpdate();
            LOGGER.log(Level.INFO, "🧹 Foto profilo duplicate rimosse: {0} righe aggiornate", righeAggiornate);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nella pulizia foto profilo duplicate", e);
        }
    }
    
    /**
     * Metodo per testare gli avatar di tutti gli utenti
     */
    public void testAvatarPerTuttiUtenti() {
        String sql = "SELECT email FROM utente WHERE attivo = TRUE ORDER BY email";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql);
             ResultSet risultato = statement.executeQuery()) {
            
            System.out.println("🎨 TEST AVATAR UNIVOCI PER TUTTI GLI UTENTI:");
            System.out.println("=============================================");
            
            while (risultato.next()) {
                String email = risultato.getString("email");
                String avatar = getFotoProfilo(email);
                String iniziali = estraiIniziali(email);
                
                System.out.printf("📧 %-30s -> %-2s -> %s%n", 
                    email, iniziali, 
                    avatar.length() > 100 ? avatar.substring(0, 100) + "..." : avatar
                );
            }
            
            System.out.println("=============================================");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nel test avatar", e);
        }
    }
    
    // ========== METODI DI SUPPORTO E VALIDAZIONE ==========
    
    /**
     * Verifica la validità di un oggetto utente
     */
    private boolean isUtenteValido(utente utente) {
        return utente != null &&
               utente.getMatricola() != null && !utente.getMatricola().trim().isEmpty() &&
               utente.getNome() != null && !utente.getNome().trim().isEmpty() &&
               utente.getCognome() != null && !utente.getCognome().trim().isEmpty() &&
               utente.getEmail() != null && !utente.getEmail().trim().isEmpty() &&
               utente.getPassword() != null && utente.getPassword().length() >= LUNGHEZZA_MINIMA_PASSWORD;
    }
    
    /**
     * Verifica la validità di una password
     */
    private boolean isPasswordValida(String nuovaPassword, String vecchiaPassword) {
        if (nuovaPassword.length() < LUNGHEZZA_MINIMA_PASSWORD) {
            LOGGER.log(Level.WARNING, "🔒 Password troppo corta: {0} caratteri", nuovaPassword.length());
            return false;
        }
        
        if (nuovaPassword.equals(vecchiaPassword)) {
            LOGGER.log(Level.WARNING, "🔒 La nuova password è uguale alla vecchia");
            return false;
        }
        
        return true;
    }
    
    // ========== GESTIONE CACHE ==========
    
    /**
     * Aggiorna le cache dopo una registrazione
     */
    private void aggiornaCacheDopoRegistrazione(utente utente) {
        String email = utente.getEmail().toLowerCase();
        CACHE_EMAIL_TO_ID.put(email, utente.getId());
        CACHE_ID_TO_NOME.put(utente.getId(), utente.getNome());
        CACHE_ID_TO_EMAIL.put(utente.getId(), email);
        CACHE_EMAIL_EXISTE.put(email, true);
        pulisciCacheSeNecessario();
    }
    
    /**
     * Aggiorna les cache con i dati di un utente
     */
    private void aggiornaCacheUtente(utente utente) {
        String email = utente.getEmail().toLowerCase();
        CACHE_EMAIL_TO_ID.put(email, utente.getId());
        CACHE_ID_TO_NOME.put(utente.getId(), utente.getNome());
        CACHE_ID_TO_EMAIL.put(utente.getId(), email);
        pulisciCacheSeNecessario();
    }
    
    /**
     * Invalida le cache relative a un utente
     */
    private void invalidaCacheUtente(String email) {
        String emailNormalizzata = email.toLowerCase();
        Integer idUtente = CACHE_EMAIL_TO_ID.get(emailNormalizzata);
        
        CACHE_EMAIL_TO_ID.remove(emailNormalizzata);
        CACHE_EMAIL_EXISTE.remove(emailNormalizzata);
        
        if (idUtente != null) {
            CACHE_ID_TO_NOME.remove(idUtente);
            CACHE_ID_TO_EMAIL.remove(idUtente);
        }
    }
    
    /**
     * Pulizia periodica delle cache per prevenire memory leak
     */
    private void pulisciCacheSeNecessario() {
        if (CACHE_EMAIL_TO_ID.size() > DIMENSIONE_MAX_CACHE) {
            CACHE_EMAIL_TO_ID.clear();
            CACHE_ID_TO_NOME.clear();
            CACHE_ID_TO_EMAIL.clear();
            CACHE_EMAIL_EXISTE.clear();
            LOGGER.log(Level.INFO, "🧹 Cache pulita per prevenire memory leak");
        }
    }
    
    // ========== METODI DI UTILITÀ E STATISTICHE ==========
    
    /**
     * Restituisce le statistiche della cache
     * 
     * @return Stringa formattata con le statistiche
     */
    public String getStatisticheCache() {
        int totale = contatoreCacheHit.get() + contatoreCacheMiss.get();
        double percentualeHit = totale > 0 ? (contatoreCacheHit.get() * 100.0) / totale : 0;
        
        return String.format(
            "📊 Statistiche Cache - Hit: %d, Miss: %d, Percentuale Hit: %.1f%%, Dimensione: %d",
            contatoreCacheHit.get(), 
            contatoreCacheMiss.get(), 
            percentualeHit, 
            CACHE_EMAIL_TO_ID.size()
        );
    }
    
    /**
     * Resetta le statistiche della cache
     */
    public void resettaStatisticheCache() {
        contatoreCacheHit.set(0);
        contatoreCacheMiss.set(0);
    }
    
    /**
     * Pulisce completamente tutte le cache
     */
    public void pulisciCacheCompleta() {
        CACHE_EMAIL_TO_ID.clear();
        CACHE_ID_TO_NOME.clear();
        CACHE_ID_TO_EMAIL.clear();
        CACHE_EMAIL_EXISTE.clear();
        resettaStatisticheCache();
        LOGGER.log(Level.INFO, "🧹 Cache completamente pulita");
    }
    
    /**
     * Ottiene un report completo dello stato del DAO
     * 
     * @return Report dettagliato
     */
    public String getReportStato() {
        return String.format(
            "📈 Report UtentiDAO - %s\nCache Size: %d, Hit Rate: %.1f%%",
            getStatisticheCache(),
            CACHE_EMAIL_TO_ID.size(),
            (contatoreCacheHit.get() + contatoreCacheMiss.get() > 0 ? 
             (contatoreCacheHit.get() * 100.0) / (contatoreCacheHit.get() + contatoreCacheMiss.get()) : 0)
        );
    }
    
    /**
     * Aggiorna la foto profilo di un utente
     */
    public boolean aggiornaFotoProfilo(String email, String percorsoFoto) {
        if (email == null || email.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "🚫 Email non valida per aggiornamento foto profilo");
            return false;
        }
        
        String percorsoFinale = percorsoFoto;
        
        // Se Cloudinary è abilitato, prova a caricare lì
        if (cloudinaryService.isEnabled() && percorsoFoto != null && !percorsoFoto.isEmpty()) {
            try {
                // Recupera l'utente per ottenere l'ID
                Optional<utente> utenteOpt = getUtenteByEmail(email);
                if (utenteOpt.isPresent()) {
                    utente utente = utenteOpt.get();
                    String publicId = cloudinaryService.generateUserPublicId(utente.getId(), email);
                    String cloudinaryUrl = cloudinaryService.uploadImage(percorsoFoto, publicId);
                    
                    if (cloudinaryUrl != null) {
                        percorsoFinale = cloudinaryUrl;
                        LOGGER.log(Level.INFO, "✅ Immagine caricata su Cloudinary per: {0}", email);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "⚠️ Fallback a percorso locale per: {0}, errore: {1}", 
                           new Object[]{email, e.getMessage()});
            }
        }
        
        // Aggiorna il database con il percorso (Cloudinary o locale)
        String sql = "UPDATE utente SET foto_profilo = ? WHERE email = ? AND attivo = TRUE";
        
        try (Connection connessione = ConnessioneDB.getConnessione();
             PreparedStatement statement = connessione.prepareStatement(sql)) {
            
            if (percorsoFinale == null || percorsoFinale.isEmpty()) {
                statement.setNull(1, Types.VARCHAR);
            } else {
                statement.setString(1, percorsoFinale);
            }
            statement.setString(2, email.trim().toLowerCase());
            
            int righeAggiornate = statement.executeUpdate();
            boolean successo = righeAggiornate > 0;
            
            if (successo) {
                LOGGER.log(Level.INFO, "✅ Foto profilo aggiornata per: {0}", email);
                invalidaCacheUtente(email);
            }
            
            return successo;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nell'aggiornamento foto profilo per: " + email, e);
            return false;
        }
    }
}
