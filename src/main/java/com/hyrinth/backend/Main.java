package com.hyrinth.backend;

import org.zyneonstudios.apex.utilities.logger.ApexLogger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public class Main {

    private static HyrinthBackend hyrinthBackend = null;
    private static String[] args = new String[]{};
    private static ApexLogger logger = new ApexLogger("HYB");

    static void main(String[] args) {
        Main.args = args;
        getLogger().log("Starting HyrinthBackend with arguments: "+ Arrays.toString(Main.args).replace("[","").replace("]","").replace(", "," "));
        getHyrinthBackend().start();
    }

    public static HyrinthBackend getHyrinthBackend() {
        if(hyrinthBackend == null) {
            hyrinthBackend = new HyrinthBackend(Main.args);
        }
        return hyrinthBackend;
    }

    public static ApexLogger getLogger() {
        return logger;
    }

    private static String hashPassword(String input) {
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
}