package application.Classe;

import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import schermata.button.CarrelloManager;
import application.DB.SessionManager;
import application.DB.MessaggioDAO;

import java.util.Optional;

public class AzioneAnnuncioHandler {
    private final MessaggioDAO messaggioDAO;
    
    public AzioneAnnuncioHandler() {
        this.messaggioDAO = new MessaggioDAO();
    }
    
    public void gestisciAzione(Annuncio annuncio) {
        if (annuncio == null) return;

        if (!annuncio.isDisponibile()) {
            mostraMessaggio("Questo annuncio non è più disponibile");
            return;
        }

        if (!isUtenteLoggato()) {
            mostraMessaggio("Devi essere loggato per effettuare questa azione");
            return;
        }

        if (annuncio.getTipologia() == Tipologia.ASTA) {
            faiOfferta(annuncio);
        } else if (isVendita(annuncio)) {
            aggiungiAlCarrello(annuncio);
        } else if (annuncio.getOggetto() != null && 
                  annuncio.getOggetto().getOrigine() == OrigineOggetto.SCAMBIO) {
            proponiScambio(annuncio);
        } else {
            contattaPerRegalo(annuncio);
        }
    }
    
    private boolean isVendita(Annuncio a) {
        if (a == null) return false;
        if (a.getTipologia() == Tipologia.ASTA) return false;

        double prezzo = a.getPrezzo();
        OrigineOggetto org = (a.getOggetto() != null) ? a.getOggetto().getOrigine() : null;

        if (org == OrigineOggetto.SCAMBIO || org == OrigineOggetto.REGALO) return false;
        return prezzo > 0;
    }
    
    private boolean isUtenteLoggato() {
        return SessionManager.getCurrentUserId() != -1;
    }
    
    private void faiOfferta(Annuncio annuncio) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fai un'offerta");
        dialog.setHeaderText("Inserisci la tua offerta per: " + annuncio.getTitolo());
        dialog.setContentText("Offerta (€):");
        
        Optional<String> risultato = dialog.showAndWait();
        risultato.ifPresent(offertaStr -> {
            try {
                double offerta = Double.parseDouble(offertaStr);
                // Invia messaggio al venditore
                String messaggioTesto = String.format("Ho fatto un'offerta di €%.2f per il tuo articolo '%s'", 
                        offerta, annuncio.getTitolo());
                
                Messaggio messaggio = new Messaggio(
                    SessionManager.getCurrentUserId(),
                    annuncio.getVenditoreId(),
                    messaggioTesto,
                    annuncio.getId()
                );
                
                messaggioDAO.inviaMessaggio(messaggio);
                mostraMessaggio("Offerta inviata con successo!");
                
            } catch (NumberFormatException e) {
                mostraMessaggio("Inserisci un importo valido");
            }
        });
    }
    
    private void aggiungiAlCarrello(Annuncio annuncio) {
        CarrelloManager.getInstance().aggiungiAlCarrello(annuncio);
        mostraMessaggio("Articolo aggiunto al carrello!");
    }
    
    private void proponiScambio(Annuncio annuncio) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Proposta di scambio");
        dialog.setHeaderText("Descrivi cosa vorresti scambiare per: " + annuncio.getTitolo());
        dialog.setContentText("La tua proposta:");
        
        Optional<String> risultato = dialog.showAndWait();
        risultato.ifPresent(proposta -> {
            // Invia messaggio al venditore
            String messaggioTesto = String.format("Vorrei proporre uno scambio per il tuo articolo '%s': %s", 
                    annuncio.getTitolo(), proposta);
            
            Messaggio messaggio = new Messaggio(
                SessionManager.getCurrentUserId(),
                annuncio.getVenditoreId(),
                messaggioTesto,
                annuncio.getId()
            );
            
            messaggioDAO.inviaMessaggio(messaggio);
            mostraMessaggio("Proposta di scambio inviata!");
        });
    }
    
    private void contattaPerRegalo(Annuncio annuncio) {
        // Invia messaggio automatico per regalo
        String messaggioTesto = String.format("Sono interessato al tuo articolo in regalo '%s'", 
                annuncio.getTitolo());
        
        Messaggio messaggio = new Messaggio(
            SessionManager.getCurrentUserId(),
            annuncio.getVenditoreId(),
            messaggioTesto,
            annuncio.getId()
        );
        
        messaggioDAO.inviaMessaggio(messaggio);
        mostraMessaggio("Richiesta inviata! Il venditore ti contatterà presto.");
    }
    
    private void mostraMessaggio(String testo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(testo);
        alert.showAndWait();
    }
}