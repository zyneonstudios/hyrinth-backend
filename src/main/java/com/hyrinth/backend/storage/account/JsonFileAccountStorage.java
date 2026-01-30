package com.hyrinth.backend.storage.account;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class JsonFileAccountStorage implements AccountStorage {

    private static final String USERS_KEY = "users";

    private final Path filePath;
    private final Object lock = new Object();
    private JSONObject data;

    public JsonFileAccountStorage(Path filePath) {
        this.filePath = filePath;
        loadOrCreate();
    }

    @Override
    public Optional<AccountRecord> findById(String id) {
        synchronized (lock) {
            return usersArray().stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::toRecord)
                    .filter(record -> Objects.equals(record.id(), id))
                    .findFirst();
        }
    }

    @Override
    public Optional<AccountRecord> findByEmail(String email) {
        synchronized (lock) {
            return usersArray().stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::toRecord)
                    .filter(record -> Objects.equals(record.email(), email))
                    .findFirst();
        }
    }

    @Override
    public Optional<AccountRecord> findByUsername(String username) {
        synchronized (lock) {
            return usersArray().stream()
                    .filter(JSONObject.class::isInstance)
                    .map(JSONObject.class::cast)
                    .map(this::toRecord)
                    .filter(record -> Objects.equals(record.username(), username))
                    .findFirst();
        }
    }

    @Override
    public List<AccountRecord> findPage(int limit, int offset) {
        synchronized (lock) {
            if (limit <= 0 || offset < 0) {
                return List.of();
            }
            List<AccountRecord> records = new ArrayList<>();
            for (Object entry : usersArray()) {
                if (entry instanceof JSONObject user) {
                    records.add(toRecord(user));
                }
            }
            records.sort(Comparator.comparingLong(AccountRecord::createdAt)
                    .thenComparing(AccountRecord::id));
            if (offset >= records.size()) {
                return List.of();
            }
            int toIndex = Math.min(records.size(), offset + limit);
            return records.subList(offset, toIndex);
        }
    }

    @Override
    public boolean hasAdminAccount() {
        synchronized (lock) {
            for (Object entry : usersArray()) {
                if (entry instanceof JSONObject user) {
                    if (user.getBooleanValue("isAdmin")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public boolean create(AccountRecord record) {
        synchronized (lock) {
            if (existsByIdOrIdentity(record)) {
                return false;
            }
            usersArray().add(fromRecord(record));
            return save();
        }
    }

    @Override
    public boolean updatePasswordHash(String id, String passwordHash, long updatedAt) {
        synchronized (lock) {
            JSONArray users = usersArray();
            for (int i = 0; i < users.size(); i++) {
                Object entry = users.get(i);
                if (entry instanceof JSONObject user) {
                    if (Objects.equals(user.getString("id"), id)) {
                        user.put("passwordHash", passwordHash);
                        user.put("updatedAt", updatedAt);
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
            JSONArray users = usersArray();
            for (int i = 0; i < users.size(); i++) {
                Object entry = users.get(i);
                if (entry instanceof JSONObject user) {
                    if (Objects.equals(user.getString("id"), id)) {
                        users.remove(i);
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
                    data.put(USERS_KEY, new JSONArray());
                    save();
                    return;
                }
                String raw = Files.readString(filePath, StandardCharsets.UTF_8);
                data = JSONObject.parseObject(raw);
                if (data == null) {
                    data = new JSONObject();
                }
                if (!data.containsKey(USERS_KEY)) {
                    data.put(USERS_KEY, new JSONArray());
                    save();
                }
                boolean updated = false;
                JSONArray users = data.getJSONArray(USERS_KEY);
                if (users != null) {
                    for (Object entry : users) {
                        if (entry instanceof JSONObject user) {
                            if (!user.containsKey("permissions")) {
                                user.put("permissions", new JSONArray());
                                updated = true;
                            }
                            if (!user.containsKey("projects")) {
                                user.put("projects", new JSONArray());
                                updated = true;
                            }
                            if (!user.containsKey("teams")) {
                                if (user.containsKey("organizations")) {
                                    user.put("teams", user.getJSONArray("organizations"));
                                } else {
                                    user.put("teams", new JSONArray());
                                }
                                updated = true;
                            }
                            if (!user.containsKey("profilePicture")) {
                                user.put("profilePicture", "");
                                updated = true;
                            }
                            if (!user.containsKey("isHidden")) {
                                user.put("isHidden", false);
                                updated = true;
                            }
                            if (!user.containsKey("isAdmin")) {
                                user.put("isAdmin", hasAdminPermission(user));
                                updated = true;
                            }
                        }
                    }
                }
                if (updated) {
                    save();
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load account storage file: " + filePath, e);
            }
        }
    }

    private JSONArray usersArray() {
        JSONArray users = data.getJSONArray(USERS_KEY);
        if (users == null) {
            users = new JSONArray();
            data.put(USERS_KEY, users);
        }
        return users;
    }

    private boolean save() {
        try {
            Files.writeString(filePath, data.toJSONString(), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean existsByIdOrIdentity(AccountRecord record) {
        return findById(record.id()).isPresent()
                || findByEmail(record.email()).isPresent()
                || findByUsername(record.username()).isPresent();
    }

    private AccountRecord toRecord(JSONObject json) {
        java.util.List<String> permissions = toStringList(json.getJSONArray("permissions"));
        java.util.List<String> projects = toStringList(json.getJSONArray("projects"));
        java.util.List<String> teams = toStringList(json.getJSONArray("teams"));
        return new AccountRecord(
                json.getString("id"),
                json.getString("email"),
                json.getString("username"),
                json.getString("profilePicture"),
                json.getBooleanValue("isHidden"),
                json.getString("passwordHash"),
                json.getBooleanValue("isAdmin"),
                permissions,
                projects,
                teams,
                json.getLongValue("createdAt"),
                json.getLongValue("updatedAt")
        );
    }

    private JSONObject fromRecord(AccountRecord record) {
        JSONObject json = new JSONObject();
        json.put("id", record.id());
        json.put("email", record.email());
        json.put("username", record.username());
        json.put("profilePicture", record.profilePicture());
        json.put("isHidden", record.isHidden());
        json.put("passwordHash", record.passwordHash());
        json.put("isAdmin", record.isAdmin());
        json.put("permissions", record.permissions());
        json.put("projects", record.projects());
        json.put("teams", record.teams());
        json.put("createdAt", record.createdAt());
        json.put("updatedAt", record.updatedAt());
        return json;
    }

    private java.util.List<String> toStringList(JSONArray array) {
        java.util.List<String> list = new java.util.ArrayList<>();
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

    private boolean hasAdminPermission(JSONObject user) {
        JSONArray permissions = user.getJSONArray("permissions");
        if (permissions == null) {
            return false;
        }
        for (Object permission : permissions) {
            if (permission != null && "admin".equalsIgnoreCase(permission.toString())) {
                return true;
            }
        }
        return false;
    }
}
