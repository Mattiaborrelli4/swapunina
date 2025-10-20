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
import java.util.function.Consumer;

/**
 * Gestore centrale per tutte le azioni sugli annunci
 * Coordina operazioni come offerte, acquisti, scambi e gestione annunci
 */
public class AzioneAnnuncioHandler {
    private final MessaggioDAO messaggioDAO;
    private Consumer<Annuncio> onAnnuncioModificato;
    private Consumer<Annuncio> onAnnuncioEliminato;
    
    public AzioneAnnuncioHandler() {
        this.messaggioDAO = new MessaggioDAO();
    }
    
    /**
     * Imposta callback per notificare modifica annuncio
     */
    public void setOnAnnuncioModificato(Consumer<Annuncio> callback) {
        this.onAnnuncioModificato = callback;
    }
    
    /**
     * Imposta callback per notificare eliminazione annuncio
     */
    public void setOnAnnuncioEliminato(Consumer<Annuncio> callback) {
        this.onAnnuncioEliminato = callback;
    }
    
    /**
     * Gestisce l'azione principale su un annuncio in base al suo tipo
     * @param annuncio L'annuncio su cui eseguire l'azione
     */
    public void gestisciAzione(Annuncio annuncio) {
        if (annuncio == null) return;

        // Controlli preliminari
        if (!annuncio.isDisponibile()) {
            mostraMessaggio("Questo annuncio non è più disponibile");
            return;
        }

        if (!isUtenteLoggato()) {
            mostraMessaggio("Devi essere loggato per effettuare questa azione");
            return;
        }

        // Routing in base al tipo di annuncio
        if (annuncio.getTipologia() == Tipologia.ASTA) {
            faiOfferta(annuncio);
        } else if (isVenditaDiretta(annuncio)) {
            aggiungiAlCarrello(annuncio);
        } else if (isScambio(annuncio)) {
            proponiScambio(annuncio);
        } else {
            contattaPerRegalo(annuncio);
        }
    }
    
    /**
     * Gestisce la modifica di un annuncio esistente
     * @param annuncio L'annuncio da modificare
     */
    public void gestisciModificaAnnuncio(Annuncio annuncio) {
        if (annuncio == null) return;
        
        // TODO: Implementare logica di modifica annuncio
        // Aprire dialog di modifica simile a InserisciAnnuncioDialog
        
        // Notifica modifica annuncio
        if (onAnnuncioModificato != null) {
            onAnnuncioModificato.accept(annuncio);
        }
    }
    
    /**
     * Gestisce l'eliminazione di un annuncio con conferma
     * @param annuncio L'annuncio da eliminare
     */
    public void gestisciEliminazioneAnnuncio(Annuncio annuncio) {
        if (annuncio == null) return;
        
        // Richiedi conferma eliminazione
        if (confermaEliminazione(annuncio.getTitolo())) {
            // TODO: Implementare eliminazione dal database
            // AnnuncioDAO.eliminaAnnuncio(annuncio.getId());
            
            // Notifica eliminazione annuncio
            if (onAnnuncioEliminato != null) {
                onAnnuncioEliminato.accept(annuncio);
            }
            
            mostraMessaggio("Annuncio eliminato con successo!");
        }
    }
    
    /**
     * Determina se un annuncio è in vendita diretta
     */
    private boolean isVenditaDiretta(Annuncio annuncio) {
        if (annuncio == null) return false;
        if (annuncio.getTipologia() == Tipologia.ASTA) return false;

        // Controlla origine oggetto
        OrigineOggetto origine = (annuncio.getOggetto() != null) 
            ? annuncio.getOggetto().getOrigine() 
            : null;

        boolean nonVendita = origine == OrigineOggetto.SCAMBIO || 
                            origine == OrigineOggetto.REGALO;
        
        return !nonVendita && annuncio.getPrezzo() > 0;
    }
    
    /**
     * Determina se un annuncio è per scambio
     */
    private boolean isScambio(Annuncio annuncio) {
        return annuncio != null && 
               annuncio.getOggetto() != null && 
               annuncio.getOggetto().getOrigine() == OrigineOggetto.SCAMBIO;
    }
    
