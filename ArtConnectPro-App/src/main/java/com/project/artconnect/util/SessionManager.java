package com.project.artconnect.util;

/**
 * Singleton qui stocke la session de l'utilisateur connecté.
 * Accessible depuis n'importe quelle couche de l'application
 * sans passer par les contrôleurs.
 */
public class SessionManager {

    public enum Role {
        ADMIN,
        ARTIST,
        MEMBER,        // membre premium — accès complet
        GUEST_MEMBER,  // membre free — accès limité
        PROMOTER
    }

    private static SessionManager instance;

    private int     accountId;
    private String  email;
    private String  fullName;   // stage_name pour artiste, full_name pour membre/admin
    private Role    role;
    private boolean loggedIn;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ------------------------------------------------------------------
    // Ouverture / fermeture de session
    // ------------------------------------------------------------------

    public void openSession(int accountId, String email, String fullName, Role role) {
        this.accountId = accountId;
        this.email     = email;
        this.fullName  = fullName;
        this.role      = role;
        this.loggedIn  = true;
    }

    public void closeSession() {
        this.accountId = 0;
        this.email     = null;
        this.fullName  = null;
        this.role      = null;
        this.loggedIn  = false;
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public boolean isLoggedIn()  { return loggedIn; }
    public int     getAccountId(){ return accountId; }
    public String  getEmail()    { return email; }
    public String  getFullName() { return fullName; }
    public Role    getRole()     { return role; }

    // ------------------------------------------------------------------
    // Helpers de vérification de rôle
    // ------------------------------------------------------------------

    public boolean isAdmin()       { return role == Role.ADMIN; }
    public boolean isArtist()      { return role == Role.ARTIST; }
    public boolean isMember()      { return role == Role.MEMBER || role == Role.GUEST_MEMBER; }
    public boolean isPremiumMember(){ return role == Role.MEMBER; }
    public boolean isPromoter()    { return role == Role.PROMOTER; }

    /**
     * Retourne un libellé lisible du rôle pour l'affichage dans l'UI.
     */
    public String getRoleLabel() {
        if (role == null) return "Invité";
        return switch (role) {
            case ADMIN        -> "Administrateur";
            case ARTIST       -> "Artiste";
            case MEMBER       -> "Membre Premium";
            case GUEST_MEMBER -> "Membre Free";
            case PROMOTER     -> "Promoteur";
        };
    }
}
