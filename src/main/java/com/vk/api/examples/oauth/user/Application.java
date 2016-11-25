package com.vk.api.examples.oauth.user;

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

/**
 * Created by Anton Tsivarev on 15.10.16.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        Properties properties = loadConfiguration();
        initServer(properties);
    }

    private static void initServer(Properties properties) throws Exception {
        String client_id = properties.getProperty("client.id");
        String scope = "status";

        String url = 
            "http://oauth.vk.com/oauth/authorize?" +
            "redirect_uri=http://oauth.vk.com/blank.html&response_type=token&" +
            "client_id=" + client_id + "&scope=" + scope + "&display=wap";

        Response docResp = Jsoup.connect(url).execute();
        Document doc = docResp.parse();

        String postUrl = doc.getElementsByTag("form").first().attr("action").toString();
        /*
        for (Map.Entry<String, String> entry: docResp.cookies().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        */
        Map<String, String> params = new HashMap<String, String>();
        //System.out.println("attributes");
        for (Element input: doc.getElementsByTag("input")) {
            if (input.hasAttr("name")
                    && input.hasAttr("type")
                    && (input.attr("type").equals("hidden")
                    || input.attr("type").equals("text")
                    || input.attr("type").equals("password"))) {
                String value = input.attr("value").length() > 0?input.attr("value"):"";
                params.put(input.attr("name"), value);
                //System.out.println(input.attr("name") + ": " + value);
            }
        }
        /*
        System.out.println("/attributes");
        System.out.println(doc.toString());
        String predUrl = "https://login.vk.com/?act=login&soft=1&utf8=1";
        String predUrl = "https://login.vk.com/?act=login&amp;soft=1&amp;utf8=1"
        */

        Response resp = Jsoup.connect(postUrl)
            .data(params)
            .data("email", properties.getProperty("client.email"))
            .data("pass", properties.getProperty("client.pass"))
            .cookies(docResp.cookies())
            .followRedirects(true)
            .method(Method.POST)
            .execute();
        /*
        for (Map.Entry<String, String> entry: resp.cookies().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        */

        String token = "";
        Integer userId = 0;
        if (Pattern.compile(".*/blank.html").matcher(resp.url().getPath().toString()).matches()) {
            //System.out.println(resp.url());
            //System.out.println(resp.parse().toString());
            try {
                token = splitQuery(resp.url()).get("access_token");
                userId = Integer.parseInt(splitQuery(resp.url()).get("user_id"));
            } catch (UnsupportedEncodingException e) {
                System.out.println("Oops, fail!");
                System.exit(0);
            } catch (MalformedURLException e) {
                System.out.println("VK API is a piece of SHIT!");
                System.exit(0);
            }
        } else {
            Document respDoc = resp.parse();
            String authUrl = respDoc.getElementsByTag("form").first().attr("action").toString();
            Response authResp = Jsoup.connect(authUrl)
                .data(params)
                .data("email", properties.getProperty("client.email"))
                .data("pass", properties.getProperty("client.pass"))
                .cookies(resp.cookies())
                .followRedirects(true)
                .method(Method.POST)
                .execute();
            System.out.println(authUrl);
            System.out.println(authResp.url().toString());
            System.out.println(authResp.parse().toString());
            if (Pattern.compile(".*/blank.html").matcher(authResp.url().getPath().toString()).matches()) {
                try {
                    token = splitQuery(authResp.url()).get("access_token");
                    userId = Integer.parseInt(splitQuery(authResp.url()).get("user_id"));
                } catch (UnsupportedEncodingException e) {
                    System.out.println("Oops, fail!");
                    System.exit(0);
                } catch (MalformedURLException e) {
                    System.out.println("VK API is a piece of SHIT!");
                    System.exit(0);
                }
            } else {
                System.out.println("Unexpected error!");
                System.exit(0);
            }
        }
        // execute api method
        VkApiClient vk = new VkApiClient(new HttpTransportClient());
        UserActor actor = new UserActor(userId, token); 
        vk.status().set(actor)
            .text("Java says: server's " +
                execute("uptime -p | awk '{ print $1 $2 $3 $4 substr($5, 0, length($5)-1); }'"))
            .execute();
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException, MalformedURLException {
        Map<String, String> query_pairs = new HashMap<String, String>();
        URL queryurl = new URL(url.toString().replaceAll("#", "?"));
        String query = queryurl.getQuery();
        System.out.println(queryurl.toString());
        System.out.println(query);
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
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
        } catch (IOException e) {
            //LOG.error("Can't load properties file", e);
            throw new IllegalStateException(e);
        }

        return properties;
    }

}
