package application.DB;

import application.Classe.Annuncio;
import application.Classe.CarrelloItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarrelloDAO {
    private static final String TABLE_NAME = "carrello";

    public CarrelloDAO() {
        creaTabellaSeMancante();
    }

    /**
     * Crea la tabella carrello se non esiste
     */
    private void creaTabellaSeMancante() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "utente_id INTEGER NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                    "annuncio_id INTEGER NOT NULL REFERENCES annuncio(id) ON DELETE CASCADE, " +
                    "quantita INTEGER NOT NULL DEFAULT 1, " +
                    "data_aggiunta TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(utente_id, annuncio_id)" +
                    ")";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            System.err.println("Errore nella creazione della tabella carrello");
            e.printStackTrace();
        }
    }

    /**
     * Aggiunge un annuncio al carrello con upsert per incrementare la quantità
     */
    public boolean aggiungiAlCarrello(int utenteId, int annuncioId) {
        String sql = "INSERT INTO " + TABLE_NAME + " (utente_id, annuncio_id, quantita) " +
                    "VALUES (?, ?, 1) " +
                    "ON CONFLICT (utente_id, annuncio_id) " +
                    "DO UPDATE SET quantita = " + TABLE_NAME + ".quantita + 1, " +
                    "data_aggiunta = CURRENT_TIMESTAMP " +
                    "RETURNING id";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, utenteId);
            pstmt.setInt(2, annuncioId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiunta al carrello: " + e.getMessage());
            return false;
        }
    }

    /**
     * Rimuove un annuncio dal carrello dell'utente
     */
    public boolean rimuoviDalCarrello(int utenteId, int annuncioId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE utente_id = ? AND annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nella rimozione dal carrello: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recupera tutti gli elementi nel carrello di un utente con informazioni complete
     */
    public List<CarrelloItem> getCarrelloPerUtente(int utenteId) {
        List<CarrelloItem> carrelloItems = new ArrayList<>();
        
        String sql = "SELECT c.id AS carrello_id, c.quantita, c.data_aggiunta, " +
                    "a.id AS annuncio_id, a.titolo, a.prezzo, a.venditore_id, " +
                    "u.nome AS venditore_nome, u.cognome AS venditore_cognome " +
                    "FROM " + TABLE_NAME + " c " +
                    "JOIN annuncio a ON c.annuncio_id = a.id " +
                    "JOIN utente u ON a.venditore_id = u.id " +
                    "WHERE c.utente_id = ? " +
                    "ORDER BY c.data_aggiunta DESC";
    
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Annuncio annuncio = creaAnnuncioCompleto(rs);
                    
                    CarrelloItem item = new CarrelloItem(
                        rs.getInt("carrello_id"),
                        annuncio,
                        rs.getInt("quantita"),
                        rs.getTimestamp("data_aggiunta").toLocalDateTime()
                    );
                    
                    carrelloItems.add(item);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Errore nel recupero del carrello: " + e.getMessage());
        }
        
        return carrelloItems;
    }

    /**
     * Crea un oggetto Annuncio con i campi essenziali per il carrello
     */
    private Annuncio creaAnnuncioCompleto(ResultSet rs) throws SQLException {
        Annuncio annuncio = new Annuncio();
        
        annuncio.setId(rs.getInt("annuncio_id"));
        annuncio.setTitolo(rs.getString("titolo"));
        annuncio.setPrezzo(rs.getDouble("prezzo"));
        annuncio.setVenditoreId(rs.getInt("venditore_id"));
        
        try {
            annuncio.setNomeVenditore(rs.getString("venditore_nome") + " " + rs.getString("venditore_cognome"));
        } catch (Exception e) {
            // Ignora se il setter non esiste
        }
        
        return annuncio;
    }

    /**
     * Aggiorna la quantità di un item nel carrello
     */
    public boolean aggiornaQuantita(int carrelloId, int nuovaQuantita) {
        if (nuovaQuantita <= 0) {
            return rimuoviItem(carrelloId);
        }
        
        String sql = "UPDATE " + TABLE_NAME + " SET quantita = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nuovaQuantita);
            stmt.setInt(2, carrelloId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento della quantità: " + e.getMessage());
            return false;
        }
    }

    /**
     * Rimuove un item specifico dal carrello usando l'ID del carrello
     */
    public boolean rimuoviItem(int carrelloId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrelloId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nella rimozione dell'item: " + e.getMessage());
            return false;
        }
    }

    /**
     * Svuota completamente il carrello di un utente
     */
    public boolean svuotaCarrello(int utenteId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            return stmt.executeUpdate() >= 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nello svuotamento del carrello: " + e.getMessage());
            return false;
        }
    }

    /**
     * Conta il numero di elementi nel carrello di un utente
     */
    public int contaElementiCarrello(int utenteId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Errore nel conteggio elementi carrello: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Verifica se un annuncio è già nel carrello dell'utente
     */
    public boolean isNelCarrello(int utenteId, int annuncioId) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE utente_id = ? AND annuncio_id = ? LIMIT 1";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Errore nella verifica carrello: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcola il prezzo totale del carrello per un utente
     */
    public double getPrezzoTotaleCarrello(int utenteId) {
        String sql = "SELECT SUM(a.prezzo * c.quantita) as totale " +
                    "FROM " + TABLE_NAME + " c " +
                    "JOIN annuncio a ON c.annuncio_id = a.id " +
                    "WHERE c.utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("totale") : 0.0;
            }
        } catch (SQLException e) {
            System.err.println("Errore nel calcolo del totale carrello: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Ottiene la quantità di un item specifico nel carrello
     */
    public int getQuantitaItem(int utenteId, int annuncioId) {
        String sql = "SELECT quantita FROM " + TABLE_NAME + " WHERE utente_id = ? AND annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("quantita") : 0;
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero quantità item: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Metodo batch per aggiornare multiple quantità
     */
    public boolean aggiornaQuantitaBatch(List<int[]> updates) {
        String sql = "UPDATE " + TABLE_NAME + " SET quantita = ? WHERE id = ? AND utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (int[] update : updates) {
                stmt.setInt(1, update[1]); // quantità
                stmt.setInt(2, update[0]); // carrello_id
                stmt.setInt(3, update[2]); // utente_id
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            return results.length > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento batch quantità: " + e.getMessage());
            return false;
        }
    }
}