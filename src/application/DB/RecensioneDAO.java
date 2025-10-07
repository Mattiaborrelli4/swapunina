package application.DB;

import application.Classe.Recensioni;
import application.Classe.Annuncio;
import application.Classe.utente;
import application.Classe.StatisticheEconomiche;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RecensioneDAO {
    private static final String TABLE_NAME = "recensione";

    public RecensioneDAO() {
        creaTabellaSeMancante();
    }

    private void creaTabellaSeMancante() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "acquirente_id INTEGER NOT NULL REFERENCES utente(id), " +
                "venditore_id INTEGER NOT NULL REFERENCES utente(id), " +
                "annuncio_id INTEGER NOT NULL REFERENCES annuncio(id), " +
                "commento TEXT NOT NULL, " +
                "punteggio INTEGER CHECK (punteggio >= 1 AND punteggio <= 5), " +
                "data_recensione TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "visibile BOOLEAN DEFAULT TRUE, " +
                "CONSTRAINT recensione_acquirente_id_annuncio_id_key UNIQUE (acquirente_id, annuncio_id)" +
                ")";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Tabella recensione verificata/creata con successo");
        } catch (SQLException e) {
            System.err.println("Errore creazione tabella recensione: " + e.getMessage());
        }
    }

    // METODO PRINCIPALE: Recupera recensioni + statistiche per annuncio specifico
    public StatisticheRecensioni getRecensioniEStatistichePerAnnuncio(int idAnnuncio) {
        List<Recensioni> recensioni = new ArrayList<>();
        StatisticheEconomiche statistiche = null;
        
        // Recupera le recensioni con JOIN per ottenere il titolo dall'annuncio
        String sqlRecensioni = "SELECT r.*, " +
                 "acquirente.nome as acqu_nome, acquirente.cognome as acqu_cognome, " +
                 "venditore.nome as vend_nome, venditore.cognome as vend_cognome, " +
                 "a.titolo as annuncio_titolo, a.titolo as titolo_annuncio " +
                 "FROM " + TABLE_NAME + " r " +
                 "JOIN utente acquirente ON r.acquirente_id = acquirente.id " +
                 "JOIN utente venditore ON r.venditore_id = venditore.id " +
                 "JOIN annuncio a ON r.annuncio_id = a.id " +
                 "WHERE r.annuncio_id = ? AND r.visibile = TRUE " +
                 "ORDER BY r.data_recensione DESC";
        
        // Recupera le statistiche dalla vista
        String sqlStatistiche = "SELECT * FROM statistiche_recensioni_annuncio WHERE annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmtRecensioni = conn.prepareStatement(sqlRecensioni);
             PreparedStatement stmtStatistiche = conn.prepareStatement(sqlStatistiche)) {
            
            // === RECUPERO RECENSIONI ===
            stmtRecensioni.setInt(1, idAnnuncio);
            ResultSet rsRecensioni = stmtRecensioni.executeQuery();
            
            List<Float> punteggi = new ArrayList<>();
            
            while (rsRecensioni.next()) {
                // Crea utente acquirente
                utente acquirente = new utente();
                acquirente.setId(rsRecensioni.getInt("acquirente_id"));
                acquirente.setNome(rsRecensioni.getString("acqu_nome"));
                acquirente.setCognome(rsRecensioni.getString("acqu_cognome"));
                
                // Crea utente venditore
                utente venditore = new utente();
                venditore.setId(rsRecensioni.getInt("venditore_id"));
                venditore.setNome(rsRecensioni.getString("vend_nome"));
                venditore.setCognome(rsRecensioni.getString("vend_cognome"));
                
                // Crea annuncio con titolo dal database
                Annuncio annuncio = new Annuncio();
                annuncio.setId(rsRecensioni.getInt("annuncio_id"));
                
                // Recupera il titolo con fallback sicuro
                String titolo = rsRecensioni.getString("annuncio_titolo");
                if (titolo == null || titolo.isEmpty()) {
                    titolo = rsRecensioni.getString("titolo_annuncio");
                }
                annuncio.setTitolo(titolo != null ? titolo : "Annuncio #" + rsRecensioni.getInt("annuncio_id"));
                
                String commento = rsRecensioni.getString("commento");
                int punteggio = rsRecensioni.getInt("punteggio");
                punteggi.add((float) punteggio);
                
                // Crea la recensione
                Recensioni recensione = new Recensioni(acquirente, venditore, annuncio, commento, punteggio);
                recensione.setId(rsRecensioni.getInt("id"));
                
                Timestamp timestamp = rsRecensioni.getTimestamp("data_recensione");
                if (timestamp != null) {
                    recensione.setDataRecensione(timestamp.toLocalDateTime());
                }
                
                recensione.setVisibile(rsRecensioni.getBoolean("visibile"));
                recensioni.add(recensione);
            }
            
            // === RECUPERO STATISTICHE ===
            stmtStatistiche.setInt(1, idAnnuncio);
            ResultSet rsStatistiche = stmtStatistiche.executeQuery();
            
            if (rsStatistiche.next()) {
                // Crea le statistiche usando i dati del database
                statistiche = new StatisticheEconomiche(
                    punteggi,
                    LocalDateTime.now().minusMonths(1).toLocalDate(), // periodo inizio
                    LocalDateTime.now().toLocalDate() // periodo fine
                );
            } else {
                // Se non ci sono statistiche, crea statistiche vuote
                statistiche = new StatisticheEconomiche(
                    new ArrayList<>(),
                    LocalDateTime.now().minusMonths(1).toLocalDate(),
                    LocalDateTime.now().toLocalDate()
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Errore recupero recensioni e statistiche per annuncio " + idAnnuncio + ": " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: ritorna oggetto vuoto in caso di errore
            statistiche = new StatisticheEconomiche(
                new ArrayList<>(),
                LocalDateTime.now().minusMonths(1).toLocalDate(),
                LocalDateTime.now().toLocalDate()
            );
        }
        
        return new StatisticheRecensioni(recensioni, statistiche);
    }

    // METODO COMPATIBILITÀ: Recupera solo recensioni per annuncio (senza statistiche)
    public List<Recensioni> getRecensioniPerAnnuncio(int idAnnuncio) {
        StatisticheRecensioni risultato = getRecensioniEStatistichePerAnnuncio(idAnnuncio);
        return risultato.getRecensioni();
    }

    // METODO COMPATIBILITÀ: Recupera recensioni per venditore (tutti gli annunci)
    public List<Recensioni> getRecensioniPerVenditore(int idVenditore) {
        List<Recensioni> recensioni = new ArrayList<>();
        
        String sql = "SELECT r.*, " +
                     "acquirente.nome as acqu_nome, acquirente.cognome as acqu_cognome, " +
                     "venditore.nome as vend_nome, venditore.cognome as vend_cognome, " +
                     "a.titolo as annuncio_titolo " +
                     "FROM " + TABLE_NAME + " r " +
                     "JOIN utente acquirente ON r.acquirente_id = acquirente.id " +
                     "JOIN utente venditore ON r.venditore_id = venditore.id " +
                     "JOIN annuncio a ON r.annuncio_id = a.id " +
                     "WHERE r.venditore_id = ? AND r.visibile = TRUE " +
                     "ORDER BY r.data_recensione DESC";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idVenditore);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                utente acquirente = new utente();
                acquirente.setId(rs.getInt("acquirente_id"));
                acquirente.setNome(rs.getString("acqu_nome"));
                acquirente.setCognome(rs.getString("acqu_cognome"));
                
                utente venditore = new utente();
                venditore.setId(rs.getInt("venditore_id"));
                venditore.setNome(rs.getString("vend_nome"));
                venditore.setCognome(rs.getString("vend_cognome"));
                
                Annuncio annuncio = new Annuncio();
                annuncio.setId(rs.getInt("annuncio_id"));
                annuncio.setTitolo(rs.getString("annuncio_titolo"));
                
                String commento = rs.getString("commento");
                int punteggio = rs.getInt("punteggio");
                
                Recensioni recensione = new Recensioni(acquirente, venditore, annuncio, commento, punteggio);
                recensione.setId(rs.getInt("id"));
                
                Timestamp timestamp = rs.getTimestamp("data_recensione");
                if (timestamp != null) {
                    recensione.setDataRecensione(timestamp.toLocalDateTime());
                }
                
                recensione.setVisibile(rs.getBoolean("visibile"));
                recensioni.add(recensione);
            }
            
        } catch (SQLException e) {
            System.err.println("Errore recupero recensioni per venditore " + idVenditore + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return recensioni;
    }

    // METODO: Recupera statistiche complete per venditore
    public StatisticheEconomiche getStatisticheVenditore(int venditoreId) {
        String sql = "SELECT * FROM statistiche_recensioni_venditore WHERE venditore_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int numeroRecensioni = rs.getInt("numero_recensioni");
                double punteggioMedio = rs.getDouble("punteggio_medio");
                int punteggioMinimo = rs.getInt("punteggio_minimo");
                int punteggioMassimo = rs.getInt("punteggio_massimo");
                
                // Crea una lista di punteggi fittizia per il costruttore
                List<Float> punteggi = new ArrayList<>();
                for (int i = 0; i < numeroRecensioni; i++) {
                    punteggi.add((float) punteggioMedio);
                }
                
                return new StatisticheEconomiche(
                    punteggi,
                    LocalDateTime.now().minusMonths(1).toLocalDate(),
                    LocalDateTime.now().toLocalDate()
                );
            }
            
        } catch (SQLException e) {
            System.err.println("Errore recupero statistiche venditore " + venditoreId + ": " + e.getMessage());
        }
        
        // Ritorna statistiche vuote in caso di errore o nessun dato
        return new StatisticheEconomiche(
            new ArrayList<>(),
            LocalDateTime.now().minusMonths(1).toLocalDate(),
            LocalDateTime.now().toLocalDate()
        );
    }

    // METODO SEMPLICE: Solo punteggio medio annuncio (per compatibilità)
    public double getPunteggioMedioAnnuncio(int annuncioId) {
        String sql = "SELECT punteggio_medio FROM statistiche_recensioni_annuncio WHERE annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, annuncioId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("punteggio_medio");
            }
        } catch (SQLException e) {
            System.err.println("Errore calcolo punteggio medio annuncio " + annuncioId + ": " + e.getMessage());
        }
        
        return 0.0;
    }

    // METODO SEMPLICE: Solo punteggio medio venditore (per compatibilità)
    public double getPunteggioMedioVenditore(int venditoreId) {
        String sql = "SELECT punteggio_medio FROM statistiche_recensioni_venditore WHERE venditore_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("punteggio_medio");
            }
        } catch (SQLException e) {
            System.err.println("Errore calcolo punteggio medio venditore " + venditoreId + ": " + e.getMessage());
        }
        
        return 0.0;
    }

    // METODO: Verifica se l'utente ha già recensito l'annuncio
    public boolean haGiaRecensito(int idAcquirente, int idAnnuncio) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE acquirente_id = ? AND annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idAcquirente);
            stmt.setInt(2, idAnnuncio);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Errore durante la verifica recensione esistente: " + e.getMessage());
        }
        
        return false;
    }
    
    // METODO: Inserisce una nuova recensione
    public boolean inserisciRecensione(Recensioni recensione) {
        String sql = "INSERT INTO " + TABLE_NAME + " (acquirente_id, venditore_id, annuncio_id, commento, punteggio) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, recensione.getAcquirente().getId());
            stmt.setInt(2, recensione.getVenditore().getId());
            stmt.setInt(3, recensione.getAnnuncio().getId());
            stmt.setString(4, recensione.getCommento());
            stmt.setInt(5, recensione.getPunteggio());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore durante l'inserimento della recensione: " + e.getMessage());
            return false;
        }
    }

    // METODO: Metodo legacy per compatibilità
    public boolean aggiungiRecensione(Recensioni recensione) {
        return inserisciRecensione(recensione);
    }

    // METODO DEBUG: Stampa la struttura delle tabelle
    public void debugSchema() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            // Verifica tabella annuncio
            String sqlAnnunci = "SELECT column_name, data_type FROM information_schema.columns " +
                             "WHERE table_name = 'annuncio' ORDER BY ordinal_position";
            
            PreparedStatement stmt = conn.prepareStatement(sqlAnnunci);
            ResultSet rs = stmt.executeQuery();
            
            System.out.println("=== SCHEMA TABELLA ANNUNCIO ===");
            while (rs.next()) {
                System.out.println(rs.getString("column_name") + " - " + rs.getString("data_type"));
            }
            
            // Verifica tabella recensione
            String sqlRecensioni = "SELECT column_name, data_type FROM information_schema.columns " +
                                 "WHERE table_name = 'recensione' ORDER BY ordinal_position";
            
            stmt = conn.prepareStatement(sqlRecensioni);
            rs = stmt.executeQuery();
            
            System.out.println("=== SCHEMA TABELLA RECENSIONE ===");
            while (rs.next()) {
                System.out.println(rs.getString("column_name") + " - " + rs.getString("data_type"));
            }

            // Verifica viste statistiche
            String sqlViste = "SELECT table_name FROM information_schema.views " +
                            "WHERE table_schema = 'public' AND table_name LIKE 'statistiche%'";
            
            stmt = conn.prepareStatement(sqlViste);
            rs = stmt.executeQuery();
            
            System.out.println("=== VISTE STATISTICHE DISPONIBILI ===");
            while (rs.next()) {
                System.out.println(rs.getString("table_name"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Classe helper per restituire recensioni + statistiche
    public static class StatisticheRecensioni {
        private final List<Recensioni> recensioni;
        private final StatisticheEconomiche statistiche;
        
        public StatisticheRecensioni(List<Recensioni> recensioni, StatisticheEconomiche statistiche) {
            this.recensioni = recensioni;
            this.statistiche = statistiche;
        }
        
        public List<Recensioni> getRecensioni() { return recensioni; }
        public StatisticheEconomiche getStatistiche() { return statistiche; }
        
        // Metodo helper per ottenere il punteggio medio
        public double getPunteggioMedio() {
            return statistiche.getValoreMedio();
        }
        
        // Metodo helper per ottenere il numero di recensioni
        public int getNumeroRecensioni() {
            return recensioni.size();
        }
    }
}