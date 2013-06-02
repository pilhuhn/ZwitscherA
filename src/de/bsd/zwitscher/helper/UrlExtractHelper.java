package de.bsd.zwitscher.helper;


import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import twitter4j.Status;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to extract image preview urls from input urls
 * that are shortened or point to html pages or such
 *
 * @author Heiko W. Rupp
 */
public class UrlExtractHelper {


    public static String extractOneUrl(String url, int screenWidth, Status status) {

        String finalUrlString;


        if (url.contains("yfrog.com")) {
            // http://twitter.yfrog.com/page/api
            finalUrlString = url + ":iphone";
        } else if (url.contains("youtube.com") || url.contains("youtu.be")) {
            // http://i.ytimg.com/vi/%s/  0.jpg => default 480x360
            //                            default.png => 120x90
            //                            1... jpg => other 120x90 thumbs
            String videoId = null;

            if (url.startsWith("http://youtu.be/")) {
                Pattern youTuBePattern = Pattern.compile(".*youtu.be/([\\w\\-]+).*",Pattern.CASE_INSENSITIVE);
                videoId = url.substring(16);
                Matcher m = youTuBePattern.matcher(url);
                if (m.matches()) {
                    videoId = m.group(1);
                }
            } else {
                Pattern youTubePattern = Pattern.compile(".*v=([\\w\\-]+)&?.*",Pattern.CASE_INSENSITIVE);
                Matcher m = youTubePattern.matcher(url);
                if (m.matches()) {
                    videoId = m.group(1);
                }
            }
            if (videoId==null) {
                videoId="NOT_FOUND";
                Log.i("Youtube","No preview match for " + url);
            }
            finalUrlString = "http://i.ytimg.com/vi/" + videoId + "/0.jpg";
        }
        else if (url.contains("twitpic.com")) {
            String tmp = url;
            tmp = tmp.substring(tmp.lastIndexOf("/")+1);
            finalUrlString = "http://twitpic.com/show/thumb/" + tmp;
        }
        else if (url.contains("plixi.com") || url.contains("lockerz.com")) {
            // http://support.lockerz.com/entries/350297-image-from-url
            String tmp;
            /*
            thumbnail sizes of this api are
            big - original
            medium - 600px scaled
            mobile - 320px scaled
            small - 150px cropped
            thumbnail - 79px cropped
            */
            if (screenWidth>600)
                tmp = "medium"; // big enough on a SGN in portrait mode
            else if (screenWidth>320)
                tmp = "mobile";
            else if (screenWidth>150)
                tmp = "small";
            else
                tmp = "thumbnail";
            finalUrlString = "http://api.plixi.com/api/tpapi.svc/imagefromurl?url=" +  url + "&size=" + tmp;
        }
        else if (url.contains("twimg")) { // This is the normal twitter picture entity
            finalUrlString = url;
        }
        else if (url.contains("i.imgur.com")) {
            finalUrlString = url.substring(0,url.lastIndexOf('.'));
            finalUrlString += (screenWidth>320)? "l" : "s";
            finalUrlString += url.substring(url.lastIndexOf('.')); // 's'mall or 'l'arge
        }
        else if (url.contains("://instagr.am/p/") || url.contains("://instagram.com/p/")) {

            finalUrlString = url;
            if (!url.endsWith("/"))
                finalUrlString +="/";
            finalUrlString += "media";  //   /?size= { t, m ,l } default is m
        }
        else if (url.contains("://picplz.com")) {
            int size = screenWidth-20;
            finalUrlString = url + "/thumb/" + size; // last parameter gives max size of longest side--
        }
        else if (url.contains("://img.ly")) {
            String tmp =  url.substring(url.lastIndexOf("/")+1);
            finalUrlString = "http://img.ly/show/medium/" + tmp; // mini/thumb/medium/large/full
        }
        else if (url.contains("://campl.us")) {
            String tmp;
            if (screenWidth > 480)
                tmp = ":480px";
            else
                tmp = ":120px";

            finalUrlString = url + tmp; // 120px , 480px or 800px
        }
        else if (url.contains("://vine.co/v")) {
            finalUrlString = getVinePreview(url);
        }
        else if (url.contains("://vimeo.com/")) {
            finalUrlString = getVimeoPreview(url);
        }
        else if (url.contains("://photos.app.net")) {
            finalUrlString = getAppNetPreviewUrl(url);
        }
        else if (urlPointsToImage(url)) {
            finalUrlString = url;
        }
        else {
            String screenName;
            long statusId;
            if (!status.isRetweet()) {
                screenName = status.getUser().getScreenName();
                statusId = status.getId();
            }
            else {
                screenName = status.getRetweetedStatus().getUser().getScreenName();
                statusId = status.getRetweetedStatus().getId();
            }
            String twitterPic = "http://twitter.com/" + screenName + "/status/" + statusId +
                    "/photo";
            if (url.startsWith(twitterPic)) {
                // TODO forward to
                // "http://twitter.com/#!" + status.getUser().getScreenName() + "/status/" + status.getId()
                // and then grab the image url from there
//                    finalUrlString = UrlHelper.grabPictureUrlFromTwitter(url);
                finalUrlString = url; //TODO
            }
            else {
                // We had no luck parsing, so return null
                finalUrlString = null;

            }
        }
        return finalUrlString;

    }

