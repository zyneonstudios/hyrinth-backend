package com.hyrinth.backend.entities;

import com.hyrinth.backend.HyrinthBackend;
import com.hyrinth.backend.storage.project.ProjectRecord;

import java.util.List;

public class HyrinthProject {

    private ProjectRecord projectRecord;
    private String id;
    private String slug;
    private String title;
    private String description;
    private java.util.List<String> categoryIds;
    private java.util.List<String> additionalTags;
    private java.util.List<String> donationUrls;
    private java.util.List<String> galleryUrls;
    private java.util.List<String> gameVersions;
    private java.util.List<String> versionIds;
    private String body;
    private String status;
    private String requestedStatus;
    private String issuesUrl;
    private String sourceUrl;
    private String wikiUrl;
    private  String discordUrl;
    private String projectType;
    private int downloads;
    private String iconUrl;
    private String colorHex;
    private String ownerId;
    private String moderatorMessage;
    private long createdAt;
    private long updatedAt;
    private long approvedAt;
    private long queuedAt;
    private int followers;
    private String license;

    public HyrinthProject(ProjectRecord record) {
        projectRecord = record;
    }

    public ProjectRecord getProjectRecord() {
        return projectRecord;
    }

    public void reload() {
        projectRecord = HyrinthBackend.getInstance().getStorageProvider().getProjectStorage().findBySlug(slug).orElse(null);
        if (projectRecord == null) throw new IllegalStateException("ProjectRecord for slug " + slug + " was not found!");
        load();
    }

    private void load() {
        id = projectRecord.id();
        slug = projectRecord.slug();
        title = projectRecord.title();
        description = projectRecord.description();
        categoryIds = projectRecord.categoryIds();
        additionalTags = projectRecord.additionalTags();
        donationUrls = projectRecord.donationUrls();
        galleryUrls = projectRecord.galleryUrls();
        gameVersions = projectRecord.gameVersions();
        versionIds = projectRecord.versionIds();
        body = projectRecord.body();
        status = projectRecord.status();
        requestedStatus = projectRecord.requestedStatus();
        issuesUrl = projectRecord.issuesUrl();
        sourceUrl = projectRecord.sourceUrl();
        wikiUrl = projectRecord.wikiUrl();
        discordUrl = projectRecord.discordUrl();
        projectType = projectRecord.projectType();
        downloads = projectRecord.downloads();
        iconUrl = projectRecord.iconUrl();
        colorHex = projectRecord.colorHex();
        ownerId = projectRecord.ownerId();
        moderatorMessage = projectRecord.moderatorMessage();
        createdAt = projectRecord.createdAt();
        updatedAt = projectRecord.updatedAt();
        approvedAt = projectRecord.approvedAt();
        queuedAt = projectRecord.queuedAt();
        followers = projectRecord.followers();
        license = projectRecord.license();
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCategoryIds() {
        return categoryIds;
    }

    public List<String> getAdditionalTags() {
        return additionalTags;
    }

    public List<String> getDonationUrls() {
        return donationUrls;
    }

    public List<String> getGalleryUrls() {
        return galleryUrls;
    }

    public List<String> getGameVersions() {
        return gameVersions;
    }

    public List<String> getVersionIds() {
        return versionIds;
    }

    public String getBody() {
        return body;
    }

    public String getStatus() {
        return status;
    }

    public String getRequestedStatus() {
        return requestedStatus;
    }

    public String getIssuesUrl() {
        return issuesUrl;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getWikiUrl() {
        return wikiUrl;
    }

    public String getDiscordUrl() {
        return discordUrl;
    }

    public String getProjectType() {
        return projectType;
    }

    public int getDownloads() {
        return downloads;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getColorHex() {
        return colorHex;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getModeratorMessage() {
        return moderatorMessage;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getApprovedAt() {
        return approvedAt;
    }

    public long getQueuedAt() {
        return queuedAt;
    }

    public int getFollowers() {
        return followers;
    }

    public String getLicense() {
        return license;
    }
}