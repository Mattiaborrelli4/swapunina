package application.DB;

import application.Classe.Oggetto;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestisce le operazioni CRUD per gli oggetti nel database
 * Fornisce metodi per salvataggio, recupero e gestione degli oggetti
 */
public class OggettoDAO {
    private static final String TABLE_NAME = "oggetto";

    public OggettoDAO() {
        creaTabellaSeMancante();
    }

    /**
     * Crea la tabella oggetto se non esiste
     */
    private void creaTabellaSeMancante() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "nome VARCHAR(255) NOT NULL, " +
                "descrizione TEXT, " +
                "categoria_id INTEGER NOT NULL, " +
                "origine VARCHAR(50), " +
                "dettagli TEXT, " +
                "image_url VARCHAR(255) DEFAULT NULL)";

        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Errore creazione tabella oggetto: " + e.getMessage());
        }
    }

    /**
     * Salva un oggetto nel database e restituisce l'ID generato
     */
    public static int salvaOggetto(Oggetto oggetto) {
        String query = "INSERT INTO oggetto (nome, descrizione, categoria_id, image_url, origine) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id";
        
        try (Connection connection = ConnessioneDB.getConnessione();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setString(1, oggetto.getNome());
            stmt.setString(2, oggetto.getDescrizione());
            
            int categoriaId = convertiCategoriaInId(oggetto.getCategoria());
            stmt.setInt(3, categoriaId);
            
            String imageUrl = oggetto.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, imageUrl);
            }
            
            stmt.setString(5, oggetto.getOrigine().name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
            
        } catch (SQLException e) {
            System.err.println("Errore nel salvataggio oggetto: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Inserisce un oggetto usando una connessione esistente (per transazioni)
     */
    public int inserisciOggetto(Connection conn, Oggetto oggetto) throws SQLException {
        String sql = "INSERT INTO oggetto (nome, descrizione, categoria_id, image_url, origine) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, oggetto.getNome());
            stmt.setString(2, oggetto.getDescrizione());
            
            int categoriaId = convertiCategoriaInId(oggetto.getCategoria());
            stmt.setInt(3, categoriaId);
            
            String imageUrl = oggetto.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, imageUrl);
            }
            
            stmt.setString(5, oggetto.getOrigine().name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Inserimento oggetto fallito");
    }

    /**
     * Recupera un oggetto tramite ID
     */
    public Oggetto getOggettoById(int id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapResultSetToOggetto(rs) : null;
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero oggetto per id: " + id);
            return null;
        }
    }

    /**
     * Converte un ResultSet in un oggetto Oggetto
     */
    private Oggetto mapResultSetToOggetto(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nome = rs.getString("nome");
        String descrizione = rs.getString("descrizione");
        
        int categoriaId = rs.getInt("categoria_id");
        Categoria categoria = fromIntCategoria(categoriaId);

        String imageUrl = rs.getString("image_url");
        String dettagliStr = rs.getString("dettagli");

        File immagineFile = caricaFileImmagine(imageUrl);

        String origineStr = rs.getString("origine");
        OrigineOggetto origine = OrigineOggetto.parseOrigine(origineStr);

        Oggetto oggetto = new Oggetto(id, nome, descrizione, categoria, imageUrl, immagineFile, origine);

        if (dettagliStr != null && !dettagliStr.isEmpty()) {
            Map<String, String> dettagli = parseDettagli(dettagliStr);
            for (Map.Entry<String, String> entry : dettagli.entrySet()) {
                oggetto.aggiungiDettaglio(entry.getKey(), entry.getValue());
            }
        }

        return oggetto;
    }

    /**
     * Carica il file immagine dal percorso specificato
     */
    private File caricaFileImmagine(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            try {
                File file = new File("C:/Users/matti/Desktop/project" + imageUrl);
                return file.exists() ? file : null;
            } catch (Exception e) {
                System.err.println("Errore caricamento immagine: " + imageUrl);
            }
        }
        return null;
    }

    /**
     * Parsa la stringa dei dettagli in una mappa chiave-valore
     */
    private Map<String, String> parseDettagli(String dettagliStr) {
        Map<String, String> dettagli = new HashMap<>();
        if (dettagliStr != null && !dettagliStr.isEmpty()) {
            String[] entries = dettagliStr.split(";");
            for (String entry : entries) {
                if (!entry.isEmpty()) {
                    String[] parts = entry.split("=", 2);
                    if (parts.length == 2) {
                        String key = parts[0];
                        String value = parts[1].replace("\\;", ";");
                        dettagli.put(key, value);
                    }
                }
            }
        }
        return dettagli;
    }

    /**
     * Recupera tutti gli oggetti dal database
     */
    public List<Oggetto> getTuttiOggetti() {
        List<Oggetto> oggetti = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME;

        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                oggetti.add(mapResultSetToOggetto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero oggetti: " + e.getMessage());
        }
        return oggetti;
    }

    /**
     * Verifica se un oggetto ha un'immagine associata
     */
    public boolean hasImmagine(int oggettoId) {
        String sql = "SELECT image_url FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, oggettoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String imageUrl = rs.getString("image_url");
                    return imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null");
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore verifica immagine oggetto: " + oggettoId);
        }
        return false;
    }
    
    /**
     * Recupera l'ID di un oggetto esistente con lo stesso nome e categoria
     */
    public static int recuperaIdOggettoEsistente(Oggetto oggettoDaCercare) {
        if (oggettoDaCercare == null || oggettoDaCercare.getNome() == null) {
            return -1;
        }
        
        String query = "SELECT id FROM oggetto WHERE nome = ? AND categoria_id = ? LIMIT 1";
        
        try (Connection connection = ConnessioneDB.getConnessione();
             PreparedStatement stmt = connection.prepareStatement(query)) {
            
            stmt.setString(1, oggettoDaCercare.getNome());
            int categoriaId = convertiCategoriaInId(oggettoDaCercare.getCategoria());
            stmt.setInt(2, categoriaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("id") : -1;
            }
            
        } catch (SQLException e) {
            System.err.println("Errore nel recupero ID oggetto: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Aggiorna un oggetto esistente nel database
     */
    public boolean aggiornaOggetto(Oggetto oggetto) throws SQLException {
        String sql = "UPDATE oggetto SET nome = ?, descrizione = ?, categoria_id = ?, image_url = ?, origine = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, oggetto.getNome());
            stmt.setString(2, oggetto.getDescrizione());
            
            int categoriaId = convertiCategoriaInId(oggetto.getCategoria());
            stmt.setInt(3, categoriaId);
            
            String imageUrl = oggetto.getImageUrl();
            if (imageUrl == null || imageUrl.isEmpty()) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, imageUrl);
            }
            
            stmt.setString(5, oggetto.getOrigine().name());
            stmt.setInt(6, oggetto.getId());
            
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Elimina un oggetto dal database
     */
    public boolean eliminaOggetto(int oggettoId) throws SQLException {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, oggettoId);
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Cerca oggetti per nome (ricerca case-insensitive)
     */
    public List<Oggetto> cercaOggettiPerNome(String nome) {
        List<Oggetto> oggetti = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE LOWER(nome) LIKE LOWER(?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + nome + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    oggetti.add(mapResultSetToOggetto(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore ricerca oggetti per nome: " + nome);
        }
        return oggetti;
    }

    /**
     * Cerca oggetti per categoria
     */
    public List<Oggetto> cercaOggettiPerCategoria(Categoria categoria) {
        List<Oggetto> oggetti = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE categoria_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            int categoriaId = convertiCategoriaInId(categoria);
            stmt.setInt(1, categoriaId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    oggetti.add(mapResultSetToOggetto(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore ricerca oggetti per categoria: " + categoria);
        }
        return oggetti;
    }

    /**
     * Converte ID categoria in enum Categoria
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
     * Converte enum Categoria in ID numerico per database
     */
    private static int convertiCategoriaInId(Categoria categoria) {
        switch (categoria) {
            case LIBRI: return 1;
            case CASA: return 2;
            case ABBIGLIAMENTO: return 3;
            case ELETTRONICA: return 4;
            case ALTRO: return 5;
            default: return 5;
        }
    }

    /**
     * Aggiorna solo l'URL dell'immagine di un oggetto
     */
    public boolean aggiornaImmagineOggetto(int oggettoId, String imageUrl) throws SQLException {
        String sql = "UPDATE " + TABLE_NAME + " SET image_url = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (imageUrl == null || imageUrl.isEmpty()) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, imageUrl);
            }
            stmt.setInt(2, oggettoId);
            
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * Conta il numero totale di oggetti nel database
     */
    public int contaOggettiTotali() {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME;
        
        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            System.err.println("Errore conteggio oggetti: " + e.getMessage());
            return 0;
        }
    }
    
    
    /**
     * Aggiorna l'URL dell'immagine Cloudinary per un oggetto
     */
    public boolean aggiornaImmagineCloudinary(int oggettoId, String cloudinaryUrl) {
        String sql = "UPDATE oggetto SET image_url = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
                stmt.setNull(1, Types.VARCHAR);
            } else {
                stmt.setString(1, cloudinaryUrl);
            }
            stmt.setInt(2, oggettoId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento dell'immagine Cloudinary per oggetto " + oggettoId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Recupera oggetti con immagini Cloudinary
     */
    public List<Oggetto> getOggettiConImmaginiCloudinary() {
        List<Oggetto> oggetti = new ArrayList<>();
        String sql = "SELECT * FROM oggetto WHERE image_url LIKE '%cloudinary%'";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                oggetti.add(mapResultSetToOggetto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero oggetti Cloudinary: " + e.getMessage());
        }
        return oggetti;
    }
}
