package com.project.artconnect.service.impl;

import com.project.artconnect.util.ConnectionManager;
import com.project.artconnect.util.SessionManager;
import com.project.artconnect.util.SessionManager.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de connexion — vérifie les credentials et résout le rôle
 * de l'utilisateur depuis la base ArtConnect.
 *
 * <p>Le password_hash stocké en base est comparé directement à la valeur
 * saisie. En production on utiliserait BCrypt ; ici on compare en clair
 * pour rester cohérent avec les données d'exemple du script 02_donnees.sql.</p>
 */
public class LoginService {

    // ------------------------------------------------------------------
    // Authentification principale
    // ------------------------------------------------------------------

    /**
     * Tente de connecter l'utilisateur avec son email et son mot de passe.
     *
     * @return true si la connexion réussit, false sinon.
     */
    public boolean login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return false;
        }

        String sql = """
                SELECT account_id, email, account_status, password_hash
                FROM account
                WHERE email = ?
                """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return false;                          // email inconnu

                String status = rs.getString("account_status");
                if (!"ACTIVE".equalsIgnoreCase(status)) return false;  // compte inactif

                String storedHash = rs.getString("password_hash");
                if (!storedHash.equals(password)) return false;        // mauvais mot de passe

                int accountId = rs.getInt("account_id");
                resolveAndOpenSession(conn, accountId, email.trim());
                return true;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur de connexion à la base lors du login.", e);
        }
    }

    // ------------------------------------------------------------------
    // Résolution du rôle
    // ------------------------------------------------------------------

    /**
     * Détermine le rôle de l'utilisateur en cherchant son account_id
     * dans chaque table de profil, puis ouvre la session.
     */
    private void resolveAndOpenSession(Connection conn, int accountId, String email)
            throws SQLException {

        // 1. Admin ?
        String name = lookupName(conn,
                "SELECT full_name FROM admin WHERE account_id = ?", accountId);
        if (name != null) {
            SessionManager.getInstance().openSession(accountId, email, name, Role.ADMIN);
            return;
        }

        // 2. Artiste ?
        name = lookupName(conn,
                "SELECT stage_name AS full_name FROM artist WHERE account_id = ?", accountId);
        if (name != null) {
            SessionManager.getInstance().openSession(accountId, email, name, Role.ARTIST);
            return;
        }

        // 3. Promoteur ?
        name = lookupName(conn,
                "SELECT full_name FROM promoter WHERE account_id = ?", accountId);
        if (name != null) {
            SessionManager.getInstance().openSession(accountId, email, name, Role.PROMOTER);
            return;
        }

        // 4. Membre (FREE ou PREMIUM) ?
        String memberSql = """
                SELECT full_name, membership_type
                FROM community_member
                WHERE account_id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(memberSql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String fullName      = rs.getString("full_name");
                    String membershipType = rs.getString("membership_type");
                    Role role = "PREMIUM".equalsIgnoreCase(membershipType)
                            ? Role.MEMBER : Role.GUEST_MEMBER;
                    SessionManager.getInstance().openSession(accountId, email, fullName, role);
                    return;
                }
            }
        }

        // Aucun profil trouvé — compte sans rôle (ne devrait pas arriver)
        throw new IllegalStateException(
                "Aucun profil trouvé pour ce compte (account_id=" + accountId + ").");
    }

    private String lookupName(Connection conn, String sql, int accountId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, accountId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("full_name") : null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Comptes demo — pour le ComboBox de connexion rapide
    // ------------------------------------------------------------------

    public record DemoAccount(String label, String email, String password) {
        @Override public String toString() { return label; }
    }

    /**
     * Charge les comptes demo depuis la base pour alimenter le ComboBox.
     * Retourne un compte par rôle principal pour faciliter la démo.
     */
    public List<DemoAccount> loadDemoAccounts() {
        List<DemoAccount> accounts = new ArrayList<>();

        // 5 requêtes simples — une par rôle — pour éviter les problèmes
        // de syntaxe MySQL avec LIMIT dans UNION
        try (Connection conn = ConnectionManager.getConnection()) {
            addDemoAccount(accounts, conn,
                    "SELECT a.email, a.password_hash, CONCAT('Admin - ', ad.full_name) AS label " +
                            "FROM account a JOIN admin ad ON a.account_id = ad.account_id " +
                            "WHERE a.account_status = 'ACTIVE' LIMIT 1");

            addDemoAccount(accounts, conn,
                    "SELECT a.email, a.password_hash, CONCAT('Promoteur - ', p.full_name) AS label " +
                            "FROM account a JOIN promoter p ON a.account_id = p.account_id " +
                            "WHERE a.account_status = 'ACTIVE' LIMIT 1");

            addDemoAccount(accounts, conn,
                    "SELECT a.email, a.password_hash, CONCAT('Artiste - ', ar.stage_name) AS label " +
                            "FROM account a JOIN artist ar ON a.account_id = ar.account_id " +
                            "WHERE a.account_status = 'ACTIVE' LIMIT 1");

            addDemoAccount(accounts, conn,
                    "SELECT a.email, a.password_hash, CONCAT('Membre Premium - ', cm.full_name) AS label " +
                            "FROM account a JOIN community_member cm ON a.account_id = cm.account_id " +
                            "WHERE a.account_status = 'ACTIVE' AND cm.membership_type = 'PREMIUM' LIMIT 1");

            addDemoAccount(accounts, conn,
                    "SELECT a.email, a.password_hash, CONCAT('Membre Free - ', cm.full_name) AS label " +
                            "FROM account a JOIN community_member cm ON a.account_id = cm.account_id " +
                            "WHERE a.account_status = 'ACTIVE' AND cm.membership_type = 'FREE' LIMIT 1");

        } catch (SQLException e) {
            System.err.println("Impossible de charger les comptes demo : " + e.getMessage());
        }

        return accounts;
    }

    private void addDemoAccount(List<DemoAccount> accounts, Connection conn, String sql)
            throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                accounts.add(new DemoAccount(
                        rs.getString("label"),
                        rs.getString("email"),
                        rs.getString("password_hash")));
            }
        }
    }


}