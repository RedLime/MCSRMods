package com.redlimerl.mcsr.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpRequestHelper {

    @SafeVarargs
    private static HttpURLConnection getConnection(String u, Map.Entry<String, String>... entries) throws IOException {
        URL url = new URL(u);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        // optional default is GET
        connection.setRequestMethod("GET");
        //add request header
        connection.setRequestProperty("User-Agent", "ModCheck/v4");
        for (Map.Entry<String, String> entry : entries) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        return connection;
    }

    @SafeVarargs
    public static JsonElement getJsonFromUrl(String u, Map.Entry<String, String>... entries) throws IOException {
        InputStreamReader input = new InputStreamReader(getConnection(u, entries).getInputStream());
        JsonElement result = JsonParser.parseReader(input);
        input.close();
        return result;
    }

    @SafeVarargs
    public static String getStringFromUrl(String u, Map.Entry<String, String>... entries) throws IOException {
        InputStream inputStream = getInputStreamFromUrl(u, entries);
        String s = new String(inputStream.readAllBytes());
        inputStream.close();
        return s;
    }

    @SafeVarargs
    public static ByteArrayInputStream getInputStreamFromUrl(String u, Map.Entry<String, String>... entries) throws IOException {
        HttpURLConnection connection = getConnection(u, entries);
        if (connection.getResponseCode() >= 300) throw new RuntimeException("request error: " + connection.getResponseCode() + " | " + connection.getResponseMessage());

        InputStream inputStream = connection.getInputStream();
        ByteArrayInputStream result = new ByteArrayInputStream(inputStream.readAllBytes());
        inputStream.close();
        return result;
    }
}
