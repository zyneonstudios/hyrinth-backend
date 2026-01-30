package com.hyrinth.backend.storage.sql;

import com.hyrinth.backend.Main;
import org.zyneonstudios.apex.utilities.sql.SQL;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class H2Database implements SQL {

    private Connection connection;
    private final String url;
    private final String path;

    public H2Database(String path) {
        this.path = path.replace("\\", "/");
        if (this.path.startsWith("mem:")) {
            url = "jdbc:h2:" + this.path + ";DB_CLOSE_DELAY=-1";
        } else {
            url = "jdbc:h2:file:" + this.path + ";AUTO_SERVER=TRUE";
            File file = new File(this.path);
            File folder = file.getParentFile();
            if (folder != null && !folder.exists()) {
                Main.getLogger().deb("[H2] Created db folder: " + folder.mkdirs());
            }
        }
    }

    @Override
    public boolean connect() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url);
            }
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            Main.getLogger().err("[H2] Can't connect to database: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean reconnect() {
        try {
            connection = DriverManager.getConnection(url);
            return connection != null;
        } catch (Exception e) {
            Main.getLogger().err("[H2] Can't reconnect to database: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean disconnect() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
            return true;
        } catch (Exception e) {
            Main.getLogger().err("[H2] Can't disconnect from database: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean execute(String statement) {
        if (connect()) {
            try (PreparedStatement prepStatement = getConnection().prepareStatement(statement)) {
                prepStatement.execute();
                return true;
            } catch (Exception e) {
                Main.getLogger().err("[H2] Couldn't execute statement: " + e.getMessage());
            }
        }
        return false;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }
}
