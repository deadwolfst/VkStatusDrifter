package com.vk.api.examples.oauth.user;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

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

    public void run(){
        int vkStatusMaxCharacters = 140;

        /*
        String statusFilename = "$HOME/status.txt";
        String counterFilename = "$HOME/aphorism-counter.number";
        final String statusCommand = String.format(
                "uptime -p | awk '{ print $1, $2, $3, $4, substr($5, 0, length($5)-1); };'; more %s; echo -n \". Profound nonsense #$(more %s): \"; echo $(($(more %s) + 1)) > %s; /usr/games/fortune -n 50 -s;",
                statusFilename, counterFilename, counterFilename, counterFilename);
        */
        try {
            String[] command = {
                    "bash",
                    "/home/whobscr/drifter/command.sh"
            };
            /*
            String[] command = {
                "/bin/bash",
                "-c",
                statusCommand
            };
            System.out.println(statusCommand);
            */
            String status = execute(command);
            System.out.println(status);
            int status_len = (vkStatusMaxCharacters > status.length() ?
                    status.length() : vkStatusMaxCharacters);
            vk.status().set(actor)
                    .text(status.substring(0, status_len - 1))
                    .execute();
        } catch (Exception e) {
            System.out.println("Exception: " + e.toString());
            try {
                Map.Entry<Integer, String> credentials =
                        CredentialsManager.authenticate(clientId, clientEmail, clientPass, properties);
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
