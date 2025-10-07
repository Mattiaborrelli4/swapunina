package test;

import static org.junit.Assert.*;
import org.junit.*;
import java.sql.*;

public class TestAccesso {

    private static Connection conn;

    @BeforeClass
    public static void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tuo_database", "utente", "password");
    }

    @Test
    public void testAccessoUtenteValido() throws Exception {
        String email = "test@example.com";
        String password = "password123";

        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM utenti WHERE email = ? AND password = ?");
        stmt.setString(1, email);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        assertTrue("Utente dovrebbe esistere", rs.next());
    }

    @Test
    public void testAccessoUtenteInvalido() throws Exception {
        String email = "fake@example.com";
        String password = "wrongpass";

        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM utenti WHERE email = ? AND password = ?");
        stmt.setString(1, email);
        stmt.setString(2, password);
        ResultSet rs = stmt.executeQuery();

        assertFalse("Utente non dovrebbe esistere", rs.next());
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (conn != null) conn.close();
    }
}
