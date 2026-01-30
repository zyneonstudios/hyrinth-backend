package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.entities.HyrinthUser;
import com.hyrinth.backend.webserver.resolver.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public class UserController {

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
}