    private boolean isUtenteLoggato() {
        return SessionManager.getCurrentUserId() != -1;
    }
    
    /**
     * Gestisce la creazione di un'offerta per un'asta
     */
    private void faiOfferta(Annuncio annuncio) {
        TextInputDialog dialog = creaDialog("Fai un'offerta", 
            "Inserisci la tua offerta per: " + annuncio.getTitolo(), "Offerta (€):");
        
        dialog.showAndWait().ifPresent(offertaStr -> {
            try {
                double offerta = Double.parseDouble(offertaStr);
                inviaMessaggioOfferta(annuncio, offerta);
                mostraMessaggio("Offerta inviata con successo!");
            } catch (NumberFormatException e) {
                mostraMessaggio("Inserisci un importo valido");
            }
        });
    }
    
    /**
     * Aggiunge un annuncio al carrello acquisti
     */
    private void aggiungiAlCarrello(Annuncio annuncio) {
        CarrelloManager.getInstance().aggiungiAlCarrello(annuncio);
        mostraMessaggio("Articolo aggiunto al carrello!");
    }
    
    /**
     * Gestisce la proposta di scambio per un annuncio
     */
    private void proponiScambio(Annuncio annuncio) {
        TextInputDialog dialog = creaDialog("Proposta di scambio",
            "Descrivi cosa vorresti scambiare per: " + annuncio.getTitolo(), 
            "La tua proposta:");
        
        dialog.showAndWait().ifPresent(proposta -> {
            inviaMessaggioScambio(annuncio, proposta);
            mostraMessaggio("Proposta di scambio inviata!");
        });
    }
    
    /**
     * Gestisce la richiesta per un articolo in regalo
     */
    private void contattaPerRegalo(Annuncio annuncio) {
        inviaMessaggioRegalo(annuncio);
        mostraMessaggio("Richiesta inviata! Il venditore ti contatterà presto.");
    }
    
    // === METODI DI SUPPORTO ===
    
    /**
     * Crea un dialog di input testuale standardizzato
     */
    private TextInputDialog creaDialog(String titolo, String header, String contenuto) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(titolo);
        dialog.setHeaderText(header);
        dialog.setContentText(contenuto);
        return dialog;
    }
    
    /**
     * Mostra un messaggio informativo all'utente
     */
    private void mostraMessaggio(String testo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(testo);
        alert.showAndWait();
    }
    
    /**
     * Richiede conferma per l'eliminazione
     */
    private boolean confermaEliminazione(String titoloAnnuncio) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Conferma eliminazione");
        alert.setHeaderText("Stai per eliminare l'annuncio: " + titoloAnnuncio);
        alert.setContentText("Questa azione non può essere annullata. Procedere?");
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    // === METODI INVIO MESSAGGI ===
    
    private void inviaMessaggioOfferta(Annuncio annuncio, double offerta) {
        String messaggioTesto = String.format("Ho fatto un'offerta di €%.2f per il tuo articolo '%s'", 
                offerta, annuncio.getTitolo());
        creaEInviaMessaggio(annuncio, messaggioTesto);
    }
    
    private void inviaMessaggioScambio(Annuncio annuncio, String proposta) {
        String messaggioTesto = String.format("Vorrei proporre uno scambio per il tuo articolo '%s': %s", 
                annuncio.getTitolo(), proposta);
        creaEInviaMessaggio(annuncio, messaggioTesto);
    }
    
    private void inviaMessaggioRegalo(Annuncio annuncio) {
        String messaggioTesto = String.format("Sono interessato al tuo articolo in regalo '%s'", 
                annuncio.getTitolo());
        creaEInviaMessaggio(annuncio, messaggioTesto);
    }
    
    /**
     * Factory method per creare e inviare messaggi
     */
    private void creaEInviaMessaggio(Annuncio annuncio, String testo) {
        Messaggio messaggio = new Messaggio(
            SessionManager.getCurrentUserId(),
            annuncio.getVenditoreId(),
            testo,
            annuncio.getId()
        );
        messaggioDAO.inviaMessaggio(messaggio);
    }
}