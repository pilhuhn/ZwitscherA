package de.bsd.zwitscher.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import de.bsd.zwitscher.TweetDB;

/**
 * Handler that expands the passed urls and stores them in the
 * respective database table
 * @author Heiko W. Rupp
 */
public class ExpandUrlRunner implements Runnable {

    Context context;
    TweetDB tweetDB;
    List<String> urls;

    public ExpandUrlRunner(Context context, List<String> urls) {
        this.urls = urls;
        this.context = context;
        tweetDB = TweetDB.getInstance(context);
    }

    @Override
    public void run() {

        List<UrlPair> pairs = fetchUrls(urls);
        persistUrls(pairs);
    }

    private List<UrlPair> fetchUrls(List<String> srcUrls) {
        List<UrlPair> result = new ArrayList<UrlPair>(srcUrls.size());

        for (String url : srcUrls) {
            String res = UrlHelper.expandUrl(url);
            UrlPair pair = new UrlPair(url,res);
            result.add(pair);
        }
        return result;
    }


    private void persistUrls(Collection<UrlPair> urlPairs) {
        if (urlPairs.isEmpty())
            return;

        List<ContentValues> values = new ArrayList<ContentValues>(urlPairs.size());
        for (UrlPair pair : urlPairs) {
            ContentValues cv = new ContentValues(3);
            cv.put("src",pair.getSrc());
            cv.put("target",pair.getTarget());
            cv.put("last_modified",System.currentTimeMillis());
            values.add(cv);
        }
        tweetDB.storeValues(TweetDB.TABLE_URLS,values);
    }

}
