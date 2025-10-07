package schermata;

import application.Classe.Messaggio;
import application.DB.MessaggioDAO;
import application.DB.UtentiDAO;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class FinestraMessaggi {
    private final int utente1;
    private final int utente2;
    private final String nomeInterlocutore;

    // Costruttore esistente
    public FinestraMessaggi(int utente1, int utente2, String nomeInterlocutore) {
        this.utente1 = utente1;
        this.utente2 = utente2;
        this.nomeInterlocutore = nomeInterlocutore;
        mostraFinestra();
    }
    
    // Nuovo costruttore
    public FinestraMessaggi(int utente1, int utente2) {
        this(utente1, utente2, ottenereNomeUtente(utente2));
    }
    
    private static String ottenereNomeUtente(int userId) {
        UtentiDAO utentiDAO = new UtentiDAO();
        return utentiDAO.getNomeUtenteById(userId);
    }
    
    private void mostraFinestra() {
        Stage stage = new Stage();
        stage.setTitle("Conversazione con " + nomeInterlocutore);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        TextArea areaMessaggi = new TextArea();
        areaMessaggi.setEditable(false);
        areaMessaggi.setWrapText(true);
        aggiornaMessaggi(areaMessaggi);

        TextField input = new TextField();
        input.setPromptText("Scrivi un messaggio...");

        Button inviaBtn = new Button("Invia");
        inviaBtn.setOnAction(e -> {
            String testo = input.getText().trim();
            if (!testo.isEmpty()) {
                Messaggio nuovo = new Messaggio(utente1, utente2, testo, null);
                new MessaggioDAO().inviaMessaggio(nuovo);
                input.clear();
                aggiornaMessaggi(areaMessaggi);
            }
        });

        HBox box = new HBox(10, input, inviaBtn);
        box.setHgrow(input, Priority.ALWAYS);

        root.getChildren().addAll(areaMessaggi, box);

        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    private void aggiornaMessaggi(TextArea area) {
        MessaggioDAO dao = new MessaggioDAO();
        List<Messaggio> conversazione = dao.getConversazione(utente1, utente2);
        StringBuilder testo = new StringBuilder();
        for (Messaggio m : conversazione) {
            // Determine if the message is from the current user or the interlocutor
            String prefisso = (m.getMittenteId() == utente1) ? "Tu: " : (nomeInterlocutore + ": ");
            testo.append(prefisso).append(m.getTesto()).append("\n");
        }
        area.setText(testo.toString());
    }
}