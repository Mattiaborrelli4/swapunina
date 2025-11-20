package application.DB;

import application.servic;
import application.Classe.Annuncio;
import application.Classe.Oggetto;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnnuncioDAO {
    private static final String TABLE_NAME = "annuncio";
    private static final String CARATTERISTICHE_TABLE = "annuncio_caratteristica";

    /**
     * Inserisce un annuncio completo nel database con validazione
     */
    public int inserisciAnnuncioComplessivo(Annuncio annuncio, int venditoreId) {
        annuncio.setVenditoreId(venditoreId);

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

    /**
     * Crea un annuncio con gestione delle eccezioni per tipologia mancante
     */
    public int creaAnnuncioConValidazione(Annuncio annuncio, int oggettoId) throws SQLException {
        if (annuncio.getTipologia() == null) {
            throw new SQLException("Tipologia annuncio è null!");
        }

        String tipologiaDB = annuncio.getTipologia().name();

        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            try {
                int id = inserisciAnnuncio(conn, annuncio, oggettoId, tipologiaDB);
                conn.commit();
                return id;
            } catch (SQLException e) {
                if (e.getMessage().contains("violates foreign key constraint") && e.getMessage().contains("tipologia")) {
                    String insertTipologiaSQL = "INSERT INTO tipologia (nome) VALUES (?) ON CONFLICT DO NOTHING";
                    try (PreparedStatement stmt = conn.prepareStatement(insertTipologiaSQL)) {
                        stmt.setString(1, tipologiaDB);
                        stmt.executeUpdate();
                    }
                    int id = inserisciAnnuncio(conn, annuncio, oggettoId, tipologiaDB);
                    conn.commit();
                    return id;
                } else {
                    conn.rollback();
                    throw e;
                }
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Inserisce l'annuncio nel database e le sue caratteristiche
     */
    private int inserisciAnnuncio(Connection conn, Annuncio annuncio, int oggettoId, String tipologiaDB) throws SQLException {
        String sql = "INSERT INTO annuncio (titolo, oggetto_id, prezzo, in_evidenza, tipologia, modalita_consegna, stato, venditore_id, data_pubblicazione, image_url, descrizione) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            String titolo = annuncio.getTitolo();
            if (titolo == null || titolo.trim().isEmpty()) {
                titolo = annuncio.getOggetto() != null ? annuncio.getOggetto().getNome() : "Senza titolo";
            }
            
            stmt.setString(1, titolo);
            stmt.setInt(2, oggettoId);
            stmt.setDouble(3, annuncio.getPrezzo());
            stmt.setBoolean(4, annuncio.isInEvidenza());
            stmt.setString(5, tipologiaDB);
            stmt.setString(6, annuncio.getModalitaConsegna());
            stmt.setString(7, annuncio.getStato());
            stmt.setInt(8, annuncio.getVenditoreId());
            stmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now()));
            
            String imageUrl = annuncio.getOggetto() != null ? annuncio.getOggetto().getImageUrl() : "";
            if (imageUrl == null || imageUrl.isEmpty()) {
                stmt.setNull(10, Types.VARCHAR);
            } else {
                stmt.setString(10, imageUrl);
            }

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
                    return annuncioId;
                }
            }
        }
        throw new SQLException("Inserimento annuncio fallito.");
    }

    /**
     * Inserisce le caratteristiche speciali dell'annuncio
     */
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

    /**
     * Recupera un annuncio completo dal database tramite ID
     */
    public Annuncio getAnnuncioById(int id) {
        String sql = "SELECT " +
                   "a.id AS annuncio_id, a.titolo, a.prezzo, a.in_evidenza, a.tipologia, " +
                   "a.modalita_consegna, a.stato, a.venditore_id, a.data_pubblicazione, " +
                   "a.image_url, a.descrizione, " +
                   "o.id AS oggetto_id, o.nome AS oggetto_nome, o.descrizione AS oggetto_descrizione, " +
                   "o.categoria_id, o.image_url AS oggetto_image_url, o.origine, " +
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

    /**
     * Recupera le caratteristiche speciali di un annuncio
     */
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

    /**
     * Recupera tutti gli annunci attivi
     */
    public List<Annuncio> getAnnunciAttivi() {
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

    /**
     * Aggiorna lo stato di un annuncio
     */
    public boolean aggiornaStatoAnnuncio(int annuncioId, String nuovoStato) {
        String sql = "UPDATE " + TABLE_NAME + " SET stato = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuovoStato);
            stmt.setInt(2, annuncioId);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento dello stato dell'annuncio " + annuncioId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina un annuncio dal database
     */
    public boolean eliminaAnnuncio(int annuncioId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, annuncioId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore nell'eliminazione dell'annuncio " + annuncioId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Recupera l'ID utente tramite matricola
     */
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

    /**
     * Verifica se un annuncio ha un'immagine associata
     */
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
    
    /**
     * Mappa un ResultSet in un oggetto Annuncio
     */
    private Annuncio mapResultSetToAnnuncio(ResultSet rs) throws SQLException {
        try {
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

            Tipologia tipologia;
            try {
                tipologia = Tipologia.valueOf(tipologiaStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                tipologia = Tipologia.VENDITA;
            }
            
            int idOggetto = rs.getInt("oggetto_id");
            String nomeOggetto = rs.getString("oggetto_nome");
            String descrizioneOggetto = rs.getString("oggetto_descrizione");
            int categoriaId = rs.getInt("categoria_id");
            String oggettoImageUrl = rs.getString("oggetto_image_url");
            String origineStr = rs.getString("origine");
            
            Categoria categoria = fromIntCategoria(categoriaId);
            
            OrigineOggetto origineOggetto;
            try {
                origineOggetto = OrigineOggetto.valueOf(origineStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                origineOggetto = OrigineOggetto.USATO;
            }
            
            File immagineFile = null;
            Oggetto oggetto = new Oggetto(
                idOggetto,
                nomeOggetto,
                descrizioneOggetto,
                categoria,
                oggettoImageUrl,
                immagineFile,
                origineOggetto
            );
            
            Annuncio annuncio = new Annuncio(
                oggetto,
                prezzo,
                tipologia,
                modalitaConsegna,
                venditoreId
            );
            
            annuncio.setId(idAnnuncio);
            annuncio.setTitolo(titolo);
            annuncio.setInEvidenza(inEvidenza);
            annuncio.setStato(stato);
            
            if (dataPubblicazione != null) {
                annuncio.setDataPubblicazione(dataPubblicazione.toLocalDateTime());
            }
            
            annuncio.setDescrizione(descrizione);
            annuncio.setNomeVenditore(nomeVenditore);
            
            List<String> caratteristiche = getCaratteristiche(idAnnuncio);
            annuncio.setCaratteristicheSpeciali(caratteristiche);
            
            return annuncio;
            
        } catch (SQLException e) {
            System.err.println("Errore critico nel mapping ResultSet to Annuncio: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Converte un ID categoria nel corrispondente enum Categoria
     */
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
    
    /**
     * Aggiorna un annuncio completo nel database
     */
    public boolean aggiornaAnnuncioCompleto(Annuncio annuncio) {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            try {
                OggettoDAO oggettoDAO = new OggettoDAO();
                boolean oggettoAggiornato = oggettoDAO.aggiornaOggetto(annuncio.getOggetto());
                
                if (!oggettoAggiornato) {
                    conn.rollback();
                    return false;
                }

                String sql = "UPDATE annuncio SET titolo = ?, prezzo = ?, in_evidenza = ?, tipologia = ?, " +
                             "modalita_consegna = ?, stato = ?, image_url = ?, descrizione = ? WHERE id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, annuncio.getTitolo());
                    stmt.setDouble(2, annuncio.getPrezzo());
                    stmt.setBoolean(3, annuncio.isInEvidenza());
                    stmt.setString(4, annuncio.getTipologia().name());
                    stmt.setString(5, annuncio.getModalitaConsegna());
                    stmt.setString(6, annuncio.getStato());
                    
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

    /**
     * Aggiorna le caratteristiche speciali di un annuncio
     */
    private void aggiornaCaratteristiche(Connection conn, int annuncioId, List<String> caratteristiche) throws SQLException {
        String deleteSql = "DELETE FROM " + CARATTERISTICHE_TABLE + " WHERE annuncio_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, annuncioId);
            stmt.executeUpdate();
        }
        
        inserisciCaratteristiche(conn, annuncioId, caratteristiche);
    }

    /**
     * Recupera tutti gli annunci attivi di un venditore
     */
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

    /**
     * Cerca annunci per titolo (ricerca case-insensitive)
     */
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

    /**
     * Cerca annunci per categoria
     */
    public List<Annuncio> cercaAnnunciPerCategoria(String categoria) {
        List<Annuncio> annunci = new ArrayList<>();
        
        String sql = "SELECT a.id AS annuncio_id, a.titolo " +
                     "FROM annuncio a " +
                     "JOIN oggetto o ON a.oggetto_id = o.id " +
                     "WHERE o.categoria_id = ? AND a.stato = 'ATTIVO'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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

    /**
     * Recupera tutti gli annunci in evidenza
     */
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

    /**
     * Converte il nome di una categoria nel suo ID corrispondente
     */
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
    
    /**
     * Aggiorna l'immagine di un annuncio con un URL Cloudinary
     */
    public boolean aggiornaImmagineAnnuncio(int annuncioId, String cloudinaryUrl) {
        try (Connection conn = ConnessioneDB.getConnessione()) {
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
     * Elimina l'immagine di un annuncio sia da Cloudinary che dal database
     */
    public boolean eliminaImmagineAnnuncio(int annuncioId) {
        try {
            String imageUrl = getImageUrlAnnuncio(annuncioId);
            
            if (imageUrl != null && imageUrl.contains("cloudinary")) {
                servic cloudinaryService = new servic();
                cloudinaryService.eliminaImmagine(imageUrl);
            }
            
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
}
