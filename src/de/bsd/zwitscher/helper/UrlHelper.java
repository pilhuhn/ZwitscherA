package de.bsd.zwitscher.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class that deals with url shortening etc.
 * @author Heiko W. Rupp
 */
public class UrlHelper {

    private static final String SHORTENER = "http://b1t.it";

    /**
     * Shortens the passed URL and returns the short form.
     * @param inputUrl URL to shorten
     * @return shortened form
     */
    public static String shortenUrl(String inputUrl) {

//     $ curl -d "url=http://bsd.de/zwitscher" http://b1t.it
//     {"id":"r2W","url":"http:\/\/b1t.it\/r2W"}

        try {
            HttpURLConnection conn;
            URL url = new URL (SHORTENER);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.connect();
            OutputStream out = conn.getOutputStream();
            String toWrite = "url=" + inputUrl;
            out.write(toWrite.getBytes());
            out.flush();
            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            JSONObject jsonObject = new JSONObject(builder.toString());
            String newUrl  = (String) jsonObject.get("url");
            System.out.println(newUrl);
            return newUrl;
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (JSONException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return null;
    }

    /**
     * Expands the passed URL from an URL shortener into its final
     * form.
     * @param inputUrl URL to expand
     * @return expanded URL
     */
    public static String expandUrl(String inputUrl) {

        HttpURLConnection conn = null;
        int code = 0;
        try {
            URL url = new URL (inputUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("Accept-Encoding", "identity"); // Disable GZIP compression.
            conn.getHeaderFields();
            code = conn.getResponseCode();
            System.out.println("Response code: " + code + ", result: " + conn.getURL());
        } catch (IOException e) {
            System.err.println("Input URL was " + inputUrl);
            e.printStackTrace();
        } finally {
            if (conn!=null)
                conn.disconnect();
        }
        if (code==200 || code==204)
            return conn.getURL().toString();
        if (code==301) {
            String tmp = conn.getHeaderField("Location");
            System.out.println("Tmp location: " + tmp);
            return expandUrl(tmp);
        }
        return inputUrl;
    }

    public static void main(String[] args) {
        String tmp = expandUrl("http://j.mp/oCNWbt");
        System.out.println(tmp);
        tmp = expandUrl("http://t.co/E2xLzlB");
        System.out.println(tmp);
        tmp = shortenUrl("http://bsd.de/zwitscher");
        System.out.println(tmp);
    }
}
