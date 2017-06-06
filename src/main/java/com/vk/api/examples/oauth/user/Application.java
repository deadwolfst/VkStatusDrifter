package com.vk.api.examples.oauth.user;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MINUTES;

public class Application {

    private static final int initialDelay;

    static {
        initialDelay = 0;
    }
    
    public static void main(String[] args) {
        Properties properties = loadConfiguration();
        try {
            initServer(properties);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unexpected error! " + e.toString() + " Exiting.");
            System.exit(1);
        }
    }

    private static void initServer(Properties properties) throws Exception {
        String clientId = properties.getProperty("client.id");
        String clientEmail = properties.getProperty("client.email");
        String clientPass = properties.getProperty("client.pass");

        Map.Entry<Integer, String>  credentials = CredentialsManager.authenticate(clientId, clientEmail, clientPass, properties);

        // Create vkCilent, scheduler and begin DRIFTING
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getKey(), credentials.getValue()); 

        Drifter drifter = new Drifter(actor, vk, clientId, clientEmail, clientPass, properties);
        Executors.newScheduledThreadPool(1)
            .scheduleAtFixedRate(drifter, initialDelay,
            30, MINUTES);
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream is = Application.class.getResourceAsStream("/config.properties")) {
            properties.load(is);
        } catch (IOException e) { throw new IllegalStateException(e); }
        return properties;
    }
}
