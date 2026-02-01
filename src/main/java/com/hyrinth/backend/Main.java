package com.hyrinth.backend;

import org.zyneonstudios.apex.utilities.logger.ApexLogger;

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
}