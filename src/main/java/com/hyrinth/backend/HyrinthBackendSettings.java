package com.hyrinth.backend;

import org.zyneonstudios.apex.utilities.json.JsonFile;
import org.zyneonstudios.apex.utilities.storage.EditableStorage;

public class HyrinthBackendSettings {

    private final EditableStorage storage;
    private boolean allowAccountCreation = false;
    private STORAGE_TYPE storageType = STORAGE_TYPE.LOCAL;
    private String dataPath = "./data/";
    private boolean h2Memory = false;

    private String mysqlHost = "127.0.0.1";
    private int mysqlPort = 3306;
    private String mysqlUser = "root";
    private String mysqlPassword = "";
    private String mysqlDatabase = "hyrinth";
    private boolean mysqlSsl = false;

    private String name = "hyrinth-backend-api";
    private String docs = "https://docs.hyrinth.com";

    public HyrinthBackendSettings(EditableStorage storage) {
        this.storage = storage;
        if(storage instanceof JsonFile) {
            ((JsonFile)storage).setPrettyPrint(true);
        }
        reload();
    }

    public boolean allowAccountCreation() {
        return allowAccountCreation;
    }

    public void setAllowAccountCreation(boolean allow) {
        allowAccountCreation = allow;
        storage.set("settings.account.allowCreation", allow);
    }

    public EditableStorage getStorage() {
        return storage;
    }

    public void setStorageType(STORAGE_TYPE storageType) {
        this.storageType = storageType;
    }

    public STORAGE_TYPE getStorageType() {
        return storageType;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = normalizePath(dataPath);
        storage.set("settings.storage.dataPath", this.dataPath);
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setH2Memory(boolean h2Memory) {
        this.h2Memory = h2Memory;
        storage.set("settings.storage.H2.memory", h2Memory);
    }

    public boolean isH2Memory() {
        return h2Memory;
    }

    public void setMysqlHost(String mysqlHost) {
        this.mysqlHost = mysqlHost;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public void setMysqlPort(int mysqlPort) {
        this.mysqlPort = mysqlPort;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public void setMysqlUser(String mysqlUser) {
        this.mysqlUser = mysqlUser;
    }

    public String getMysqlUser() {
        return mysqlUser;
    }

    public void setMysqlPassword(String mysqlPassword) {
        this.mysqlPassword = mysqlPassword;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public void setMysqlDatabase(String mysqlDatabase) {
        this.mysqlDatabase = mysqlDatabase;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public void setMysqlSsl(boolean mysqlSsl) {
        this.mysqlSsl = mysqlSsl;
    }

    public boolean isMysqlSsl() {
        return mysqlSsl;
    }

    public void reload() {
        if(storage instanceof JsonFile) {
            ((JsonFile)storage).reload();
        }
        storage.ensure("settings.account.allowCreation",false);
        allowAccountCreation = Boolean.parseBoolean(storage.getString("settings.account.allowCreation"));

        storage.ensure("settings.storage.persistent",false);
        storage.ensure("settings.storage.type",STORAGE_TYPE.SQLITE3);
        if(Boolean.parseBoolean(storage.getString("settings.storage.persistent"))) {
            storageType = STORAGE_TYPE.valueOf(storage.getString("settings.storage.type").toUpperCase().replace("MARIADB","MYSQL").replace("MARIA","MYSQL"));
        } else {
            storageType = STORAGE_TYPE.LOCAL;
        }
        storage.ensure("settings.storage.dataPath","./data/");
        dataPath = normalizePath(storage.getString("settings.storage.dataPath"));
        storage.ensure("settings.storage.H2.memory", false);
        h2Memory = Boolean.parseBoolean(storage.getString("settings.storage.H2.memory"));

        storage.ensure("settings.name","hyrinth-backend-api");
        name = storage.getString("settings.name");
        storage.ensure("settings.docs","https://docs.hyrinth.com");
        docs = storage.getString("settings.docs");

        storage.ensure("settings.storage.MySQL.host","127.0.0.1");
        storage.ensure("settings.storage.MySQL.port",3306);
        storage.ensure("settings.storage.MySQL.user","root");
        storage.ensure("settings.storage.MySQL.password","");
        storage.ensure("settings.storage.MySQL.database","hyrinth");
        storage.ensure("settings.storage.MySQL.ssl", false);
        mysqlHost = getSetting("settings.storage.MySQL.host", "settings.storage.mysql.host");
        mysqlPort = Integer.parseInt(getSetting("settings.storage.MySQL.port", "settings.storage.mysql.port"));
        mysqlUser = getSetting("settings.storage.MySQL.user", "settings.storage.mysql.user");
        mysqlPassword = getSetting("settings.storage.MySQL.password", "settings.storage.mysql.password");
        mysqlDatabase = getSetting("settings.storage.MySQL.database", "settings.storage.mysql.database");
        mysqlSsl = Boolean.parseBoolean(getSetting("settings.storage.MySQL.ssl", "settings.storage.mysql.ssl"));
    }

    public enum STORAGE_TYPE {
        H2,
        SQLITE3,
        MYSQL,
        JSON,
        LOCAL
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "./data/";
        }
        String normalized = path.replace("\\", "/");
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return normalized;
    }

    private String getSetting(String primaryKey, String fallbackKey) {
        String value = storage.getString(primaryKey);
        if (value == null || value.isBlank()) {
            value = storage.getString(fallbackKey);
        }
        return value == null ? "" : value;
    }

    public String getName() {
        return name;
    }

    public String getDocs() {
        return docs;
    }
}
