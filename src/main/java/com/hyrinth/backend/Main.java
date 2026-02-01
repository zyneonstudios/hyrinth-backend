package com.hyrinth.backend;

import org.zyneonstudios.apex.utilities.logger.ApexLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

public class Main {

    private static HyrinthBackend hyrinthBackend = null;
    private static String[] args = new String[]{};
    private static ApexLogger logger = new ApexLogger("HYB");

    static void main(String[] args) {
        Main.args = args;
        getLogger().log("Starting HyrinthBackend with arguments: "+ Arrays.toString(Main.args).replace("[","").replace("]","").replace(", "," "));
        getHyrinthBackend().start();
        System.out.println("---------------(started: running debug code)------------------------------------------------------------");
        String jsonBody = "{\"name\": \"sack\", \"message\": \"fantasticos sack!\"}";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8085/test/yay"))
                .header("Content-Type", "application/json")
                .header("x-api-key", "sedzhdsfhjodsjfklnhldkfjhdfklhj")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status Code: " + response.statusCode());
            System.out.println("Response: " + response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }


        System.out.println("---------------(runned debug code)----------------------------------------------------------------------");
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