package application.DB;

import application.Classe.utente;

/**
 * Gestisce la sessione dell'utente corrente nell'applicazione
 * Fornisce metodi per impostare, recuperare e gestire lo stato di autenticazione
 */
public class SessionManager {
    private static utente currentUser;
    private static UtentiDAO utentiDAO = new UtentiDAO();
    
    /**
     * Imposta l'utente corrente della sessione
     * @param user l'utente da impostare come corrente
     */
    public static void setCurrentUser(utente user) {
        currentUser = user;
        // Carica la foto profilo quando si imposta l'utente
        if (user != null && user.getEmail() != null) {
            caricaFotoProfiloUtente(user.getEmail());
        }
    }
    
    /**
     * Carica la foto profilo dell'utente dal database
     * @param email l'email dell'utente
     */
    private static void caricaFotoProfiloUtente(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        
        try {
            String fotoPath = utentiDAO.getFotoProfilo(email);
            if (currentUser != null) {
                currentUser.setFotoProfilo(fotoPath);
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento foto profilo per sessione: " + e.getMessage());
        }
    }
    
    /**
     * Recupera l'utente corrente della sessione
     * @return l'utente corrente, null se nessun utente è loggato
     */
    public static utente getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Recupera l'ID dell'utente corrente
     * @return l'ID dell'utente corrente, -1 se nessun utente è loggato
     */
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : -1;
    }
    
    /**
     * Recupera l'email dell'utente corrente
     * @return l'email dell'utente corrente, null se nessun utente è loggato
     */
    public static String getCurrentUserEmail() {
        return currentUser != null ? currentUser.getEmail() : null;
    }
    
    /**
     * Recupera il nome dell'utente corrente
     * @return il nome dell'utente corrente, null se nessun utente è loggato
     */
    public static String getCurrentUserName() {
        return currentUser != null ? currentUser.getNome() : null;
    }
    
    /**
     * Recupera il percorso della foto profilo dell'utente corrente
     * @return il percorso della foto profilo, null se non presente
     */
    public static String getCurrentUserFotoProfilo() {
        return currentUser != null ? currentUser.getFotoProfilo() : null;
    }
    
    /**
     * Verifica se l'utente corrente ha una foto profilo
     * @return true se l'utente ha una foto profilo, false altrimenti
     */
    public static boolean hasFotoProfilo() {
        return currentUser != null && currentUser.hasFotoProfilo();
    }
    
    /**
     * Aggiorna la foto profilo dell'utente corrente
     * @param fotoPath il nuovo percorso della foto profilo
     * @return true se l'aggiornamento è avvenuto con successo
     */
    public static boolean aggiornaFotoProfilo(String fotoPath) {
        if (currentUser == null || currentUser.getEmail() == null) {
            return false;
        }
        
        try {
            boolean successo = utentiDAO.aggiornaFotoProfilo(currentUser.getEmail(), fotoPath);
            if (successo) {
                currentUser.setFotoProfilo(fotoPath);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Errore nell'aggiornamento foto profilo: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Aggiorna i dati dell'utente corrente dal database
     * Utile per sincronizzare i dati dopo modifiche
     */
    public static void refreshCurrentUser() {
        if (currentUser != null && currentUser.getEmail() != null) {
            try {
                utente utenteAggiornato = utentiDAO.getUtenteCompletoByEmail(currentUser.getEmail()).orElse(null);
                if (utenteAggiornato != null) {
                    currentUser = utenteAggiornato;
                }
            } catch (Exception e) {
                System.err.println("Errore nel refresh dell'utente: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica se un utente è attualmente loggato
     * @return true se un utente è loggato, false altrimenti
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Effettua il logout dell'utente corrente
     */
    public static void logout() {
        currentUser = null;
    }
    
    /**
     * Verifica se l'utente corrente ha un determinato ruolo
     * @param ruolo il ruolo da verificare
     * @return true se l'utente corrente ha il ruolo specificato
     */
    public static boolean hasRuolo(String ruolo) {
        if (currentUser == null) return false;
        // Implementazione dipende dalla struttura della classe utente
        return true; // Placeholder
    }
    
    /**
     * Verifica se l'utente corrente è il proprietario di una risorsa
     * @param proprietarioId l'ID del proprietario da verificare
     * @return true se l'utente corrente è il proprietario
     */
    public static boolean isProprietario(int proprietarioId) {
        return isLoggedIn() && getCurrentUserId() == proprietarioId;
    }
    
    /**
     * Verifica se l'utente corrente può modificare una risorsa
     * @param proprietarioId l'ID del proprietario della risorsa
     * @return true se l'utente può modificare la risorsa
     */
    public static boolean canModifica(int proprietarioId) {
        return isProprietario(proprietarioId) || hasRuolo("ADMIN");
    }
    
    /**
     * Restituisce una rappresentazione stringa dello stato della sessione
     * @return stringa descrittiva dello stato
     */
    public static String getSessionInfo() {
        if (currentUser == null) {
            return "Nessun utente loggato";
        }
        
        return String.format(
            "Utente: %s %s (%s) - Foto: %s",
            currentUser.getNome(),
            currentUser.getCognome(),
            currentUser.getEmail(),
            hasFotoProfilo() ? "Presente" : "Assente"
        );
    }
    
    /**
     * Verifica se l'utente corrente può accedere a una determinata funzionalità
     * @param funzionalita il nome della funzionalità
     * @return true se l'utente può accedere
     */
    public static boolean canAccess(String funzionalita) {
        if (!isLoggedIn()) {
            return false;
        }
        
        // Implementa la logica di autorizzazione in base alle tue esigenze
        switch (funzionalita) {
            case "INSERISCI_ANNUNCIO":
            case "MESSAGGI":
            case "MODIFICA_PROFILO":
                return true;
            case "GESTIONE_UTENTI":
                return hasRuolo("ADMIN");
            default:
                return true;
        }
    }
}