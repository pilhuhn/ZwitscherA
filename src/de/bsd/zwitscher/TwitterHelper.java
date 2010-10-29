package de.bsd.zwitscher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.UserList;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;


public class TwitterHelper {


	Context context;
    TweetDB tweetDB;

	public TwitterHelper(Context context) {
		this.context = context;
        tweetDB = new TweetDB(context);
	}

	public List<Status> getTimeline(Paging paging, int timeline, boolean fromDbOnly) {
        Twitter twitter = getTwitter();

        List<Status> statuses = null;
        int pseudoListId =0;
		try {
			switch (timeline) {
			case R.string.home_timeline:
                if (!fromDbOnly)
				    statuses = twitter.getHomeTimeline(paging ); //like friends + including retweet
                pseudoListId =0;
				break;
			case R.string.mentions:
                if (!fromDbOnly)
				    statuses = twitter.getMentions(paging);
                pseudoListId = -1;
				break;
			default:
				statuses = new ArrayList<Status>();
			}
            if (statuses==null)
                statuses=new ArrayList<Status>();
            TweetDB tdb = new TweetDB(context);
            for (Status status : statuses) {
                persistStatus(tdb, status,pseudoListId);
            }

        }
        catch (Exception e) {
            System.err.println("Got exception: " + e.getMessage() );
            if (e.getCause()!=null)
                System.err.println("   " + e.getCause().getMessage());
            statuses = new ArrayList<Status>();

        }
        fillUpStatusesFromDB(pseudoListId,statuses);
        Log.i("getTimeline","Now we have " + statuses.size());

        return statuses;
	}

    private List<Status> getStatuesFromDb(long sinceId, int number, long list_id) {
        List<Status> ret = new ArrayList<Status>();
        List<byte[]> oStats = tweetDB.getStatusesObjsOlderThan(sinceId,number,list_id);
        for (byte[] bytes : oStats) {
            Status status = materializeStatus(bytes);
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

	public UpdateResponse updateStatus(UpdateRequest request) {
		Twitter twitter = getTwitter();
        UpdateResponse updateResponse = new UpdateResponse(request.updateType,request.statusUpdate);
		Log.i("TwitterHelper", "Sending update: " + request.statusUpdate);
		try {
			twitter.updateStatus(request.statusUpdate);
            updateResponse.setMessage("Tweet sent");
            updateResponse.setSuccess();
		} catch (TwitterException e) {
            updateResponse.setMessage("Failed to send tweet: " + e.getLocalizedMessage());
            updateResponse.setFailure();
        }
        return updateResponse;
	}

	public UpdateResponse retweet(UpdateRequest request) {
		Twitter twitter = getTwitter();
        UpdateResponse response = new UpdateResponse(request.updateType,request.id);
		try {
			twitter.retweetStatus(request.id);
            response.setSuccess();
			response.setMessage("Retweeted successfully");
		} catch (TwitterException e) {
            response.setFailure();
            response.setMessage("Failed to  retweet: " + e.getLocalizedMessage());
		}
        return response;
	}

	public UpdateResponse favorite(UpdateRequest request) {
        Status status = request.status;
        UpdateResponse updateResponse = new UpdateResponse(request.updateType, status);
		Twitter twitter = getTwitter();
		try {
			if (status.isFavorited()) {
				twitter.destroyFavorite(status.getId());
            }
			else {
				twitter.createFavorite(status.getId());
            }

            // reload tweet and update in DB - twitter4j should have some status.setFav()..
            status = getStatusById(status.getId(),0L, true); // TODO list_id ?
            updateStatus(tweetDB,status,0); // TODO list id ???
			updateResponse.setSuccess();
            updateResponse.setMessage("(Un)favorite set");
		} catch (Exception e) {
            updateResponse.setFailure();
            updateResponse.setMessage("Failed to (un)create a favorite: " + e.getLocalizedMessage());
		}
        updateResponse.setStatus(status);
        return updateResponse;
	}


    public UpdateResponse direct(UpdateRequest request) {
        UpdateResponse updateResponse = new UpdateResponse(request.updateType, (Status) null); // TODO
        Twitter twitter = getTwitter();
        try {
            twitter.sendDirectMessage((int)request.id,request.message);
            updateResponse.setSuccess();
            updateResponse.setMessage("Direct message sent");
        } catch (TwitterException e) {
            updateResponse.setFailure();
            updateResponse.setMessage("Sending of direct tweet failed: " + e.getLocalizedMessage());
        }
        return updateResponse;
    }

	public List<Status> getUserList(Paging paging, int listId, boolean fromDbOnly) {
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
        } catch (Exception e) {
            statuses = new ArrayList<Status>();

            System.err.println("Got exception: " + e.getMessage() );
            if (e.getCause()!=null)
                System.err.println("   " + e.getCause().getMessage());
        }

        fillUpStatusesFromDB(listId, statuses);
        Log.i("getUserList","Now we have " + statuses.size());

        return statuses;
	}

    /**
     * Fill the passed status list with old tweets from the DB. This is wanted in
     * two occasions:<ul>
     * <li>No tweets came from the server, so we want to show something</li>
     * <li>A small number is fetched, we want to show more (to have some timely context)</li>
     * </ul>
     * For a given number of passed statuses, we
     * <ul>
     * <li>Always add minOld tweets from the DB</li>
     * <li>If no incoming tweets, show maxOld</li>
     * </ul>
     * See also preferences.xml
     * @param listId The list for which tweets are fetched
     * @param statuses The list of incoming statuses to fill up
     */
    private void fillUpStatusesFromDB(int listId, List<Status> statuses) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int minOld = Integer.valueOf(preferences.getString("minOldTweets","5"));
        int maxOld = Integer.valueOf(preferences.getString("maxOldTweets","10"));


        int size = statuses.size();
        if (size==0)
            statuses.addAll(getStatuesFromDb(-1,maxOld,listId));
        else
            statuses.addAll(getStatuesFromDb(statuses.get(size-1).getId(),minOld,listId));
    }


