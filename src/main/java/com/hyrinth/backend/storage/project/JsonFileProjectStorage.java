package com.hyrinth.backend.storage.project;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JsonFileProjectStorage implements ProjectStorage {

    private static final String PROJECTS_KEY = "projects";

    private final Path filePath;
    private final Object lock = new Object();
    private JSONObject data;

    public JsonFileProjectStorage(Path filePath) {
        this.filePath = filePath;
        loadOrCreate();
    }

    @Override
    public Optional<ProjectRecord> findBySlug(String slug) {
        synchronized (lock) {
            return projectsArray().stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::toRecord)
                    .filter(record -> record.slug().equals(slug))
                    .findFirst();
        }
    }

    @Override
    public List<ProjectRecord> findPage(int limit, int offset) {
        synchronized (lock) {
            if (limit <= 0 || offset < 0) {
                return List.of();
            }
            List<ProjectRecord> records = new ArrayList<>();
            for (Object entry : projectsArray()) {
                if (entry instanceof JSONObject project) {
                    records.add(toRecord(project));
                }
            }
            records.sort(Comparator.comparingLong(ProjectRecord::createdAt)
                    .thenComparing(ProjectRecord::slug));
            if (offset >= records.size()) {
                return List.of();
            }
            int toIndex = Math.min(records.size(), offset + limit);
            return records.subList(offset, toIndex);
        }
    }

    @Override
    public boolean create(ProjectRecord record) {
        synchronized (lock) {
            if (record == null || record.slug() == null || record.slug().isBlank()) {
                return false;
            }
            if (findBySlug(record.slug()).isPresent()) {
                return false;
            }
            projectsArray().add(fromRecord(record));
            return save();
        }
    }

    @Override
    public boolean update(ProjectRecord record) {
        synchronized (lock) {
            if (record == null || record.slug() == null || record.slug().isBlank()) {
                return false;
            }
            JSONArray projects = projectsArray();
            for (int i = 0; i < projects.size(); i++) {
                Object entry = projects.get(i);
                if (entry instanceof JSONObject project) {
                    if (record.slug().equals(project.getString("slug"))) {
                        projects.set(i, fromRecord(record));
                        return save();
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean delete(String slug) {
        synchronized (lock) {
            JSONArray projects = projectsArray();
            for (int i = 0; i < projects.size(); i++) {
                Object entry = projects.get(i);
                if (entry instanceof JSONObject project) {
                    if (slug.equals(project.getString("slug"))) {
                        projects.remove(i);
                        return save();
                    }
                }
            }
            return false;
        }
    }

    private void loadOrCreate() {
        synchronized (lock) {
            try {
                if (Files.notExists(filePath)) {
                    Files.createDirectories(filePath.getParent());
                    data = new JSONObject();
                    data.put(PROJECTS_KEY, new JSONArray());
                    save();
                    return;
                }
                String raw = Files.readString(filePath, StandardCharsets.UTF_8);
                data = JSONObject.parseObject(raw);
                if (data == null) {
                    data = new JSONObject();
                }
                if (!data.containsKey(PROJECTS_KEY)) {
                    data.put(PROJECTS_KEY, new JSONArray());
                    save();
                }
                boolean updated = false;
                JSONArray projects = data.getJSONArray(PROJECTS_KEY);
                if (projects != null) {
                    for (Object entry : projects) {
                        if (entry instanceof JSONObject project) {
                            if (!project.containsKey("id")) {
                                project.put("id", "");
                                updated = true;
                            }
                            if (!project.containsKey("slug")) {
                                project.put("slug", "");
                                updated = true;
                            }
                            if (!project.containsKey("title")) {
                                project.put("title", "");
                                updated = true;
                            }
                            if (!project.containsKey("description")) {
                                project.put("description", "");
                                updated = true;
                            }
                            if (!project.containsKey("categoryIds")) {
                                if (project.containsKey("categories")) {
                                    project.put("categoryIds", project.getJSONArray("categories"));
                                } else {
                                    project.put("categoryIds", new JSONArray());
                                }
                                updated = true;
                            }
                            if (!project.containsKey("additionalTags")) {
                                project.put("additionalTags", new JSONArray());
                                updated = true;
                            }
                            if (!project.containsKey("donationUrls")) {
                                project.put("donationUrls", new JSONArray());
                                updated = true;
                            }
                            if (!project.containsKey("galleryUrls")) {
                                project.put("galleryUrls", new JSONArray());
                                updated = true;
                            }
                            if (!project.containsKey("gameVersions")) {
                                project.put("gameVersions", new JSONArray());
                                updated = true;
                            }
                            if (!project.containsKey("versionIds")) {
                                project.put("versionIds", new JSONArray());
                                updated = true;
                            }
                            if (!project.containsKey("body")) {
                                project.put("body", "");
                                updated = true;
                            }
                            if (!project.containsKey("status")) {
                                project.put("status", "");
                                updated = true;
                            }
                            if (!project.containsKey("requestedStatus")) {
                                project.put("requestedStatus", "");
                                updated = true;
                            }
                            if (!project.containsKey("issuesUrl")) {
                                project.put("issuesUrl", "");
                                updated = true;
                            }
                            if (!project.containsKey("sourceUrl")) {
                                project.put("sourceUrl", "");
                                updated = true;
                            }
                            if (!project.containsKey("wikiUrl")) {
                                project.put("wikiUrl", "");
                                updated = true;
                            }
                            if (!project.containsKey("discordUrl")) {
                                project.put("discordUrl", "");
                                updated = true;
                            }
                            if (!project.containsKey("projectType")) {
                                project.put("projectType", "");
                                updated = true;
                            }
                            if (!project.containsKey("downloads")) {
                                project.put("downloads", 0);
                                updated = true;
                            }
                            if (!project.containsKey("iconUrl")) {
                                project.put("iconUrl", "");
                                updated = true;
                            }
                            if (!project.containsKey("colorHex")) {
                                project.put("colorHex", "");
                                updated = true;
                            }
                            if (!project.containsKey("ownerId")) {
                                project.put("ownerId", "");
                                updated = true;
                            }
                            if (!project.containsKey("moderatorMessage")) {
                                project.put("moderatorMessage", "");
                                updated = true;
                            }
                            if (!project.containsKey("createdAt")) {
                                project.put("createdAt", 0);
                                updated = true;
                            }
                            if (!project.containsKey("updatedAt")) {
                                project.put("updatedAt", 0);
                                updated = true;
                            }
                            if (!project.containsKey("approvedAt")) {
                                project.put("approvedAt", 0);
                                updated = true;
                            }
                            if (!project.containsKey("queuedAt")) {
                                project.put("queuedAt", 0);
                                updated = true;
                            }
                            if (!project.containsKey("followers")) {
                                project.put("followers", 0);
                                updated = true;
                            }
                            if (!project.containsKey("license")) {
                                project.put("license", "");
                                updated = true;
                            }
                        }
                    }
                }
                if (updated) {
                    save();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load project storage file: " + filePath, e);
            }
        }
    }

    private JSONArray projectsArray() {
        JSONArray projects = data.getJSONArray(PROJECTS_KEY);
        if (projects == null) {
            projects = new JSONArray();
            data.put(PROJECTS_KEY, projects);
        }
        return projects;
    }

    private boolean save() {
        try {
            Files.writeString(filePath, data.toJSONString(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private ProjectRecord toRecord(JSONObject json) {
        return new ProjectRecord(
                json.getString("id"),
                json.getString("slug"),
                json.getString("title"),
                json.getString("description"),
                toStringList(json.getJSONArray("categoryIds")),
                toStringList(json.getJSONArray("additionalTags")),
                toStringList(json.getJSONArray("donationUrls")),
                toStringList(json.getJSONArray("galleryUrls")),
                toStringList(json.getJSONArray("gameVersions")),
                toStringList(json.getJSONArray("versionIds")),
                json.getString("body"),
                json.getString("status"),
                json.getString("requestedStatus"),
                json.getString("issuesUrl"),
                json.getString("sourceUrl"),
                json.getString("wikiUrl"),
                json.getString("discordUrl"),
                json.getString("projectType"),
                json.getIntValue("downloads"),
                json.getString("iconUrl"),
                json.getString("colorHex"),
                json.getString("ownerId"),
                json.getString("moderatorMessage"),
                json.getLongValue("createdAt"),
                json.getLongValue("updatedAt"),
                json.getLongValue("approvedAt"),
                json.getLongValue("queuedAt"),
                json.getIntValue("followers"),
                json.getString("license")
        );
    }

    private JSONObject fromRecord(ProjectRecord record) {
        JSONObject json = new JSONObject();
        json.put("id", record.id());
        json.put("slug", record.slug());
        json.put("title", record.title());
        json.put("description", record.description());
        json.put("categoryIds", record.categoryIds());
        json.put("additionalTags", record.additionalTags());
        json.put("donationUrls", record.donationUrls());
        json.put("galleryUrls", record.galleryUrls());
        json.put("gameVersions", record.gameVersions());
        json.put("versionIds", record.versionIds());
        json.put("body", record.body());
        json.put("status", record.status());
        json.put("requestedStatus", record.requestedStatus());
        json.put("issuesUrl", record.issuesUrl());
        json.put("sourceUrl", record.sourceUrl());
        json.put("wikiUrl", record.wikiUrl());
        json.put("discordUrl", record.discordUrl());
        json.put("projectType", record.projectType());
        json.put("downloads", record.downloads());
        json.put("iconUrl", record.iconUrl());
        json.put("colorHex", record.colorHex());
        json.put("ownerId", record.ownerId());
        json.put("moderatorMessage", record.moderatorMessage());
        json.put("createdAt", record.createdAt());
        json.put("updatedAt", record.updatedAt());
        json.put("approvedAt", record.approvedAt());
        json.put("queuedAt", record.queuedAt());
        json.put("followers", record.followers());
        json.put("license", record.license());
        return json;
    }

    private List<String> toStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (Object entry : array) {
            if (entry != null) {
                list.add(entry.toString());
            }
        }
        return list;
    }
}