    /**
     * Determine if the passed argument ends with a image file type extension like
     * .gif, .jpg, .jpeg or .png
     * @param in String to test
     * @return True if a matching ending is found, false otherwise
     */
    private static boolean urlPointsToImage(String in) {
        String tmp = in.toLowerCase();
        if (tmp.endsWith(".jpg") || tmp.endsWith(".png") || tmp.endsWith(".jpeg") || tmp.endsWith(".gif")) {
            return true;
        }
        return false;
    }

    /**
     * Get a thumbnail for photos.app.net
     * See http://stackoverflow.com/questions/16384266/how-to-retrieve-photo-previews-in-app-net
     * @param url Input url containing photos.appn.et
     * @return Expanded url of a thumbnail
     */
    private static String getAppNetPreviewUrl(String url) {

        Pattern photosPattern = Pattern.compile(".*photos.app.net/([0-9]+)/.*");
        Matcher m = photosPattern.matcher(url);
        if (!m.matches()) {
            return null;
        }
        String id = m.group(1);

        String streamUrl = "https://alpha-api.app.net/stream/0/posts/" + id + "?include_annotations=1";

        // Now that we have the posting url, we can get it and parse for the thumbnail
        BufferedReader br = null;
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(streamUrl).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.setRequestProperty("Accept","application/json");
            urlConnection.connect();

            StringBuilder builder = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line=br.readLine())!=null) {
                builder.append(line);
            }
            urlConnection.disconnect();

            JSONObject post = new JSONObject(builder.toString());
            JSONObject data = post.getJSONObject("data");
            JSONArray annotations = data.getJSONArray("annotations");
            JSONObject annotationValue = annotations.getJSONObject(0);
            JSONObject value = annotationValue.getJSONObject("value");
            String finalUrl;
            if (value.has("thumbnail_large_url")) {
                finalUrl = value.getString("thumbnail_large_url");
            } else if (value.has("thumbnail_url")) {
                finalUrl = value.getString("thumbnail_url");
            } else {
                Log.i("app.net","no thumbnails found for " + builder.toString());
                finalUrl = null;
            }

            return finalUrl;


        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (JSONException e) {
            e.printStackTrace();
        }

        finally {
            if (urlConnection!=null)
                urlConnection.disconnect();
            if ( br!=null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }

        }
        return null;

    }

    /**
     * Get a vine.co thumbnail of the video by parsing the web site content
     * and looking for a og:image meta tag
     * @param url Url of the vine video page
     * @return the url of the thumbnail or null if it can't be found.
     */
    private static String getVinePreview(String url) {
        BufferedReader br = null;
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.connect();

            br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line=br.readLine())!=null) {
                if (line.contains("og:image")) {
                    Pattern vineCoPattern = Pattern.compile(".*<.* content=\"(.*)\">");
                    Matcher m = vineCoPattern.matcher(line);
                    if (m.matches()) {
                        return m.group(1);
                    }
                }
            }

            urlConnection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        finally {
            if (urlConnection!=null)
                urlConnection.disconnect();
            if ( br!=null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }

        }
        return null;
    }

    private static String getVimeoPreview(String url) {


        String infoUrl = url.replace("http://vimeo.com/","http://vimeo.com/api/v2/video/");
        infoUrl += ".xml";

        BufferedReader br = null;
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(infoUrl).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.connect();

            br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;
            while ((line=br.readLine())!=null) {
                if (line.contains("thumbnail_large")) {
                    Pattern vimeoPattern = Pattern.compile(".*<thumbnail_large>(.*)</thumbnail_large>.*");
                    Matcher m = vimeoPattern.matcher(line);
                    if (m.matches()) {
                        return m.group(1);
                    }
                }
            }

            urlConnection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        finally {
            if (urlConnection!=null)
                urlConnection.disconnect();
            if ( br!=null)
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                }

        }
        return null;
    }
}
