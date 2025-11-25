package application.DB;

import application.Classe.Codice;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DAO per la gestione dei codici di conferma criptati
 */
public class CodiceDAO {

    /**
     * Genera un nuovo codice di conferma criptato per un utente e annuncio
     * Restituisce il codice in chiaro per mostrarlo al venditore
     */
    public String generaCodiceConferma(int utenteId, int annuncioId) {
        // Prima elimina eventuali codici esistenti per questa combinazione
        eliminaCodiciEsistenti(utenteId, annuncioId);

        // Genera codice casuale a 6 caratteri alfanumerici
        String codicePlain = generaCodiceAlfanumerico();
        
        // Cripta il codice usando jBCrypt
        String codiceHash = BCrypt.hashpw(codicePlain, BCrypt.gensalt());

        String sql = "INSERT INTO codice_conferma (utente_id, annuncio_id, codice_hash, codice_plain, data_creazione, tentativi_errati) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            stmt.setString(3, codiceHash);
            stmt.setString(4, codicePlain);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(6, 0);

            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                return codicePlain; // Restituisce il codice in chiaro solo per la visualizzazione
            }
        } catch (SQLException e) {
            System.err.println("Errore nella generazione del codice di conferma: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Verifica un codice senza bisogno dell'ID annuncio
     * Cerca tra tutti i codici attivi e verifica se il codice inserito corrisponde
     * Se corretto, elimina l'annuncio correlato
     */
    public boolean verificaCodice(String codiceInserito) {
        String sql = "SELECT cc.*, cc.annuncio_id FROM codice_conferma cc " +
                 "WHERE cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String codiceHash = rs.getString("codice_hash");
                int tentativi = rs.getInt("tentativi_errati");
                int codiceId = rs.getInt("id");
                int annuncioId = rs.getInt("annuncio_id");

                if (tentativi >= 3) {
                    continue;
                }

                boolean codiceCorretto = BCrypt.checkpw(codiceInserito, codiceHash);

                if (codiceCorretto) {
                    // ✅ CORREZIONE: Cambia stato a VENDUTO invece di eliminare
                    AnnuncioDAO annuncioDAO = new AnnuncioDAO();
                    boolean statoAggiornato = annuncioDAO.aggiornaStatoAnnuncio(annuncioId, "VENDUTO");
                    
                    if (statoAggiornato) {
                        // ✅ NOTIFICA LA SCHERMATA PRINCIPALE CHE L'ANNUNCIO È VENDUTO
                        notificaSchermataPrincipale(annuncioId);
                        
                        eliminaCodice(codiceId);
                        return true;
                    }
                } else {
                    incrementaTentativiErrati(codiceId);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nella verifica del codice: " + codiceInserito);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Recupera un codice per utente e annuncio
     */
    private Codice getCodiceByUtenteAnnuncio(int utenteId, int annuncioId) {
        String sql = "SELECT * FROM codice_conferma WHERE utente_id = ? AND annuncio_id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mappaCodice(rs);
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero del codice per utente " + utenteId + " e annuncio " + annuncioId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Elimina i codici esistenti per una combinazione utente-annuncio
     */
    private void eliminaCodiciEsistenti(int utenteId, int annuncioId) {
        String sql = "DELETE FROM codice_conferma WHERE utente_id = ? AND annuncio_id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'eliminazione dei codici esistenti: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Elimina un codice specifico dal database
     */
    private void eliminaCodice(int codiceId) {
        String sql = "DELETE FROM codice_conferma WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, codiceId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'eliminazione del codice: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Incrementa il contatore dei tentativi errati
     */
    private void incrementaTentativiErrati(int codiceId) {
        String sql = "UPDATE codice_conferma SET tentativi_errati = tentativi_errati + 1 WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, codiceId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'incremento dei tentativi errati: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica se esiste un codice di conferma pendente per utente e annuncio
     */
    public boolean hasCodiceConfermaPendente(int utenteId, int annuncioId) {
        Codice codice = getCodiceByUtenteAnnuncio(utenteId, annuncioId);
        return codice != null && codice.isValido();
    }

    /**
     * Pulisce i codici scaduti dal database (manutenzione)
     */
    public int pulisciCodiciScaduti() {
        String sql = "DELETE FROM codice_conferma WHERE data_creazione < ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // MODIFICA: Cambiato da 24 ore a 2 settimane (14 giorni)
            LocalDateTime scadenza = LocalDateTime.now().minusDays(14);
            stmt.setTimestamp(1, Timestamp.valueOf(scadenza));
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Errore nella pulizia dei codici scaduti: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Mappa un ResultSet in un oggetto Codice
     */
    private Codice mappaCodice(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int utenteId = rs.getInt("utente_id");
        int annuncioId = rs.getInt("annuncio_id");
        String codiceHash = rs.getString("codice_hash");
        String codicePlain = rs.getString("codice_plain");
        Timestamp dataCreazione = rs.getTimestamp("data_creazione");
        int tentativiErrati = rs.getInt("tentativi_errati");

        LocalDateTime dataCreazioneLD = dataCreazione.toLocalDateTime();

        return new Codice(id, utenteId, annuncioId, codiceHash, codicePlain, dataCreazioneLD, tentativiErrati);
    }

    /**
     * Genera un codice casuale a 6 caratteri alfanumerici
     */
    private String generaCodiceAlfanumerico() {
        String caratteri = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
        Random random = new Random();
        StringBuilder codice = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            codice.append(caratteri.charAt(random.nextInt(caratteri.length())));
        }
        
        return codice.toString();
    }

    /**
     * Recupera tutti i codici attivi per un utente con i dettagli dell'annuncio
     */
    public List<Map<String, String>> getCodiciAttiviPerUtente(int utenteId) {
        List<Map<String, String>> codici = new ArrayList<>();
        
        // MODIFICA: Cambiato da 24 ore a 14 giorni
        String sql = "SELECT cc.id, cc.codice_plain, cc.data_creazione, a.titolo, a.id as annuncio_id " +
                 "FROM codice_conferma cc " +
                 "JOIN annuncio a ON cc.annuncio_id = a.id " +
                 "WHERE cc.utente_id = ? AND cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days' " +
                 "ORDER BY cc.data_creazione DESC";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> codiceInfo = new HashMap<>();
                codiceInfo.put("id", String.valueOf(rs.getInt("id")));
                codiceInfo.put("codice", rs.getString("codice_plain"));
                codiceInfo.put("titolo", rs.getString("titolo"));
                codiceInfo.put("annuncio_id", String.valueOf(rs.getInt("annuncio_id")));
                codiceInfo.put("data_creazione", rs.getTimestamp("data_creazione").toString());
                codici.add(codiceInfo);
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei codici attivi per l'utente " + utenteId);
            e.printStackTrace();
        }
        return codici;
    }

    /**
     * Notifica la schermata principale che un annuncio è stato venduto
     */
    private void notificaSchermataPrincipale(int annuncioId) {
        try {
            // Ottieni un riferimento alla schermata principale tramite SessionManager o altro metodo
            // Questo è un esempio - potresti dover adattarlo al tuo architettura
            notificaAnnuncioVendutoStatic(annuncioId);
        } catch (Exception e) {
            System.err.println("Errore nella notifica della schermata principale: " + e.getMessage());
        }
    }

    // Metodo statico per permettere l'accesso globale
private static void notificaAnnuncioVendutoStatic(int annuncioId) {
    try {
        // ✅ APPROCCIO SEMPLICE: Chiama direttamente il metodo statico
        Class<?> schermataClass = Class.forName("schermata.SchermataPrincipale");
        java.lang.reflect.Method notifyMethod = schermataClass.getMethod("notificaAnnuncioVenduto", int.class);
        notifyMethod.invoke(null, annuncioId);
        
        System.out.println("🔔 Notifica inviata per annuncio venduto: " + annuncioId);
    } catch (Exception e) {
        // Fallback: stampa il messaggio
        System.out.println("🔔 Annuncio " + annuncioId + " venduto - [Fallback: SchermataPrincipale non trovata]");
        e.printStackTrace();
    }
}


/**
 * Verifica un codice per un annuncio specifico (per il venditore)
 * con controllo che solo il proprietario dell'annuncio possa verificare il codice
 */
public boolean verificaCodicePerAnnuncio(int annuncioId, String codiceInserito, int venditoreId) {
    System.out.println("=== 🚀 INIZIO VERIFICA CODICE DETTAGLIATA ===");
    System.out.println("📋 PARAMETRI:");
    System.out.println("   - Annuncio ID: " + annuncioId);
    System.out.println("   - Codice inserito: " + codiceInserito);
    System.out.println("   - Venditore ID: " + venditoreId);
    System.out.println("   - Codice atteso: 7H203C");

    // Test connessione database
    try (Connection conn = ConnessioneDB.getConnessione()) {
        if (conn != null && !conn.isClosed()) {
            System.out.println("✅ Connessione database OK");
        }
    } catch (SQLException e) {
        System.err.println("❌ Errore connessione database: " + e.getMessage());
    }

    // VERIFICA VENDITORE
    System.out.println("🔍 VERIFICA VENDITORE:");
    boolean isVenditore = isVenditoreAnnuncio(annuncioId, venditoreId);
    System.out.println("   - Risultato: " + isVenditore);
    
    if (!isVenditore) {
        System.err.println("❌ ACCESSO NEGATO: Non sei il venditore di questo annuncio");
        return false;
    }

    // VERIFICA CODICE
    String sql = "SELECT cc.* FROM codice_conferma cc " +
             "WHERE cc.annuncio_id = ? AND cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days'";

    try (Connection conn = ConnessioneDB.getConnessione();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, annuncioId);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            String codiceHash = rs.getString("codice_hash");
            String codicePlain = rs.getString("codice_plain");
            int tentativi = rs.getInt("tentativi_errati");
            int codiceId = rs.getInt("id");

            System.out.println("🔍 DATI CODICE DAL DB:");
            System.out.println("   - Codice plain: " + codicePlain);
            System.out.println("   - Tentativi errati: " + tentativi);
            System.out.println("   - Codice ID: " + codiceId);

            if (tentativi >= 3) {
                System.err.println("❌ CODICE BLOCCATO: Troppi tentativi errati");
                return false;
            }

            // VERIFICA BCrypt
            System.out.println("🔍 VERIFICA CRITTOGRAFICA:");
            System.out.println("   - Codice inserito: " + codiceInserito);
            System.out.println("   - Codice atteso: " + codicePlain);
            System.out.println("   - Hash nel DB: " + codiceHash.substring(0, 20) + "...");
            
            boolean codiceCorretto = BCrypt.checkpw(codiceInserito, codiceHash);
            System.out.println("   - Risultato BCrypt: " + codiceCorretto);

            if (codiceCorretto) {
                System.out.println("✅ CODICE CORRETTO - Aggiornamento stato...");
                AnnuncioDAO annuncioDAO = new AnnuncioDAO();
                boolean statoAggiornato = annuncioDAO.aggiornaStatoAnnuncio(annuncioId, "VENDUTO");
                
                System.out.println("   - Stato aggiornato: " + statoAggiornato);
                
                if (statoAggiornato) {
                    notificaSchermataPrincipale(annuncioId);
                    eliminaCodice(codiceId);
                    System.out.println("🎉 SUCCESSO: Codice verificato e annuncio venduto!");
                    return true;
                } else {
                    System.err.println("❌ FALLITO: Aggiornamento stato annuncio");
                    return false;
                }
            } else {
                incrementaTentativiErrati(codiceId);
                System.err.println("❌ CODICE ERRATO: Tentativo " + (tentativi + 1) + " di 3");
                return false;
            }
        } else {
            System.err.println("❌ NESSUN CODICE TROVATO per annuncio " + annuncioId);
            return false;
        }
    } catch (SQLException e) {
        System.err.println("❌ ERRORE SQL: " + e.getMessage());
        e.printStackTrace();
    }
    
    System.out.println("=== ❌ FINE VERIFICA CODICE CON ERRORI ===");
    return false;
}

/**
 * Verifica se l'utente è il venditore dell'annuncio
 */
private boolean isVenditoreAnnuncio(int annuncioId, int venditoreId) {
    System.out.println("🔍 VERIFICA VENDITORE:");
    System.out.println("   - Annuncio ID: " + annuncioId);
    System.out.println("   - Venditore ID richiesto: " + venditoreId);
    
    String sql = "SELECT venditore_id, titolo FROM annuncio WHERE id = ?";
    
    try (Connection conn = ConnessioneDB.getConnessione();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        
        stmt.setInt(1, annuncioId);
        ResultSet rs = stmt.executeQuery();
        
        if (rs.next()) {
            int venditoreReale = rs.getInt("venditore_id");
            String titoloAnnuncio = rs.getString("titolo");
            boolean isProprietario = (venditoreReale == venditoreId);
            
            System.out.println("   - Venditore reale nel DB: " + venditoreReale);
            System.out.println("   - Titolo annuncio: " + titoloAnnuncio);
            System.out.println("   - È proprietario: " + isProprietario);
            
            if (!isProprietario) {
                System.err.println("⚠️  TENTATIVO DI ACCESSO NON AUTORIZZATO:");
                System.err.println("   - Utente richiedente: " + venditoreId);
                System.err.println("   - Venditore reale: " + venditoreReale);
                System.err.println("   - Annuncio: " + annuncioId + " - " + titoloAnnuncio);
            }
            
            return isProprietario;
        } else {
            System.err.println("❌ ANNUNCIO NON TROVATO: " + annuncioId);
            return false;
        }
    } catch (SQLException e) {
        System.err.println("❌ Errore SQL nella verifica del proprietario per annuncio " + annuncioId);
        e.printStackTrace();
    }
    return false;
}
}
