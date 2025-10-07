package test;

import static org.junit.Assert.*;
import org.junit.*;
import java.sql.*;

public class TestRegistrazione {

    private static Connection conn;

    @BeforeClass
    public static void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tuo_database", "utente", "password");
    }

    @Test
    public void testRegistrazioneNuovoUtente() throws Exception {
        String matricola = "12345678";
        String email = "nuovo@example.com";
        String nome = "Mario";
        String cognome = "Rossi";
        String password = "pass123";

        PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO utenti (matricola, email, nome, cognome, password) VALUES (?, ?, ?, ?, ?)"
        );
        stmt.setString(1, matricola);
        stmt.setString(2, email);
        stmt.setString(3, nome);
        stmt.setString(4, cognome);
        stmt.setString(5, password);

        int rows = stmt.executeUpdate();
        assertEquals("Un utente dovrebbe essere inserito", 1, rows);
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (conn != null) conn.close();
    }
}
