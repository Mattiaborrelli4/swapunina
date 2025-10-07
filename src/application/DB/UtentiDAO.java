package application.DB;

import application.Classe.utente;
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;


public class UtentiDAO {
    
    public UtentiDAO() {
    }

    private void creaTabellaSeMancante() {
        // Prima verifica se la tabella esiste
        String checkTableSQL = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'utente')";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement checkStmt = conn.prepareStatement(checkTableSQL)) {
            
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            boolean tabellaEsiste = rs.getBoolean(1);
            
            if (tabellaEsiste) {
                System.out.println("Tabella utente già esistente - verificata");
            } else {
                // Crea la tabella se non esiste
                String createSQL = "CREATE TABLE utente (id SERIAL PRIMARY KEY, matricola VARCHAR(20) UNIQUE NOT NULL, nome VARCHAR(100) NOT NULL, cognome VARCHAR(100) NOT NULL, email VARCHAR(255) UNIQUE NOT NULL, password VARCHAR(255) NOT NULL)";
                try (PreparedStatement createStmt = conn.prepareStatement(createSQL)) {
                    createStmt.executeUpdate();
                    System.out.println("Tabella utente creata con successo");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Errore nella gestione della tabella utente:");
            e.printStackTrace();
        }
    }
    
    public boolean emailEsiste(String email) {
        return campoEsiste("email", email);
    }
    
    public boolean matricolaEsiste(String matricola) {
        return campoEsiste("matricola", matricola);
    }
    
    private boolean campoEsiste(String campo, String valore) {
        String query = "SELECT COUNT(*) FROM utente WHERE " + campo + " = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, valore);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean registraUtente(utente utente) {
        String query = "INSERT INTO utente (matricola, nome, cognome, email, password) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            // Hash della password con BCrypt
            String hashedPassword = BCrypt.hashpw(utente.getPassword(), BCrypt.gensalt());
            
            stmt.setString(1, utente.getMatricola());
            stmt.setString(2, utente.getNome());
            stmt.setString(3, utente.getCognome());
            stmt.setString(4, utente.getEmail());
            stmt.setString(5, hashedPassword);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean verificaCredenziali(String email, String password) {
        String query = "SELECT password FROM utente WHERE email = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");
                    return BCrypt.checkpw(password, hashedPassword);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public utente getUtenteByEmail(String email) {
        String query = "SELECT id, matricola, nome, cognome, email FROM utente WHERE email = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    utente user = new utente(
                        rs.getString("matricola"),
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email"),
                        ""  // Password not needed
                    );
                    user.setId(rs.getInt("id")); // Imposta l'ID
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public utente autenticaUtente(String email, String password) {
        // Esegui la query al database per verificare email e password
        // Se trovata, restituisci un oggetto utente, altrimenti null
        try (Connection conn = ConnessioneDB.getConnessione()) {
            String sql = "SELECT * FROM utente WHERE email = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new utente(
                    rs.getString("matricola"),
                    rs.getString("nome"),
                    rs.getString("cognome"),
                    rs.getString("email"),
                    rs.getString("password")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getIdDaUsername(String username) {
    	String query = "SELECT id FROM utente WHERE matricola = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // o lancia eccezione
    }

    public int getIdByEmail(String email) {
        String query = "SELECT id FROM utente WHERE email = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    
    public boolean aggiornaPassword(String email, String vecchiaPassword, String nuovaPassword) {
        System.out.println("Tentativo cambio password per: " + email);
        
        // PRIMA verifica che la vecchia password sia corretta
        if (!verificaCredenziali(email, vecchiaPassword)) {
            System.out.println("ERRORE: La password attuale non è corretta per " + email);
            return false;
        }
        System.out.println("Password attuale verificata con successo");
        
        // POI controlla che la nuova password non sia uguale alla vecchia
        if (vecchiaPassword.equals(nuovaPassword)) {
            System.out.println("ERRORE: La nuova password è uguale alla vecchia per " + email);
            return false;
        }
        
        // Controllo: la nuova password deve avere almeno 6 caratteri
        if (nuovaPassword.length() < 6) {
            System.out.println("ERRORE: Password troppo corta per " + email + " (" + nuovaPassword.length() + " caratteri)");
            return false;
        }

        String sql = "UPDATE utente SET password = ? WHERE email = ?";
        System.out.println("Eseguendo query: " + sql);

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // ⚠️ CORREZIONE: Hash della nuova password con BCrypt (come nella registrazione)
            String hashedNuovaPassword = BCrypt.hashpw(nuovaPassword, BCrypt.gensalt());
            
            stmt.setString(1, hashedNuovaPassword);
            stmt.setString(2, email);

            System.out.println("Parametri: password (hash)='" + hashedNuovaPassword + "', email='" + email + "'");
            
            int rows = stmt.executeUpdate();
            System.out.println("Righe aggiornate: " + rows);
            
            boolean success = rows > 0;
            System.out.println(success ? "SUCCESSO: Password aggiornata" : "ERRORE: Nessuna riga aggiornata");
            
            return success;

        } catch (SQLException e) {
            System.out.println("ERRORE SQL durante aggiornamento password:");
            e.printStackTrace();
            return false;
        }
    }
    
    public String getNomeUtenteById(int id) {
        String sql = "SELECT nome FROM utente WHERE id = ?";
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("nome");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Utente Sconosciuto";
    }
}
    
   