package com.hyrinth.backend.storage.session;

public record AccountSessionRecord(
        String token,
        String accountId,
        long createdAt,
        long expiresAt,
        AccountTokenType type,
        int remainingUses
) {
}
