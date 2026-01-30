package com.hyrinth.backend.storage;

import com.hyrinth.backend.HyrinthBackend;
import com.hyrinth.backend.HyrinthBackendSettings;
import com.hyrinth.backend.storage.account.AccountStorage;
import com.hyrinth.backend.storage.account.JsonFileAccountStorage;
import com.hyrinth.backend.storage.account.LocalAccountStorage;
import com.hyrinth.backend.storage.account.SqlAccountStorage;
import com.hyrinth.backend.storage.project.JsonFileProjectStorage;
import com.hyrinth.backend.storage.project.LocalProjectStorage;
import com.hyrinth.backend.storage.project.ProjectStorage;
import com.hyrinth.backend.storage.project.SqlProjectStorage;
import com.hyrinth.backend.storage.session.AccountSessionStorage;
import com.hyrinth.backend.storage.session.JsonFileAccountSessionStorage;
import com.hyrinth.backend.storage.session.LocalAccountSessionStorage;
import com.hyrinth.backend.storage.session.SqlAccountSessionStorage;
import com.hyrinth.backend.storage.sql.H2Database;
import com.hyrinth.backend.storage.team.JsonFileTeamStorage;
import com.hyrinth.backend.storage.team.LocalTeamStorage;
import com.hyrinth.backend.storage.team.SqlTeamStorage;
import com.hyrinth.backend.storage.team.TeamStorage;
import org.zyneonstudios.apex.utilities.sql.MySQL;
import org.zyneonstudios.apex.utilities.sql.SQLite;

import java.nio.file.Path;

public class StorageProvider {

    private final HyrinthBackend backend;
    private AccountStorage accountStorage;
    private AccountSessionStorage sessionStorage;
    private ProjectStorage projectStorage;
    private TeamStorage teamStorage;
    private HyrinthBackendSettings.STORAGE_TYPE activeType;

    public StorageProvider(HyrinthBackend backend) {
        this.backend = backend;
        initialize();
    }

    public synchronized AccountStorage getAccountStorage() {
        ensureInitialized();
        return accountStorage;
    }

    public synchronized AccountSessionStorage getSessionStorage() {
        ensureInitialized();
        return sessionStorage;
    }

    public synchronized ProjectStorage getProjectStorage() {
        ensureInitialized();
        return projectStorage;
    }

    public synchronized TeamStorage getTeamStorage() {
        ensureInitialized();
        return teamStorage;
    }

    private void ensureInitialized() {
        HyrinthBackendSettings.STORAGE_TYPE current = backend.getSettings().getStorageType();
        if (accountStorage == null || sessionStorage == null || projectStorage == null || teamStorage == null || current != activeType) {
            initialize();
        }
    }

    private void initialize() {
        HyrinthBackendSettings settings = backend.getSettings();
        activeType = settings.getStorageType();
        switch (activeType) {
            case LOCAL -> {
                accountStorage = new LocalAccountStorage();
                sessionStorage = new LocalAccountSessionStorage();
                projectStorage = new LocalProjectStorage();
                teamStorage = new LocalTeamStorage();
            }
            case JSON -> {
                Path base = resolveDataPath(settings.getDataPath());
                accountStorage = new JsonFileAccountStorage(base.resolve("accounts.json"));
                sessionStorage = new JsonFileAccountSessionStorage(base.resolve("sessions.json"));
                projectStorage = new JsonFileProjectStorage(base.resolve("projects.json"));
                teamStorage = new JsonFileTeamStorage(base.resolve("teams.json"));
            }
            case SQLITE3 -> {
                Path base = resolveDataPath(settings.getDataPath());
                String sqlitePath = base.resolve("storage.db").toString();
                SQLite sqlite = new SQLite(sqlitePath);
                accountStorage = new SqlAccountStorage(sqlite);
                sessionStorage = new SqlAccountSessionStorage(sqlite);
                projectStorage = new SqlProjectStorage(sqlite);
                teamStorage = new SqlTeamStorage(sqlite);
            }
            case H2 -> {
                Path base = resolveDataPath(settings.getDataPath());
                if (settings.isH2Memory()) {
                    String h2Path = "mem:hyrinth";
                    H2Database h2 = new H2Database(h2Path);
                    accountStorage = new SqlAccountStorage(h2);
                    sessionStorage = new SqlAccountSessionStorage(h2);
                    projectStorage = new SqlProjectStorage(h2);
                    teamStorage = new SqlTeamStorage(h2);
                    return;
                } else {
                    String h2Path = base.resolve("h2").toString();
                    Path h2PathResolved = Path.of(h2Path);
                    if (!h2PathResolved.isAbsolute()) {
                        h2Path = Path.of(backend.getRunPath(), h2Path).toString();
                    }
                    H2Database h2 = new H2Database(h2Path);
                    accountStorage = new SqlAccountStorage(h2);
                    sessionStorage = new SqlAccountSessionStorage(h2);
                    projectStorage = new SqlProjectStorage(h2);
                    teamStorage = new SqlTeamStorage(h2);
                    return;
                }
            }
            case MYSQL -> {
                MySQL mysql = new MySQL(
                        settings.getMysqlHost(),
                        settings.getMysqlUser(),
                        settings.getMysqlPassword(),
                        settings.getMysqlDatabase(),
                        settings.getMysqlPort(),
                        settings.isMysqlSsl()
                );
                accountStorage = new SqlAccountStorage(mysql);
                sessionStorage = new SqlAccountSessionStorage(mysql);
                projectStorage = new SqlProjectStorage(mysql);
                teamStorage = new SqlTeamStorage(mysql);
            }
        }
    }

    private Path resolveDataPath(String dataPath) {
        Path path = Path.of(dataPath);
        if (path.isAbsolute()) {
            return path;
        }
        return Path.of(backend.getRunPath(), dataPath);
    }
}
