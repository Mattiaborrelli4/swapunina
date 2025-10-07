package application.messagistica;

import application.DB.ConnessioneDB;
import application.DB.SessionManager;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;
import application.Classe.Annuncio;
import application.Classe.Oggetto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import schermata.button.MessaggiDialog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.File;
import java.math.BigDecimal;
import java.security.Key;

public class ChatListDialog extends Stage {
    private final int currentUserId;

    public ChatListDialog() {
        this.currentUserId = SessionManager.getCurrentUserId();
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Le tue chat");
        setWidth(700);
        setHeight(500);

        TableView<ChatSummary> table = new TableView<>();
        table.setItems(getChatsForUser());

        TableColumn<ChatSummary, String> venditoreCol = new TableColumn<>("Venditore");
        venditoreCol.setCellValueFactory(cellData -> cellData.getValue().venditoreNomeProperty());
        venditoreCol.setPrefWidth(150);

        TableColumn<ChatSummary, String> annuncioCol = new TableColumn<>("Annuncio");
        annuncioCol.setCellValueFactory(cellData -> cellData.getValue().annuncioTitoloProperty());
        annuncioCol.setPrefWidth(200);

        TableColumn<ChatSummary, String> messaggioCol = new TableColumn<>("Ultimo messaggio");
        messaggioCol.setCellValueFactory(cellData -> cellData.getValue().ultimoMessaggioProperty());
        messaggioCol.setPrefWidth(250);

        TableColumn<ChatSummary, String> timeCol = new TableColumn<>("Data");
        timeCol.setCellValueFactory(cellData -> cellData.getValue().timestampFormattedProperty());
        timeCol.setPrefWidth(100);

        table.getColumns().addAll(venditoreCol, annuncioCol, messaggioCol, timeCol);

        // Doppio click per aprire la chat
        table.setRowFactory(tv -> {
            TableRow<ChatSummary> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ChatSummary selectedChat = row.getItem();
                    openChat(selectedChat.getAnnuncioId());
                    close();
                }
            });
            return row;
        });

        VBox layout = new VBox(10, table);
        layout.setPadding(new Insets(10));
        Scene scene = new Scene(layout);
        setScene(scene);
    }

    private ObservableList<ChatSummary> getChatsForUser() {
        ObservableList<ChatSummary> chats = FXCollections.observableArrayList();
        try (Connection connection = ConnessioneDB.getConnessione()) {
            
            // QUERY CORRETTA - tabella "utente" invece di "utenti"
            String query = "SELECT DISTINCT ON (a.id) " +
                          "a.id as annuncio_id, " +
                          "o.nome as annuncio_titolo, " + // Cambiato da o.titolo a o.nome
                          "CASE " +
                          "  WHEN m.mittente_id = ? THEN u_dest.nome " +
                          "  WHEN m.destinatario_id = ? THEN u_mitt.nome " +
                          "END as interlocutore_nome, " +
                          "m.id as messaggio_id, " +
                          "m.data_invio, " +
                          "m.testo_plaintext_backup " +
                          "FROM messaggio m " +
                          "INNER JOIN annuncio a ON m.annuncio_id = a.id " +
                          "INNER JOIN oggetto o ON a.oggetto_id = o.id " +
                          "INNER JOIN utente u_mitt ON m.mittente_id = u_mitt.id " +
                          "INNER JOIN utente u_dest ON m.destinatario_id = u_dest.id " +
                          "WHERE (m.mittente_id = ? OR m.destinatario_id = ?) " +
                          "ORDER BY a.id, m.data_invio DESC";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, currentUserId);
            stmt.setInt(3, currentUserId);
            stmt.setInt(4, currentUserId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int annuncioId = rs.getInt("annuncio_id");
                String annuncioTitolo = rs.getString("annuncio_titolo");
                String interlocutoreNome = rs.getString("interlocutore_nome");
                int messaggioId = rs.getInt("messaggio_id");
                LocalDateTime timestamp = rs.getTimestamp("data_invio").toLocalDateTime();
                
                String ultimoMessaggio = rs.getString("testo_plaintext_backup");
                if (ultimoMessaggio == null || ultimoMessaggio.trim().isEmpty()) {
                    ultimoMessaggio = decriptaMessaggio(messaggioId);
                }
                
                chats.add(new ChatSummary(annuncioId, annuncioTitolo, interlocutoreNome, 
                                        ultimoMessaggio, timestamp));
            }
            
        } catch (SQLException e) {
            System.err.println("ERRORE SQL: " + e.getMessage());
            e.printStackTrace();
            showAlert("Errore Database", "Impossibile caricare le chat: " + e.getMessage());
        }
        return chats;
    } 

    private void debugRicercaChatAlternative() {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            System.out.println("DEBUG: Provo query alternativa...");
            
            // Query alternativa più semplice - CORRETTA
            String query = "SELECT COUNT(*) as total FROM messaggio WHERE mittente_id = ? OR destinatario_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, currentUserId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int totalMessages = rs.getInt("total");
                System.out.println("DEBUG: Messaggi totali per utente: " + totalMessages);
            }
            
            // Cerca annunci con messaggi - CORRETTA
            query = "SELECT DISTINCT annuncio_id FROM messaggio WHERE mittente_id = ? OR destinatario_id = ?";
            stmt = connection.prepareStatement(query);
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, currentUserId);
            rs = stmt.executeQuery();
            
            int annunciCount = 0;
            while (rs.next()) {
                annunciCount++;
                System.out.println("DEBUG: Annuncio con messaggi: " + rs.getInt("annuncio_id"));
            }
            System.out.println("DEBUG: Annunci con messaggi: " + annunciCount);
            
        } catch (SQLException e) {
            System.err.println("ERRORE debug: " + e.getMessage());
        }
    }

    private String decriptaMessaggio(int messaggioId) {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            String query = "SELECT testo_plaintext_backup, testo_encrypted, iv, algoritmo_encryption, key_id " +
                          "FROM messaggio WHERE id = ?";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, messaggioId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // Prima prova con il testo in chiaro (backup)
                String testoPlaintext = rs.getString("testo_plaintext_backup");
                if (testoPlaintext != null && !testoPlaintext.trim().isEmpty()) {
                    return testoPlaintext;
                }
                
                // Se non c'è backup, decripta il testo criptato
                byte[] testoEncrypted = rs.getBytes("testo_encrypted");
                byte[] iv = rs.getBytes("iv");
                String algoritmo = rs.getString("algoritmo_encryption");
                int keyId = rs.getInt("key_id");
                
                if (testoEncrypted != null && iv != null && algoritmo != null) {
                    return decriptaTesto(testoEncrypted, iv, algoritmo, keyId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Errore nella decrittografia del messaggio ID: " + messaggioId);
            e.printStackTrace();
        }
        return "Messaggio criptato";
    }

    private String decriptaTesto(byte[] testoEncrypted, byte[] iv, String algoritmo, int keyId) {
        try {
            // Recupera la chiave di crittografia dal database
            String chiaveSegreta = recuperaChiaveCrittografia(keyId);
            if (chiaveSegreta == null) {
                return "Errore: chiave di crittografia non trovata";
            }

            // Configura il cipher per la decrittografia
            Cipher cipher = Cipher.getInstance(algoritmo);
            SecretKeySpec keySpec = new SecretKeySpec(chiaveSegreta.getBytes(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            
            // Decripta il testo
            byte[] testoDecriptato = cipher.doFinal(testoEncrypted);
            return new String(testoDecriptato);
            
        } catch (Exception e) {
            System.err.println("Errore nella decrittografia: " + e.getMessage());
            return "Errore decrittografia";
        }
    }

    private String recuperaChiaveCrittografia(int keyId) {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            String query = "SELECT chiave_segreta FROM encryption_keys WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, keyId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("chiave_segreta");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void openChat(int annuncioId) {
        Annuncio annuncio = getAnnuncioById(annuncioId);
        if (annuncio != null) {
            // Crea una finestra di messaggistica per questo annuncio
            MessaggiDialog dialog = new MessaggiDialog(annuncio, currentUserId);
            dialog.showAndWait();
            
            // Ricarica la lista dopo aver chiuso la chat
            initializeUI();
        } else {
            showAlert("Errore", "Impossibile aprire la chat: annuncio non trovato.");
        }
    }

    private Annuncio getAnnuncioById(int annuncioId) {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            // Correggi la query: cambia o.immagine_url in o.image_url
            String query = "SELECT a.*, o.id as oggetto_id, o.nome as oggetto_nome, " +
                          "o.descrizione as oggetto_descrizione, o.origine as oggetto_origine, " +
                          "o.categoria as oggetto_categoria, o.image_url as oggetto_image_url " +
                          "FROM annuncio a " +
                          "INNER JOIN oggetto o ON a.oggetto_id = o.id " +
                          "WHERE a.id = ?";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, annuncioId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Annuncio annuncio = new Annuncio();
                annuncio.setId(rs.getInt("id"));
                annuncio.setVenditoreId(rs.getInt("venditore_id"));
                
                // Prezzo
                BigDecimal prezzoBigDecimal = rs.getBigDecimal("prezzo");
                annuncio.setPrezzo(prezzoBigDecimal != null ? prezzoBigDecimal.doubleValue() : 0.0);
                
                // Crea oggetto
                int oggettoId = rs.getInt("oggetto_id");
                String nome = rs.getString("oggetto_nome");
                String descrizione = rs.getString("oggetto_descrizione");
                String imageUrl = rs.getString("oggetto_image_url"); // Modificato qui
                
                // Gestisci enum
                OrigineOggetto origine = OrigineOggetto.USATO;
                String origineStr = rs.getString("oggetto_origine");
                if (origineStr != null) {
                    try {
                        origine = OrigineOggetto.valueOf(origineStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.err.println("Origine non valida: " + origineStr);
                    }
                }
                
                Categoria categoria = Categoria.ALTRO;
                String categoriaStr = rs.getString("oggetto_categoria");
                if (categoriaStr != null) {
                    try {
                        categoria = Categoria.valueOf(categoriaStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        System.err.println("Categoria non valida: " + categoriaStr);
                    }
                }
                
                // Crea l'oggetto
                Oggetto oggetto = new Oggetto(oggettoId, nome, descrizione, categoria, imageUrl, null, origine);
                
                annuncio.setOggetto(oggetto);
                annuncio.setTitolo(nome);
                
                // Altri campi
                Timestamp timestamp = rs.getTimestamp("data_pubblicazione");
                if (timestamp != null) {
                    annuncio.setDataPubblicazione(timestamp.toLocalDateTime());
                }
                
                return annuncio;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}