package com.hyrinth.backend.storage.team;

public record TeamRecord(
        String id,
        String name,
        String picture,
        String ownerId,
        boolean isHidden,
        java.util.List<String> projects,
        java.util.List<String> memberIds,
        long createdAt,
        long updatedAt
) {
}