    /**
     * Get a single status. If directOnly is false, we first look in the local
     * db if it is already present. Otherwise we directly go to the server.
     * @param statusId
     * @param list_id
     * @param directOnly
     * @return
     */
    public Status getStatusById(long statusId, Long list_id, boolean directOnly) {
        Status status = null;

        if (!directOnly) {
            byte[] obj  = tweetDB.getStatusObjectById(statusId,list_id);
            if (obj!=null) {
                status = materializeStatus(obj);
                if (status!=null)
                    return status;
            }
        }

        Twitter twitter = getTwitter();
        try {
            status = twitter.showStatus(statusId);
            long id;
            if (list_id==null)
                id = 0;
            else
                id = list_id;
            persistStatus(tweetDB,status,id);
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return status;
    }


    private void persistStatus(TweetDB tdb, Status status, long list_id) throws IOException {
        if (tdb.getStatusObjectById(status.getId(),list_id)!=null)
            return; // This is already in DB, so do nothing

        // Serialize and then store in DB
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(status);
        tdb.storeOrUpdateStatus(status.getId(), status.getInReplyToStatusId(), list_id, bos.toByteArray(), true);
    }

    private void updateStatus(TweetDB tdb, Status status, long list_id) throws IOException {
        if (tdb.getStatusObjectById(status.getId(),list_id)==null) {
            persistStatus(tdb,status,list_id);
            return;
        }

        // Serialize and then store in DB
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(status);
        tdb.storeOrUpdateStatus(status.getId(), status.getInReplyToStatusId(), list_id, bos.toByteArray(), false);
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

    public String getStatusDate(Status status) {
        Date date = status.getCreatedAt();
        long time = date.getTime();

        return (String) DateUtils.getRelativeDateTimeString(context,
                time,
                DateUtils.SECOND_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL);
    }
}
