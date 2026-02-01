package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.entities.HyrinthUser;
import com.hyrinth.backend.storage.account.AccountRecord;
import com.hyrinth.backend.storage.account.AccountStorage;
import com.hyrinth.backend.storage.team.TeamRecord;
import com.hyrinth.backend.webserver.resolver.TeamResolver;
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
public class TeamController {

    @GetMapping({"/team/**","/teams/**"})
    public static ResponseEntity<Object> handleGetTeamRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String id = path.split("/")[2];
        JSONObject team = TeamResolver.getTeam(id);
        String apiKey = request.getHeader("X-API-Key");
        if (team != null) {
            boolean hidden = team.getBooleanValue("isHidden");
            if (apiKey != null) {
                JSONObject user = UserResolver.getAuthenticatedUser(apiKey);
                if (user != null) {
                    if(user.getString("id").equals(team.getString("ownerId"))) {
                        return ResponseEntity.ok(team);
                    } else if(hidden) {
                        HyrinthUser hyrinthUser = UserResolver.findById(user.getString("id"));
                        if(hyrinthUser.hasPermission("team.bypass.hidden")) {
                            hidden = false;
                        } else {
                            return ErrorController.handleError403(request);
                        }
                    }
                }
            }
            if (hidden) {
                return ErrorController.handleError401(request);
            }
            return ResponseEntity.ok(team);
        }
        return ErrorController.handleError404(request);
    }

    @GetMapping({"/team","/teams","/team/","/teams/"})
    public static ResponseEntity<Object> handleListTeamsRequest(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if(apiKey!=null) {
            HyrinthUser user = UserResolver.findByToken(apiKey);
            if(user != null && user.hasPermission("team.list")) {
                String limitParam = request.getParameter("limit");
                String offsetParam = request.getParameter("offset");
                if(limitParam == null) limitParam = "25";
                if(offsetParam == null) offsetParam = "0";
                return ResponseEntity.ok(TeamResolver.getTeams(Integer.parseInt(limitParam),Integer.parseInt(offsetParam)));
            }
        }
        return ErrorController.handleError401(request);
    }

    @RequestMapping(value={"/team","/teams","/team/","/teams/"},method = RequestMethod.POST)
    public ResponseEntity<Object> handleTeamAction(@RequestHeader("action") String action_, @RequestBody JSONObject jsonObject, @RequestHeader("x-api-key") String token) {
        try {
            PostAction action = PostAction.valueOf(action_.toUpperCase());
            HyrinthUser user = UserResolver.findByToken(token);
            JSONObject response = new JSONObject();
            response.put("endpoint", "/team");
            response.put("action", action_);

            if (user == null) {
                response.put("status", 401);
                response.put("message", "unauthorized");
                return ResponseEntity.ok(response);
            }

            String id = jsonObject.getString("id");
            if (id != null && !id.isBlank()) {
                Optional<TeamRecord> target = Main.getHyrinthBackend().getStorageProvider().getTeamStorage().findById(id);
                if (target.isPresent()) {
                    TeamRecord existing = target.get();
                    boolean isOwner = existing.ownerId().equals(user.getId());
                    if (action == PostAction.UPDATE && (isOwner || user.hasPermission("team.edit"))) {
                        String name = existing.name();
                        String picture = existing.picture();
                        String ownerId = existing.ownerId();
                        boolean isHidden = existing.isHidden();
                        List<String> projects = existing.projects();
                        List<String> memberIds = existing.memberIds();
                        long createdAt = existing.createdAt();
                        long updatedAt = Instant.now().toEpochMilli();

                        if (jsonObject.containsKey("name")) name = jsonObject.getString("name");
                        if (jsonObject.containsKey("picture")) picture = jsonObject.getString("picture");
                        if (jsonObject.containsKey("isHidden")) isHidden = jsonObject.getBooleanValue("isHidden");
                        projects = readStringList(jsonObject, "projects", projects);
                        memberIds = readStringList(jsonObject, "memberIds", memberIds);

                        TeamRecord updated = new TeamRecord(id, name, picture, ownerId, isHidden, projects, memberIds, createdAt, updatedAt);
                        if (!Main.getHyrinthBackend().getStorageProvider().getTeamStorage().update(updated)) {
                            response.put("status", 400);
                            response.put("message", "failed to update team");
                            return ResponseEntity.ok(response);
                        }
                        response.put("response", TeamResolver.getTeam(id));
                    } else if (action == PostAction.DELETE && (isOwner || user.hasPermission("team.delete"))) {
                        if (!Main.getHyrinthBackend().getStorageProvider().getTeamStorage().delete(id)) {
                            response.put("status", 400);
                            response.put("message", "failed to delete team");
                            return ResponseEntity.ok(response);
                        }
                        removeTeamFromAllAccounts(id);
                    }
                } else {
                    response.put("status", 404);
                    response.put("message", "team not found");
                    return ResponseEntity.ok(response);
                }
            } else if (action == PostAction.CREATE) {
                String name = jsonObject.getString("name");
                if (name != null && !name.isBlank()) {
                    id = UUID.randomUUID().toString();
                    String picture = jsonObject.getString("picture");
                    boolean isHidden = jsonObject.getBooleanValue("isHidden");
                    List<String> projects = readStringList(jsonObject, "projects", new ArrayList<>());
                    List<String> memberIds = readStringList(jsonObject, "memberIds", new ArrayList<>());
                    long now = Instant.now().toEpochMilli();

                    TeamRecord record = new TeamRecord(id, name, picture == null ? "" : picture, user.getId(), isHidden, projects, memberIds, now, now);
                    if (!Main.getHyrinthBackend().getStorageProvider().getTeamStorage().create(record)) {
                        response.put("status", 400);
                        response.put("message", "failed to create team");
                        return ResponseEntity.ok(response);
                    }
                    addTeamToOwner(user.getId(), id, now);
                    response.put("response", TeamResolver.getTeam(id));
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

    private static void addTeamToOwner(String ownerId, String teamId, long now) {
        AccountStorage accountStorage = Main.getHyrinthBackend().getStorageProvider().getAccountStorage();
        accountStorage.findById(ownerId).ifPresent(account -> {
            List<String> teams = new ArrayList<>(account.teams());
            if (!teams.contains(teamId)) {
                teams.add(teamId);
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
                    account.projects(),
                    teams,
                    account.createdAt(),
                    now
            );
            accountStorage.update(updated);
        });
    }

    static void removeTeamFromAllAccounts(String teamId) {
        AccountStorage accountStorage = Main.getHyrinthBackend().getStorageProvider().getAccountStorage();
        List<AccountRecord> accounts = accountStorage.findAll(200);
        long now = Instant.now().toEpochMilli();
        for (AccountRecord account : accounts) {
            if (!account.teams().contains(teamId)) {
                continue;
            }
            List<String> teams = new ArrayList<>(account.teams());
            teams.removeIf(teamId::equals);
            AccountRecord updated = new AccountRecord(
                    account.id(),
                    account.email(),
                    account.username(),
                    account.profilePicture(),
                    account.isHidden(),
                    account.passwordHash(),
                    account.isAdmin(),
                    account.permissions(),
                    account.projects(),
                    teams,
                    account.createdAt(),
                    now
            );
            accountStorage.update(updated);
        }
    }
}
