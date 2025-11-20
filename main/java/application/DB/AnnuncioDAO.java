package com.example.dao;

import com.example.model.Utente;
import com.example.service.CloudinaryService;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtentiDAO {
    private CloudinaryService cloudinaryService;

    public UtentiDAO() {
        this.cloudinaryService = new CloudinaryService();
        // inizializzaDatabase(); // DISABILITATO: schema DB gestito esternamente via migrazioni
    }

    // ===================================================================
    // METODI CRUD - MANTENUTI
    // ===================================================================

    public Utente trovaPerId(int id) {
        String sql = "SELECT * FROM utenti WHERE id = ?";
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mappaUtente(rs);
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dell'utente per ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Utente trovaPerEmail(String email) {
        String sql = "SELECT * FROM utenti WHERE email = ?";
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mappaUtente(rs);
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero dell'utente per email: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean inserisci(Utente utente) {
        String sql = "INSERT INTO utenti (nome, cognome, email, password_hash, attivo, ultimo_accesso, foto_profilo) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, utente.getNome());
            stmt.setString(2, utente.getCognome());
            stmt.setString(3, utente.getEmail());
            stmt.setString(4, utente.getPasswordHash());
            stmt.setBoolean(5, utente.isAttivo());
            stmt.setTimestamp(6, utente.getUltimoAccesso() != null ? 
                Timestamp.valueOf(utente.getUltimoAccesso()) : null);
            stmt.setString(7, utente.getFotoProfilo());
            
            int righeInserite = stmt.executeUpdate();
            
            if (righeInserite > 0) {
                // Recupera l'ID generato
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    utente.setId(generatedKeys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Errore durante l'inserimento dell'utente: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean aggiorna(Utente utente) {
        String sql = "UPDATE utenti SET nome = ?, cognome = ?, email = ?, password_hash = ?, " +
                    "attivo = ?, ultimo_accesso = ?, foto_profilo = ? WHERE id = ?";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setString(1, utente.getNome());
            stmt.setString(2, utente.getCognome());
            stmt.setString(3, utente.getEmail());
            stmt.setString(4, utente.getPasswordHash());
            stmt.setBoolean(5, utente.isAttivo());
            stmt.setTimestamp(6, utente.getUltimoAccesso() != null ? 
                Timestamp.valueOf(utente.getUltimoAccesso()) : null);
            stmt.setString(7, utente.getFotoProfilo());
            stmt.setInt(8, utente.getId());
            
            int righeAggiornate = stmt.executeUpdate();
            return righeAggiornate > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore durante l'aggiornamento dell'utente: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean elimina(int id) {
        String sql = "DELETE FROM utenti WHERE id = ?";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            int righeEliminate = stmt.executeUpdate();
            return righeEliminate > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore durante l'eliminazione dell'utente: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Utente> trovaTutti() {
        List<Utente> utenti = new ArrayList<>();
        String sql = "SELECT * FROM utenti ORDER BY id";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                utenti.add(mappaUtente(rs));
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero di tutti gli utenti: " + e.getMessage());
            e.printStackTrace();
        }
        return utenti;
    }

    public boolean aggiornaUltimoAccesso(int idUtente) {
        String sql = "UPDATE utenti SET ultimo_accesso = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setInt(1, idUtente);
            int righeAggiornate = stmt.executeUpdate();
            return righeAggiornate > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore durante l'aggiornamento dell'ultimo accesso: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean aggiornaFotoProfilo(int idUtente, String urlFoto) {
        String sql = "UPDATE utenti SET foto_profilo = ? WHERE id = ?";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setString(1, urlFoto);
            stmt.setInt(2, idUtente);
            int righeAggiornate = stmt.executeUpdate();
            return righeAggiornate > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore durante l'aggiornamento della foto profilo: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean emailEsiste(String email) {
        String sql = "SELECT COUNT(*) FROM utenti WHERE email = ?";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la verifica dell'email: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<Utente> trovaUtentiAttivi() {
        List<Utente> utenti = new ArrayList<>();
        String sql = "SELECT * FROM utenti WHERE attivo = true ORDER BY nome, cognome";
        
        try (Connection connessione = DatabaseConfig.getConnection();
             PreparedStatement stmt = connessione.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                utenti.add(mappaUtente(rs));
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il recupero degli utenti attivi: " + e.getMessage());
            e.printStackTrace();
        }
        return utenti;
    }

    // ===================================================================
    // METODI PRIVATI DI SUPPORTO
    // ===================================================================

    private Utente mappaUtente(ResultSet rs) throws SQLException {
        Utente utente = new Utente();
        utente.setId(rs.getInt("id"));
        utente.setNome(rs.getString("nome"));
        utente.setCognome(rs.getString("cognome"));
        utente.setEmail(rs.getString("email"));
        utente.setPasswordHash(rs.getString("password_hash"));
        utente.setDataRegistrazione(rs.getTimestamp("data_registrazione").toLocalDateTime());
        utente.setAttivo(rs.getBoolean("attivo"));
        
        Timestamp ultimoAccesso = rs.getTimestamp("ultimo_accesso");
        if (ultimoAccesso != null) {
            utente.setUltimoAccesso(ultimoAccesso.toLocalDateTime());
        }
        
        utente.setFotoProfilo(rs.getString("foto_profilo"));
        return utente;
    }
}
