package application.DB;

import application.Classe.Conto;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestisce le operazioni di conto e movimenti finanziari nel database
 * Fornisce metodi per creazione conti, ricariche, acquisti e trasferimenti
 */
public class ContoDAO {
    private static final String TABLE_NAME = "conto";
    private static final String MOVIMENTI_TABLE = "movimento_conto";

    public ContoDAO() {
        creaTabelleSeMancanti();
    }

    /**
     * Crea le tabelle conto e movimenti se non esistono
     */
    private void creaTabelleSeMancanti() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            String sqlConto = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "utente_id INTEGER UNIQUE NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                    "saldo DECIMAL(10,2) NOT NULL DEFAULT 0.0)";
            
            String sqlMovimenti = "CREATE TABLE IF NOT EXISTS " + MOVIMENTI_TABLE + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "conto_id INTEGER NOT NULL REFERENCES conto(id) ON DELETE CASCADE, " +
                    "importo DECIMAL(10,2) NOT NULL, " +
                    "tipo VARCHAR(50) NOT NULL, " +
                    "descrizione TEXT, " +
                    "data_operazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt1 = conn.prepareStatement(sqlConto);
                 PreparedStatement stmt2 = conn.prepareStatement(sqlMovimenti)) {
                stmt1.execute();
                stmt2.execute();
            }
        } catch (SQLException e) {
            System.err.println("Errore nella creazione delle tabelle per Conto: " + e.getMessage());
        }
    }

    /**
     * Crea un conto per un utente se non esiste già
     */
    public Conto creaContoSeMancante(int utenteId) {
        if (utenteId <= 0) {
            System.err.println("ID utente non valido per creazione conto: " + utenteId);
            return null;
        }
        
        if (!utenteEsiste(utenteId)) {
            System.err.println("Utente non trovato nel database: " + utenteId);
            return null;
        }
        
        Conto conto = getContoByUtenteId(utenteId);
        return conto != null ? conto : creaConto(utenteId);
    }

    /**
     * Verifica se un utente esiste nel database
     */
    private boolean utenteEsiste(int utenteId) {
        String sql = "SELECT 1 FROM utente WHERE id = ? LIMIT 1";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Errore nella verifica esistenza utente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Crea un nuovo conto per un utente
     */
    public Conto creaConto(int utenteId) {
        if (utenteId <= 0 || !utenteEsiste(utenteId)) {
            System.err.println("Impossibile creare conto: utente " + utenteId + " non valido");
            return null;
        }
        
        String sql = "INSERT INTO " + TABLE_NAME + " (utente_id, saldo) VALUES (?, 0.0) RETURNING id";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Conto conto = new Conto(utenteId);
                    conto.setId(rs.getInt("id"));
                    return conto;
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nella creazione del conto per utente: " + utenteId);
        }
        return null;
    }

    /**
     * Recupera il conto di un utente con i relativi movimenti
     */
    public Conto getContoByUtenteId(int utenteId) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Conto conto = new Conto(utenteId);
                    conto.setId(rs.getInt("id"));
                    conto.accredita(rs.getBigDecimal("saldo"), "Saldo iniziale");
                    
                    caricaMovimenti(conto);
                    return conto;
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero conto per utente: " + utenteId);
        }
        return null;
    }

    /**
     * Aggiorna il saldo del conto nel database
     */
    public boolean aggiornaSaldo(Conto conto) {
        String sql = "UPDATE " + TABLE_NAME + " SET saldo = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, conto.getSaldo());
            stmt.setInt(2, conto.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiornamento del saldo per conto: " + conto.getId());
            return false;
        }
    }

    /**
     * Registra un movimento nel conto con transazione
     */
    public boolean registraMovimento(Conto conto, Conto.Movimento movimento) {
        String sql = "INSERT INTO " + MOVIMENTI_TABLE + " (conto_id, importo, tipo, descrizione, data_operazione) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conto.getId());
            stmt.setBigDecimal(2, movimento.getImporto());
            stmt.setString(3, movimento.getTipo().name());
            stmt.setString(4, movimento.getDescrizione());
            stmt.setTimestamp(5, Timestamp.valueOf(movimento.getData()));
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Errore nella registrazione del movimento per conto: " + conto.getId());
            return false;
        }
    }

    /**
     * Carica i movimenti del conto dalla base dati
     */
    private void caricaMovimenti(Conto conto) {
        // Questo metodo è mantenuto per compatibilità ma non fa più nulla
        // poiché i movimenti vengono gestiti internamente dalla classe Conto
    }

    /**
     * Ricarica il conto di un utente con un importo specifico
     */
    public boolean ricaricaConto(int utenteId, BigDecimal importo, String metodoPagamento) {
        Conto conto = creaContoSeMancante(utenteId);
        if (conto != null) {
            conto.ricarica(importo, metodoPagamento);
            List<Conto.Movimento> movimenti = conto.getMovimenti();
            
            if (!movimenti.isEmpty()) {
                boolean success = aggiornaSaldo(conto) && 
                                registraMovimento(conto, movimenti.get(movimenti.size() - 1));
                return success;
            }
        }
        return false;
    }

    /**
     * Verifica se l'utente ha saldo sufficiente per un acquisto
     */
    public boolean verificaSaldoSufficiente(int utenteId, BigDecimal importoRichiesto) {
        Conto conto = getContoByUtenteId(utenteId);
        return conto != null && conto.saldoSufficiente(importoRichiesto);
    }

    /**
     * Effettua un acquisto scalando il saldo con transazione
     */
    public boolean effettuaAcquisto(int utenteId, BigDecimal importo, String descrizione) {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            
            try {
                Conto conto = getContoByUtenteId(utenteId);
                if (conto == null || !conto.saldoSufficiente(importo)) {
                    conn.rollback();
                    return false;
                }
                
                boolean successAcquisto = conto.effettuaAcquisto(importo, descrizione);
                if (!successAcquisto) {
                    conn.rollback();
                    return false;
                }
                
                List<Conto.Movimento> movimenti = conto.getMovimenti();
                boolean successUpdate = aggiornaSaldo(conto);
                boolean successMovimento = registraMovimento(conto, movimenti.get(movimenti.size() - 1));
                
                if (successUpdate && successMovimento) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("Errore durante l'acquisto per utente: " + utenteId);
            return false;
        }
    }

    /**
     * Trasferisce fondi da acquirente a venditore con transazione atomica
     */
    public boolean trasferisciFondi(int acquirenteId, int venditoreId, BigDecimal importo, String descrizione) {
        if (acquirenteId <= 0 || venditoreId <= 0 || importo.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("Parametri non validi per trasferimento");
            return false;
        }
        
        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            
            try {
                // Verifica saldo acquirente
                Conto contoAcquirente = getContoByUtenteId(acquirenteId);
                if (contoAcquirente == null || !contoAcquirente.saldoSufficiente(importo)) {
                    conn.rollback();
                    return false;
                }
                
                // Assicura che il venditore abbia un conto
                Conto contoVenditore = creaContoSeMancante(venditoreId);
                if (contoVenditore == null) {
                    conn.rollback();
                    return false;
                }
                
                // Esegue operazioni
                boolean successAcquisto = contoAcquirente.effettuaAcquisto(importo, descrizione);
                if (!successAcquisto) {
                    conn.rollback();
                    return false;
                }
                
                contoVenditore.accredita(importo, "Vendita: " + descrizione);
                
                // Aggiorna database
                List<Conto.Movimento> movimentiAcquirente = contoAcquirente.getMovimenti();
                List<Conto.Movimento> movimentiVenditore = contoVenditore.getMovimenti();
                
                boolean successUpdate1 = aggiornaSaldo(contoAcquirente);
                boolean successUpdate2 = aggiornaSaldo(contoVenditore);
                boolean successMovimento1 = registraMovimento(contoAcquirente, 
                    movimentiAcquirente.get(movimentiAcquirente.size() - 1));
                boolean successMovimento2 = registraMovimento(contoVenditore, 
                    movimentiVenditore.get(movimentiVenditore.size() - 1));
                
                if (successUpdate1 && successUpdate2 && successMovimento1 && successMovimento2) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("Errore durante il trasferimento fondi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ottiene lo storico movimenti di un conto come semplice record
     */
    public List<Object[]> getStoricoMovimenti(int utenteId, int limit) {
        List<Object[]> movimenti = new ArrayList<>();
        String sql = "SELECT m.importo, m.tipo, m.descrizione, m.data_operazione " +
                    "FROM " + MOVIMENTI_TABLE + " m " +
                    "JOIN " + TABLE_NAME + " c ON m.conto_id = c.id " +
                    "WHERE c.utente_id = ? ORDER BY m.data_operazione DESC LIMIT ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Object[] movimento = new Object[] {
                        rs.getBigDecimal("importo"),
                        rs.getString("tipo"),
                        rs.getString("descrizione"),
                        rs.getTimestamp("data_operazione").toLocalDateTime()
                    };
                    movimenti.add(movimento);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero storico movimenti: " + e.getMessage());
        }
        return movimenti;
    }

    /**
     * Ottiene il saldo corrente di un utente
     */
    public BigDecimal getSaldoCorrente(int utenteId) {
        String sql = "SELECT saldo FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("saldo");
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero saldo: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}