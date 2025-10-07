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

public class OggettoDAO {
    private static final String TABLE_NAME = "oggetto";

    public OggettoDAO() {
        creaTabellaSeMancante();
    }

    private void creaTabellaSeMancante() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "nome VARCHAR(255) NOT NULL, " +
                "descrizione TEXT, " +
                "categoria_id INTEGER NOT NULL, " + // ✅ CORRETTO: categoria_id invece di categoria
                "origine VARCHAR(50), " +
                "dettagli TEXT, " +
                "image_url VARCHAR(255) DEFAULT NULL)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
                System.out.println("Tabella " + TABLE_NAME + " verificata/creata");
            }
        } catch (SQLException e) {
            System.err.println("Errore verifica/creazione tabella " + TABLE_NAME);
            e.printStackTrace();
        }
    }

    /**
     * Salva un oggetto nel database (metodo statico per compatibilità)
     */
    public static int salvaOggetto(Oggetto oggetto) {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            // Query aggiornata con categoria_id
            String query = "INSERT INTO oggetto (nome, descrizione, categoria_id, image_url, origine) " +
                         "VALUES (?, ?, ?, ?, ?) RETURNING id";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, oggetto.getNome());
            stmt.setString(2, oggetto.getDescrizione());
            
            // ✅ CORREZIONE: Converti categoria enum in ID numerico
            int categoriaId = convertiCategoriaInId(oggetto.getCategoria());
            stmt.setInt(3, categoriaId);
            
            // Gestione immagine (può essere null)
            String imageUrl = oggetto.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                stmt.setNull(4, Types.VARCHAR);
            } else {
                stmt.setString(4, imageUrl);
            }
            
            // Origine come stringa
            stmt.setString(5, oggetto.getOrigine().name());
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int nuovoId = rs.getInt("id");
                System.out.println("✅ Oggetto salvato con ID: " + nuovoId);
                return nuovoId;
            }
            
            return -1;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nel salvataggio oggetto: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Inserisce un oggetto con connessione esistente (per transazioni)
     */
    public int inserisciOggetto(Connection conn, Oggetto oggetto) throws SQLException {
        String sql = "INSERT INTO oggetto (nome, descrizione, categoria_id, image_url, origine) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, oggetto.getNome());
            stmt.setString(2, oggetto.getDescrizione());
            
            // ✅ CORREZIONE: Converti categoria enum in ID numerico
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

    public Oggetto getOggettoById(int id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOggetto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero oggetto per id: " + id);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Mappa un ResultSet a un oggetto Oggetto
     */
    private Oggetto mapResultSetToOggetto(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String nome = rs.getString("nome");
        String descrizione = rs.getString("descrizione");
        
        // ✅ CORREZIONE: Recupera categoria_id e converti in enum
        int categoriaId = rs.getInt("categoria_id");
        Categoria categoria = fromIntCategoria(categoriaId);

        String imageUrl = rs.getString("image_url");
        String dettagliStr = rs.getString("dettagli");

        // Gestione file immagine
        File immagineFile = null;
        if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
            try {
                // Percorso corretto per l'immagine
                immagineFile = new File("C:/Users/matti/Desktop/project" + imageUrl);
                if (!immagineFile.exists()) {
                    immagineFile = null;
                    System.err.println("⚠️ Immagine non trovata: " + imageUrl);
                }
            } catch (Exception e) {
                System.err.println("❌ Errore caricamento immagine: " + imageUrl);
                immagineFile = null;
            }
        }

        // Conversione origine da stringa a enum
        String origineStr = rs.getString("origine");
        OrigineOggetto origine = OrigineOggetto.parseOrigine(origineStr);

        // Creazione oggetto
        Oggetto oggetto = new Oggetto(id, nome, descrizione, categoria, imageUrl, immagineFile, origine);

        // Aggiunta dettagli se presenti
        if (dettagliStr != null && !dettagliStr.isEmpty()) {
            Map<String, String> dettagli = parseDettagli(dettagliStr);
            for (Map.Entry<String, String> entry : dettagli.entrySet()) {
                oggetto.aggiungiDettaglio(entry.getKey(), entry.getValue());
            }
        }

        return oggetto;
    }

    /**
     * Parsa i dettagli dalla stringa del database
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
            System.err.println("Errore recupero oggetti");
            e.printStackTrace();
        }
        return oggetti;
    }

    /**
     * Verifica se un oggetto ha un'immagine
     */
    public boolean hasImmagine(int oggettoId) {
        String sql = "SELECT image_url FROM " + TABLE_NAME + " WHERE id = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, oggettoId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String imageUrl = rs.getString("image_url");
                return imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null");
            }
        } catch (SQLException e) {
            System.err.println("Errore verifica immagine oggetto: " + oggettoId);
        }
        return false;
    }
    
    /**
     * Recupera l'ID di un oggetto esistente
     */
    public static int recuperaIdOggettoEsistente(Oggetto oggettoDaCercare) {
        if (oggettoDaCercare == null || oggettoDaCercare.getNome() == null) {
            return -1;
        }
        
        try (Connection connection = ConnessioneDB.getConnessione()) {
            String query = "SELECT id FROM oggetto WHERE nome = ? AND categoria_id = ? LIMIT 1";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, oggettoDaCercare.getNome());
            
            // ✅ CORREZIONE: Converti categoria in ID per la ricerca
            int categoriaId = convertiCategoriaInId(oggettoDaCercare.getCategoria());
            stmt.setInt(2, categoriaId);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
            
            return -1;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nel recupero ID oggetto: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Aggiorna un oggetto esistente
     */
    public boolean aggiornaOggetto(Oggetto oggetto) throws SQLException {
        String sql = "UPDATE oggetto SET nome = ?, descrizione = ?, categoria_id = ?, image_url = ?, origine = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, oggetto.getNome());
            stmt.setString(2, oggetto.getDescrizione());
            
            // ✅ CORREZIONE: Converti categoria enum in ID numerico
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
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("✅ Oggetto aggiornato: " + rowsAffected + " righe modificate");
            return rowsAffected > 0;
        }
    }

    /**
     * Elimina un oggetto
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
     * Cerca oggetti per nome
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
     * Metodo di compatibilità - converte ID categoria in enum
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
     * Converte l'enum Categoria in ID numerico per il database
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
     * Metodo di compatibilità - ottiene ID categoria da nome
     */
    private int ottieniCategoriaIdDalNome(String nomeCategoria) {
        String sql = "SELECT id FROM categoria WHERE LOWER(nome) = LOWER(?)";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nomeCategoria);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dell'id categoria");
        }
        return -1;
    }
}