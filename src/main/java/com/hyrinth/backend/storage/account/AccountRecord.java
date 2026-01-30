package com.hyrinth.backend.storage.account;

public record AccountRecord(
        String id,
        String email,
        String username,
        String profilePicture,
        boolean isHidden,
        String passwordHash,
        boolean isAdmin,
        java.util.List<String> permissions,
        java.util.List<String> projects,
        java.util.List<String> teams,
        long createdAt,
        long updatedAt
) {
}
