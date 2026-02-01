package com.hyrinth.backend.webserver.resolver;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.entities.HyrinthProject;
import com.hyrinth.backend.storage.project.ProjectRecord;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectResolver {

    public static JSONObject getProject(String slug) {
        if(slug == null) return null;
        Optional<ProjectRecord> project = Main.getHyrinthBackend().getStorageProvider().getProjectStorage().findBySlug(slug);
        if (project.isPresent()) {
            JSONObject json = new JSONObject();
            json.put("id", project.get().id());
            json.put("slug", project.get().slug());
            json.put("title", project.get().title());
            json.put("description", project.get().description());
            json.put("categoryIds", project.get().categoryIds());
            json.put("additionalTags", project.get().additionalTags());
            json.put("donationUrls", project.get().donationUrls());
            json.put("galleryUrls", project.get().galleryUrls());
            json.put("gameVersions", project.get().gameVersions());
            json.put("versionIds", project.get().versionIds());
            json.put("body", project.get().body());
            json.put("status", project.get().status());
            json.put("requestedStatus", project.get().requestedStatus());
            json.put("issuesUrl", project.get().issuesUrl());
            json.put("sourceUrl", project.get().sourceUrl());
            json.put("wikiUrl", project.get().wikiUrl());
            json.put("discordUrl", project.get().discordUrl());
            json.put("projectType", project.get().projectType());
            json.put("downloads", project.get().downloads());
            json.put("iconUrl", project.get().iconUrl());
            json.put("colorHex", project.get().colorHex());
            json.put("ownerId", project.get().ownerId());
            json.put("moderatorMessage", project.get().moderatorMessage());
            json.put("createdAt", project.get().createdAt());
            json.put("updatedAt", project.get().updatedAt());
            json.put("approvedAt", project.get().approvedAt());
            json.put("queuedAt", project.get().queuedAt());
            json.put("followers", project.get().followers());
            json.put("license", project.get().license());
            return json;
        } else {
            return null;
        }
    }

    public static JSONObject getAuthenticatedProject(String slug, String token) {
        return getProject(slug);
    }

    public static HyrinthProject findBySlug(String slug) {
        return Main.getHyrinthBackend().getStorageProvider().getProjectStorage().findBySlug(slug).map(HyrinthProject::new).orElse(null);
    }

    public static JSONObject getProjects(int limit, int offset) {
        if(limit>100) {
            limit = 100;
        }

        JSONObject response = new JSONObject();
        response.put("limit", limit);
        response.put("offset", offset);

        JSONArray projects = new JSONArray();
        List<ProjectRecord> projectRecords = Main.getHyrinthBackend().getStorageProvider().getProjectStorage().findPage(limit, offset);
        AtomicInteger i = new AtomicInteger();
        projectRecords.forEach(project -> {
            projects.add(getProject(project.slug()));
            i.getAndIncrement();
        });

        response.put("matches", projectRecords.size());
        response.put("total_hits",-1);
        response.put("hits", projects);
        return response;
    }
}
