package application.DB;

import application.Classe.Messaggio;
import application.Classe.utente;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessaggioDAO {
    private static final String TABLE_NAME = "messaggio";

    public MessaggioDAO() {
        creaTabellaSeMancante();
    }

    private void creaTabellaSeMancante() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "mittente_id INTEGER NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                "destinatario_id INTEGER NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                "testo_plaintext_backup TEXT NOT NULL, " +
                "testo_encrypted BYTEA NOT NULL, " +
                "iv BYTEA NOT NULL, " +
                "data_invio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE SET NULL, " +
                "algoritmo_encryption VARCHAR(20) DEFAULT 'AES/GCM/NoPadding', " +
                "key_id INTEGER REFERENCES encryption_keys(id) ON DELETE SET NULL" +
                ")";
        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Errore creazione tabella messaggio");
            e.printStackTrace();
        }
    }

    public boolean inviaMessaggio(Messaggio msg) {
        String sql = "INSERT INTO " + TABLE_NAME + " (mittente_id, destinatario_id, testo_plaintext_backup, testo_encrypted, iv, data_invio, annuncio_id, algoritmo_encryption) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, msg.getMittenteId());
            stmt.setInt(2, msg.getDestinatarioId());
            
            // Salva il testo in chiaro come backup
            stmt.setString(3, msg.getTesto());
            
            // Converti il testo in byte array (UTF-8 encoding)
            stmt.setBytes(4, msg.getTesto().getBytes(StandardCharsets.UTF_8));
            
            // Genera un IV random (12 bytes per AES-GCM)
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            stmt.setBytes(5, iv);
            
            stmt.setTimestamp(6, Timestamp.valueOf(msg.getDataInvio()));
            
            if (msg.getAnnuncioId() != null) {
                stmt.setInt(7, msg.getAnnuncioId());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            
            stmt.setString(8, "UTF-8_ENCODING"); // Usiamo encoding semplice per ora
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore durante l'invio del messaggio");
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Integer> getInterlocutori(int mioId) {
        List<Integer> interlocutori = new ArrayList<>();
        String query = "SELECT DISTINCT CASE " +
                       "WHEN mittente_id = ? THEN destinatario_id " +
                       "WHEN destinatario_id = ? THEN mittente_id " +
                       "END AS interlocutore " +
                       "FROM " + TABLE_NAME + " " +
                       "WHERE mittente_id = ? OR destinatario_id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, mioId);
            stmt.setInt(2, mioId);
            stmt.setInt(3, mioId);
            stmt.setInt(4, mioId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                interlocutori.add(rs.getInt("interlocutore"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return interlocutori;
    }

    public List<Messaggio> getConversazione(int utente1, int utente2) {
        List<Messaggio> messaggi = new ArrayList<>();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " +
                "(mittente_id = ? AND destinatario_id = ?) OR " +
                "(mittente_id = ? AND destinatario_id = ?) " +
                "ORDER BY data_invio";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utente1);
            stmt.setInt(2, utente2);
            stmt.setInt(3, utente2);
            stmt.setInt(4, utente1);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String testo = null;
                
                try {
                    // Prima prova a leggere il testo encrypted
                    byte[] encryptedData = rs.getBytes("testo_encrypted");
                    if (encryptedData != null) {
                        testo = new String(encryptedData, StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    System.err.println("Errore decodifica messaggio encrypted ID: " + rs.getInt("id"));
                    e.printStackTrace();
                }
                
                // Se non riesci a decodificare l'encrypted, usa il backup
                if (testo == null || testo.isEmpty()) {
                    try {
                        testo = rs.getString("testo_plaintext_backup");
                        if (testo == null) {
                            testo = "[Messaggio non decodificabile]";
                        }
                    } catch (Exception ex) {
                        System.err.println("Errore lettura backup messaggio ID: " + rs.getInt("id"));
                        testo = "[Messaggio illeggibile]";
                    }
                }
                
                Messaggio m = new Messaggio(
                    rs.getInt("id"),
                    rs.getInt("mittente_id"),
                    rs.getInt("destinatario_id"),
                    testo,
                    rs.getTimestamp("data_invio").toLocalDateTime(),
                    rs.getObject("annuncio_id") != null ? rs.getInt("annuncio_id") : null
                );
                messaggi.add(m);
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero conversazione");
            e.printStackTrace();
        }

        return messaggi;
    }
    
    public List<utente> getInterlocutoriUtenti(int mioId) {
        List<utente> utenti = new ArrayList<>();
        
        String query = """
            SELECT DISTINCT u.*
            FROM utente u
            WHERE u.id IN (
                SELECT mittente_id FROM messaggio WHERE destinatario_id = ?
                UNION
                SELECT destinatario_id FROM messaggio WHERE mittente_id = ?
            )
            AND u.id <> ?
            """;

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, mioId);
            stmt.setInt(2, mioId);
            stmt.setInt(3, mioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    utente u = new utente(
                        rs.getString("matricola"),
                        rs.getString("nome"),
                        rs.getString("cognome"),
                        rs.getString("email"),
                        ""
                    );
                    u.setId(rs.getInt("id"));
                    u.setFotoProfilo(rs.getString("foto_profilo"));
                    utenti.add(u);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return utenti;
    }

    // Metodo per migrare i messaggi esistenti
    public boolean migraMessaggiEsistenti() {
        String sql = "UPDATE " + TABLE_NAME + " SET " +
                     "testo_encrypted = convert_to(testo_plaintext_backup, 'UTF8'), " +
                     "iv = E'\\\\x000000000000000000000000', " +
                     "algoritmo_encryption = 'UTF-8_ENCODING' " +
                     "WHERE testo_encrypted IS NULL";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement()) {
            int rowsAffected = stmt.executeUpdate(sql);
            System.out.println("Messaggi migrati: " + rowsAffected);
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Errore migrazione messaggi");
            e.printStackTrace();
            return false;
        }
    }

    public List<Messaggio> getConversazionePerAnnuncio(int currentUserId, int interlocutoreId, int annuncioId) {
        List<Messaggio> messaggi = new ArrayList<>();
        
        System.out.println("🔍 Ricerca messaggi per annuncio:");
        System.out.println("   Utente corrente: " + currentUserId);
        System.out.println("   Interlocutore: " + interlocutoreId);
        System.out.println("   Annuncio ID: " + annuncioId);
        
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " +
                "((mittente_id = ? AND destinatario_id = ?) OR " +
                "(mittente_id = ? AND destinatario_id = ?)) " +
                "AND annuncio_id = ? " +
                "ORDER BY data_invio ASC";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentUserId);
            stmt.setInt(2, interlocutoreId);
            stmt.setInt(3, interlocutoreId);
            stmt.setInt(4, currentUserId);
            stmt.setInt(5, annuncioId);

            System.out.println("📊 Esecuzione query: " + stmt.toString());
            
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            
            while (rs.next()) {
                count++;
                String testo = null;
                
                try {
                    // Prima prova a leggere il testo encrypted
                    byte[] encryptedData = rs.getBytes("testo_encrypted");
                    if (encryptedData != null) {
                        testo = new String(encryptedData, StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    System.err.println("Errore decodifica messaggio encrypted ID: " + rs.getInt("id"));
                    e.printStackTrace();
                }
                
                // Se non riesci a decodificare l'encrypted, usa il backup
                if (testo == null || testo.isEmpty()) {
                    try {
                        testo = rs.getString("testo_plaintext_backup");
                        if (testo == null) {
                            testo = "[Messaggio non decodificabile]";
                        }
                    } catch (Exception ex) {
                        System.err.println("Errore lettura backup messaggio ID: " + rs.getInt("id"));
                        testo = "[Messaggio illeggibile]";
                    }
                }
                
                Messaggio m = new Messaggio(
                    rs.getInt("id"),
                    rs.getInt("mittente_id"),
                    rs.getInt("destinatario_id"),
                    testo,
                    rs.getTimestamp("data_invio").toLocalDateTime(),
                    rs.getObject("annuncio_id") != null ? rs.getInt("annuncio_id") : null
                );
                messaggi.add(m);
                
                System.out.println("   📨 Messaggio " + count + ": " + m.getTesto() + 
                                 " (da: " + m.getMittenteId() + ")");
            }
            
            System.out.println("✅ Trovati " + messaggi.size() + " messaggi per annuncio " + annuncioId);
            
        } catch (SQLException e) {
            System.err.println("❌ Errore recupero conversazione per annuncio");
            e.printStackTrace();
        }

        return messaggi;
    }
}