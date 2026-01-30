package com.hyrinth.backend.storage.session;

import com.hyrinth.backend.Main;
import org.zyneonstudios.apex.utilities.sql.SQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class SqlAccountSessionStorage implements AccountSessionStorage {

    private final SQL sql;
    private final String table;
    private boolean schemaEnsured = false;

    public SqlAccountSessionStorage(SQL sql) {
        this(sql, "account_sessions");
    }

    public SqlAccountSessionStorage(SQL sql, String table) {
        this.sql = sql;
        this.table = table;
        ensureSchema();
    }

    private synchronized void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        if (!sql.reconnect()) {
            Main.getLogger().err("Session storage could not reconnect to SQL backend for schema init.");
            return;
        }
        String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`token` VARCHAR(128) NOT NULL PRIMARY KEY,"
                + "`account_id` VARCHAR(36) NOT NULL,"
                + "`created_at` BIGINT NOT NULL,"
                + "`expires_at` BIGINT NOT NULL,"
                + "`type` VARCHAR(16) NOT NULL DEFAULT 'SESSION',"
                + "`remaining_uses` INT NOT NULL DEFAULT -1"
                + ");";
        String accountIndex = "CREATE INDEX IF NOT EXISTS `idx_" + table + "_account` ON `" + table + "` (`account_id`);";
        try (Connection connection = sql.getConnection()) {
            if (connection == null) {
                Main.getLogger().err("Session storage schema init failed: SQL connection is null.");
                return;
            }
            try (PreparedStatement createStatement = connection.prepareStatement(createTable)) {
                createStatement.execute();
            }
            try (PreparedStatement accountStatement = connection.prepareStatement(accountIndex)) {
                accountStatement.execute();
            } catch (Exception e) {
                Main.getLogger().err("Session storage account index init failed: " + e.getMessage());
            }
            String typeColumn = "ALTER TABLE `" + table + "` ADD COLUMN `type` VARCHAR(16) NOT NULL DEFAULT 'SESSION'";
            try (PreparedStatement typeStatement = connection.prepareStatement(typeColumn)) {
                typeStatement.execute();
            } catch (Exception e) {
                Main.getLogger().deb("Session storage type column init skipped: " + e.getMessage());
            }
            String remainingColumn = "ALTER TABLE `" + table + "` ADD COLUMN `remaining_uses` INT NOT NULL DEFAULT -1";
            try (PreparedStatement remainingStatement = connection.prepareStatement(remainingColumn)) {
                remainingStatement.execute();
            } catch (Exception e) {
                Main.getLogger().deb("Session storage remaining uses column init skipped: " + e.getMessage());
            }
            schemaEnsured = true;
        } catch (Exception e) {
            Main.getLogger().err("Session storage schema init failed: " + e.getMessage());
        }
    }

    @Override
    public Optional<AccountSessionRecord> findByToken(String token) {
        ensureSchema();
        if (!sql.reconnect()) {
            Main.getLogger().err("Session storage could not reconnect to SQL backend.");
            return Optional.empty();
        }
        String query = "SELECT * FROM `" + table + "` WHERE `token` = ?";
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new AccountSessionRecord(
                            resultSet.getString("token"),
                            resultSet.getString("account_id"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("expires_at"),
                            parseTokenType(resultSet.getString("type")),
                            resultSet.getInt("remaining_uses")
                    ));
                }
            }
        } catch (Exception e) {
            Main.getLogger().err("Session storage query failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public boolean create(AccountSessionRecord session) {
        ensureSchema();
        if (!sql.reconnect()) {
            Main.getLogger().err("Session storage could not reconnect to SQL backend.");
            return false;
        }
        String query = "INSERT INTO `" + table + "` (`token`, `account_id`, `created_at`, `expires_at`, `type`, `remaining_uses`) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, session.token());
            statement.setString(2, session.accountId());
            statement.setLong(3, session.createdAt());
            statement.setLong(4, session.expiresAt());
            statement.setString(5, session.type().name());
            statement.setInt(6, session.remainingUses());
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            Main.getLogger().err("Session storage insert failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(AccountSessionRecord session) {
        ensureSchema();
        if (!sql.reconnect()) {
            Main.getLogger().err("Session storage could not reconnect to SQL backend.");
            return false;
        }
        String query = "UPDATE `" + table + "` SET `account_id` = ?, `created_at` = ?, `expires_at` = ?, `type` = ?, `remaining_uses` = ? WHERE `token` = ?";
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, session.accountId());
            statement.setLong(2, session.createdAt());
            statement.setLong(3, session.expiresAt());
            statement.setString(4, session.type().name());
            statement.setInt(5, session.remainingUses());
            statement.setString(6, session.token());
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            Main.getLogger().err("Session storage update failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(String token) {
        ensureSchema();
        if (!sql.reconnect()) {
            Main.getLogger().err("Session storage could not reconnect to SQL backend.");
            return false;
        }
        String query = "DELETE FROM `" + table + "` WHERE `token` = ?";
        try (Connection connection = sql.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, token);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            Main.getLogger().err("Session storage delete failed: " + e.getMessage());
            return false;
        }
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
}
