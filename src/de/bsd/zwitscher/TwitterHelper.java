package de.bsd.zwitscher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.MetaList;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;
import twitter4j.json.DataObjectFactory;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploaderFactory;
import twitter4j.media.MediaProvider;

public class TwitterHelper {

    private static final String HTTP_IDENTI_CA_API = "http://identi.ca/api/";
    Context context;
    TweetDB tweetDB;
    Twitter twitter;
    int accountId;
    Account account;


    public TwitterHelper(Context context, Account account) {
		this.context = context;
        this.account = account;
        if (account!=null)
            accountId = account.getId();
        else
            accountId = -1; // default if no account selected.
        tweetDB = new TweetDB(context,accountId);
        twitter = getTwitter();
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
//            for (Status status : statuses) { // TODO persist in one go
//                persistStatus(tweetDB, status,list_id);
//            }
            persistStatus(statuses,list_id);

        }
        catch (Exception e) {
            System.err.println("Got exception: " + e.getMessage() );
            if (e.getCause()!=null)
                System.err.println("   " + e.getCause().getMessage());
            statuses = new ArrayList<Status>();

        }
        int numStatuses = statuses.size();
        int filled = fillUpStatusesFromDB(list_id,statuses);
        Log.i("getTimeline","Now we have " + statuses.size());

