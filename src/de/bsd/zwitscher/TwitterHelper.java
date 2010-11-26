package de.bsd.zwitscher;

import java.io.File;
import java.io.IOException;
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

import de.bsd.zwitscher.helper.MetaList;
import twitter4j.*;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;
import twitter4j.json.DataObjectFactory;
import twitter4j.util.ImageUpload;


public class TwitterHelper {


	Context context;
    TweetDB tweetDB;

	public TwitterHelper(Context context) {
		this.context = context;
        tweetDB = new TweetDB(context,0); // TODO set real account
	}

    /**
     * Get a timeline for Status (Home time line, mentions) or lists
     * @param paging paging object to control fetching
     * @param list_id The list to fetch (0 = home time line , -1 = mentions
     * @param fromDbOnly Should fetching only come from the DB or should a remote server be contacted?
     * @return List of Status objects
     * @see twitter4j.Status
     */
	public MetaList<Status> getTimeline(Paging paging, int list_id, boolean fromDbOnly) {
        Twitter twitter = getTwitter();

        List<Status> statuses = null;
		try {
			switch (list_id) {
			case 0:
                if (!fromDbOnly)
				    statuses = twitter.getHomeTimeline(paging ); //like friends + including retweet
				break;
			case -1:
                if (!fromDbOnly)
				    statuses = twitter.getMentions(paging);
				break;

			default:
				statuses = new ArrayList<Status>();
			}
            if (statuses==null)
                statuses=new ArrayList<Status>();
            for (Status status : statuses) {
                persistStatus(tweetDB, status,list_id);
            }

        }
        catch (Exception e) {
            System.err.println("Got exception: " + e.getMessage() );
            if (e.getCause()!=null)
                System.err.println("   " + e.getCause().getMessage());
            statuses = new ArrayList<>();

        }
        int numStatuses = statuses.size();
        int filled = fillUpStatusesFromDB(list_id,statuses);
        Log.i("getTimeline","Now we have " + statuses.size());

        MetaList<Status> metaList = new MetaList<Status>(statuses,numStatuses,filled);
        Log.i("getTimeline","returning  " + metaList);
        return metaList ;
	}

    /**
     * Read status objects from the database only.
     * @param sinceId The oldest Id that should
     * @param howMany
     * @param list_id
     * @return
     */
    public List<Status> getStatuesFromDb(long sinceId, int howMany, long list_id) {
        List<Status> ret = new ArrayList<>();
        List<String> responseList = tweetDB.getStatusesObjsOlderThan(sinceId,howMany,list_id);
        for (String json : responseList) {
            Status status = materializeStatus(json);
            ret.add(status);
        }
        return ret;
    }


