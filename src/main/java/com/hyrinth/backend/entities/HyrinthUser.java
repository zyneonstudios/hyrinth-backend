package com.hyrinth.backend.entities;

import com.hyrinth.backend.HyrinthBackend;
import com.hyrinth.backend.storage.account.AccountRecord;
import java.sql.Timestamp;
import java.util.List;

public class HyrinthUser {

    private AccountRecord accountRecord;
    private String id;
    private String email;
    private String username;
    private String profilePicture;
    private boolean isHidden;
    private long createdAt;
    private long updatedAt;
    private boolean isAdmin;
    private List<String> permissions;
    private List<String> projects;
    private List<String> teams;

    public HyrinthUser(AccountRecord accountRecord) {
        this.accountRecord = accountRecord;
        load();
    }

    public AccountRecord getAccountRecord() {
        return accountRecord;
    }

    public boolean hasPermission(String permission) {
        return isAdmin || permissions.contains(permission);
    }

    public void reload() {
        accountRecord = HyrinthBackend.getInstance().getStorageProvider().getAccountStorage().findById(id).orElse(null);
        if (accountRecord == null) throw new IllegalStateException("AccountRecord for id " + id + " was not found!");
        load();
    }

    private void load() {
        this.id = accountRecord.id();
        this.email = accountRecord.email();
        this.username = accountRecord.username();
        this.profilePicture = accountRecord.profilePicture();
        this.isHidden = accountRecord.isHidden();
        this.createdAt = accountRecord.createdAt();
        this.updatedAt = accountRecord.updatedAt();
        this.permissions = accountRecord.permissions();
        this.projects = accountRecord.projects();
        this.teams = accountRecord.teams();
        this.isAdmin = accountRecord.isAdmin();
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Timestamp getCreatedAtTimestamp() {
        return new Timestamp(createdAt);
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public Timestamp getUpdatedAtTimestamp() {
        return new Timestamp(updatedAt);
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<String> getProjects() {
        return projects;
    }

    public List<String> getTeams() {
        return teams;
    }
}
