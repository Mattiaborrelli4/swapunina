package application.DB;

import application.Classe.Annuncio;
import application.Classe.Oggetto;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;
import application.Enum.StatoAnnuncio;
import application.Enum.Tipologia;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnnuncioDAO {
    private static final String TABLE_NAME = "annuncio";
    private static final String CARATTERISTICHE_TABLE = "annuncio_caratteristica";

    // Costruttore che verifica e crea le tabelle se mancanti
    public AnnuncioDAO() {
        creaTabelleSeMancanti();
    }

    // Crea le tabelle annuncio e annuncio_caratteristica se non esistono
    private void creaTabelleSeMancanti() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            String sqlAnnuncio = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "titolo VARCHAR(255) NOT NULL, " +
                    "oggetto_id INTEGER NOT NULL REFERENCES oggetto(id) ON DELETE CASCADE, " +
                    "prezzo DECIMAL(10,2) NOT NULL, " +
                    "in_evidenza BOOLEAN NOT NULL DEFAULT false, " +
                    "tipologia VARCHAR(50) NOT NULL CHECK (tipologia IN ('VENDITA', 'SCAMBIO', 'REGALO', 'ASTA')), " +
                    "modalita_consegna VARCHAR(100) NOT NULL, " +
                    "stato VARCHAR(50) NOT NULL DEFAULT 'ATTIVO', " +
                    "CHECK (stato IN ('ATTIVO', 'VENDUTO', 'RITIRATO', 'SCADUTO')), " +
                    "venditore_id INTEGER NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                    "data_pubblicazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "image_url TEXT, " +
                    "descrizione TEXT)";

            String sqlCaratteristiche = "CREATE TABLE IF NOT EXISTS " + CARATTERISTICHE_TABLE + " (" +
                    "annuncio_id INTEGER NOT NULL REFERENCES " + TABLE_NAME + "(id) ON DELETE CASCADE, " +
                    "caratteristica VARCHAR(255) NOT NULL, " +
                    "valore VARCHAR(255), " +
                    "PRIMARY KEY (annuncio_id, caratteristica))";

            try (PreparedStatement stmt1 = conn.prepareStatement(sqlAnnuncio);
                 PreparedStatement stmt2 = conn.prepareStatement(sqlCaratteristiche)) {
                stmt1.execute();
                stmt2.execute();
                System.out.println("✅ Tabelle annuncio verificate/create");
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella creazione delle tabelle per Annuncio");
            e.printStackTrace();
        }
    }

    // Inserisce un annuncio completo con oggetto associato
    public int inserisciAnnuncioComplessivo(Annuncio annuncio, int venditoreId) throws SQLException {
        annuncio.setVenditoreId(venditoreId);

        // Assicurati che l'oggetto abbia l'origine impostata
        if (annuncio.getOggetto().getOrigine() == null) {
            annuncio.getOggetto().setOrigine(OrigineOggetto.USATO);
        }

        // ✅ DEBUG prima del salvataggio
        System.out.println("=== PRIMA DEL SALVATAGGIO ANNUNCIO ===");
        System.out.println("📝 Titolo annuncio: '" + annuncio.getTitolo() + "'");
        System.out.println("📦 Oggetto: " + annuncio.getOggetto());
        System.out.println("💰 Prezzo: " + annuncio.getPrezzo());
        System.out.println("🎯 Tipologia: " + annuncio.getTipologia());
        
        int oggettoId = OggettoDAO.salvaOggetto(annuncio.getOggetto());

        if (oggettoId != -1) {
            System.out.println("✅ Oggetto salvato con ID: " + oggettoId);
            int annuncioId = creaAnnuncioConValidazione(annuncio, oggettoId);
            System.out.println("✅ Annuncio salvato con ID: " + annuncioId);
            return annuncioId;
        } else {
            System.out.println("❌ Errore nel salvataggio dell'oggetto");
            return -1;
        }
    }

    // Crea un annuncio con validazione della tipologia
    public int creaAnnuncioConValidazione(Annuncio annuncio, int oggettoId) throws SQLException {
        if (annuncio.getTipologia() == null) {
            throw new SQLException("[ERRORE] Tipologia annuncio è null!");
        }

        String tipologiaDB = annuncio.getTipologia().name();

        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            try {
                // Prova ad inserire annuncio
                int id = inserisciAnnuncio(conn, annuncio, oggettoId, tipologiaDB);
                conn.commit();
                return id;
            } catch (SQLException e) {
                // Se errore FK tipologia non valida
                if (e.getMessage().contains("violates foreign key constraint") && e.getMessage().contains("tipologia")) {
                    // Inserisci la nuova tipologia nella tabella tipologia
                    String insertTipologiaSQL = "INSERT INTO tipologia (nome) VALUES (?) ON CONFLICT DO NOTHING";
                    try (PreparedStatement stmt = conn.prepareStatement(insertTipologiaSQL)) {
                        stmt.setString(1, tipologiaDB);
                        stmt.executeUpdate();
                    }
                    // Riprova inserimento annuncio
                    int id = inserisciAnnuncio(conn, annuncio, oggettoId, tipologiaDB);
                    conn.commit();
                    return id;
                } else {
                    conn.rollback();
                    throw e; // rilancia l'eccezione se non è errore tipologia
                }
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // Inserisce un annuncio nel database
    private int inserisciAnnuncio(Connection conn, Annuncio annuncio, int oggettoId, String tipologiaDB) throws SQLException {
        String sql = "INSERT INTO annuncio (titolo, oggetto_id, prezzo, in_evidenza, tipologia, modalita_consegna, stato, venditore_id, data_pubblicazione, image_url, descrizione) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // ✅ CORREZIONE CRUCIALE: Imposta il titolo dall'oggetto Annuncio
            String titolo = annuncio.getTitolo();
            if (titolo == null || titolo.trim().isEmpty()) {
                // Fallback: usa il nome dell'oggetto se il titolo è vuoto
                titolo = annuncio.getOggetto() != null ? annuncio.getOggetto().getNome() : "Senza titolo";
            }
            
            stmt.setString(1, titolo); // ✅ Titolo dalla tabella annuncio
            stmt.setInt(2, oggettoId);
            stmt.setDouble(3, annuncio.getPrezzo());
            stmt.setBoolean(4, annuncio.isInEvidenza());
            stmt.setString(5, tipologiaDB);
            stmt.setString(6, annuncio.getModalitaConsegna());
            stmt.setString(7, annuncio.getStato());
            stmt.setInt(8, annuncio.getVenditoreId());
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            
            // Gestione immagine null
            String imageUrl = annuncio.getImageUrlSafe();
            if (imageUrl == null || imageUrl.isEmpty()) {
                stmt.setNull(10, Types.VARCHAR);
            } else {
                stmt.setString(10, imageUrl);
            }

            // Gestione descrizione
            String descrizione = annuncio.getDescrizione();
            if (descrizione == null || descrizione.isEmpty()) {
                stmt.setNull(11, Types.VARCHAR);
            } else {
                stmt.setString(11, descrizione);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int annuncioId = rs.getInt("id");
                    inserisciCaratteristiche(conn, annuncioId, annuncio.getCaratteristicheSpeciali());
                    
                    // ✅ DEBUG: Verifica che il titolo sia stato salvato
                    System.out.println("✅ Annuncio salvato nel database:");
                    System.out.println("   ID: " + annuncioId);
                    System.out.println("   Titolo: '" + titolo + "'");
                    System.out.println("   Oggetto ID: " + oggettoId);
                    
                    return annuncioId;
                }
            }
        }
        throw new SQLException("Inserimento annuncio fallito.");
    }

    // Inserisce le caratteristiche speciali dell'annuncio
    private void inserisciCaratteristiche(Connection conn, int annuncioId, List<String> caratteristiche) throws SQLException {
        if (caratteristiche == null || caratteristiche.isEmpty()) return;

        String sql = "INSERT INTO " + CARATTERISTICHE_TABLE + " (annuncio_id, caratteristica) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (String caratteristica : caratteristiche) {
                stmt.setInt(1, annuncioId);
                stmt.setString(2, caratteristica);
                stmt.addBatch();
            }
            stmt.executeBatch();
            System.out.println("✅ Caratteristiche speciali inserite: " + caratteristiche.size());
        }
    }

    // Recupera un annuncio dal database tramite ID
    public Annuncio getAnnuncioById(int id) throws SQLException {
        // ✅ QUERY CORRETTA - usa o.categoria_id invece di o.categoria
        String sql = "SELECT " +
                   "a.id AS annuncio_id, a.titolo, a.prezzo, a.in_evidenza, a.tipologia, " +
                   "a.modalita_consegna, a.stato, a.venditore_id, a.data_pubblicazione, " +
                   "a.image_url, a.descrizione, " +
                   "o.id AS oggetto_id, o.nome AS oggetto_nome, o.descrizione AS oggetto_descrizione, " +
                   "o.categoria_id, o.image_url AS oggetto_image_url, o.origine, " + // ✅ CORRETTO: categoria_id
                   "u.nome AS nome_venditore " +
                   "FROM annuncio a " +
                   "JOIN oggetto o ON a.oggetto_id = o.id " +
                   "JOIN utente u ON a.venditore_id = u.id " +
                   "WHERE a.id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Annuncio annuncio = mapResultSetToAnnuncio(rs);
                    System.out.println("✅ Annuncio recuperato - Titolo: '" + annuncio.getTitolo() + "'");
                    return annuncio;
                }
            }
        }
        System.out.println("❌ Annuncio non trovato con ID: " + id);
        return null;
    }

    // Recupera le caratteristiche speciali di un annuncio
    private List<String> getCaratteristiche(int annuncioId) throws SQLException {
        List<String> caratteristiche = new ArrayList<>();
        String sql = "SELECT caratteristica FROM " + CARATTERISTICHE_TABLE + " WHERE annuncio_id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annuncioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    caratteristiche.add(rs.getString("caratteristica"));
                }
            }
        }
        return caratteristiche;
    }

    // Recupera gli annunci attivi
    public List<Annuncio> getAnnunciAttivi() throws SQLException {
        List<Annuncio> annunci = new ArrayList<>();
        
        String sql = "SELECT a.id AS annuncio_id, a.titolo, u.nome AS nome_venditore " +
                     "FROM annuncio a " +
                     "JOIN utente u ON a.venditore_id = u.id " +
                     "WHERE a.stato = 'ATTIVO'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int annuncioId = rs.getInt("annuncio_id");
                String titolo = rs.getString("titolo");
                System.out.println("📋 Caricamento annuncio ID: " + annuncioId + " - Titolo: '" + titolo + "'");
                
                Annuncio annuncio = getAnnuncioById(annuncioId);
                if (annuncio != null) {
                    annunci.add(annuncio);
                }
            }
        }
        System.out.println("✅ Caricati " + annunci.size() + " annunci attivi");
        return annunci;
    }

    // Aggiorna lo stato di un annuncio
    public boolean aggiornaStatoAnnuncio(int annuncioId, String nuovoStato) throws SQLException {
        String sql = "UPDATE " + TABLE_NAME + " SET stato = ? WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuovoStato);
            stmt.setInt(2, annuncioId);
            boolean risultato = stmt.executeUpdate() > 0;
            if (risultato) {
                System.out.println("✅ Stato annuncio " + annuncioId + " aggiornato a: " + nuovoStato);
            }
            return risultato;
        }
    }

    // Elimina un annuncio dal database
    public boolean eliminaAnnuncio(int annuncioId) throws SQLException {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annuncioId);
            boolean risultato = stmt.executeUpdate() > 0;
            if (risultato) {
                System.out.println("✅ Annuncio " + annuncioId + " eliminato");
            }
            return risultato;
        }
    }
    
    // Recupera l'ID utente tramite matricola
    public int getIdUtenteByMatricola(String matricola) throws SQLException {
        String query = "SELECT id FROM utente WHERE matricola = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, matricola);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    return -1;
                }
            }
        }
    }

    // Verifica se l'annuncio ha un'immagine
    public boolean hasImmagine(int annuncioId) throws SQLException {
        String sql = "SELECT image_url FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annuncioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String imageUrl = rs.getString("image_url");
                    return imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null");
                }
            }
        }
        return false;
    }
    
    // Metodo helper per mappare ResultSet ad Annuncio
    private Annuncio mapResultSetToAnnuncio(ResultSet rs) throws SQLException {
        // ✅ Il titolo ora viene SOLO dalla tabella annuncio
        String titolo = rs.getString("titolo");
        if (titolo == null || titolo.isEmpty()) {
            titolo = "Senza titolo";
        }

        // Converti le stringhe del DB negli enum corrispondenti
        String tipologiaStr = rs.getString("tipologia");
        Tipologia tipologia = Tipologia.valueOf(tipologiaStr);

        // ✅ CORREZIONE: Recupera categoria_id e converti in enum Categoria
        int categoriaId = rs.getInt("categoria_id");
        Categoria categoria = fromIntCategoria(categoriaId);

        String statoStr = rs.getString("stato");
        StatoAnnuncio stato = StatoAnnuncio.parseStato(statoStr);

        String origineStr = rs.getString("origine");
        OrigineOggetto origine = OrigineOggetto.parseOrigine(origineStr);

        // Gestione immagine
        String imageUrl = rs.getString("image_url");
        File immagineFile = null;
        
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            try {
                immagineFile = new File("C:/Users/matti/Desktop/project/" + imageUrl);
                if (!immagineFile.exists()) {
                    immagineFile = null;
                }
            } catch (Exception e) {
                System.err.println("Immagine non trovata: " + imageUrl);
                immagineFile = null;
            }
        }

        // Crea l'oggetto con il nome corretto
        Oggetto oggetto = new Oggetto(
            rs.getInt("oggetto_id"),
            rs.getString("oggetto_nome"), // ✅ Usa il nome dell'oggetto dal database
            rs.getString("oggetto_descrizione"),
            categoria, 
            rs.getString("oggetto_image_url"),
            immagineFile,
            origine
        );

        // Crea l'annuncio
        Annuncio annuncio = new Annuncio(
            oggetto,
            rs.getDouble("prezzo"),
            tipologia,
            rs.getString("modalita_consegna"),
            rs.getInt("venditore_id")
        );
        
        // ✅ IMPOSTA IL TITOLO RECUPERATO DAL DATABASE
        annuncio.setTitolo(titolo);
        annuncio.setDescrizione(rs.getString("descrizione"));
        annuncio.setNomeVenditore(rs.getString("nome_venditore"));

        annuncio.setId(rs.getInt("annuncio_id"));
        annuncio.setInEvidenza(rs.getBoolean("in_evidenza"));
        annuncio.setStato(stato.name());
        annuncio.setDataPubblicazione(rs.getTimestamp("data_pubblicazione").toLocalDateTime());
        annuncio.getCaratteristicheSpeciali().addAll(getCaratteristiche(annuncio.getId()));

        return annuncio;
    }

    // Metodo per convertire ID categoria in enum (per compatibilità)
    private Categoria fromIntCategoria(int id) {
        switch (id) {
            case 1: return Categoria.LIBRI;
            case 2: return Categoria.CASA;
            case 3: return Categoria.ABBIGLIAMENTO;
            case 4: return Categoria.ELETTRONICA;
            case 5: return Categoria.ALTRO;
            default: return Categoria.ALTRO;
        }
    }
    
    // Aggiorna un annuncio completo
    public boolean aggiornaAnnuncioCompleto(Annuncio annuncio) throws SQLException {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            try {
                // 1. Aggiorna prima l'oggetto associato
                OggettoDAO oggettoDAO = new OggettoDAO();
                boolean oggettoAggiornato = oggettoDAO.aggiornaOggetto(annuncio.getOggetto());
                
                if (!oggettoAggiornato) {
                    conn.rollback();
                    return false;
                }

                // 2. Aggiorna l'annuncio - ✅ ASSICURATI DI AGGIORNARE ANCHE IL TITOLO
                String sql = "UPDATE annuncio SET titolo = ?, prezzo = ?, in_evidenza = ?, tipologia = ?, " +
                             "modalita_consegna = ?, stato = ?, image_url = ?, descrizione = ? WHERE id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    // ✅ IMPORTANTE: Aggiorna anche il titolo
                    stmt.setString(1, annuncio.getTitolo());
                    stmt.setDouble(2, annuncio.getPrezzo());
                    stmt.setBoolean(3, annuncio.isInEvidenza());
                    stmt.setString(4, annuncio.getTipologia().name());
                    stmt.setString(5, annuncio.getModalitaConsegna());
                    stmt.setString(6, annuncio.getStato());
                    
                    String imageUrl = annuncio.getImageUrlSafe();
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        stmt.setNull(7, Types.VARCHAR);
                    } else {
                        stmt.setString(7, imageUrl);
                    }
                    
                    stmt.setString(8, annuncio.getDescrizione());
                    stmt.setInt(9, annuncio.getId());
                    
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        // 3. Aggiorna le caratteristiche
                        aggiornaCaratteristiche(conn, annuncio.getId(), annuncio.getCaratteristicheSpeciali());
                        conn.commit();
                        
                        System.out.println("✅ Annuncio aggiornato con titolo: '" + annuncio.getTitolo() + "'");
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return false;
    }

    private void aggiornaCaratteristiche(Connection conn, int annuncioId, List<String> caratteristiche) throws SQLException {
        // Prima elimina tutte le caratteristiche esistenti
        String deleteSql = "DELETE FROM " + CARATTERISTICHE_TABLE + " WHERE annuncio_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, annuncioId);
            stmt.executeUpdate();
        }
        
        // Poi inserisci le nuove caratteristiche
        inserisciCaratteristiche(conn, annuncioId, caratteristiche);
    }

    // Metodo per ottenere annunci per venditore
    public List<Annuncio> getAnnunciPerVenditore(int venditoreId) throws SQLException {
        List<Annuncio> annunci = new ArrayList<>();
        
        String sql = "SELECT a.id AS annuncio_id, a.titolo " +
                     "FROM annuncio a " +
                     "WHERE a.venditore_id = ? AND a.stato = 'ATTIVO'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int annuncioId = rs.getInt("annuncio_id");
                    Annuncio annuncio = getAnnuncioById(annuncioId);
                    if (annuncio != null) {
                        annunci.add(annuncio);
                    }
                }
            }
        }
        System.out.println("✅ Caricati " + annunci.size() + " annunci per venditore " + venditoreId);
        return annunci;
    }

    // Metodo per cercare annunci per titolo
    public List<Annuncio> cercaAnnunciPerTitolo(String query) throws SQLException {
        List<Annuncio> annunci = new ArrayList<>();
        
        String sql = "SELECT a.id AS annuncio_id, a.titolo " +
                     "FROM annuncio a " +
                     "WHERE LOWER(a.titolo) LIKE LOWER(?) AND a.stato = 'ATTIVO'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int annuncioId = rs.getInt("annuncio_id");
                    Annuncio annuncio = getAnnuncioById(annuncioId);
                    if (annuncio != null) {
                        annunci.add(annuncio);
                    }
                }
            }
        }
        System.out.println("✅ Trovati " + annunci.size() + " annunci per ricerca: '" + query + "'");
        return annunci;
    }

    // Metodo per cercare annunci per categoria
    public List<Annuncio> cercaAnnunciPerCategoria(String categoria) throws SQLException {
        List<Annuncio> annunci = new ArrayList<>();
        
        String sql = "SELECT a.id AS annuncio_id, a.titolo " +
                     "FROM annuncio a " +
                     "JOIN oggetto o ON a.oggetto_id = o.id " +
                     "WHERE o.categoria_id = ? AND a.stato = 'ATTIVO'"; // ✅ CORRETTO: usa categoria_id

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Converti il nome categoria in ID
            int categoriaId = convertiCategoriaInId(categoria);
            stmt.setInt(1, categoriaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int annuncioId = rs.getInt("annuncio_id");
                    Annuncio annuncio = getAnnuncioById(annuncioId);
                    if (annuncio != null) {
                        annunci.add(annuncio);
                    }
                }
            }
        }
        System.out.println("✅ Trovati " + annunci.size() + " annunci per categoria: '" + categoria + "'");
        return annunci;
    }

    // Metodo per ottenere annunci in evidenza
    public List<Annuncio> getAnnunciInEvidenza() throws SQLException {
        List<Annuncio> annunci = new ArrayList<>();
        
        String sql = "SELECT a.id AS annuncio_id, a.titolo " +
                     "FROM annuncio a " +
                     "WHERE a.in_evidenza = true AND a.stato = 'ATTIVO'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int annuncioId = rs.getInt("annuncio_id");
                Annuncio annuncio = getAnnuncioById(annuncioId);
                if (annuncio != null) {
                    annunci.add(annuncio);
                }
            }
        }
        System.out.println("✅ Caricati " + annunci.size() + " annunci in evidenza");
        return annunci;
    }

    // Metodo helper per convertire nome categoria in ID
    private int convertiCategoriaInId(String categoriaNome) {
        switch (categoriaNome.toUpperCase()) {
            case "LIBRI": return 1;
            case "CASA": return 2;
            case "ABBIGLIAMENTO": return 3;
            case "ELETTRONICA": return 4;
            case "ALTRO": return 5;
            default: return 5;
        }
    }
}