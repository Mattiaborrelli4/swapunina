package application.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnessioneDB {
    private static final String HOST_DB = "localhost";
    private static final int PORTA_DB = 5432;
    private static final String NOME_DB = "postgres";
    private static final String UTENTE_DB = "postgres";
    private static final String PASSWORD_DB = "1234";

    public static Connection getConnessione() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL non trovato", e);
        }

        String url = String.format("jdbc:postgresql://%s:%d/%s", HOST_DB, PORTA_DB, NOME_DB);
        Properties proprieta = new Properties();
        proprieta.setProperty("user", UTENTE_DB);
        proprieta.setProperty("password", PASSWORD_DB);

        return DriverManager.getConnection(url, proprieta);
    }

    public static boolean verificaConnessione() {
        try (Connection conn = getConnessione()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Errore di connessione al database: " + e.getMessage());
            return false;
        }
    }


}