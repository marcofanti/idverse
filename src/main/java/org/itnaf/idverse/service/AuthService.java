package org.itnaf.idverse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.itnaf.idverse.client.service.JwtService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing authentication tokens and session keys.
 * Maintains an in-memory store of jwt_key -> JWT token mappings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;

    // In-memory storage: jwt_key -> TokenData
    private final Map<String, TokenData> tokenStore = new ConcurrentHashMap<>();

    private static final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String SPECIAL_CHARS = "!@#$%&*";
    private static final String KEY_CHARS = ALPHANUMERIC + SPECIAL_CHARS;
    private static final int KEY_LENGTH = 5;
    private static final long TOKEN_EXPIRATION_HOURS = 1;

    /**
     * Generates a new JWT token and random jwt_key.
     * Stores the mapping in memory for one-time use.
     *
     * @return A TokenPair containing both the jwt_key and JWT token
     */
    public TokenPair generateTokenPair() {
        // Generate JWT token with 1-hour expiration
        String jwtToken = jwtService.generateTokenWithExpiration("authenticated-user", TOKEN_EXPIRATION_HOURS);

        // Generate random 5-character key
        String jwtKey = generateRandomKey();

        // Store in memory with expiration
        Instant expiresAt = Instant.now().plusSeconds(TOKEN_EXPIRATION_HOURS * 3600);
        tokenStore.put(jwtKey, new TokenData(jwtToken, expiresAt));

        log.info("Generated new token pair with jwt_key: {}", maskKey(jwtKey));
        log.debug("Token store size: {}", tokenStore.size());

        // Clean up expired entries
        cleanupExpiredTokens();

        return new TokenPair(jwtKey, jwtToken);
    }

    /**
     * Validates and retrieves a JWT token using the jwt_key.
     * This is a one-time operation - the key is removed after successful retrieval.
     *
     * @param jwtKey The 5-character jwt_key
     * @return The JWT token if valid and not expired, null otherwise
     */
    public String validateAndConsumeKey(String jwtKey) {
        if (jwtKey == null || jwtKey.isEmpty()) {
            log.warn("Attempted to validate null or empty jwt_key");
            return null;
        }

        TokenData tokenData = tokenStore.get(jwtKey);

        if (tokenData == null) {
            log.warn("jwt_key not found in store: {}", maskKey(jwtKey));
            return null;
        }

        // Check if expired
        if (Instant.now().isAfter(tokenData.expiresAt)) {
            log.warn("jwt_key has expired: {}", maskKey(jwtKey));
            tokenStore.remove(jwtKey);
            return null;
        }

        // Valid and not expired - remove from store (one-time use)
        tokenStore.remove(jwtKey);
        log.info("jwt_key validated and consumed: {}", maskKey(jwtKey));
        log.debug("Token store size after removal: {}", tokenStore.size());

        return tokenData.jwtToken;
    }

    /**
     * Validates a JWT token.
     *
     * @param jwtToken The JWT token to validate
     * @return true if valid and not expired, false otherwise
     */
    public boolean validateJwtToken(String jwtToken) {
        if (jwtToken == null || jwtToken.isEmpty()) {
            return false;
        }

        try {
            jwtService.validateToken(jwtToken);
            return true;
        } catch (Exception e) {
            log.debug("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generates a random 5-character key using alphanumeric and special characters.
     */
    private String generateRandomKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder key = new StringBuilder(KEY_LENGTH);

        for (int i = 0; i < KEY_LENGTH; i++) {
            key.append(KEY_CHARS.charAt(random.nextInt(KEY_CHARS.length())));
        }

        return key.toString();
    }

    /**
     * Removes expired tokens from the store.
     */
    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int removedCount = 0;

        for (Map.Entry<String, TokenData> entry : tokenStore.entrySet()) {
            if (now.isAfter(entry.getValue().expiresAt)) {
                tokenStore.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired token(s)", removedCount);
        }
    }

    /**
     * Masks the jwt_key for logging (shows first and last character only).
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 2) {
            return "***";
        }
        return key.charAt(0) + "***" + key.charAt(key.length() - 1);
    }

    /**
     * Data class to store JWT token with expiration time.
     */
    private static class TokenData {
        final String jwtToken;
        final Instant expiresAt;

        TokenData(String jwtToken, Instant expiresAt) {
            this.jwtToken = jwtToken;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * Pair of jwt_key and JWT token.
     */
    public static class TokenPair {
        private final String jwtKey;
        private final String jwtToken;

        public TokenPair(String jwtKey, String jwtToken) {
            this.jwtKey = jwtKey;
            this.jwtToken = jwtToken;
        }

        public String getJwtKey() {
            return jwtKey;
        }

        public String getJwtToken() {
            return jwtToken;
        }
    }
}
