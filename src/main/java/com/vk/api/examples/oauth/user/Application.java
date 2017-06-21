package com.vk.api.examples.oauth.user;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MINUTES;

public class Application {

    private static final int initialDelay = 0;
    public static final long WAITING_TIME_AFTER_NO_NETWORK = 60000;

    public static void main(String[] args) {
        Properties properties = loadConfiguration();
        try {
            initServer(properties);
        } catch (InterruptedException e) {
            System.out.println("Interrupted!");
            e.printStackTrace();
            System.exit(15);
        }
    }

    private static void initServer(Properties properties) throws InterruptedException {
        String clientId = properties.getProperty("client.id");
        String clientEmail = properties.getProperty("client.email");
        String clientPass = properties.getProperty("client.pass");

        try {
            Map.Entry<Integer, String>  credentials = CredentialsManager.authenticate(clientId, clientEmail, clientPass, properties);

            VkApiClient vk = new VkApiClient(new HttpTransportClient());
            UserActor actor = new UserActor(credentials.getKey(), credentials.getValue());

            Drifter drifter = new Drifter(actor, vk, clientId, clientEmail, clientPass, properties);
            Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(drifter, initialDelay, 30, MINUTES);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println(String.format("Seems no network: %s. Retrying.", e.toString()));
            Thread.sleep(WAITING_TIME_AFTER_NO_NETWORK);
            initServer(properties);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Unexpected error! " + e.toString() + " Exiting.");
            System.exit(1);
        }
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream is = Application.class.getResourceAsStream("/config.properties")) {
            properties.load(is);
        } catch (IOException e) { throw new IllegalStateException(e); }
        return properties;
    }
}
