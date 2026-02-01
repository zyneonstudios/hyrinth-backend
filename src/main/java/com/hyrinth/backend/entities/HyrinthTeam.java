package com.hyrinth.backend.entities;

import com.hyrinth.backend.HyrinthBackend;
import com.hyrinth.backend.storage.team.TeamRecord;

public class HyrinthTeam {

    private TeamRecord teamRecord;
    private String id;
    private String name;
    private String picture;
    private String ownerId;
    private boolean isHidden;
    private long createdAt;
    private long updatedAt;
    private java.util.List<String> memberIds;
    private java.util.List<String> projects;

    public HyrinthTeam(TeamRecord teamRecord) {
        this.teamRecord = teamRecord;
    }

    public TeamRecord getTeamRecord() {
        return teamRecord;
    }

    public void reload() {
        teamRecord = HyrinthBackend.getInstance().getStorageProvider().getTeamStorage().findById(id).orElse(null);
        if (teamRecord == null) throw new IllegalStateException("TeamRecord for id " + id + " was not found!");
        load();
    }

    private void load() {
        id = teamRecord.id();
        name = teamRecord.name();
        picture = teamRecord.picture();
        ownerId = teamRecord.ownerId();
        isHidden = teamRecord.isHidden();
        createdAt = teamRecord.createdAt();
        updatedAt = teamRecord.updatedAt();
        memberIds = teamRecord.memberIds();
        projects = teamRecord.projects();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public java.util.List<String> getMemberIds() {
        return memberIds;
    }

    public java.util.List<String> getProjects() {
        return projects;
    }
}