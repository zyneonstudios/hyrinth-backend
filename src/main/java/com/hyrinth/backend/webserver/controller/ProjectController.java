package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.entities.HyrinthUser;
import com.hyrinth.backend.storage.account.AccountRecord;
import com.hyrinth.backend.storage.account.AccountStorage;
import com.hyrinth.backend.storage.project.ProjectRecord;
import com.hyrinth.backend.storage.team.TeamRecord;
import com.hyrinth.backend.storage.team.TeamStorage;
import com.hyrinth.backend.webserver.resolver.ProjectResolver;
import com.hyrinth.backend.webserver.resolver.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
public class ProjectController {

    @GetMapping({"/project/**","/projects/**"})
    public static ResponseEntity<Object> handleGetProjectRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String slug = path.split("/")[2];
        JSONObject project = ProjectResolver.getProject(slug);
        if (project != null) {
            return ResponseEntity.ok(project);
        }
        return ErrorController.handleError404(request);
    }

    @GetMapping({"/project","/projects","/project/","/projects/"})
    public static ResponseEntity<Object> handleListProjectsRequest(HttpServletRequest request) {
        String limitParam = request.getParameter("limit");
        String offsetParam = request.getParameter("offset");
        if (limitParam == null) limitParam = "25";
        if (offsetParam == null) offsetParam = "0";
        return ResponseEntity.ok(ProjectResolver.getProjects(Integer.parseInt(limitParam), Integer.parseInt(offsetParam)));
    }

    @RequestMapping(value={"/project","/projects","/project/","/projects/"},method = RequestMethod.POST)
    public ResponseEntity<Object> handleProjectAction(@RequestHeader("action") String action_, @RequestBody JSONObject jsonObject, @RequestHeader("x-api-key") String token) {
        try {
            PostAction action = PostAction.valueOf(action_.toUpperCase());
            HyrinthUser user = UserResolver.findByToken(token);
            JSONObject response = new JSONObject();
            response.put("endpoint", "/project");
            response.put("action", action_);

            if (user == null) {
                response.put("status", 401);
                response.put("message", "unauthorized");
                return ResponseEntity.ok(response);
            }

            String slug = jsonObject.getString("slug");
            if (slug != null && !slug.isBlank()) {
                Optional<ProjectRecord> target = Main.getHyrinthBackend().getStorageProvider().getProjectStorage().findBySlug(slug);
                if (target.isPresent()) {
                    ProjectRecord existing = target.get();
                    boolean isOwner = existing.ownerId().equals(user.getId());
                    if (action == PostAction.UPDATE && (isOwner || user.hasPermission("project.edit"))) {
                        String id = existing.id();
                        String title = existing.title();
                        String description = existing.description();
                        List<String> categoryIds = existing.categoryIds();
                        List<String> additionalTags = existing.additionalTags();
                        List<String> donationUrls = existing.donationUrls();
                        List<String> galleryUrls = existing.galleryUrls();
                        List<String> gameVersions = existing.gameVersions();
                        List<String> versionIds = existing.versionIds();
                        String body = existing.body();
                        String status = existing.status();
                        String requestedStatus = existing.requestedStatus();
                        String issuesUrl = existing.issuesUrl();
                        String sourceUrl = existing.sourceUrl();
                        String wikiUrl = existing.wikiUrl();
                        String discordUrl = existing.discordUrl();
                        String projectType = existing.projectType();
                        int downloads = existing.downloads();
                        String iconUrl = existing.iconUrl();
                        String colorHex = existing.colorHex();
                        String ownerId = existing.ownerId();
                        String moderatorMessage = existing.moderatorMessage();
                        long createdAt = existing.createdAt();
                        long updatedAt = Instant.now().toEpochMilli();
                        long approvedAt = existing.approvedAt();
                        long queuedAt = existing.queuedAt();
                        int followers = existing.followers();
                        String license = existing.license();

                        if (jsonObject.containsKey("title")) title = jsonObject.getString("title");
                        if (jsonObject.containsKey("description")) description = jsonObject.getString("description");
                        categoryIds = readStringList(jsonObject, "categoryIds", categoryIds);
                        additionalTags = readStringList(jsonObject, "additionalTags", additionalTags);
                        donationUrls = readStringList(jsonObject, "donationUrls", donationUrls);
                        galleryUrls = readStringList(jsonObject, "galleryUrls", galleryUrls);
                        gameVersions = readStringList(jsonObject, "gameVersions", gameVersions);
                        versionIds = readStringList(jsonObject, "versionIds", versionIds);
                        if (jsonObject.containsKey("body")) body = jsonObject.getString("body");
                        if (jsonObject.containsKey("status")) status = jsonObject.getString("status");
                        if (jsonObject.containsKey("requestedStatus")) requestedStatus = jsonObject.getString("requestedStatus");
                        if (jsonObject.containsKey("issuesUrl")) issuesUrl = jsonObject.getString("issuesUrl");
                        if (jsonObject.containsKey("sourceUrl")) sourceUrl = jsonObject.getString("sourceUrl");
                        if (jsonObject.containsKey("wikiUrl")) wikiUrl = jsonObject.getString("wikiUrl");
                        if (jsonObject.containsKey("discordUrl")) discordUrl = jsonObject.getString("discordUrl");
                        if (jsonObject.containsKey("projectType")) projectType = jsonObject.getString("projectType");
                        if (jsonObject.containsKey("downloads")) downloads = jsonObject.getIntValue("downloads");
                        if (jsonObject.containsKey("iconUrl")) iconUrl = jsonObject.getString("iconUrl");
                        if (jsonObject.containsKey("colorHex")) colorHex = jsonObject.getString("colorHex");
                        if (jsonObject.containsKey("moderatorMessage")) moderatorMessage = jsonObject.getString("moderatorMessage");
                        if (jsonObject.containsKey("approvedAt")) approvedAt = jsonObject.getLongValue("approvedAt");
                        if (jsonObject.containsKey("queuedAt")) queuedAt = jsonObject.getLongValue("queuedAt");
                        if (jsonObject.containsKey("followers")) followers = jsonObject.getIntValue("followers");
                        if (jsonObject.containsKey("license")) license = jsonObject.getString("license");

                        ProjectRecord updated = new ProjectRecord(
                                id,
                                slug,
                                title,
                                description,
                                categoryIds,
                                additionalTags,
                                donationUrls,
                                galleryUrls,
                                gameVersions,
                                versionIds,
                                body,
                                status,
                                requestedStatus,
                                issuesUrl,
                                sourceUrl,
                                wikiUrl,
                                discordUrl,
                                projectType,
                                downloads,
                                iconUrl,
                                colorHex,
                                ownerId,
                                moderatorMessage,
                                createdAt,
                                updatedAt,
                                approvedAt,
                                queuedAt,
                                followers,
                                license
                        );
                        if (!Main.getHyrinthBackend().getStorageProvider().getProjectStorage().update(updated)) {
                            response.put("status", 400);
                            response.put("message", "failed to update project");
                            return ResponseEntity.ok(response);
                        }
                        response.put("response", ProjectResolver.getProject(slug));
                    } else if (action == PostAction.DELETE && (isOwner || user.hasPermission("project.delete"))) {
                        if (!Main.getHyrinthBackend().getStorageProvider().getProjectStorage().delete(slug)) {
                            response.put("status", 400);
                            response.put("message", "failed to delete project");
                            return ResponseEntity.ok(response);
                        }
                        removeProjectReferences(existing);
                    }
                } else {
                    response.put("status", 404);
                    response.put("message", "project not found");
                    return ResponseEntity.ok(response);
                }
            } else if (action == PostAction.CREATE) {
                slug = jsonObject.getString("slug");
                String title = jsonObject.getString("title");
                if (slug != null && !slug.isBlank() && title != null && !title.isBlank()) {
                    if (Main.getHyrinthBackend().getStorageProvider().getProjectStorage().findBySlug(slug).isPresent()) {
                        response.put("status", 409);
                        response.put("message", "project already exists");
                        return ResponseEntity.ok(response);
                    }
                    String id = UUID.randomUUID().toString();
                    String description = jsonObject.getString("description");
                    List<String> categoryIds = readStringList(jsonObject, "categoryIds", new ArrayList<>());
                    List<String> additionalTags = readStringList(jsonObject, "additionalTags", new ArrayList<>());
                    List<String> donationUrls = readStringList(jsonObject, "donationUrls", new ArrayList<>());
                    List<String> galleryUrls = readStringList(jsonObject, "galleryUrls", new ArrayList<>());
                    List<String> gameVersions = readStringList(jsonObject, "gameVersions", new ArrayList<>());
                    List<String> versionIds = readStringList(jsonObject, "versionIds", new ArrayList<>());
                    String body = jsonObject.getString("body");
                    String status = jsonObject.getString("status");
                    String requestedStatus = jsonObject.getString("requestedStatus");
                    String issuesUrl = jsonObject.getString("issuesUrl");
                    String sourceUrl = jsonObject.getString("sourceUrl");
                    String wikiUrl = jsonObject.getString("wikiUrl");
                    String discordUrl = jsonObject.getString("discordUrl");
                    String projectType = jsonObject.getString("projectType");
                    int downloads = jsonObject.getIntValue("downloads");
                    String iconUrl = jsonObject.getString("iconUrl");
                    String colorHex = jsonObject.getString("colorHex");
                    String moderatorMessage = jsonObject.getString("moderatorMessage");
                    long now = Instant.now().toEpochMilli();
                    long approvedAt = jsonObject.getLongValue("approvedAt");
                    long queuedAt = jsonObject.getLongValue("queuedAt");
                    int followers = jsonObject.getIntValue("followers");
                    String license = jsonObject.getString("license");

                    ProjectRecord record = new ProjectRecord(
                            id,
                            slug,
                            title,
                            description == null ? "" : description,
                            categoryIds,
                            additionalTags,
                            donationUrls,
                            galleryUrls,
                            gameVersions,
                            versionIds,
                            body == null ? "" : body,
                            status == null ? "" : status,
                            requestedStatus == null ? "" : requestedStatus,
                            issuesUrl == null ? "" : issuesUrl,
                            sourceUrl == null ? "" : sourceUrl,
                            wikiUrl == null ? "" : wikiUrl,
                            discordUrl == null ? "" : discordUrl,
                            projectType == null ? "" : projectType,
                            downloads,
                            iconUrl == null ? "" : iconUrl,
                            colorHex == null ? "" : colorHex,
                            user.getId(),
                            moderatorMessage == null ? "" : moderatorMessage,
                            now,
                            now,
                            approvedAt,
                            queuedAt,
                            followers,
                            license == null ? "" : license
                    );
                    if (!Main.getHyrinthBackend().getStorageProvider().getProjectStorage().create(record)) {
                        response.put("status", 400);
                        response.put("message", "failed to create project");
                        return ResponseEntity.ok(response);
                    }
                    addProjectToOwner(user.getId(), slug);
                    response.put("response", ProjectResolver.getProject(slug));
                }
            }

            response.put("status", 200);
            response.put("message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (Main.getHyrinthBackend().isDebug()) {
                Main.getLogger().err("Failed to resolve api request: " + action_);
            }
        }
        return ErrorController.handleError400(null);
    }

    private static List<String> readStringList(JSONObject json, String key, List<String> fallback) {
        if (json == null || !json.containsKey(key)) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        try {
            for (Object entry : json.getJSONArray(key)) {
                if (entry != null) {
                    values.add(entry.toString());
                }
            }
            return values;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void addProjectToOwner(String ownerId, String slug) {
        AccountStorage accountStorage = Main.getHyrinthBackend().getStorageProvider().getAccountStorage();
        long now = Instant.now().toEpochMilli();
        accountStorage.findById(ownerId).ifPresent(account -> {
            List<String> projects = new ArrayList<>(account.projects());
            if (!projects.contains(slug)) {
                projects.add(slug);
            }
            AccountRecord updated = new AccountRecord(
                    account.id(),
                    account.email(),
                    account.username(),
                    account.profilePicture(),
                    account.isHidden(),
                    account.passwordHash(),
                    account.isAdmin(),
                    account.permissions(),
                    projects,
                    account.teams(),
                    account.createdAt(),
                    now
            );
            accountStorage.update(updated);
        });
    }

    private static void removeProjectReferences(ProjectRecord record) {
        String slug = record.slug();
        String id = record.id();
        removeProjectFromAllAccounts(slug, id);
        removeProjectFromAllTeams(slug, id);
    }

    private static void removeProjectFromAllAccounts(String slug, String id) {
        AccountStorage accountStorage = Main.getHyrinthBackend().getStorageProvider().getAccountStorage();
        List<AccountRecord> accounts = accountStorage.findAll(200);
        long now = Instant.now().toEpochMilli();
        for (AccountRecord account : accounts) {
            if (!account.projects().contains(slug) && !account.projects().contains(id)) {
                continue;
            }
            List<String> projects = new ArrayList<>(account.projects());
            projects.removeIf(value -> value.equals(slug) || value.equals(id));
            AccountRecord updated = new AccountRecord(
                    account.id(),
                    account.email(),
                    account.username(),
                    account.profilePicture(),
                    account.isHidden(),
                    account.passwordHash(),
                    account.isAdmin(),
                    account.permissions(),
                    projects,
                    account.teams(),
                    account.createdAt(),
                    now
            );
            accountStorage.update(updated);
        }
    }

    private static void removeProjectFromAllTeams(String slug, String id) {
        TeamStorage teamStorage = Main.getHyrinthBackend().getStorageProvider().getTeamStorage();
        int offset = 0;
        int limit = 200;
        long now = Instant.now().toEpochMilli();
        while (true) {
            List<TeamRecord> teams = teamStorage.findPage(limit, offset);
            if (teams.isEmpty()) {
                break;
            }
            for (TeamRecord team : teams) {
                if (!team.projects().contains(slug) && !team.projects().contains(id)) {
                    continue;
                }
                List<String> projects = new ArrayList<>(team.projects());
                projects.removeIf(value -> value.equals(slug) || value.equals(id));
                TeamRecord updated = new TeamRecord(
                        team.id(),
                        team.name(),
                        team.picture(),
                        team.ownerId(),
                        team.isHidden(),
                        projects,
                        team.memberIds(),
                        team.createdAt(),
                        now
                );
                teamStorage.update(updated);
            }
            if (teams.size() < limit) {
                break;
            }
            offset += teams.size();
        }
    }
}
