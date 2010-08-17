package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserList;
import twitter4j.conf.Configuration;
import twitter4j.conf.PropertyConfiguration;

public class TwitterHelper {

	Configuration tconf;
	
	// TODO pull the next from preferences too
    String serverUrl = "http://api.twitter.com/"; // trailing slash is important!
    String searchBaseUrl = "http://search.twitter.com/";

    

	public TwitterHelper() {
        Properties props = new Properties();
        props.put(PropertyConfiguration.SOURCE,"Zwitscher");
        props.put(PropertyConfiguration.HTTP_USER_AGENT,"Zwitscher");
        props.put(PropertyConfiguration.SEARCH_BASE_URL,searchBaseUrl);
        props.put(PropertyConfiguration.REST_BASE_URL,serverUrl);
        tconf = new PropertyConfiguration(props);
	}
	
	public void tweet(Context context, String text) {
        Twitter twitter = getTwitter(context);
        
        try {
			twitter.updateStatus(text);
			Toast.makeText(context, R.string.tweet_sent , 2500).show();
		} catch (TwitterException e) {
			Toast.makeText(context, "Failed to send tweet: " + e.getLocalizedMessage(), 10000).show();
		}
	}
	
	public List<Status> getFriendsTimeline(Context context, Paging paging) {
        Twitter twitter = getTwitter(context);

        List<Status> statuses;
		try {
			statuses = twitter.getFriendsTimeline(paging );
			return statuses;
		}
		catch (Exception e) {
        	System.err.println("Got exception: " + e.getMessage() );
        	if (e.getCause()!=null)
        		System.err.println("   " + e.getCause().getMessage());
            String text =  context.getString(R.string.no_connect) + ": " + e.getMessage();
            Toast.makeText(context, text, 15000).show();
            return new ArrayList<Status>();
		}
	}
	
	public List<String> getListNames(Context context) {
		Twitter twitter = getTwitter(context);
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String username = preferences.getString("username", "");
		
		try {
			List<UserList> userLists = twitter.getUserLists(username, -1);
			List<String> lists = new ArrayList<String>(userLists.size());
			for (UserList uList : userLists) {
				lists.add(uList.getName());
			}
			return lists;
		} catch (TwitterException e) {
			Toast.makeText(context, "Getting lists failed: " + e.getMessage(), 15000).show();
			return new ArrayList<String>();
		} // TODO cursor?
	}


	private Twitter getTwitter(Context context) {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // TODO convert to oauth
        String username = preferences.getString("username", "");
        String password = preferences.getString("password", "");

        TwitterFactory tFactory = new TwitterFactory(tconf);
        Twitter twitter = tFactory.getInstance(username,password);
		return twitter;
	}
	
}