    public List<UserList> getUserLists() {
		Twitter twitter = getTwitter();

        List<UserList> userLists;
		try {
			String username = twitter.getScreenName();
			userLists = twitter.getUserLists(username, -1);
			return userLists;
		} catch (Exception e) {
			Toast.makeText(context, "Getting lists failed: " + e.getMessage(), 15000).show();
			e.printStackTrace();
			userLists = new ArrayList<>();
		}
        return userLists;
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
        updateResponse.view = request.view;
		Twitter twitter = getTwitter();
		try {
			if (status.isFavorited()) {
				twitter.destroyFavorite(status.getId());
            }
			else {
				twitter.createFavorite(status.getId());
            }

            // reload tweet and update in DB - twitter4j should have some status.setFav()..
            status = getStatusById(status.getId(),null, true, false); // no list id, don't persist
            updateStatus(tweetDB,status); // explicitly update in DB - we know it is there.
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

	public MetaList<Status> getUserList(Paging paging, int listId, boolean fromDbOnly) {
        Twitter twitter = getTwitter();

        List<Status> statuses;
        if (!fromDbOnly) {
            try {
                String listOwnerScreenName = twitter.getScreenName();

                statuses = twitter.getUserListStatuses(listOwnerScreenName, listId, paging);
                int size = statuses.size();
                Log.i("getUserList","Got " + size + " statuses from Twitter");

                for (Status status : statuses) {
                    persistStatus(tweetDB, status,listId);
                }
            } catch (Exception e) {
                statuses = new ArrayList<Status>();

                System.err.println("Got exception: " + e.getMessage() );
                if (e.getCause()!=null)
                    System.err.println("   " + e.getCause().getMessage());
            }
        } else
            statuses = new ArrayList<Status>();

        int numOriginal = statuses.size();
        int filled = fillUpStatusesFromDB(listId, statuses);
        Log.i("getUserList","Now we have " + statuses.size());

        MetaList<Status> metaList = new MetaList<Status>(statuses,numOriginal,filled);
Log.d("getUsetList","Returning " + metaList);
        return metaList;
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
     * @return number of added statuses
     */
    private int fillUpStatusesFromDB(int listId, List<Status> statuses) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int minOld = Integer.valueOf(preferences.getString("minOldTweets","5"));
        int maxOld = Integer.valueOf(preferences.getString("maxOldTweets","10"));

Log.d("FillUp","Incoming: " + statuses.size());
        int size = statuses.size();
        if (size==0)
            statuses.addAll(getStatuesFromDb(-1,maxOld,listId));
        else {
            int num = (size+minOld< maxOld) ? maxOld-size : minOld;
            statuses.addAll(getStatuesFromDb(statuses.get(size-1).getId(),num,listId));
        }
        int size2 = statuses.size();
Log.d("FillUp","Now: " + size2);

        int i = size2 - size;
Log.d("FillUp","Return: " + i);
        return i;
    }


    /**
     * Get a single status. If directOnly is false, we first look in the local
     * db if it is already present. Otherwise we directly go to the server.
     *
     * @param statusId
     * @param list_id
     * @param directOnly
     * @param alsoPersist
     * @return
     */
    public Status getStatusById(long statusId, Long list_id, boolean directOnly, boolean alsoPersist) {
        Status status = null;

        if (!directOnly) {
            String obj  = tweetDB.getStatusObjectById(statusId);
            if (obj!=null) {
                status = materializeStatus(obj);
                if (status!=null)
                    return status;
            }
        }

        Twitter twitter = getTwitter();
        try {
            status = twitter.showStatus(statusId);

            if (alsoPersist) {
                long id;
                if (list_id==null)
                    id = 0;
                else
                    id = list_id;
                persistStatus(tweetDB,status,id);
            }
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return status;
    }

    public List<Status> getRepliesToStatus(long statusId) {
        List<Status> ret = new ArrayList<Status>();
        List<String> replies = tweetDB.getReplies(statusId);
        for (String reply : replies) {
            Status status = materializeStatus(reply);
            ret.add(status);
        }
        return ret;
    }

    /**
     * Retrieve a User object for the passed userId.
     * @param userId Id of the user to look up
     * @param cachedOnly If true, only a cached version of the object or null is returned. Otherwise a request to the server is made
     * @return User object or null, if it can not be obtained.
     */
    public User getUserById(int userId, boolean cachedOnly) {
        Twitter twitter = getTwitter();
        User user = null;
        try {

            String userJson = tweetDB.getUserById(userId,0);
            if (userJson==null) {
                if (!cachedOnly) {
                    user = twitter.showUser(userId);
                    userJson = DataObjectFactory.getRawJSON(user);
                    tweetDB.insertUser(userId,userJson);
                }
            } else {
                user = DataObjectFactory.createUser(userJson);
            }

        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return user;
    }

    /**
     * Send a request to follow or unfollow a user
     * @param userId Id of the user to follow
     * @param doFollow If true, send a follow request; unfollow otherwise.
     * @return True in case of success
     * @todo make async
     */
    public boolean followUnfollowUser(int userId, boolean doFollow ) {
        Twitter twitter = getTwitter();
        try {
            if (doFollow)
                twitter.createFriendship(userId);
            else
                twitter.destroyFriendship(userId);

            return true;
        } catch (TwitterException e) {
            Log.w("followUnfollowUser",e.getMessage());
        }
        return false;
    }

    /**
     * Add a user to some of our user lists
     * @param userId User to add
     * @param listId ids of the list to put the user on
     * @return true if successful
     */
    public boolean addUserToLists(int userId, int listId) {
        Twitter twitter = getTwitter();
        try {
            twitter.addUserListMember(listId,userId);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return false;
        }
        return true;
    }

    private void persistStatus(TweetDB tdb, Status status, long list_id) throws IOException {
        if (tdb.getStatusObjectById(status.getId())!=null)
            return; // This is already in DB, so do nothing

        // Serialize and then store in DB
        String json = DataObjectFactory.getRawJSON(status);
        tdb.storeStatus(status.getId(), status.getInReplyToStatusId(), list_id, json);
    }

    /**
     * Update an existing status object in the database with the passed one.
     * @param tdb TweetDb to use
     * @param status Updated status object
     * @throws IOException
     */
    private void updateStatus(TweetDB tdb, Status status) throws IOException {

        // Serialize and then store in DB
        String json = DataObjectFactory.getRawJSON(status);
        tdb.updateStatus(status.getId(), json);
    }



    private Status materializeStatus(String obj) {

        Status response;
        try {
            response = DataObjectFactory.createStatus(obj);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            response = null;
        }
        return response;
    }


    private User materializeUser(String obj) {

        User response;
        try {
            response = DataObjectFactory.createUser(obj);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            response = null;
        }

        return response;
    }

    private DirectMessage materializeDirect(String obj) {

        DirectMessage response;
        try {
            response = DataObjectFactory.createDirectMessage(obj);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            response = null;
        }
        return response;
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

    /**
     * Upload a picture to a remote picture service like yfrog.
     * @param fileName Path on file system to the picture
     * @return Url where this was stored on the remote service or null on error
     */
    public String postPicture(String fileName) {
        Twitter twitter = getTwitter();

        try {
            File file = new File(fileName);
            ImageUpload upload = ImageUpload.getYFrogUploader(twitter); // TODO allow user selection of service
            String url = upload.upload(file);
            return url;
        } catch (Exception e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return null;
    }

    /**
     * Determine if the current user is following the passed one
     * @param userId id of the user to verify
     * @return true if we are following, false otherwise
     */
    public boolean areWeFollowing(Integer userId) {

        Twitter twitter = getTwitter();
        try {
            int myId = twitter.getId();
            Relationship rel = twitter.showFriendship(myId,userId);
            return rel.isSourceFollowingTarget();
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return false;
        }
    }
}
