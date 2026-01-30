package com.hyrinth.backend.storage.session;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class JsonFileAccountSessionStorage implements AccountSessionStorage {

    private static final String SESSIONS_KEY = "sessions";

    private final Path filePath;
    private final Object lock = new Object();
    private JSONObject data;

    public JsonFileAccountSessionStorage(Path filePath) {
        this.filePath = filePath;
        loadOrCreate();
    }

    @Override
    public Optional<AccountSessionRecord> findByToken(String token) {
        synchronized (lock) {
            reloadFromDisk();
            for (Object entry : sessionsArray()) {
                if (entry instanceof JSONObject session) {
                    if (token.equals(session.getString("token"))) {
                        return Optional.of(toRecord(session));
                    }
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public boolean create(AccountSessionRecord session) {
        synchronized (lock) {
            sessionsArray().add(fromRecord(session));
            return save();
        }
    }

    @Override
    public boolean update(AccountSessionRecord session) {
        synchronized (lock) {
            JSONArray sessions = sessionsArray();
            for (int i = 0; i < sessions.size(); i++) {
                Object entry = sessions.get(i);
                if (entry instanceof JSONObject existing) {
                    if (session.token().equals(existing.getString("token"))) {
                        sessions.set(i, fromRecord(session));
                        return save();
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean delete(String token) {
        synchronized (lock) {
            JSONArray sessions = sessionsArray();
            for (int i = 0; i < sessions.size(); i++) {
                Object entry = sessions.get(i);
                if (entry instanceof JSONObject session) {
                    if (token.equals(session.getString("token"))) {
                        sessions.remove(i);
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
                    data.put(SESSIONS_KEY, new JSONArray());
                    save();
                    return;
                }
                String raw = Files.readString(filePath, StandardCharsets.UTF_8);
                data = JSONObject.parseObject(raw);
                if (data == null) {
                    data = new JSONObject();
                }
                if (!data.containsKey(SESSIONS_KEY)) {
                    data.put(SESSIONS_KEY, new JSONArray());
                    save();
                }
                boolean updated = false;
                JSONArray sessions = data.getJSONArray(SESSIONS_KEY);
                if (sessions != null) {
                    for (Object entry : sessions) {
                        if (entry instanceof JSONObject session) {
                            if (!session.containsKey("type")) {
                                session.put("type", AccountTokenType.SESSION.name());
                                updated = true;
                            }
                            if (!session.containsKey("remainingUses")) {
                                session.put("remainingUses", -1);
                                updated = true;
                            }
                        }
                    }
                }
                if (updated) {
                    save();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load session storage file: " + filePath, e);
            }
        }
    }

    private void reloadFromDisk() {
        try {
            if (Files.notExists(filePath)) {
                data = new JSONObject();
                data.put(SESSIONS_KEY, new JSONArray());
                return;
            }
            String raw = Files.readString(filePath, StandardCharsets.UTF_8);
            JSONObject loaded = JSONObject.parseObject(raw);
            if (loaded == null) {
                loaded = new JSONObject();
            }
            if (!loaded.containsKey(SESSIONS_KEY)) {
                loaded.put(SESSIONS_KEY, new JSONArray());
            }
            data = loaded;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reload session storage file: " + filePath, e);
        }
    }

    private JSONArray sessionsArray() {
        JSONArray sessions = data.getJSONArray(SESSIONS_KEY);
        if (sessions == null) {
            sessions = new JSONArray();
            data.put(SESSIONS_KEY, sessions);
        }
        return sessions;
    }

    private boolean save() {
        try {
            Files.writeString(filePath, data.toJSONString(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private AccountSessionRecord toRecord(JSONObject json) {
        return new AccountSessionRecord(
                json.getString("token"),
                json.getString("accountId"),
                json.getLongValue("createdAt"),
                json.getLongValue("expiresAt"),
                parseTokenType(json.getString("type")),
                getRemainingUses(json)
        );
    }

    private JSONObject fromRecord(AccountSessionRecord session) {
        JSONObject json = new JSONObject();
        json.put("token", session.token());
        json.put("accountId", session.accountId());
        json.put("createdAt", session.createdAt());
        json.put("expiresAt", session.expiresAt());
        json.put("type", session.type().name());
        json.put("remainingUses", session.remainingUses());
        return json;
    }

    private AccountTokenType parseTokenType(String raw) {
        if (raw == null || raw.isBlank()) {
            return AccountTokenType.SESSION;
        }
        try {
            return AccountTokenType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AccountTokenType.SESSION;
        }
    }

    private int getRemainingUses(JSONObject json) {
        try {
            return json.getIntValue("remainingUses");
        } catch (Exception e) {
            return -1;
        }
    }
}
