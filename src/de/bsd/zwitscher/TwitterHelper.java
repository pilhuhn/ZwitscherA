package de.bsd.zwitscher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserList;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

public class TwitterHelper {


    private static final int MIN_TWEETS_TO_SHOW = 40;
	Context context;
    TweetDB tweetDB;

	public TwitterHelper(Context context) {
		this.context = context;
        tweetDB = new TweetDB(context);
	}

	public List<Status> getFriendsTimeline(Paging paging) {
        Twitter twitter = getTwitter();

        List<Status> statuses;
		try {
			statuses = twitter.getHomeTimeline(paging ); //like friends + including retweets
            TweetDB tdb = new TweetDB(context);
            for (Status status : statuses) {
                persistStatus(tdb, status,0);
            }
            int size = statuses.size();
            Log.i("getFriendsTimeline","Got " + size + " statuses from Twitter");
            if (size <MIN_TWEETS_TO_SHOW) {
                if (size==0)
                    statuses.addAll(getStatuesFromDb(-1,MIN_TWEETS_TO_SHOW- size,0));
                else
                    statuses.addAll(getStatuesFromDb(statuses.get(size-1).getId(),MIN_TWEETS_TO_SHOW- size,0));
            }
            Log.i("getFriendsTimeline","Now we have " + statuses.size());

			return statuses;
		}
		catch (Exception e) {
        	System.err.println("Got exception: " + e.getMessage() );
        	if (e.getCause()!=null)
        		System.err.println("   " + e.getCause().getMessage());
            return new ArrayList<Status>();
		}
	}

    private List<Status> getStatuesFromDb(long sinceId, int number, long list_id) {
        List<Long> ids = tweetDB.getStatusesOlderThan(sinceId,number,list_id);
        List<Status> ret = new ArrayList<Status>();
        for (Long id : ids) {
            Status status = getStatusById(id,list_id);
            ret.add(status);
        }
        return ret;
    }


    public List<UserList> getUserLists() {
		Twitter twitter = getTwitter();

		try {
			String username = twitter.getScreenName();
			List<UserList> userLists = twitter.getUserLists(username, -1);
			return userLists;
		} catch (Exception e) {
			Toast.makeText(context, "Getting lists failed: " + e.getMessage(), 15000).show();
			e.printStackTrace();
			return new ArrayList<UserList>();
		} // TODO cursor?
	}


	private Twitter getTwitter() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String accessTokenToken = preferences.getString("accessToken",null);
        String accessTokenSecret = preferences.getString("accessTokenSecret",null);
        if (accessTokenToken!=null && accessTokenSecret!=null) {
        	Twitter twitter = new TwitterFactory().getOAuthAuthorizedInstance(
        			TwitterConsumerToken.consumerKey, 
        			TwitterConsumerToken.consumerSecret,
        			new AccessToken(accessTokenToken, accessTokenSecret));
        	return twitter;
        }

		return null;
	}

	public String getAuthUrl() throws Exception {
		// TODO use fresh token for the first call
        RequestToken requestToken = getRequestToken(true);
        String authUrl = requestToken.getAuthorizationURL();

        return authUrl;
	}

	public RequestToken getRequestToken(boolean useFresh) throws Exception {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

		if (!useFresh) {
			if (preferences.contains("requestToken")) {
				String rt = preferences.getString("requestToken", null);
				String rts = preferences.getString("requestTokenSecret", null);
				RequestToken token = new RequestToken(rt, rts);
				return token;
			}
		}


        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(TwitterConsumerToken.consumerKey, TwitterConsumerToken.consumerSecret);
        RequestToken requestToken = twitter.getOAuthRequestToken();
        Editor editor = preferences.edit();
        editor.putString("requestToken", requestToken.getToken());
        editor.putString("requestTokenSecret", requestToken.getTokenSecret());
        editor.commit();

        return requestToken;
	}

	public void generateAuthToken(String pin) throws Exception{
        Twitter twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(TwitterConsumerToken.consumerKey, TwitterConsumerToken.consumerSecret);
        RequestToken requestToken = getRequestToken(false); // twitter.getOAuthRequestToken();
		AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, pin);


		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = preferences.edit();
		editor.putString("accessToken", accessToken.getToken());
		editor.putString("accessTokenSecret", accessToken.getTokenSecret());
		editor.commit();


	}

	public String updateStatus(StatusUpdate update) {
		Twitter twitter = getTwitter();
		Log.i("TwitterHelper", "Sending update: " + update);
		try {
			twitter.updateStatus(update);
			return "Tweet sent";
		} catch (TwitterException e) {
			return "Failed to send tweet: " + e.getLocalizedMessage();
		}

	}

	public String retweet(long id) {
		Twitter twitter = getTwitter();
		try {
			twitter.retweetStatus(id);
			return "Retweeted successfully";
		} catch (TwitterException e) {
			return "Failed to  retweet: " + e.getLocalizedMessage();
		}

	}

	public void favorite(Status status) {
		Twitter twitter = getTwitter();
		try {
			if (status.isFavorited())
				twitter.destroyFavorite(status.getId());
			else
				twitter.createFavorite(status.getId());
			Toast.makeText(context, R.string.tweet_sent , 2500).show();
		} catch (TwitterException e) {
			Toast.makeText(context, "Failed to (un)create a favorite: " + e.getLocalizedMessage(), 10000).show();
		}

	}

	public List<Status> getUserList(Paging paging, int listId) {
        Twitter twitter = getTwitter();

        List<Status> statuses;
		try {
	        String listOwnerScreenName = twitter.getScreenName();

			statuses = twitter.getUserListStatuses(listOwnerScreenName, listId, paging);
			int size = statuses.size();
            Log.i("getUserList","Got " + size + " statuses from Twitter");
			
            TweetDB tdb = new TweetDB(context);

            for (Status status : statuses) {
                persistStatus(tdb, status,listId);
            }

			if (size<MIN_TWEETS_TO_SHOW) {
                if (size==0)
                    statuses.addAll(getStatuesFromDb(-1,MIN_TWEETS_TO_SHOW- size,listId));
                else
                    statuses.addAll(getStatuesFromDb(statuses.get(size-1).getId(),MIN_TWEETS_TO_SHOW- size,listId));
			}
            Log.i("getUserList","Now we have " + statuses.size());

			return statuses;
		}
		catch (Exception e) {
        	System.err.println("Got exception: " + e.getMessage() );
        	if (e.getCause()!=null)
        		System.err.println("   " + e.getCause().getMessage());
            return new ArrayList<Status>();
		}
	}


    public Status getStatusById(long statusId,long list_id) {
        Status status = null;

        byte[] obj  = tweetDB.getStatusObjectById(statusId);
        if (obj!=null) {
            status = materializeStatus(obj);
            if (status!=null)
                return status;
        }

        Twitter twitter = getTwitter();
        try {
             status = twitter.showStatus(statusId);
            persistStatus(tweetDB,status,list_id);
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return status;
    }


    private void persistStatus(TweetDB tdb, Status status, long list_id) throws IOException {
        if (tdb.getStatusObjectById(status.getId())!=null)
            return; // This is already in DB, so do nothing

        // Serialize and then store in DB
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(status);
        tdb.storeStatus(status.getId(),status.getInReplyToStatusId(),list_id,bos.toByteArray());
    }



    private Status materializeStatus(byte[] obj) {

        Status status = null;
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(obj));
            status = (Status) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }

        return status;

    }
}
