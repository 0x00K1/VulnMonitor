package com.vulnmonitor.utils;

import com.vulnmonitor.model.User;
import com.vulnmonitor.services.DatabaseService;

import java.util.concurrent.CompletableFuture;

public class SessionManager {
    private static DatabaseService databaseService;

    /**
     * Initializes the SessionManager with the provided DatabaseService.
     *
     * @param dbService The DatabaseService instance.
     */
    public static void initialize(DatabaseService dbService) {
        databaseService = dbService;
    }

    /**
     * Checks if the session is still valid based on the database entry.
     *
     * @param user The user to check.
     * @return A CompletableFuture<Boolean> indicating session validity.
     */
    public static CompletableFuture<Boolean> isSessionValid(User user) {
        if (user == null || !user.isLoggedIn()) {
            return CompletableFuture.completedFuture(false);
        }

        return databaseService.loadUserSession(user.getUserId())
                             .thenApply(sessionUser -> sessionUser != null && sessionUser.isLoggedIn());
    }

    /**
     * Loads the user session from the database.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<User> containing the session data, or null if not found.
     */
    public static CompletableFuture<User> loadUserSession(int userId) {
        return databaseService.loadUserSession(userId);
    }

    /**
     * Saves the user session to the database.
     *
     * @param user The user to save.
     * @return A CompletableFuture<Void>.
     */
    public static CompletableFuture<Void> saveUserSession(User user) {
        return databaseService.saveUserSession(user);
    }

    /**
     * Clears the user session from the database.
     *
     * @param userId The user ID.
     * @return A CompletableFuture<Void>.
     */
    public static CompletableFuture<Void> clearSession(int userId) {
        return databaseService.deleteUserSession(userId);
    }

    /**
     * Retrieves the current active user ID.
     *
     * @return A CompletableFuture<Integer> containing the active user ID, or -1 if no active session exists.
     */
    public static CompletableFuture<Integer> getCurrentUserId() {
        return databaseService.getActiveUserId();
    }
}