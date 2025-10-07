package application.DB;

import application.Classe.utente;

public class SessionManager {
    private static utente currentUser;
    
    public static void setCurrentUser(utente user) {
        currentUser = user;
        System.out.println("[DEBUG] SessionManager: Utente impostato - " + 
                          (user != null ? user.getNome() + " " + user.getCognome() : "null"));
    }
    
    public static utente getCurrentUser() {
        System.out.println("[DEBUG] SessionManager: Recupero utente corrente - " + 
                          (currentUser != null ? currentUser.getNome() : "null"));
        return currentUser;
    }
    
    public static int getCurrentUserId() {
        int userId = currentUser != null ? currentUser.getId() : -1;
        System.out.println("[DEBUG] SessionManager: ID utente corrente - " + userId);
        return userId;
    }
    
    public static boolean isLoggedIn() {
        boolean loggedIn = currentUser != null;
        System.out.println("[DEBUG] SessionManager: Utente loggato - " + loggedIn);
        return loggedIn;
    }
    
    public static void logout() {
        System.out.println("[DEBUG] SessionManager: Logout utente - " + 
                          (currentUser != null ? currentUser.getNome() : "null"));
        currentUser = null;
    }
}
