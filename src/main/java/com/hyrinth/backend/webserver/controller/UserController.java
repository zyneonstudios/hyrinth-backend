package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.entities.HyrinthUser;
import com.hyrinth.backend.storage.account.AccountRecord;
import com.hyrinth.backend.webserver.resolver.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@RestController
public class UserController {

    @GetMapping({"/user/**","/users/**"})
    public static ResponseEntity<Object> handleGetUserRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String id = path.split("/")[2];
        JSONObject user = UserResolver.getUser(id);
        String apiKey = request.getHeader("X-API-Key");
        if (user != null) {
            boolean hidden = user.getBooleanValue("isHidden");
            if (apiKey != null) {
                JSONObject user2 = UserResolver.getAuthenticatedUser(apiKey);
                if (user2 != null) {
                    if(user2.getString("id").equals(user.getString("id"))) {
                        return ResponseEntity.ok(user2);
                    } else if(hidden) {
                        HyrinthUser hyrinthUser = UserResolver.findById(user2.getString("id"));
                        if(hyrinthUser.hasPermission("user.bypass.hidden")) {
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
            return ResponseEntity.ok(user);
        }
        return ErrorController.handleError404(request);
    }

    @GetMapping({"/user","/users","/user/","/users/"})
    public static ResponseEntity<Object> handleListUsersRequest(HttpServletRequest request) {
        String apiKey = request.getHeader("X-API-Key");
        if(apiKey!=null) {
            HyrinthUser user = UserResolver.findByToken(apiKey);
            if(user != null && user.hasPermission("user.list")) {
                String limitParam = request.getParameter("limit");
                String offsetParam = request.getParameter("offset");
                if(limitParam == null) limitParam = "25";
                if(offsetParam == null) offsetParam = "0";
                return ResponseEntity.ok(UserResolver.getUsers(Integer.parseInt(limitParam),Integer.parseInt(offsetParam)));
            }
        }
        return ErrorController.handleError401(request);
    }

    @RequestMapping(value={"/user","/users","/user/","/users/"},method = RequestMethod.POST)
    public ResponseEntity<Object> getJsonObject(@RequestHeader("action") String action_, @RequestBody JSONObject jsonObject, @RequestHeader("x-api-key") String token) {
        try {
            PostAction action = PostAction.valueOf(action_.toUpperCase());
            HyrinthUser user = UserResolver.findByToken(token);
            JSONObject response = new JSONObject();
            response.put("endpoint", "/user");
            response.put("action", action_);

            if(user == null) {
                response.put("status", 401);
                response.put("message", "unauthorized");
                return ResponseEntity.ok(response);
            }

            String id = jsonObject.getString("id");
            if (id != null && !id.isBlank()) {
                Optional<AccountRecord> target = Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findById(id);
                if (target.isPresent()) {
                    HyrinthUser targetUser = new HyrinthUser(target.get());
                    if (action == PostAction.UPDATE && (id.equals(user.getId()) || user.hasPermission("admin.user.edit"))) {
                        String email = targetUser.getEmail();
                        String username = targetUser.getUsername();
                        String profilePicture = targetUser.getProfilePicture();
                        boolean isHidden = targetUser.isHidden();
                        String passwordHash = target.get().passwordHash();
                        boolean isAdmin = target.get().isAdmin();
                        java.util.List<String> permissions = target.get().permissions();
                        java.util.List<String> projects = target.get().projects();
                        java.util.List<String> teams = target.get().teams();
                        long createdAt = target.get().createdAt();
                        long updatedAt = Instant.now().toEpochMilli();

                        if (jsonObject.containsKey("email")) email = jsonObject.getString("email");
                        if (jsonObject.containsKey("username")) username = jsonObject.getString("username");
                        if (jsonObject.containsKey("profilePicture"))
                            profilePicture = jsonObject.getString("profilePicture");
                        if (jsonObject.containsKey("isHidden")) isHidden = jsonObject.getBooleanValue("isHidden");
                        if (jsonObject.containsKey("passwordHash")) passwordHash = jsonObject.getString("passwordHash");

                        if(!Main.getHyrinthBackend().getStorageProvider().getAccountStorage().update(new AccountRecord(id, email, username, profilePicture, isHidden, passwordHash, isAdmin, permissions, projects, teams, createdAt, updatedAt))) {
                            response.put("status", 400);
                            response.put("message", "failed to update user");
                            return ResponseEntity.ok(response);
                        }
                        response.put("response", UserResolver.getUser(id));
                    } else if (action == PostAction.DELETE && (id.equals(user.getId()) || user.hasPermission("admin.user.delete"))) {
                        if(!Main.getHyrinthBackend().getStorageProvider().getAccountStorage().delete(id)) {
                            response.put("status", 400);
                            response.put("message", "failed to delete user");
                            return ResponseEntity.ok(response);
                        }
                    }
                } else {
                    response.put("status", 404);
                    response.put("message", "user not found");
                    return ResponseEntity.ok(response);
                }
            } else if(action==PostAction.CREATE && (user.hasPermission("admin.user.create"))) {
                String email = jsonObject.getString("email");
                String username = jsonObject.getString("username");
                String passwordHash = jsonObject.getString("password");
                if(username != null && email !=null && passwordHash !=null && !username.isEmpty() && !passwordHash.isEmpty() && !email.isEmpty()) {
                    do {
                        id = UUID.randomUUID().toString();
                    } while ((Main.getHyrinthBackend().getStorageProvider().getAccountStorage().findById(id).isPresent()));

                    long createdAt = Instant.now().toEpochMilli();
                    if(!Main.getHyrinthBackend().getStorageProvider().getAccountStorage().create(new AccountRecord(id, email, username, "", false, passwordHash, false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), createdAt, createdAt))) {
                        response.put("status", 400);
                        response.put("message", "failed to create user");
                        return ResponseEntity.ok(response);
                    }
                    response.put("response", UserResolver.getUser(id));
                }
            }

            response.put("status", 200);
            response.put("message", "success");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if(Main.getHyrinthBackend().isDebug()) {
                Main.getLogger().err("Failed to resolve api request: " + action_);
            }
        }
        return ErrorController.handleError400(null);
    }
}
