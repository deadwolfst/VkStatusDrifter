package com.vk.api.examples.oauth.user;

import static java.util.concurrent.TimeUnit.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Connection.Method;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.io.InputStream;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Calendar;

public class Application {

    public static void main(String[] args) throws Exception {
        Properties properties = loadConfiguration();
        initServer(properties);
    }

    private static void initServer(Properties properties) throws Exception {
        String clientId = properties.getProperty("client.id");
        String clientEmail = properties.getProperty("client.email");
        String clientPass = properties.getProperty("client.pass");

        Map.Entry<Integer, String> credentials = CredentialsManager.authenticate(clientId, clientEmail, clientPass, properties);

        // Create vkCilent, scheduler and begin DRIFTING
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(credentials.getKey(), credentials.getValue()); 

        Executors.newScheduledThreadPool(1)
            .scheduleAtFixedRate(new Runnable() {
                public void run() {
                    try {
                        vk.status().set(actor)
                            .text("Java says: server's " +
                                execute("uptime -p | awk '{ print $1 $2 $3 $4 substr($5, 0, length($5)-1); }'"))
                            .execute();
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.toString());
                    }
                }
            }, 30 - Calendar.getInstance().get(Calendar.MINUTE) % 30,
            30, MINUTES);
    }

    private static String execute(String command) {
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                output.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    private static Properties loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream is = Application.class.getResourceAsStream("/config.properties")) {
            properties.load(is);
        } catch (IOException e) { throw new IllegalStateException(e); }
        return properties;
    }
}
