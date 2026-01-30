package com.hyrinth.backend.storage.project;

public record ProjectRecord(
        String id,
        String slug,
        String title,
        String description,
        java.util.List<String> categoryIds,
        java.util.List<String> additionalTags,
        java.util.List<String> donationUrls,
        java.util.List<String> galleryUrls,
        java.util.List<String> gameVersions,
        java.util.List<String> versionIds,
        String body,
        String status,
        String requestedStatus,
        String issuesUrl,
        String sourceUrl,
        String wikiUrl,
        String discordUrl,
        String projectType,
        int downloads,
        String iconUrl,
        String colorHex,
        String ownerId,
        String moderatorMessage,
        long createdAt,
        long updatedAt,
        long approvedAt,
        long queuedAt,
        int followers,
        String license
) {
}