        MetaList<Status> metaList = new MetaList<Status>(statuses,numStatuses,filled);
        Log.i("getTimeline","returning  " + metaList);
        return metaList ;
	}


    /**
     * Return direct messages.
     * The "fetch from the server" part is a bit tricky. as Twitter4J keeps
     * the JSON representation of the objects in a ThreadLocal. So to obtain
     * it, it must be accessed before the next call to twitter.
     *
     * @param fromDbOnly
     * @param paging
     * @return
     */
    public MetaList<DirectMessage> getDirectMessages(boolean fromDbOnly, Paging paging) {

       List<DirectMessage> ret;
       List<DirectMessage> ret2 = new ArrayList<DirectMessage>();

       if (!fromDbOnly) {
          // Get direct messages sent to us
          try {
             ret = twitter.getDirectMessages(paging);
          } catch (TwitterException e) {
             Log.e("getDirects", "Got exception: " + e.getMessage());
             ret = Collections.emptyList();
          }

          long max = -1;
          //  persist directs

          if (ret.size()>0) {
              persistDirects(ret);
             if (ret.get(0).getId()>max)
                max = ret.get(0).getId();

             ret2.addAll(ret);
          }

          // get direct messages we sent
          try {
             ret = twitter.getSentDirectMessages(paging);
          } catch (TwitterException e) {
             Log.e("getDirects", "Got exception: " + e.getMessage());
             ret = Collections.emptyList();
          }
          if (ret.size()>0) {
              persistDirects(ret);
             if (ret.get(0).getId()>max)
                max = ret.get(0).getId();

             ret2.addAll(ret);
          }

          // Now sort the two collections we've got to form a linear
          // timeline.
          Collections.sort(ret2,new Comparator<DirectMessage>() {
             public int compare(DirectMessage directMessage, DirectMessage directMessage1) {
                return (int) (directMessage1.getId() - directMessage.getId());
             }
          });


          tweetDB.updateOrInsertLastRead(-2,max);
       }
       int numDirects = ret2.size();
       int filled = fillUpDirectsFromDb(ret2);

       MetaList<DirectMessage> result = new MetaList<DirectMessage>(ret2,numDirects,filled);
       return result;
    }


    /**
     * Read status objects from the database only.
     * @param sinceId The oldest Id that should
     * @param howMany
     * @param list_id
     * @return
     */
    public List<Status> getStatuesFromDb(long sinceId, int howMany, long list_id) {
        List<Status> ret = new ArrayList<Status>();
        List<String> responseList = tweetDB.getStatusesObjsOlderThan(sinceId,howMany,list_id);
        for (String json : responseList) {
            Status status = materializeStatus(json);
            ret.add(status);
        }
        return ret;
    }

    public List<DirectMessage> getDirectsFromDb(long sinceId, int num) {
        List<DirectMessage> ret = new ArrayList<DirectMessage>();
        List<String> responses = tweetDB.getDirectsOlderThan(sinceId,num);
        for (String json : responses) {
            DirectMessage msg = materializeDirect(json);
            ret.add(msg);
        }
        Log.i("Get directs","Got " + ret.size() + " messages");
        return ret;
    }


    public List<UserList> getUserLists() {

        List<UserList> userLists;
		try {
			String username = twitter.getScreenName();
			userLists = twitter.getUserLists(username, -1);
			return userLists;
		} catch (Exception e) {
            // called from background task, so no toast allowed
			e.printStackTrace();
			userLists = Collections.emptyList();
		}
        return userLists;
	}


	private Twitter getTwitter() {
        if (account!=null) {
            if (account.getServerType().equalsIgnoreCase("twitter")) {
                Twitter twitterInstance = new TwitterFactory().getOAuthAuthorizedInstance(
                        TwitterConsumerToken.consumerKey,
                        TwitterConsumerToken.consumerSecret,
                        new AccessToken(account.getAccessTokenKey(), account.getAccessTokenSecret()));
                return twitterInstance;
            }
            else if (account.getServerType().equalsIgnoreCase("identi.ca")) {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setRestBaseURL(HTTP_IDENTI_CA_API);
                cb.setSearchBaseURL(HTTP_IDENTI_CA_API);
                cb.setOAuthAccessTokenURL("https://identi.ca/api/oauth/access_token");
                cb.setOAuthAuthorizationURL("https://identi.ca/api/oauth/authorize");
                cb.setOAuthRequestTokenURL("https://identi.ca/api/oauth/request_token");
                Configuration conf = cb.build() ;


                Twitter twitterInstance = new TwitterFactory(conf).getInstance(account.getName(),account.getPassword());
                return twitterInstance;
            }
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


        Twitter twitterInstance = new TwitterFactory().getInstance();
        twitterInstance.setOAuthConsumer(TwitterConsumerToken.consumerKey, TwitterConsumerToken.consumerSecret);
        RequestToken requestToken = twitterInstance.getOAuthRequestToken();
        Editor editor = preferences.edit();
        editor.putString("requestToken", requestToken.getToken());
        editor.putString("requestTokenSecret", requestToken.getTokenSecret());
        editor.commit();

        return requestToken;
	}

    /**
     * Get an auth token the classical OAuth way
     * @param pin
     * @throws Exception
     */
	public void generateAuthToken(String pin) throws Exception{
        Twitter twitterInstance = new TwitterFactory().getInstance();
        twitterInstance.setOAuthConsumer(TwitterConsumerToken.consumerKey, TwitterConsumerToken.consumerSecret);
        RequestToken requestToken = getRequestToken(false); // twitterInstance.getOAuthRequestToken();
		AccessToken accessToken = twitterInstance.getOAuthAccessToken(requestToken, pin);


		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = preferences.edit();
		editor.putString("accessToken", accessToken.getToken());
		editor.putString("accessTokenSecret", accessToken.getTokenSecret());
		editor.commit();
	}

    /**
     * Generate an account the xAuth way for Twitter. This only works if especially enabled by Twitter
     *
     *
     *
     * @param username Username to get the token for
     * @param password password of that user and service
     * @param service service to use. Currently supported are twitter and identi.ca
     * @param makeDefault  @throws Exception If the server can not be reached or the credentials are not vaild
     * @return id of the account
     * @throws Exception when anything goes wrong (e.g wrong username/password etc.)
     */
    public Account generateAccount(String username, String password, String service, boolean makeDefault) throws Exception {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        Twitter twitterInstance;
        Account account = null;

        if (service.equalsIgnoreCase("twitter")) {
            cb.setOAuthConsumerKey(TwitterConsumerToken.consumerKey);
            cb.setOAuthConsumerSecret(TwitterConsumerToken.consumerSecret);
            Configuration conf = cb.build() ;
            twitterInstance = new TwitterFactory(conf).getInstance(username,password);
            AccessToken accessToken = twitterInstance.getOAuthAccessToken();
            int newId = tweetDB.getNewAccountId();
            account = new Account(newId,username,accessToken.getToken(),accessToken.getTokenSecret(),null,"twitter",makeDefault);
            // TODO determine account id via db sequence?
            tweetDB.insertOrUpdateAccount(account);
            if (makeDefault)
                tweetDB.setDefaultAccount(account.getId());

        }
        else if (service.equalsIgnoreCase("identi.ca")) {
            cb.setRestBaseURL(HTTP_IDENTI_CA_API);
            cb.setSearchBaseURL(HTTP_IDENTI_CA_API);
            cb.setOAuthAccessTokenURL("https://identi.ca/api/oauth/access_token");
//            cb.setOAuthAuthenticationURL();
            cb.setOAuthAuthorizationURL("https://identi.ca/api/oauth/authorize");
            cb.setOAuthRequestTokenURL("https://identi.ca/api/oauth/request_token");
            Configuration conf = cb.build() ;
            TwitterFactory twitterFactory = new TwitterFactory(conf);
            twitterInstance = twitterFactory.getInstance(username, password);
            // Trigger a fetch to validate the credentials
            Paging paging = new Paging();
            paging.setCount(1);
            twitterInstance.getHomeTimeline(paging);


            // TODO determine account id via db sequence?
            int newId = tweetDB.getNewAccountId();
            account = new Account(newId,username,null,service,makeDefault,password);
            tweetDB.insertOrUpdateAccount(account);
            if (makeDefault)
                tweetDB.setDefaultAccount(account.getId());

        }

        return account;

    }

	public UpdateResponse updateStatus(UpdateRequest request) {
        UpdateResponse updateResponse = new UpdateResponse(request.updateType,request.statusUpdate);
		Log.i("TwitterHelper", "Sending update: " + request.statusUpdate);
		try {
			twitter.updateStatus(request.statusUpdate);
            updateResponse.setMessage("Tweet sent");
            updateResponse.setSuccess();
		} catch (TwitterException e) {
            updateResponse.setMessage( e.getLocalizedMessage());
            updateResponse.setFailure();
        }
        return updateResponse;
	}

	public UpdateResponse retweet(UpdateRequest request) {
        UpdateResponse response = new UpdateResponse(request.updateType,request.id);
		try {
			twitter.retweetStatus(request.id);
            response.setSuccess();
			response.setMessage("Retweeted successfully");
		} catch (TwitterException e) {
            response.setFailure();
            response.setMessage(e.getLocalizedMessage());
		}
        return response;
	}

	public UpdateResponse favorite(UpdateRequest request) {
        Status status = request.status;
        UpdateResponse updateResponse = new UpdateResponse(request.updateType, status);
        updateResponse.view = request.view;
		try {
			if (status.isFavorited()) {
				twitter.destroyFavorite(status.getId());
            }
			else {
				twitter.createFavorite(status.getId());
            }

            // reload tweet and update in DB - twitter4j should have some status.setFav()..
            status = getStatusById(status.getId(),null, true, false); // no list id, don't persist
            updateStatus(status); // explicitly update in DB - we know it is there.
			updateResponse.setSuccess();
            updateResponse.setMessage("(Un)favorite set");
		} catch (Exception e) {
            updateResponse.setFailure();
            updateResponse.setMessage(e.getLocalizedMessage());
		}
        updateResponse.setStatus(status);
        return updateResponse;
	}


    public UpdateResponse direct(UpdateRequest request) {
        UpdateResponse updateResponse = new UpdateResponse(request.updateType, (Status) null); // TODO
        try {
            twitter.sendDirectMessage((int)request.id,request.message);
            updateResponse.setSuccess();
            updateResponse.setMessage("Direct message sent");
        } catch (TwitterException e) {
            updateResponse.setFailure();
            updateResponse.setMessage(e.getLocalizedMessage());
            updateResponse.setOrigMessage(request.message);
        }
        return updateResponse;
    }

	public MetaList<Status> getUserList(Paging paging, int listId, boolean fromDbOnly) {

        List<Status> statuses;
        if (!fromDbOnly) {
            try {
                String listOwnerScreenName = twitter.getScreenName();

                statuses = twitter.getUserListStatuses(listOwnerScreenName, listId, paging);
                int size = statuses.size();
                Log.i("getUserList","Got " + size + " statuses from Twitter");

                persistStatus(statuses,listId);
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
Log.d("getUsetList", "Returning " + metaList);
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

    private int fillUpDirectsFromDb(List<DirectMessage> messages) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int minOld = Integer.valueOf(preferences.getString("minOldTweets","5"));
        int maxOld = Integer.valueOf(preferences.getString("maxOldTweets","10"));

        int size = messages.size();
        if (size==0)
            messages.addAll(getDirectsFromDb(-1,maxOld));
        else {
            int num = (size+minOld < maxOld) ? maxOld-size : minOld;
            messages.addAll(getDirectsFromDb(messages.get(size-1).getId(),num));
        }
        int size2 = messages.size();

        return  size2 - size;
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
            String obj  = tweetDB.getStatusObjectById(statusId, list_id);
            if (obj!=null) {
                status = materializeStatus(obj);
                if (status!=null)
                    return status;
            }
        }

        try {
            status = twitter.showStatus(statusId);

            if (alsoPersist) {
                long id;
                if (list_id==null)
                    id = 0;
                else
                    id = list_id;
                List<Status> sList = new ArrayList<Status>(1);
                sList.add(status);
                persistStatus(sList, id);
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
        User user = null;
        boolean existing = false;

        try {
            String userJson = tweetDB.getUserById(userId);
            if (userJson!=null) {
                existing = true;
                user = DataObjectFactory.createUser(userJson);
            }
            if (cachedOnly) {
                return user;
            }

            // not cached only -> go to the server
            user = twitter.showUser(userId);
            userJson = DataObjectFactory.getRawJSON(user);
            if (!existing)
                tweetDB.insertUser(userId,userJson, user.getScreenName());
            else
                tweetDB.updateUser(userId,userJson);

        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return user;
    }

    /**
     * Retrieve a User object of the passed screen name
     * @param screenName Screen name of the user
     * @param cachedOnly If true, only a cached version of the object or null is returned. Otherwise a request to the server is made
     * @return User object or null, if it can not be obtained.
     */
    public User getUserByScreenName(String screenName, boolean cachedOnly) {
        User user = null;
        boolean existing = false;

        try {
            String userJson = tweetDB.getUserByName(screenName);
            if (userJson!=null) {
                existing=true;
                user = DataObjectFactory.createUser(userJson);
            }
            if (cachedOnly)
                return user;

            // not cached only -> go to the server
            user = twitter.showUser(screenName);
            userJson = DataObjectFactory.getRawJSON(user);
            if (!existing)
                tweetDB.insertUser(user.getId(),userJson, user.getScreenName());
            else
                tweetDB.updateUser(user.getId(),userJson);

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
        try {
            twitter.addUserListMember(listId,userId);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return false;
        }
        return true;
    }

    public List<Status> getUserTweets(int userId) {

        Paging paging = new Paging();
        paging.setCount(30);
        List<Status> ret = null;
        try {
            ret = twitter.getUserTimeline(userId,paging);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return Collections.emptyList();
        }
        return ret;
    }

    private void persistStatus(Collection<Status> statuses, long list_id) {
        if (statuses.isEmpty())
            return;

        List<ContentValues> values = new ArrayList<ContentValues>(statuses.size());
        long now = System.currentTimeMillis();
        for (Status status : statuses) {
            String json = DataObjectFactory.getRawJSON(status);
            ContentValues cv = new ContentValues(6);
            cv.put("ID", status.getId());
            cv.put("I_REP_TO", status.getInReplyToStatusId());
            cv.put("LIST_ID", list_id);
            cv.put("ACCOUNT_ID",accountId);
            cv.put("STATUS",json);
            cv.put("ctime",now);
            values.add(cv);
        }
        tweetDB.storeValues(TweetDB.TABLE_STATUSES,values);
    }

    private void persistDirects(Collection<DirectMessage> directs) {
        if (directs.isEmpty())
            return;

        List<ContentValues> values = new ArrayList<ContentValues>(directs.size());
        for (DirectMessage directMessage : directs) {
            String json = DataObjectFactory.getRawJSON(directMessage);
            ContentValues cv = new ContentValues(4);
            cv.put("id",directMessage.getId());
            cv.put("created_at", directMessage.getCreatedAt().getTime());
            cv.put("ACCOUNT_ID",accountId);
            cv.put("message_json",json);
            values.add(cv);
        }
        tweetDB.storeValues(TweetDB.TABLE_DIRECTS,values);

    }
    /**
     * Update an existing status object in the database with the passed one.
     *
     * @param status Updated status object
     * @throws IOException
     */
    private void updateStatus(Status status) throws IOException {

        // Serialize and then store in DB
        String json = DataObjectFactory.getRawJSON(status);
        tweetDB.updateStatus(status.getId(), json);
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

    /**
     * Return a string representation of the date the passed status was created.
     * @param status Response object from the server (Status or DirectMessage)
     * @return String showing the date
     */
    public String getStatusDate(TwitterResponse status) {
        Date date;
        if (status instanceof Status)
            date = ((Status)status).getCreatedAt();
        else if (status instanceof DirectMessage)
            date = ((DirectMessage)status).getCreatedAt();
        else
            throw new IllegalArgumentException("Type of " + status + " unknown");
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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String provider = preferences.getString("pictureService","yfrog");

        try {
            File file = new File(fileName);
            MediaProvider mProvider ;
            if (provider.equals("yfrog"))
                mProvider = MediaProvider.YFROG;
            else if (provider.equals("twitpic"))
                mProvider = MediaProvider.TWITPIC;
            else
                throw new IllegalArgumentException("Picture provider " + provider + " unknown");

            String accessTokenToken = account.getAccessTokenKey();
            String accessTokenSecret = account.getAccessTokenSecret();

            Properties props = new Properties();
            props.put(PropertyConfiguration.MEDIA_PROVIDER,mProvider);
            props.put(PropertyConfiguration.OAUTH_ACCESS_TOKEN,accessTokenToken);
            props.put(PropertyConfiguration.OAUTH_ACCESS_TOKEN_SECRET,accessTokenSecret);
            props.put(PropertyConfiguration.OAUTH_CONSUMER_KEY,TwitterConsumerToken.consumerKey);
            props.put(PropertyConfiguration.OAUTH_CONSUMER_SECRET,TwitterConsumerToken.consumerSecret);
            Configuration conf = new PropertyConfiguration(props);

            ImageUploaderFactory factory = new ImageUploaderFactory(conf);
            ImageUpload upload = factory.getInstance(mProvider);
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

        try {
            int myId = twitter.getId();
            Relationship rel = twitter.showFriendship(myId,userId);
            return rel.isSourceFollowingTarget();
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return false;
        }
    }

    public List<SavedSearch> getSavedSearchesFromServer() {

        List<SavedSearch> searches;
        try {
            searches = twitter.getSavedSearches();
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            searches = Collections.emptyList();
        }

        return searches;  // TODO: Customise this generated block
    }

    public List<SavedSearch> getSavedSearchesFromDb() {
        List<String> jsons = tweetDB.getSavedSearches();
        List<SavedSearch> searches = new ArrayList<SavedSearch>(jsons.size());

        for (String json : jsons) {
            try {
                SavedSearch search = DataObjectFactory.createSavedSearch(json);
                searches.add(search);
            } catch (TwitterException e) {
                e.printStackTrace(); // TODO Customize ...
            }
        }
        return searches;
    }

    public List<User> getUsersFromDb() {
        List<String> jsons = tweetDB.getUsers();
        List<User> users = new ArrayList<User>(jsons.size());
        for (String json : jsons) {
            try {
                User user = DataObjectFactory.createUser(json);
                users.add(user);
            } catch (TwitterException e) {
                e.printStackTrace();  // TODO: Customise this generated block
            }
        }
        return users;
    }

    public MetaList<Tweet> getSavedSearchesTweets(int searchId, boolean fromDbOnly, Paging paging) {

        List<SavedSearch> searches = getSavedSearchesFromDb();
        for (SavedSearch search : searches) {
            if (search.getId()==searchId) {
                String queryString = search.getQuery();
                Query query = new Query(queryString); // TODO paging - probably not needed as default is 15
                   // TODO set some restriction like language or such
                try {
                    QueryResult queryResult = twitter.search(query);
                    List<Tweet> tweets = queryResult.getTweets();
                    MetaList metaList = new MetaList(tweets,tweets.size(),0);
                    return metaList;
                } catch (TwitterException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                    return new MetaList<Tweet>();
                }
            }
        }

        return new MetaList<Tweet>();
    }

    public void persistSavedSearch(SavedSearch search) {
        String json = DataObjectFactory.getRawJSON(search);

        tweetDB.storeSavedSearch(search.getName(),search.getQuery(),search.getId(),json);
    }
}
