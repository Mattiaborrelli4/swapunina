package application.messagistica;

import application.Classe.utente;
import application.DB.MessaggioDAO;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import schermata.FinestraMessaggi;
import application.DB.SessionManager;

import java.util.List;


public class ListaConversazioni {

    public ListaConversazioni(int mioId) {
        Stage stage = new Stage();
        stage.setTitle("Le tue conversazioni");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        MessaggioDAO dao = new MessaggioDAO();
        List<utente> interlocutori = dao.getInterlocutoriUtenti(mioId);

        if (interlocutori.isEmpty()) {
            Label noChats = new Label("Non hai ancora contattato nessuno.");
            root.getChildren().add(noChats);
        } else {
            for (utente u : interlocutori) {
                Button btn = new Button("Chat con " + u.getNome() + " " + u.getCognome());
                btn.setOnAction(e -> new FinestraMessaggi(mioId, u.getId(), u.getNome()));
                root.getChildren().add(btn);
            }
        }


        Scene scene = new Scene(root, 300, 400);
        stage.setScene(scene);
        stage.show();
    }
}