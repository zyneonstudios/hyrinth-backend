package com.hyrinth.backend.storage.team;

import com.hyrinth.backend.Main;
import org.zyneonstudios.apex.utilities.sql.SQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlTeamStorage implements TeamStorage {

    private final SQL sql;
    private final String table;
    private boolean schemaEnsured = false;

    public SqlTeamStorage(SQL sql) {
        this(sql, "teams");
    }

    public SqlTeamStorage(SQL sql, String table) {
        this.sql = sql;
        this.table = table;
        ensureSchema();
    }

    private synchronized void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Team storage could not reconnect to SQL backend for schema init.");
            return;
        }
        String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`id` VARCHAR(36) NOT NULL PRIMARY KEY,"
                + "`name` VARCHAR(255) NOT NULL,"
                + "`picture` VARCHAR(512) NOT NULL,"
                + "`owner_id` VARCHAR(36) NOT NULL,"
                + "`is_hidden` BOOLEAN NOT NULL DEFAULT FALSE,"
                + "`projects` TEXT NOT NULL,"
                + "`member_ids` TEXT NOT NULL,"
                + "`created_at` BIGINT NOT NULL,"
                + "`updated_at` BIGINT NOT NULL"
                + ");";
        String ownerIndex = "CREATE INDEX IF NOT EXISTS `idx_" + table + "_owner` ON `" + table + "` (`owner_id`);";
        try (Connection connection = sql.getConnection()) {
            if (connection == null) {
                Main.getLogger().err("Team storage schema init failed: SQL connection is null.");
                return;
            }
            try (PreparedStatement createStatement = connection.prepareStatement(createTable)) {
                createStatement.execute();
            }
            try (PreparedStatement ownerStatement = connection.prepareStatement(ownerIndex)) {
                ownerStatement.execute();
            } catch (Exception e) {
                Main.getLogger().err("Team storage owner index init failed: " + e.getMessage());
            }
            schemaEnsured = true;
        } catch (Exception e) {
            Main.getLogger().err("Team storage schema init failed: " + e.getMessage());
        }
    }

    @Override
    public Optional<TeamRecord> findById(String id) {
        ensureSchema();
        String query = "SELECT * FROM `" + table + "` WHERE `id` = ?";
        return fetchOne(query, id);
    }

    @Override
    public List<TeamRecord> findPage(int limit, int offset) {
        ensureSchema();
        if (limit <= 0 || offset < 0) {
            return List.of();
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Team storage could not reconnect to SQL backend.");
            return List.of();
        }
        String query = "SELECT * FROM `" + table + "` ORDER BY `created_at` ASC, `id` ASC LIMIT ? OFFSET ?";
        List<TeamRecord> records = new ArrayList<>();
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, limit);
            statement.setInt(2, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(fromResultSet(resultSet));
                }
            }
        } catch (Exception e) {
            Main.getLogger().err("Team storage page query failed: " + e.getMessage());
        }
        return records;
    }

    @Override
    public boolean create(TeamRecord record) {
        ensureSchema();
        String query = "INSERT INTO `" + table + "` (`id`, `name`, `picture`, `owner_id`, `is_hidden`, `projects`, `member_ids`, `created_at`, `updated_at`)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return executeUpdate(query, statement -> {
            statement.setString(1, record.id());
            statement.setString(2, record.name());
            statement.setString(3, record.picture());
            statement.setString(4, record.ownerId());
            statement.setBoolean(5, record.isHidden());
            statement.setString(6, serializeProjects(record.projects()));
            statement.setString(7, serializeMemberIds(record.memberIds()));
            statement.setLong(8, record.createdAt());
            statement.setLong(9, record.updatedAt());
        });
    }

    @Override
    public boolean update(TeamRecord record) {
        ensureSchema();
        String query = "UPDATE `" + table + "` SET `name` = ?, `picture` = ?, `owner_id` = ?, `is_hidden` = ?, `projects` = ?, `member_ids` = ?, `updated_at` = ? WHERE `id` = ?";
        return executeUpdate(query, statement -> {
            statement.setString(1, record.name());
            statement.setString(2, record.picture());
            statement.setString(3, record.ownerId());
            statement.setBoolean(4, record.isHidden());
            statement.setString(5, serializeProjects(record.projects()));
            statement.setString(6, serializeMemberIds(record.memberIds()));
            statement.setLong(7, record.updatedAt());
            statement.setString(8, record.id());
        });
    }

    @Override
    public boolean delete(String id) {
        ensureSchema();
        String query = "DELETE FROM `" + table + "` WHERE `id` = ?";
        return executeUpdate(query, statement -> statement.setString(1, id));
    }

    private Optional<TeamRecord> fetchOne(String query, String value) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Team storage could not reconnect to SQL backend.");
            return Optional.empty();
        }
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(fromResultSet(resultSet));
                }
            }
        } catch (Exception e) {
            Main.getLogger().err("Team storage query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    private boolean executeUpdate(String query, StatementConfigurer configurer) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Team storage could not reconnect to SQL backend.");
            return false;
        }
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            configurer.configure(statement);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            Main.getLogger().err("Team storage update failed: " + e.getMessage());
            return false;
        }
    }

    private TeamRecord fromResultSet(ResultSet resultSet) throws Exception {
        return new TeamRecord(
                resultSet.getString("id"),
                resultSet.getString("name"),
                resultSet.getString("picture"),
                resultSet.getString("owner_id"),
                resultSet.getBoolean("is_hidden"),
                deserializeProjects(resultSet.getString("projects")),
                deserializeMemberIds(resultSet.getString("member_ids")),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }

    private String serializeProjects(List<String> projects) {
        if (projects == null || projects.isEmpty()) {
            return "[]";
        }
        return com.alibaba.fastjson2.JSONArray.toJSONString(projects);
    }

    private List<String> deserializeProjects(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(raw);
        List<String> projects = new ArrayList<>();
        if (array != null) {
            for (Object entry : array) {
                if (entry != null) {
                    projects.add(entry.toString());
                }
            }
        }
        return projects;
    }

    private String serializeMemberIds(List<String> members) {
        if (members == null || members.isEmpty()) {
            return "[]";
        }
        return com.alibaba.fastjson2.JSONArray.toJSONString(members);
    }

    private List<String> deserializeMemberIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(raw);
        List<String> members = new ArrayList<>();
        if (array != null) {
            for (Object entry : array) {
                if (entry != null) {
                    members.add(entry.toString());
                }
            }
        }
        return members;
    }

    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws Exception;
    }
}
