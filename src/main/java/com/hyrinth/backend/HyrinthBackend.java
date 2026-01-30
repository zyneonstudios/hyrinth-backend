package com.hyrinth.backend;

import com.hyrinth.backend.storage.StorageProvider;
import com.hyrinth.backend.storage.account.AccountRecord;
import com.hyrinth.backend.storage.session.AccountSessionService;
import com.hyrinth.backend.webserver.WebApplication;
import org.apache.commons.io.FileUtils;
import org.zyneonstudios.apex.utilities.json.JsonFile;
import org.zyneonstudios.apex.utilities.misc.StringUtility;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class HyrinthBackend {

    private final WebApplication application;
    private final String[] args;
    private boolean started = false;
    private String runPath = "./";
    private final UUID uuid = UUID.randomUUID();
    private final HyrinthBackendSettings settings;
    private final StorageProvider storageProvider;
    private final String version = "0.0.1";
    private boolean debug = false;
    private AccountSessionService accountSessionService;

    public HyrinthBackend(String[] args) {
        Main.getLogger().log("Initializing HyrinthBackend ("+uuid+")...");

        this.args = args;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-d", "--debug" -> {
                    debug = true;
                    Main.getLogger().enableDebugging(true);
                }
                case "-p", "--path" -> setRunPath(args[i+1]);
            }
        }

        settings = new HyrinthBackendSettings(new JsonFile(new File(runPath + "settings.json")));
        if ("./data/".equals(settings.getDataPath())) {
            settings.setDataPath(runPath + "data/");
        }
        storageProvider = new StorageProvider(this);
        accountSessionService = new AccountSessionService(storageProvider.getSessionStorage());
        application = new WebApplication(this);

        Main.getLogger().deb("Created HyrinthBackend ("+uuid+") running path (first-launch): "+new File(runPath).mkdirs());
        try {
            FileUtils.deleteDirectory(new File(runPath + "temp/"));
        } catch (Exception ignore) {}

        System.gc();
        Main.getLogger().log("Initialized HyrinthBackend ("+uuid+")");
        checkForAdminAccount();
    }

    private void checkForAdminAccount() {
        if(getStorageProvider().getAccountStorage().hasAdminAccount()) return;
        String username = StringUtility.generateAlphanumericString(12);
        String plainPassword = StringUtility.generateAlphanumericString(20);
        String passwordHash = hashPassword(plainPassword);
        long now = Instant.now().toEpochMilli();
        AccountRecord newAdmin = new AccountRecord(
                UUID.randomUUID().toString(),
                username+"@hyrinth.admin",
                username,
                "",
                true,
                passwordHash,
                true,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                now,
                now
        );
        getStorageProvider().getAccountStorage().create(newAdmin);
        AccountSessionService.SessionResult tokenResult = accountSessionService.createPermanentToken(newAdmin.id());

        Main.getLogger().err(" ");
        Main.getLogger().err(" ");
        Main.getLogger().err("======================================================================");
        Main.getLogger().err("NO ADMIN ACCOUNT FOUND!");
        Main.getLogger().err("----------------------------------------------------------------------");
        Main.getLogger().err("A new admin account has been created with the following credentials:");
        Main.getLogger().err("ID: " +newAdmin.id());
        Main.getLogger().err("Email: " +newAdmin.email());
        Main.getLogger().err("Username: "+username);
        Main.getLogger().err("Password: "+plainPassword);
        if (tokenResult.success()) {
            Main.getLogger().err("Permanent Token: " + tokenResult.token());
        } else {
            Main.getLogger().err("Permanent Token: <failed> " + tokenResult.message());
        }
        Main.getLogger().err("======================================================================");
        Main.getLogger().err(" ");
        Main.getLogger().err(" ");
    }

    private String hashPassword(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password", e);
        }
    }

    public void start() {
        Main.getLogger().log("Starting HyrinthBackend ("+uuid+")...");
        if(!started) {
            started = true;
            application.start();
        }
        Main.getLogger().log("Started HyrinthBackend ("+uuid+")");
        Main.getLogger().deb("Server running on: http://127.0.0.1:"+application.getPort());
        Main.getLogger().log("Ready to serve!");
    }

    public WebApplication getApplication() {
        return application;
    }

    public String[] getArgs() {
        return args;
    }

    public String getRunPath() {
        return runPath;
    }

    public void setRunPath(String runPath) {
        runPath = runPath.replace("\\", "/");
        if(!runPath.endsWith("/")) runPath = runPath + "/";
        this.runPath = runPath;
        Main.getLogger().deb("Set HyrinthBackend ("+getUuid()+") running path to: " + this.runPath);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getVersion() {
        return version;
    }

    public HyrinthBackendSettings getSettings() {
        return settings;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isStarted() {
        return started;
    }

    public static HyrinthBackend getInstance() {
        return Main.getHyrinthBackend();
    }

    public AccountSessionService getAccountSessionService() {
        return accountSessionService;
    }
}
