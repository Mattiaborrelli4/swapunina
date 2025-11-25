package application.DB;

import application.servic;
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

    // Inserisce un annuncio completo con oggetto associato
    public int inserisciAnnuncioComplessivo(Annuncio annuncio, int venditoreId) {
        annuncio.setVenditoreId(venditoreId);

        // Assicurati che l'oggetto abbia l'origine impostata
        if (annuncio.getOggetto().getOrigine() == null) {
            annuncio.getOggetto().setOrigine(OrigineOggetto.USATO);
        }

        int oggettoId = OggettoDAO.salvaOggetto(annuncio.getOggetto());

        if (oggettoId != -1) {
            try {
                int annuncioId = creaAnnuncioConValidazione(annuncio, oggettoId);
                return annuncioId;
            } catch (SQLException e) {
                System.err.println("Errore nella creazione dell'annuncio: " + e.getMessage());
                return -1;
            }
        } else {
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
            
            // CORREZIONE: L'imageUrl deve essere impostato sull'oggetto, non sull'annuncio
            String imageUrl = annuncio.getOggetto() != null ? annuncio.getOggetto().getImageUrl() : "";
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
        }
    }

    // Recupera un annuncio dal database tramite ID
    public Annuncio getAnnuncioById(int id) {
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
                    return annuncio;
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero dell'annuncio con ID " + id + ": " + e.getMessage());
        }
        return null;
    }

    // Recupera le caratteristiche speciali di un annuncio
    private List<String> getCaratteristiche(int annuncioId) {
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
        } catch (SQLException e) {
            System.err.println("Errore nel recupero delle caratteristiche per annuncio " + annuncioId + ": " + e.getMessage());
        }
        return caratteristiche;
    }

    // Recupera gli annunci attivi
