package com.hyrinth.backend.storage.project;

import com.hyrinth.backend.Main;
import org.zyneonstudios.apex.utilities.sql.SQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlProjectStorage implements ProjectStorage {

    private final SQL sql;
    private final String table;
    private boolean schemaEnsured = false;

    public SqlProjectStorage(SQL sql) {
        this(sql, "projects");
    }

    public SqlProjectStorage(SQL sql, String table) {
        this.sql = sql;
        this.table = table;
        ensureSchema();
    }

    private synchronized void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Project storage could not reconnect to SQL backend for schema init.");
            return;
        }
        String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`slug` VARCHAR(128) NOT NULL PRIMARY KEY,"
                + "`id` VARCHAR(36) NOT NULL DEFAULT '',"
                + "`title` VARCHAR(255) NOT NULL DEFAULT '',"
                + "`description` TEXT NOT NULL,"
                + "`category_ids` TEXT NOT NULL,"
                + "`additional_tags` TEXT NOT NULL,"
                + "`donation_urls` TEXT NOT NULL,"
                + "`gallery_urls` TEXT NOT NULL,"
                + "`game_versions` TEXT NOT NULL,"
                + "`version_ids` TEXT NOT NULL,"
                + "`body` TEXT NOT NULL,"
                + "`status` VARCHAR(64) NOT NULL DEFAULT '',"
                + "`requested_status` VARCHAR(64) NOT NULL DEFAULT '',"
                + "`issues_url` VARCHAR(512) NOT NULL DEFAULT '',"
                + "`source_url` VARCHAR(512) NOT NULL DEFAULT '',"
                + "`wiki_url` VARCHAR(512) NOT NULL DEFAULT '',"
                + "`discord_url` VARCHAR(512) NOT NULL DEFAULT '',"
                + "`project_type` VARCHAR(64) NOT NULL DEFAULT '',"
                + "`downloads` INT NOT NULL DEFAULT 0,"
                + "`icon_url` VARCHAR(512) NOT NULL DEFAULT '',"
                + "`color_hex` VARCHAR(16) NOT NULL DEFAULT '',"
                + "`owner_id` VARCHAR(36) NOT NULL DEFAULT '',"
                + "`moderator_message` TEXT NOT NULL,"
                + "`created_at` BIGINT NOT NULL,"
                + "`updated_at` BIGINT NOT NULL,"
                + "`approved_at` BIGINT NOT NULL DEFAULT 0,"
                + "`queued_at` BIGINT NOT NULL DEFAULT 0,"
                + "`followers` INT NOT NULL DEFAULT 0,"
                + "`license` VARCHAR(255) NOT NULL DEFAULT ''"
                + ");";
        String ownerIndex = "CREATE INDEX IF NOT EXISTS `idx_" + table + "_owner` ON `" + table + "` (`owner_id`);";
        String statusIndex = "CREATE INDEX IF NOT EXISTS `idx_" + table + "_status` ON `" + table + "` (`status`);";
        try (Connection connection = sql.getConnection()) {
            if (connection == null) {
                Main.getLogger().err("Project storage schema init failed: SQL connection is null.");
                return;
            }
            try (PreparedStatement createStatement = connection.prepareStatement(createTable)) {
                createStatement.execute();
            }
            try (PreparedStatement ownerStatement = connection.prepareStatement(ownerIndex)) {
                ownerStatement.execute();
            } catch (Exception e) {
                Main.getLogger().err("Project storage owner index init failed: " + e.getMessage());
            }
            try (PreparedStatement statusStatement = connection.prepareStatement(statusIndex)) {
                statusStatement.execute();
            } catch (Exception e) {
                Main.getLogger().err("Project storage status index init failed: " + e.getMessage());
            }
            addColumn(connection, "id", "VARCHAR(36) NOT NULL DEFAULT ''");
            addColumn(connection, "title", "VARCHAR(255) NOT NULL DEFAULT ''");
            addColumn(connection, "description", "TEXT NOT NULL");
            addColumn(connection, "category_ids", "TEXT NOT NULL DEFAULT '[]'");
            addColumn(connection, "additional_tags", "TEXT NOT NULL DEFAULT '[]'");
            addColumn(connection, "donation_urls", "TEXT NOT NULL DEFAULT '[]'");
            addColumn(connection, "gallery_urls", "TEXT NOT NULL DEFAULT '[]'");
            addColumn(connection, "game_versions", "TEXT NOT NULL DEFAULT '[]'");
            addColumn(connection, "version_ids", "TEXT NOT NULL DEFAULT '[]'");
            addColumn(connection, "body", "TEXT NOT NULL DEFAULT ''");
            addColumn(connection, "status", "VARCHAR(64) NOT NULL DEFAULT ''");
            addColumn(connection, "requested_status", "VARCHAR(64) NOT NULL DEFAULT ''");
            addColumn(connection, "issues_url", "VARCHAR(512) NOT NULL DEFAULT ''");
            addColumn(connection, "source_url", "VARCHAR(512) NOT NULL DEFAULT ''");
            addColumn(connection, "wiki_url", "VARCHAR(512) NOT NULL DEFAULT ''");
            addColumn(connection, "discord_url", "VARCHAR(512) NOT NULL DEFAULT ''");
            addColumn(connection, "project_type", "VARCHAR(64) NOT NULL DEFAULT ''");
            addColumn(connection, "downloads", "INT NOT NULL DEFAULT 0");
            addColumn(connection, "icon_url", "VARCHAR(512) NOT NULL DEFAULT ''");
            addColumn(connection, "color_hex", "VARCHAR(16) NOT NULL DEFAULT ''");
            addColumn(connection, "owner_id", "VARCHAR(36) NOT NULL DEFAULT ''");
            addColumn(connection, "moderator_message", "TEXT NOT NULL DEFAULT ''");
            addColumn(connection, "approved_at", "BIGINT NOT NULL DEFAULT 0");
            addColumn(connection, "queued_at", "BIGINT NOT NULL DEFAULT 0");
            addColumn(connection, "followers", "INT NOT NULL DEFAULT 0");
            addColumn(connection, "license", "VARCHAR(255) NOT NULL DEFAULT ''");
            schemaEnsured = true;
        } catch (Exception e) {
            Main.getLogger().err("Project storage schema init failed: " + e.getMessage());
        }
    }

    @Override
    public Optional<ProjectRecord> findBySlug(String slug) {
        ensureSchema();
        String query = "SELECT * FROM `" + table + "` WHERE `slug` = ?";
        return fetchOne(query, slug);
    }

    @Override
    public List<ProjectRecord> findPage(int limit, int offset) {
        ensureSchema();
        if (limit <= 0 || offset < 0) {
            return List.of();
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Project storage could not reconnect to SQL backend.");
            return List.of();
        }
        String query = "SELECT * FROM `" + table + "` ORDER BY `created_at` ASC, `slug` ASC LIMIT ? OFFSET ?";
        List<ProjectRecord> records = new ArrayList<>();
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
            Main.getLogger().err("Project storage page query failed: " + e.getMessage());
        }
        return records;
    }

    @Override
    public boolean create(ProjectRecord record) {
        ensureSchema();
        String query = "INSERT INTO `" + table + "` (`slug`, `id`, `title`, `description`, `category_ids`, `additional_tags`, `donation_urls`,"
                + " `gallery_urls`, `game_versions`, `version_ids`, `body`, `status`, `requested_status`, `issues_url`, `source_url`, `wiki_url`,"
                + " `discord_url`, `project_type`, `downloads`, `icon_url`, `color_hex`, `owner_id`, `moderator_message`, `created_at`, `updated_at`,"
                + " `approved_at`, `queued_at`, `followers`, `license`)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return executeUpdate(query, statement -> {
            statement.setString(1, record.slug());
            statement.setString(2, record.id());
            statement.setString(3, record.title());
            statement.setString(4, record.description());
            statement.setString(5, serializeList(record.categoryIds()));
            statement.setString(6, serializeList(record.additionalTags()));
            statement.setString(7, serializeList(record.donationUrls()));
            statement.setString(8, serializeList(record.galleryUrls()));
            statement.setString(9, serializeList(record.gameVersions()));
            statement.setString(10, serializeList(record.versionIds()));
            statement.setString(11, record.body());
            statement.setString(12, record.status());
            statement.setString(13, record.requestedStatus());
            statement.setString(14, record.issuesUrl());
            statement.setString(15, record.sourceUrl());
            statement.setString(16, record.wikiUrl());
            statement.setString(17, record.discordUrl());
            statement.setString(18, record.projectType());
            statement.setInt(19, record.downloads());
            statement.setString(20, record.iconUrl());
            statement.setString(21, record.colorHex());
            statement.setString(22, record.ownerId());
            statement.setString(23, record.moderatorMessage());
            statement.setLong(24, record.createdAt());
            statement.setLong(25, record.updatedAt());
            statement.setLong(26, record.approvedAt());
            statement.setLong(27, record.queuedAt());
            statement.setInt(28, record.followers());
            statement.setString(29, record.license());
        });
    }

    @Override
    public boolean update(ProjectRecord record) {
        ensureSchema();
        String query = "UPDATE `" + table + "` SET `id` = ?, `title` = ?, `description` = ?, `category_ids` = ?, `additional_tags` = ?,"
                + " `donation_urls` = ?, `gallery_urls` = ?, `game_versions` = ?, `version_ids` = ?, `body` = ?, `status` = ?,"
                + " `requested_status` = ?, `issues_url` = ?, `source_url` = ?, `wiki_url` = ?, `discord_url` = ?, `project_type` = ?,"
                + " `downloads` = ?, `icon_url` = ?, `color_hex` = ?, `owner_id` = ?, `moderator_message` = ?, `updated_at` = ?,"
                + " `approved_at` = ?, `queued_at` = ?, `followers` = ?, `license` = ? WHERE `slug` = ?";
        return executeUpdate(query, statement -> {
            statement.setString(1, record.id());
            statement.setString(2, record.title());
            statement.setString(3, record.description());
            statement.setString(4, serializeList(record.categoryIds()));
            statement.setString(5, serializeList(record.additionalTags()));
            statement.setString(6, serializeList(record.donationUrls()));
            statement.setString(7, serializeList(record.galleryUrls()));
            statement.setString(8, serializeList(record.gameVersions()));
            statement.setString(9, serializeList(record.versionIds()));
            statement.setString(10, record.body());
            statement.setString(11, record.status());
            statement.setString(12, record.requestedStatus());
            statement.setString(13, record.issuesUrl());
            statement.setString(14, record.sourceUrl());
            statement.setString(15, record.wikiUrl());
            statement.setString(16, record.discordUrl());
            statement.setString(17, record.projectType());
            statement.setInt(18, record.downloads());
            statement.setString(19, record.iconUrl());
            statement.setString(20, record.colorHex());
            statement.setString(21, record.ownerId());
            statement.setString(22, record.moderatorMessage());
            statement.setLong(23, record.updatedAt());
            statement.setLong(24, record.approvedAt());
            statement.setLong(25, record.queuedAt());
            statement.setInt(26, record.followers());
            statement.setString(27, record.license());
            statement.setString(28, record.slug());
        });
    }

    @Override
    public boolean delete(String slug) {
        ensureSchema();
        String query = "DELETE FROM `" + table + "` WHERE `slug` = ?";
        return executeUpdate(query, statement -> statement.setString(1, slug));
    }

    private Optional<ProjectRecord> fetchOne(String query, String value) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Project storage could not reconnect to SQL backend.");
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
            Main.getLogger().err("Project storage query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    private boolean executeUpdate(String query, StatementConfigurer configurer) {
        if (!sql.reconnect()) {
            Main.getLogger().err("Project storage could not reconnect to SQL backend.");
            return false;
        }
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            configurer.configure(statement);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            Main.getLogger().err("Project storage update failed: " + e.getMessage());
            return false;
        }
    }

    private ProjectRecord fromResultSet(ResultSet resultSet) throws Exception {
        return new ProjectRecord(
                resultSet.getString("id"),
                resultSet.getString("slug"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                deserializeList(resultSet.getString("category_ids")),
                deserializeList(resultSet.getString("additional_tags")),
                deserializeList(resultSet.getString("donation_urls")),
                deserializeList(resultSet.getString("gallery_urls")),
                deserializeList(resultSet.getString("game_versions")),
                deserializeList(resultSet.getString("version_ids")),
                resultSet.getString("body"),
                resultSet.getString("status"),
                resultSet.getString("requested_status"),
                resultSet.getString("issues_url"),
                resultSet.getString("source_url"),
                resultSet.getString("wiki_url"),
                resultSet.getString("discord_url"),
                resultSet.getString("project_type"),
                resultSet.getInt("downloads"),
                resultSet.getString("icon_url"),
                resultSet.getString("color_hex"),
                resultSet.getString("owner_id"),
                resultSet.getString("moderator_message"),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"),
                resultSet.getLong("approved_at"),
                resultSet.getLong("queued_at"),
                resultSet.getInt("followers"),
                resultSet.getString("license")
        );
    }

    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return com.alibaba.fastjson2.JSONArray.toJSONString(list);
    }

    private List<String> deserializeList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        com.alibaba.fastjson2.JSONArray array = com.alibaba.fastjson2.JSONArray.parseArray(raw);
        List<String> values = new ArrayList<>();
        if (array != null) {
            for (Object entry : array) {
                if (entry != null) {
                    values.add(entry.toString());
                }
            }
        }
        return values;
    }

    private void addColumn(Connection connection, String column, String definition) {
        String sqlStatement = "ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition;
        try (PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
            statement.execute();
        } catch (Exception e) {
            Main.getLogger().deb("Project storage column init skipped (" + column + "): " + e.getMessage());
        }
    }

    private interface StatementConfigurer {
        void configure(PreparedStatement statement) throws Exception;
    }
}
