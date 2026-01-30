package com.hyrinth.backend.storage.session;

import com.hyrinth.backend.Main;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

public class AccountSessionService {

    public static final String COOKIE_NAME = "HYRINTH_SESSION";
    public static final Duration SESSION_TTL = Duration.ofHours(4);
    public static final Duration REMEMBER_TTL = Duration.ofDays(30);

    private final AccountSessionStorage storage;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountSessionService(AccountSessionStorage storage) {
        this.storage = storage;
    }

    public SessionResult createSession(String accountId, boolean remember) {
        String token = generateToken();
        long now = System.currentTimeMillis();
        long expiresAt = now + (remember ? REMEMBER_TTL.toMillis() : SESSION_TTL.toMillis());
        AccountSessionRecord record = new AccountSessionRecord(
                token,
                accountId,
                now,
                expiresAt,
                AccountTokenType.SESSION,
                -1
        );
        if (!storage.create(record)) {
            return SessionResult.failure("Could not create session.");
        }
        return SessionResult.success(token, expiresAt);
    }

    public SessionResult createTokenWithDays(String accountId, int days) {
        if (days <= 0) {
            return SessionResult.failure("Token days must be > 0.");
        }
        String token = generateToken();
        long now = System.currentTimeMillis();
        long expiresAt = now + Duration.ofDays(days).toMillis();
        AccountSessionRecord record = new AccountSessionRecord(
                token,
                accountId,
                now,
                expiresAt,
                AccountTokenType.DAYS,
                -1
        );
        if (!storage.create(record)) {
            return SessionResult.failure("Could not create token.");
        }
        return SessionResult.success(token, expiresAt);
    }

    public SessionResult createTokenWithUses(String accountId, int uses) {
        if (uses <= 0) {
            return SessionResult.failure("Token uses must be > 0.");
        }
        String token = generateToken();
        long now = System.currentTimeMillis();
        AccountSessionRecord record = new AccountSessionRecord(
                token,
                accountId,
                now,
                0L,
                AccountTokenType.USES,
                uses
        );
        if (!storage.create(record)) {
            return SessionResult.failure("Could not create token.");
        }
        return SessionResult.success(token, 0L);
    }

    public SessionResult createPermanentToken(String accountId) {
        String token = generateToken();
        long now = System.currentTimeMillis();
        AccountSessionRecord record = new AccountSessionRecord(
                token,
                accountId,
                now,
                0L,
                AccountTokenType.PERMANENT,
                -1
        );
        if (!storage.create(record)) {
            return SessionResult.failure("Could not create token.");
        }
        return SessionResult.success(token, 0L);
    }

    public Optional<String> findAccountId(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<AccountSessionRecord> record = storage.findByToken(token);
        if (record.isEmpty()) {
            return Optional.empty();
        }
        AccountSessionRecord session = record.get();
        if (isExpired(session)) {
            storage.delete(token);
            return Optional.empty();
        }
        if (session.remainingUses() == 0) {
            storage.delete(token);
            return Optional.empty();
        }
        if (session.remainingUses() > 0) {
            AccountSessionRecord updated = new AccountSessionRecord(
                    session.token(),
                    session.accountId(),
                    session.createdAt(),
                    session.expiresAt(),
                    session.type(),
                    session.remainingUses() - 1
            );
            if (!storage.update(updated)) {
                return Optional.empty();
            }
        }
        return Optional.of(session.accountId());
    }

    public boolean deleteSession(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return storage.delete(token);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isExpired(AccountSessionRecord session) {
        if (session.expiresAt() <= 0) {
            return false;
        }
        return session.expiresAt() <= System.currentTimeMillis();
    }

    public record SessionResult(boolean success, String token, long expiresAt, String message) {
        public static SessionResult success(String token, long expiresAt) {
            return new SessionResult(true, token, expiresAt, null);
        }

        public static SessionResult failure(String message) {
            Main.getLogger().err(message);
            return new SessionResult(false, null, 0L, message);
        }
    }
}
