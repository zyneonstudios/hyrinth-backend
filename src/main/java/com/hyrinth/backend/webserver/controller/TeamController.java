package com.hyrinth.backend.webserver.controller;

import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.entities.HyrinthUser;
import com.hyrinth.backend.webserver.resolver.TeamResolver;
import com.hyrinth.backend.webserver.resolver.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}