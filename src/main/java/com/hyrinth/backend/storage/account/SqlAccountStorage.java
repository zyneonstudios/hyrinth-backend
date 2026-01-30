package com.hyrinth.backend.storage.account;

import com.hyrinth.backend.Main;
import org.zyneonstudios.apex.utilities.sql.SQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlAccountStorage implements AccountStorage {

    private final SQL sql;
    private final String table;
    private boolean schemaEnsured = false;

    public SqlAccountStorage(SQL sql) {
        this(sql, "accounts");
    }

    public SqlAccountStorage(SQL sql, String table) {
        this.sql = sql;
        this.table = table;
        ensureSchema();
    }

    private synchronized void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Account storage could not reconnect to SQL backend for schema init.");
            return;
        }
        String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`id` VARCHAR(36) NOT NULL PRIMARY KEY,"
                + "`email` VARCHAR(255) NOT NULL,"
                + "`username` VARCHAR(64) NOT NULL,"
                + "`profile_picture` VARCHAR(512) NOT NULL DEFAULT '',"
                + "`is_hidden` BOOLEAN NOT NULL DEFAULT FALSE,"
                + "`password_hash` VARCHAR(255) NOT NULL,"
                + "`is_admin` BOOLEAN NOT NULL DEFAULT FALSE,"
                + "`permissions` TEXT NOT NULL,"
                + "`projects` TEXT NOT NULL,"
                + "`teams` TEXT NOT NULL,"
                + "`created_at` BIGINT NOT NULL,"
                + "`updated_at` BIGINT NOT NULL"
                + ");";
        String emailIndex = "CREATE UNIQUE INDEX IF NOT EXISTS `idx_" + table + "_email` ON `" + table + "` (`email`);";
        String usernameIndex = "CREATE UNIQUE INDEX IF NOT EXISTS `idx_" + table + "_username` ON `" + table + "` (`username`);";
        try (Connection connection = sql.getConnection()) {
            if (connection == null) {
                Main.getLogger().err("Account storage schema init failed: SQL connection is null.");
                return;
            }
            try (PreparedStatement createStatement = connection.prepareStatement(createTable)) {
                createStatement.execute();
            }
            try (PreparedStatement emailStatement = connection.prepareStatement(emailIndex)) {
                emailStatement.execute();
            } catch (Exception e) {
                Main.getLogger().err("Account storage email index init failed: " + e.getMessage());
            }
            try (PreparedStatement usernameStatement = connection.prepareStatement(usernameIndex)) {
                usernameStatement.execute();
            } catch (Exception e) {
                Main.getLogger().err("Account storage username index init failed: " + e.getMessage());
            }
            String profilePictureColumn = "ALTER TABLE `" + table + "` ADD COLUMN `profile_picture` VARCHAR(512) NOT NULL DEFAULT ''";
            try (PreparedStatement profileStatement = connection.prepareStatement(profilePictureColumn)) {
                profileStatement.execute();
            } catch (Exception e) {
                Main.getLogger().deb("Account storage profile picture column init skipped: " + e.getMessage());
            }
            String hiddenColumn = "ALTER TABLE `" + table + "` ADD COLUMN `is_hidden` BOOLEAN NOT NULL DEFAULT FALSE";
            try (PreparedStatement hiddenStatement = connection.prepareStatement(hiddenColumn)) {
                hiddenStatement.execute();
            } catch (Exception e) {
                Main.getLogger().deb("Account storage hidden column init skipped: " + e.getMessage());
            }
            String adminColumn = "ALTER TABLE `" + table + "` ADD COLUMN `is_admin` BOOLEAN NOT NULL DEFAULT FALSE";
            try (PreparedStatement adminStatement = connection.prepareStatement(adminColumn)) {
                adminStatement.execute();
                String backfill = "UPDATE `" + table + "` SET `is_admin` = TRUE WHERE `permissions` LIKE '%\"admin\"%'";
                try (PreparedStatement backfillStatement = connection.prepareStatement(backfill)) {
                    backfillStatement.executeUpdate();
                } catch (Exception e) {
                    Main.getLogger().err("Account storage admin backfill failed: " + e.getMessage());
                }
            } catch (Exception e) {
                Main.getLogger().deb("Account storage admin column init skipped: " + e.getMessage());
            }
            String projectsColumn = "ALTER TABLE `" + table + "` ADD COLUMN `projects` TEXT NOT NULL DEFAULT '[]'";
            try (PreparedStatement projectsStatement = connection.prepareStatement(projectsColumn)) {
                projectsStatement.execute();
            } catch (Exception e) {
                Main.getLogger().deb("Account storage projects column init skipped: " + e.getMessage());
            }
            String teamsColumn = "ALTER TABLE `" + table + "` ADD COLUMN `teams` TEXT NOT NULL DEFAULT '[]'";
            try (PreparedStatement teamsStatement = connection.prepareStatement(teamsColumn)) {
                teamsStatement.execute();
            } catch (Exception e) {
                Main.getLogger().deb("Account storage teams column init skipped: " + e.getMessage());
            }
            String teamsBackfill = "UPDATE `" + table + "` SET `teams` = `organizations` WHERE `teams` = '[]'";
            try (PreparedStatement teamsBackfillStatement = connection.prepareStatement(teamsBackfill)) {
                teamsBackfillStatement.executeUpdate();
            } catch (Exception e) {
                Main.getLogger().deb("Account storage teams backfill skipped: " + e.getMessage());
            }
            schemaEnsured = true;
        } catch (Exception e) {
            Main.getLogger().err("Account storage schema init failed: " + e.getMessage());
        }
    }

    @Override
    public Optional<AccountRecord> findById(String id) {
        ensureSchema();
        String query = "SELECT * FROM `" + table + "` WHERE `id` = ?";
        return fetchOne(query, id);
    }

    @Override
    public Optional<AccountRecord> findByEmail(String email) {
        ensureSchema();
        String query = "SELECT * FROM `" + table + "` WHERE `email` = ?";
        return fetchOne(query, email);
    }

    @Override
    public Optional<AccountRecord> findByUsername(String username) {
        ensureSchema();
        String query = "SELECT * FROM `" + table + "` WHERE `username` = ?";
        return fetchOne(query, username);
    }

    @Override
    public List<AccountRecord> findPage(int limit, int offset) {
        ensureSchema();
        if (limit <= 0 || offset < 0) {
            return List.of();
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Account storage could not reconnect to SQL backend.");
            return List.of();
        }
        String query = "SELECT * FROM `" + table + "` ORDER BY `created_at` ASC, `id` ASC LIMIT ? OFFSET ?";
        List<AccountRecord> records = new ArrayList<>();
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
            Main.getLogger().err("Account storage page query failed: " + e.getMessage());
        }
        return records;
    }

    @Override
    public boolean hasAdminAccount() {
        ensureSchema();
        String query = "SELECT 1 FROM `" + table + "` WHERE `is_admin` = ? LIMIT 1";
        return exists(query, true);
    }

    @Override
    public boolean create(AccountRecord record) {
        ensureSchema();
        String query = "INSERT INTO `" + table + "` (`id`, `email`, `username`, `profile_picture`, `is_hidden`, `password_hash`, `is_admin`, `permissions`, `projects`, `teams`, `created_at`, `updated_at`)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return executeUpdate(query, statement -> {
            statement.setString(1, record.id());
            statement.setString(2, record.email());
            statement.setString(3, record.username());
            statement.setString(4, record.profilePicture());
            statement.setBoolean(5, record.isHidden());
            statement.setString(6, record.passwordHash());
            statement.setBoolean(7, record.isAdmin());
            statement.setString(8, serializePermissions(record.permissions()));
            statement.setString(9, serializeProjects(record.projects()));
            statement.setString(10, serializeTeams(record.teams()));
            statement.setLong(11, record.createdAt());
            statement.setLong(12, record.updatedAt());
        });
    }

    @Override
    public boolean updatePasswordHash(String id, String passwordHash, long updatedAt) {
        ensureSchema();
        String query = "UPDATE `" + table + "` SET `password_hash` = ?, `updated_at` = ? WHERE `id` = ?";
        return executeUpdate(query, statement -> {
            statement.setString(1, passwordHash);
            statement.setLong(2, updatedAt);
            statement.setString(3, id);
        });
    }

    @Override
    public boolean delete(String id) {
        ensureSchema();
        String query = "DELETE FROM `" + table + "` WHERE `id` = ?";
        return executeUpdate(query, statement -> statement.setString(1, id));
    }

    private Optional<AccountRecord> fetchOne(String query, String value) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Account storage could not reconnect to SQL backend.");
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
            Main.getLogger().err("Account storage query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    private boolean exists(String query, String value) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Account storage could not reconnect to SQL backend.");
            return false;
        }
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            Main.getLogger().err("Account storage exists query failed: " + e.getMessage());
        }
        return false;
    }

    private boolean exists(String query, boolean value) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Account storage could not reconnect to SQL backend.");
            return false;
        }
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setBoolean(1, value);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            Main.getLogger().err("Account storage exists query failed: " + e.getMessage());
        }
        return false;
    }

    private boolean executeUpdate(String query, StatementConfigurer configurer) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Account storage could not reconnect to SQL backend.");
            return false;
        }
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            configurer.configure(statement);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            Main.getLogger().err("Account storage update failed: " + e.getMessage());
            return false;
        }
    }

    private AccountRecord fromResultSet(ResultSet resultSet) throws Exception {
        return new AccountRecord(
                resultSet.getString("id"),
                resultSet.getString("email"),
                resultSet.getString("username"),
                resultSet.getString("profile_picture"),
                resultSet.getBoolean("is_hidden"),
                resultSet.getString("password_hash"),
                resultSet.getBoolean("is_admin"),
                deserializePermissions(resultSet.getString("permissions")),
                deserializeProjects(resultSet.getString("projects")),
                deserializeTeams(resultSet.getString("teams")),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at")
        );
    }

    private String serializePermissions(java.util.List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        return com.alibaba.fastjson2.JSONArray.toJSONString(permissions);
    }

    private java.util.List<String> deserializePermissions(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.List.of();
        }
        com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(raw);
        java.util.List<String> permissions = new java.util.ArrayList<>();
        if (array != null) {
            for (Object entry : array) {
                if (entry != null) {
                    permissions.add(entry.toString());
                }
            }
        }
        return permissions;
    }

    private String serializeProjects(java.util.List<String> projects) {
        if (projects == null || projects.isEmpty()) {
            return "[]";
        }
        return com.alibaba.fastjson2.JSONArray.toJSONString(projects);
    }

    private java.util.List<String> deserializeProjects(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.List.of();
        }
        com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(raw);
        java.util.List<String> projects = new java.util.ArrayList<>();
        if (array != null) {
            for (Object entry : array) {
                if (entry != null) {
                    projects.add(entry.toString());
                }
            }
        }
        return projects;
    }

    private String serializeTeams(java.util.List<String> teams) {
        if (teams == null || teams.isEmpty()) {
            return "[]";
        }
        return com.alibaba.fastjson2.JSONArray.toJSONString(teams);
    }

    private java.util.List<String> deserializeTeams(String raw) {
        if (raw == null || raw.isBlank()) {
            return java.util.List.of();
        }
        com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(raw);
        java.util.List<String> teams = new java.util.ArrayList<>();
        if (array != null) {
            for (Object entry : array) {
                if (entry != null) {
                    teams.add(entry.toString());
                }
            }
        }
        return teams;
    }

    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws Exception;
    }
}
