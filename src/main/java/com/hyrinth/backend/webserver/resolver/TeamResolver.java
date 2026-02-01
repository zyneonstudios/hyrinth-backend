package com.hyrinth.backend.webserver.resolver;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.hyrinth.backend.Main;
import com.hyrinth.backend.entities.HyrinthTeam;
import com.hyrinth.backend.storage.team.TeamRecord;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class TeamResolver {

    public static JSONObject getTeam(String id) {
        if(id == null) return null;
        Optional<TeamRecord> team = Main.getHyrinthBackend().getStorageProvider().getTeamStorage().findById(id);
        if (team.isPresent()) {
            JSONObject json = new JSONObject();
            json.put("id", team.get().id());
            json.put("name", team.get().name());
            json.put("picture", team.get().picture());
            json.put("ownerId", team.get().ownerId());
            json.put("isHidden", team.get().isHidden());
            json.put("projects", team.get().projects());
            json.put("memberIds", team.get().memberIds());
            json.put("createdAt", team.get().createdAt());
            json.put("updatedAt", team.get().updatedAt());
            return json;
        } else {
            return null;
        }
    }

    public static JSONObject getAuthenticatedTeam(String id, String token) {
        return getTeam(id);
    }

    public static HyrinthTeam findById(String id) {
        return Main.getHyrinthBackend().getStorageProvider().getTeamStorage().findById(id).map(HyrinthTeam::new).orElse(null);
    }

    public static JSONObject getTeams(int limit, int offset) {
        if(limit>100) {
            limit = 100;
        }

        JSONObject response = new JSONObject();
        response.put("limit", limit);
        response.put("offset", offset);

        JSONArray teams = new JSONArray();
        List<TeamRecord> teamRecords = Main.getHyrinthBackend().getStorageProvider().getTeamStorage().findPage(limit, offset);
        AtomicInteger i = new AtomicInteger();
        teamRecords.forEach(team -> {
            teams.add(getTeam(team.id()));
            i.getAndIncrement();
        });

        response.put("matches", teamRecords.size());
        response.put("total_hits",-1);
        response.put("hits", teams);
        return response;
    }
}