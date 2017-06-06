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

public class Drifter implements Runnable {

    //private static Map.Entry<Integer, String> credentials;
    private static UserActor actor;
    private static VkApiClient vk;
    private static String clientId;
    private static String clientEmail;
    private static Properties properties;
    private static String clientPass;

    Drifter(UserActor a, VkApiClient v, String id, String email, String pass, Properties props) {
        actor = a;
        vk = v;
        clientId = id;
        clientEmail = email;
        clientPass = pass;
        properties = props;
    }

    public void run(){
        try {
            String[] command = {
                "/bin/bash",
                "-c",
                "uptime -p | awk '{ print $1, $2, $3, $4, substr($5, 0, length($5)-1); };'; cat $HOME/status;" +
                        " echo -n '. '; fortune -n 50 -s;"
            };
            vk.status().set(actor)
                .text("server " + execute(command))
                .execute();
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
            try {
                Map.Entry<Integer, String> credentials = CredentialsManager.authenticate(clientId, clientEmail, clientPass, properties);
                actor = new UserActor(credentials.getKey(), credentials.getValue());
                run();
            } catch (Exception ue) {
                ue.printStackTrace();
                System.out.println("Unexpected error! " + ue.toString() + " Exiting.");
                System.exit(1);
            }
        }
    }

    private static String execute(String[] command) {
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
}
