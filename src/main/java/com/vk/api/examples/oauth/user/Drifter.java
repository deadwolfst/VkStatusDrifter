package com.vk.api.examples.oauth.user;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Properties;

import static com.vk.api.examples.oauth.user.Application.WAITING_TIME_AFTER_NO_NETWORK;

public class Drifter implements Runnable {
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

    public void run() {
        int vkStatusMaxCharacters = 140;
        try {
            String[] command = {
                    "bash",
                    "-c",
                    "cd /home/whobscr/drifter/; ./command.sh"
            };
            String status = execute(command);
            int status_len = (vkStatusMaxCharacters > status.length() ?
                    status.length() : vkStatusMaxCharacters) - 1;
            vk.status().set(actor)
                    .text(status.substring(0, status_len > 0 ? status_len : 1))
                    .execute();
        } catch (ApiException | ClientException e) {
            System.out.println("Exception: " + e.toString());
            try {
                tryAuthenticateAgain();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
                System.exit(15);
            }
        }
    }

    private void tryAuthenticateAgain() throws InterruptedException {
        try {
            Map.Entry<Integer, String> credentials =
                    CredentialsManager.authenticate(clientId, clientEmail, clientPass, properties);
            actor = new UserActor(credentials.getKey(), credentials.getValue());
            run();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.out.println(String.format("Seems no network: %s. Retrying.", e.toString()));
            Thread.sleep(WAITING_TIME_AFTER_NO_NETWORK);
            tryAuthenticateAgain();
        } catch (Exception ue) {
            ue.printStackTrace();
            System.out.println("Unexpected error! " + ue.toString() + " Exiting.");
            System.exit(1);
        }
    }

    private static String execute(String[] command) {
        StringBuilder output = new StringBuilder();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            if (errorReader.readLine() != null) {
                String line;
                while ((line = errorReader.readLine())!= null) {
                    output.append(line).append("\n");
                }
            } else {
                String line;
                while ((line = reader.readLine())!= null) {
                    output.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
}
