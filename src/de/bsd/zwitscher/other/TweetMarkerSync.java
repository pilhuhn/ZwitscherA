package de.bsd.zwitscher.other;


import android.util.Log;
import de.bsd.zwitscher.Tokens;
import org.json.JSONException;
import org.json.JSONObject;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.internal.http.HttpParameter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Code to sync last read status ids with TweetMarker (http://tweetmarker.net)
 * There are a few clients out there that already use the service, so this not
 * only allows you to sync between multiple instances of Zwitscher, but also
 * your favorite desktop client (if it supports TM too).
 *
 * @author Heiko W. Rupp
 */
public class TweetMarkerSync {

    private static final String TWITTER_VERIFY_CREDENTIALS_JSON = "https://api.twitter.com/1/account/verify_credentials.json";

    public static void syncToTweetMarker(String collection,long lastRead, String user, OAuthAuthorization oauth) {
        String jsonString;
        try {
            JSONObject idObject = new JSONObject();
            idObject.put("id",lastRead);
            JSONObject baseObject = new JSONObject();
            baseObject.put(collection,idObject);
            jsonString = baseObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return;
        }

        String urlString =  "https://api.tweetmarker.net/v2/lastread?api_key=" + Tokens.tweetMarkerToken +
                "&username=" + user;

        String auth = generateVerifyCredentialsAuthorizationHeader(TWITTER_VERIFY_CREDENTIALS_JSON, oauth);

        URL url;
        HttpURLConnection conn = null;
        try {
            url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(false);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("X-Auth-Service-Provider", TWITTER_VERIFY_CREDENTIALS_JSON); // TODO status.net
            conn.addRequestProperty("X-Verify-Credentials-Authorization",auth);
            OutputStream out = conn.getOutputStream();
            out.write(jsonString.getBytes());
            out.flush();
            out.close();
            int code = conn.getResponseCode();
            if (code!=200) {
                Log.e("Write to tm", "Response was " + code);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (conn!=null)
                conn.disconnect();
        }

    }

    // Copied from Twiter4j's media helper AbstractImageUploadImpl
    static protected String generateVerifyCredentialsAuthorizationHeader(String verifyCredentialsUrl, OAuthAuthorization oauth) {
        List<HttpParameter> oauthSignatureParams = oauth.generateOAuthSignatureHttpParams("GET", verifyCredentialsUrl);
        return "OAuth realm=\"http://api.twitter.com/\"," + OAuthAuthorization.encodeParameters(oauthSignatureParams, ",", true);
    }


    /**
     * Read last-read data from TweetMarker
     * @param collection Name of the collection (i.e. Twitter list)
     * @param user Name of the Tweetmarker user (= the twitter user name)
     * @return The latest synced id value
     */
    public static long syncFromTweetMarker(String collection,String user) {

        String urlString = "https://api.tweetmarker.net/v2/lastread?collection=" + collection +
                "&username=" + user + "&api_key=" + Tokens.tweetMarkerToken;

        URL url;
        HttpURLConnection conn = null;
        BufferedReader in = null;
        try {
            url= new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(false);
            int code = conn.getResponseCode();
            if (code==200) {


                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = in.readLine())!=null) {
                    builder.append(line);
                }
                String result = builder.toString();
                // input is now JSON

                JSONObject jsonObject = new JSONObject(result);
                JSONObject timeline = jsonObject.getJSONObject(collection);
                if (timeline!=null) {
                    long val = timeline.getLong("id");
                    return val;
                }
            }
            else {
                return -1;
            }
        } catch (Exception e) {
            Log.e("syncFromTM","failed", e);
        } finally {
            if (conn!=null)
                conn.disconnect();
            if (in!=null)
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }
        }
        return -1;
    }
}
