package com.hyrinth.backend.storage.team;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class JsonFileTeamStorage implements TeamStorage {

    private static final String TEAMS_KEY = "teams";

    private final Path filePath;
    private final Object lock = new Object();
    private JSONObject data;

    public JsonFileTeamStorage(Path filePath) {
        this.filePath = filePath;
        loadOrCreate();
    }

    @Override
    public Optional<TeamRecord> findById(String id) {
        synchronized (lock) {
            return teamsArray().stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::toRecord)
                    .filter(record -> record.id().equals(id))
                    .findFirst();
        }
    }

    @Override
    public List<TeamRecord> findPage(int limit, int offset) {
        synchronized (lock) {
            if (limit <= 0 || offset < 0) {
                return List.of();
            }
            List<TeamRecord> records = new ArrayList<>();
            for (Object entry : teamsArray()) {
                if (entry instanceof JSONObject team) {
                    records.add(toRecord(team));
                }
            }
            records.sort(Comparator.comparingLong(TeamRecord::createdAt)
                    .thenComparing(TeamRecord::id));
            if (offset >= records.size()) {
                return List.of();
            }
            int toIndex = Math.min(records.size(), offset + limit);
            return records.subList(offset, toIndex);
        }
    }

    @Override
    public boolean create(TeamRecord record) {
        synchronized (lock) {
            if (record == null || record.id() == null || record.id().isBlank()) {
                return false;
            }
            if (findById(record.id()).isPresent()) {
                return false;
            }
            teamsArray().add(fromRecord(record));
            return save();
        }
    }

    @Override
    public boolean update(TeamRecord record) {
        synchronized (lock) {
            if (record == null || record.id() == null || record.id().isBlank()) {
                return false;
            }
            JSONArray teams = teamsArray();
            for (int i = 0; i < teams.size(); i++) {
                Object entry = teams.get(i);
                if (entry instanceof JSONObject team) {
                    if (record.id().equals(team.getString("id"))) {
                        teams.set(i, fromRecord(record));
                        return save();
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean delete(String id) {
        synchronized (lock) {
            JSONArray teams = teamsArray();
            for (int i = 0; i < teams.size(); i++) {
                Object entry = teams.get(i);
                if (entry instanceof JSONObject team) {
                    if (id.equals(team.getString("id"))) {
                        teams.remove(i);
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
                    data.put(TEAMS_KEY, new JSONArray());
                    save();
                    return;
                }
                String raw = Files.readString(filePath, StandardCharsets.UTF_8);
                data = JSONObject.parseObject(raw);
                if (data == null) {
                    data = new JSONObject();
                }
                if (!data.containsKey(TEAMS_KEY)) {
                    data.put(TEAMS_KEY, new JSONArray());
                    save();
                }
                boolean updated = false;
                JSONArray teams = data.getJSONArray(TEAMS_KEY);
                if (teams != null) {
                    for (Object entry : teams) {
                        if (entry instanceof JSONObject team) {
                            if (!team.containsKey("projects")) {
                                team.put("projects", new JSONArray());
                                updated = true;
                            }
                            if (!team.containsKey("memberIds")) {
                                team.put("memberIds", new JSONArray());
                                updated = true;
                            }
                            if (!team.containsKey("picture")) {
                                team.put("picture", "");
                                updated = true;
                            }
                            if (!team.containsKey("isHidden")) {
                                team.put("isHidden", false);
                                updated = true;
                            }
                        }
                    }
                }
                if (updated) {
                    save();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load team storage file: " + filePath, e);
            }
        }
    }

    private JSONArray teamsArray() {
        JSONArray teams = data.getJSONArray(TEAMS_KEY);
        if (teams == null) {
            teams = new JSONArray();
            data.put(TEAMS_KEY, teams);
        }
        return teams;
    }

    private boolean save() {
        try {
            Files.writeString(filePath, data.toJSONString(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private TeamRecord toRecord(JSONObject json) {
        return new TeamRecord(
                json.getString("id"),
                json.getString("name"),
                json.getString("picture"),
                json.getString("ownerId"),
                json.getBooleanValue("isHidden"),
                toStringList(json.getJSONArray("projects")),
                toStringList(json.getJSONArray("memberIds")),
                json.getLongValue("createdAt"),
                json.getLongValue("updatedAt")
        );
    }

    private JSONObject fromRecord(TeamRecord record) {
        JSONObject json = new JSONObject();
        json.put("id", record.id());
        json.put("name", record.name());
        json.put("picture", record.picture());
        json.put("ownerId", record.ownerId());
        json.put("isHidden", record.isHidden());
        json.put("projects", record.projects());
        json.put("memberIds", record.memberIds());
        json.put("createdAt", record.createdAt());
        json.put("updatedAt", record.updatedAt());
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