public List<Annuncio> getAnnunciAttivi() {
    List<Annuncio> annunci = new ArrayList<>();
    
    String sql = "SELECT a.id AS annuncio_id, a.titolo, u.nome AS nome_venditore " +
                 "FROM annuncio a " +
                 "JOIN utente u ON a.venditore_id = u.id " +
                 "WHERE a.stato = 'ATTIVO'"; // ✅ Solo annunci ATTIVI (non CONSEGNATI)

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
    } catch (SQLException e) {
        System.err.println("Errore nel recupero degli annunci attivi: " + e.getMessage());
    }
    return annunci;
}

    // Aggiorna lo stato di un annuncio
    public boolean aggiornaStatoAnnuncio(int annuncioId, String nuovoStato) {
        String sql = "UPDATE " + TABLE_NAME + " SET stato = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuovoStato);
            stmt.setInt(2, annuncioId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Stato annuncio " + annuncioId + " aggiornato a: " + nuovoStato);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento dello stato dell'annuncio " + annuncioId + ": " + e.getMessage());
            return false;
        }
    }

    
    // Recupera l'ID utente tramite matricola
    public int getIdUtenteByMatricola(String matricola) {
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
        } catch (SQLException e) {
            System.err.println("Errore nel recupero dell'ID utente per matricola " + matricola + ": " + e.getMessage());
            return -1;
        }
    }

    // Verifica se l'annuncio ha un'immagine
    public boolean hasImmagine(int annuncioId) {
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
        } catch (SQLException e) {
            System.err.println("Errore nella verifica dell'immagine per annuncio " + annuncioId + ": " + e.getMessage());
        }
        return false;
    }
    
    // CORREZIONE COMPLETA: Metodo helper per mappare ResultSet ad Annuncio
    private Annuncio mapResultSetToAnnuncio(ResultSet rs) throws SQLException {
        try {
            // Recupera i dati base dell'annuncio
            int idAnnuncio = rs.getInt("annuncio_id");
            String titolo = rs.getString("titolo");
            double prezzo = rs.getDouble("prezzo");
            boolean inEvidenza = rs.getBoolean("in_evidenza");
            String tipologiaStr = rs.getString("tipologia");
            String modalitaConsegna = rs.getString("modalita_consegna");
            String stato = rs.getString("stato");
            int venditoreId = rs.getInt("venditore_id");
            Timestamp dataPubblicazione = rs.getTimestamp("data_pubblicazione");
            String imageUrl = rs.getString("image_url");
            String descrizione = rs.getString("descrizione");
            String nomeVenditore = rs.getString("nome_venditore");

            // Mappa la tipologia
            Tipologia tipologia;
            try {
                tipologia = Tipologia.valueOf(tipologiaStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Tipologia non riconosciuta: " + tipologiaStr + ", usando DEFAULT");
                tipologia = Tipologia.VENDITA;
            }
            
            // Recupera i dati dell'oggetto
            int idOggetto = rs.getInt("oggetto_id");
            String nomeOggetto = rs.getString("oggetto_nome");
            String descrizioneOggetto = rs.getString("oggetto_descrizione");
            int categoriaId = rs.getInt("categoria_id");
            String oggettoImageUrl = rs.getString("oggetto_image_url");
            String origineStr = rs.getString("origine");
            
            // Mappa la categoria da ID a enum
            Categoria categoria = fromIntCategoria(categoriaId);
            
            // Mappa l'origine dell'oggetto
            OrigineOggetto origineOggetto;
            try {
                origineOggetto = OrigineOggetto.valueOf(origineStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Origine oggetto non riconosciuta: " + origineStr + ", usando USATO");
                origineOggetto = OrigineOggetto.USATO;
            }
            
            // CORREZIONE: Crea l'oggetto con il costruttore corretto (File invece di List<String>)
            File immagineFile = null; // Se hai bisogno di un File, dovrai crearlo dall'URL
            Oggetto oggetto = new Oggetto(
                idOggetto,
                nomeOggetto,
                descrizioneOggetto,
                categoria,
                oggettoImageUrl, // usa imageUrl invece di lista immagini
                immagineFile,    // File invece di List<String>
                origineOggetto
            );
            
            // CORREZIONE: Crea l'annuncio con il costruttore corretto
            Annuncio annuncio = new Annuncio(
                oggetto,
                prezzo,
                tipologia,
                modalitaConsegna,
                venditoreId
            );
            
            // CORREZIONE: Imposta gli attributi usando i metodi corretti
            annuncio.setId(idAnnuncio); // usa setId invece di setIdAnnuncio
            annuncio.setTitolo(titolo);
            annuncio.setInEvidenza(inEvidenza);
            annuncio.setStato(stato);
            
            // CORREZIONE: Converti Timestamp in LocalDateTime
            if (dataPubblicazione != null) {
                annuncio.setDataPubblicazione(dataPubblicazione.toLocalDateTime());
            }
            
            annuncio.setDescrizione(descrizione);
            annuncio.setNomeVenditore(nomeVenditore); // usa setNomeVenditore invece di setNomeUtente
            
            // CORREZIONE: Non chiamare setImageUrl() perché non esiste nella classe Annuncio
            // L'immagine è gestita attraverso l'oggetto
            
            // Carica le caratteristiche speciali
            List<String> caratteristiche = getCaratteristiche(idAnnuncio);
            annuncio.setCaratteristicheSpeciali(caratteristiche);
            
            return annuncio;
            
        } catch (SQLException e) {
            System.err.println("Errore critico nel mapping ResultSet to Annuncio: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Metodo per convertire ID categoria in enum (per compatibilità)
   private Categoria fromIntCategoria(int id) {
    switch (id) {
        case 1: return Categoria.LIBRI;
        case 2: return Categoria.INFORMATICA;
        case 3: return Categoria.ABBIGLIAMENTO;
        case 4: return Categoria.ELETTRONICA;
        case 5: return Categoria.MUSICA;
        case 6: return Categoria.CASA;
        case 7: return Categoria.SPORT;
        case 8: return Categoria.GIOCATTOLI;
        case 9: return Categoria.ALTRO;
        default: return Categoria.ALTRO;
    }
}

    
    // Aggiorna un annuncio completo
    public boolean aggiornaAnnuncioCompleto(Annuncio annuncio) {
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
                    
                    // CORREZIONE: Usa l'imageUrl dell'oggetto, non dell'annuncio
                    String imageUrl = annuncio.getOggetto() != null ? annuncio.getOggetto().getImageUrl() : "";
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
                        return true;
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Errore nell'aggiornamento dell'annuncio: " + e.getMessage());
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Errore di connessione durante l'aggiornamento dell'annuncio: " + e.getMessage());
            return false;
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
    public List<Annuncio> getAnnunciPerVenditore(int venditoreId) {
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
        } catch (SQLException e) {
            System.err.println("Errore nel recupero degli annunci per venditore " + venditoreId + ": " + e.getMessage());
        }
        return annunci;
    }

    // Metodo per cercare annunci per titolo
    public List<Annuncio> cercaAnnunciPerTitolo(String query) {
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
        } catch (SQLException e) {
            System.err.println("Errore nella ricerca annunci per titolo '" + query + "': " + e.getMessage());
        }
        return annunci;
    }

    // Metodo per cercare annunci per categoria
    public List<Annuncio> cercaAnnunciPerCategoria(String categoria) {
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
        } catch (SQLException e) {
            System.err.println("Errore nella ricerca annunci per categoria '" + categoria + "': " + e.getMessage());
        }
        return annunci;
    }

    // Metodo per ottenere annunci in evidenza
    public List<Annuncio> getAnnunciInEvidenza() {
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
        } catch (SQLException e) {
            System.err.println("Errore nel recupero degli annunci in evidenza: " + e.getMessage());
        }
        return annunci;
    }

    // Metodo helper per convertire nome categoria in ID
    private int convertiCategoriaInId(String categoriaNome) {
    if (categoriaNome == null) return 9; // ALTRO

    switch (categoriaNome.toUpperCase().replace("🎵", "").trim()) {
        case "LIBRI": return 1;
        case "INFORMATICA": return 2;
        case "ABBIGLIAMENTO": return 3;
        case "ELETTRONICA": return 4;
        case "MUSICA": return 5;
        case "CASA": return 6;
        case "SPORT": return 7;
        case "GIOCATTOLI": return 8;
        default: return 9; // ALTRO come fallback
    }
}
    
    
    /**
     * Aggiorna l'immagine di un annuncio su Cloudinary
     */
    public boolean aggiornaImmagineAnnuncio(int annuncioId, String cloudinaryUrl) {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            // Prima recupera l'oggetto_id dall'annuncio
            String sqlSelect = "SELECT oggetto_id FROM annuncio WHERE id = ?";
            int oggettoId = -1;
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlSelect)) {
                stmt.setInt(1, annuncioId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        oggettoId = rs.getInt("oggetto_id");
                    }
                }
            }
            
            if (oggettoId == -1) {
                return false;
            }
            
            // Aggiorna l'URL dell'immagine nell'oggetto
            String sqlUpdate = "UPDATE oggetto SET image_url = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                stmt.setString(1, cloudinaryUrl);
                stmt.setInt(2, oggettoId);
                
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento dell'immagine per annuncio " + annuncioId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina l'immagine di un annuncio (sia da Cloudinary che dal database)
     */
    public boolean eliminaImmagineAnnuncio(int annuncioId) {
        try {
            // Prima recupera l'URL dell'immagine
            String imageUrl = getImageUrlAnnuncio(annuncioId);
            
            if (imageUrl != null && imageUrl.contains("cloudinary")) {
                // Elimina da Cloudinary
                servic cloudinaryService = new servic();
                cloudinaryService.eliminaImmagine(imageUrl);
            }
            
            // Aggiorna il database impostando image_url a NULL
            return aggiornaImmagineAnnuncio(annuncioId, null);
            
        } catch (Exception e) {
            System.err.println("Errore nell'eliminazione dell'immagine per annuncio " + annuncioId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Recupera l'URL dell'immagine di un annuncio
     */
    private String getImageUrlAnnuncio(int annuncioId) {
        String sql = "SELECT o.image_url FROM annuncio a JOIN oggetto o ON a.oggetto_id = o.id WHERE a.id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annuncioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("image_url");
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero dell'URL immagine per annuncio " + annuncioId + ": " + e.getMessage());
        }
        return null;
    }

    /**
 * Registra un acquisto senza cambiare lo stato dell'annuncio a VENDUTO
 * L'annuncio rimane ATTIVO fino alla verifica del codice
 */
public boolean registraAcquisto(int annuncioId, int acquirenteId, String codiceSicurezza) {
    String sql = "INSERT INTO acquisti (annuncio_id, acquirente_id, codice_sicurezza, data_acquisto) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
    
    try (Connection conn = ConnessioneDB.getConnessione();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setInt(1, annuncioId);
        stmt.setInt(2, acquirenteId);
        stmt.setString(3, codiceSicurezza);
        
        int affectedRows = stmt.executeUpdate();
        return affectedRows > 0;
        
    } catch (SQLException e) {
        System.err.println("Errore nella registrazione dell'acquisto: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

/**
 * Verifica un codice di sicurezza e elimina l'annuncio se corretto
 */
public boolean verificaCodiceEEliminaAnnuncio(int annuncioId, String codice) {
    // Prima verifica che il codice sia corretto per questo annuncio
    String sqlVerifica = "SELECT COUNT(*) FROM acquisti WHERE annuncio_id = ? AND codice_sicurezza = ?";
    
    try (Connection conn = ConnessioneDB.getConnessione();
         PreparedStatement stmt = conn.prepareStatement(sqlVerifica)) {
        
        stmt.setInt(1, annuncioId);
        stmt.setString(2, codice);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next() && rs.getInt(1) > 0) {
            // Codice corretto - elimina l'annuncio usando il metodo esistente
            return eliminaAnnuncioCompleto(annuncioId);
        } else {
            // Codice errato
            return false;
        }
        
    } catch (SQLException e) {
        System.err.println("Errore nella verifica del codice: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

/**
 * Elimina definitivamente un annuncio dal database (inclusi acquisti correlati)
 */
// ✅ MANTIENI QUESTO METODO (il primo che appare)
/**
 * Elimina definitivamente un annuncio dal database (inclusi acquisti correlati)
 */
public boolean eliminaAnnuncioCompleto(int annuncioId) {
    String sqlDeleteAcquisti = "DELETE FROM acquisti WHERE annuncio_id = ?";
    String sqlDeleteAnnuncio = "DELETE FROM annuncio WHERE id = ?";
    
    try (Connection conn = ConnessioneDB.getConnessione()) {
        conn.setAutoCommit(false);
        
        try (PreparedStatement stmtAcquisti = conn.prepareStatement(sqlDeleteAcquisti);
             PreparedStatement stmtAnnuncio = conn.prepareStatement(sqlDeleteAnnuncio)) {
            
            // Elimina acquisti
            stmtAcquisti.setInt(1, annuncioId);
            stmtAcquisti.executeUpdate();
            
            // Elimina annuncio
            stmtAnnuncio.setInt(1, annuncioId);
            int affectedRows = stmtAnnuncio.executeUpdate();
            
            conn.commit();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
        
    } catch (SQLException e) {
        System.err.println("Errore nell'eliminazione dell'annuncio: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}


/**
 * Verifica se esiste un acquisto pendente per l'annuncio
 */
public boolean isAcquistoPendente(int annuncioId) {
    String sql = "SELECT COUNT(*) FROM acquisti WHERE annuncio_id = ?";
    
    try (Connection conn = ConnessioneDB.getConnessione();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setInt(1, annuncioId);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
    } catch (SQLException e) {
        System.err.println("Errore nel verificare l'acquisto pendente per l'annuncio " + annuncioId);
        e.printStackTrace();
    }
    return false;
}


public boolean eliminaAnnuncio(int annuncioId) {
    return eliminaAnnuncioCompleto(annuncioId); // Delega al metodo completo
}


}
