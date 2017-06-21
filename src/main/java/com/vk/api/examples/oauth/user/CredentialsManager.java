package com.vk.api.examples.oauth.user;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class CredentialsManager {
    public static Map.Entry<Integer, String> authenticate(
            String clientId, String clientEmail,
            String clientPass, Properties properties) throws IOException {
        String scope = "status";
        String url = 
            "http://oauth.vk.com/oauth/authorize?" +
            "redirect_uri=http://oauth.vk.com/blank.html&response_type=token&" +
            "client_id=" + clientId + "&scope=" + scope + "&display=wap";

        Response docResp = Jsoup.connect(url).execute();
        Document doc = docResp.parse();

        String postUrl = doc.getElementsByTag("form").first().attr("action");

        Map<String, String> params = new HashMap<>();
        for (Element input: doc.getElementsByTag("input")) {
            if (input.hasAttr("name")
                    && input.hasAttr("type")
                    && (input.attr("type").equals("hidden")
                    || input.attr("type").equals("text")
                    || input.attr("type").equals("password"))) {
                String value = input.attr("value").length() > 0?input.attr("value"):"";
                params.put(input.attr("name"), value);
            }
        }

        Response resp = Jsoup.connect(postUrl)
            .data(params)
            .data("email", clientEmail)
            .data("pass", clientPass)
            .cookies(docResp.cookies())
            .followRedirects(true)
            .method(Method.POST)
            .execute();

        String token = "";
        Integer userId = 0;
        if (Pattern.compile(".*/blank.html").matcher(resp.url().getPath()).matches()) {
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
            String authUrl = respDoc.getElementsByTag("form").first().attr("action");
            Response authResp = Jsoup.connect(authUrl)
                .data(params)
                .data("email", properties.getProperty("client.email"))
                .data("pass", properties.getProperty("client.pass"))
                .cookies(resp.cookies())
                .followRedirects(true)
                .method(Method.POST)
                .execute();
            if (Pattern.compile(".*/blank.html").matcher(authResp.url().getPath()).matches()) {
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
                System.out.println("Unexpected error during authorization. Not authorized.");
                System.exit(0);
            }
        }
        return new AbstractMap.SimpleEntry<>(userId, token);
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException, MalformedURLException {
        Map<String, String> query_pairs = new HashMap<>();
        URL queryurl = new URL(url.toString().replaceAll("#", "?"));
        String query = queryurl.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
}
