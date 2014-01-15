package de.bsd.zwitscher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.helper.ExpandUrlRunner;
import de.bsd.zwitscher.helper.MetaList;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.BasicAuthorization;
import twitter4j.auth.OAuthAuthorization;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.conf.PropertyConfiguration;
import twitter4j.json.DataObjectFactory;
import twitter4j.media.ImageUpload;
import twitter4j.media.ImageUploadFactory;
import twitter4j.media.MediaProvider;

public class TwitterHelper {

    private Context context;
    private TweetDB tweetDB;
    private Twitter twitter;
    private int accountId;
    private Account account;


    public TwitterHelper(Context context, Account account) {
		this.context = context;
        this.account = account;
        if (account!=null)
            accountId = account.getId();
        else
            accountId = -1; // default if no account selected.
        tweetDB = TweetDB.getInstance(context.getApplicationContext());
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
                if (!fromDbOnly) {
//                    if (!account.isStatusNet())
				        statuses = twitter.getMentionsTimeline(paging);
//                    else
//                        statuses = twitter.getSNMentions(paging);
                }
				break;
// -2 is directs
            case -3 :
                if (!fromDbOnly)
                    statuses = twitter.getUserTimeline(paging);
                    break;
            case -4 :
                if (!fromDbOnly)
                    statuses = twitter.getFavorites(); // TODO paging?  TODO favorites only live - or how to sync them?
                break;
			default:
				statuses = new ArrayList<Status>();
			}
            if (statuses==null)
                statuses=new ArrayList<Status>();

            persistStatus(statuses,list_id);

        }
        catch (TwitterException e) {
            Log.e("TwitterHelper","getTimeline: Got exception: " + e.getMessage() );
            if (e.getCause()!=null)
                System.err.println("   " + e.getCause().getMessage());
            statuses = new ArrayList<Status>();

        }
        int numStatuses = statuses.size();
        int filled = fillUpStatusesFromDB(list_id,statuses, -1);
        Log.i("getTimeline","Now we have " + statuses.size());

        MetaList<Status> metaList = new MetaList<Status>(statuses,numStatuses,filled);
        Log.i("getTimeline","returning  " + metaList);
        return metaList ;
	}

    OAuthAuthorization getOAuth() {
        return (OAuthAuthorization) twitter.getAuthorization();
    }


    /**
     * Return direct messages.
     * The "fetch from the server" part is a bit tricky. as Twitter4J keeps
     * the JSON representation of the objects in a ThreadLocal. So to obtain
     * it, it must be accessed before the next call to twitter.
     *
     * @param fromDbOnly If true, only messages that are found in the DB are returned
     * @param paging A paging object to tell how many items to fetch.
     * @return A list of direct messages
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

          if (max > -1) {
             tweetDB.updateOrInsertLastRead(accountId, -2,max);
          }
       }
       int numDirects = ret2.size();
       int filled = fillUpDirectsFromDb(ret2);

          // Now sort the two collections we've got to form a linear
          // timeline.
          Collections.sort(ret2,new Comparator<DirectMessage>() {
             public int compare(DirectMessage directMessage, DirectMessage directMessage1) {
                return directMessage1.getCreatedAt().compareTo(directMessage.getCreatedAt());
             }
          });


       MetaList<DirectMessage> result = new MetaList<DirectMessage>(ret2,numDirects,filled);
       return result;
    }


    /**
     * Read status objects from the database only.
     * @param sinceId The oldest Id that should
     * @param howMany How many items should be retrieved
     * @param list_id THe id of the list to retrieve the items for
     * @return List of Statuses
     */
    public List<Status> getStatuesFromDb(long sinceId, int howMany, long list_id) {
        List<Status> ret = new ArrayList<Status>();
        List<String> responseList = tweetDB.getStatusesObjsOlderThan(accountId, sinceId,howMany,list_id);
        for (String json : responseList) {
            Status status = materializeStatus(json);
            ret.add(status);
        }
        return ret;
    }

    public List<DirectMessage> getDirectsFromDb(long sinceId, int num) {
        List<DirectMessage> ret = new ArrayList<DirectMessage>();
        List<String> responses = tweetDB.getDirectsOlderThan(accountId, sinceId,num);
        for (String json : responses) {
            DirectMessage msg = materializeDirect(json);
            ret.add(msg);
        }
        Log.i("Get directs","Got " + ret.size() + " messages");
        return ret;
    }


    public List<UserList> getUserListsFromServer() {

        List<UserList> userLists;
		try {
			String username = account.getName();
			userLists = twitter.getUserLists(username); // Lists I created
            userLists.addAll( twitter.getUserListSubscriptions(username, -1) ); // Lists from other users
			return userLists;
		} catch (TwitterException e) {
            // called from background task, so no toast allowed
			userLists = Collections.emptyList();
		}
        return userLists;
	}

    /**
     * Retrieve the lists, the passed user is subscribed to. The returned
     * list is filtered down to the lists the current account owns
     * @param screenName Name of the user to investigate
     * @return List of list names
     */
    public List<String> getListMembershipFromServer(String screenName) {
        List<String> userLists;
        List<UserList> tmp;
        try {
            tmp = twitter.getUserListMemberships(screenName, -1, true);

            userLists = new ArrayList<String>(tmp.size());
            for (UserList ul : tmp) {
                userLists.add(ul.getName());
            }
        }
        catch (TwitterException te) {
            te.printStackTrace();
            userLists = Collections.emptyList();
        }

        return userLists;
    }

    /**
     * Get the default account from the tweet db.
     * @return Default account
     */
    private Account getDefaultAccount() {
        Account acc = tweetDB.getDefaultAccount();
        return acc;
    }

    /**
     * Get an authorized org.twitter4j.Twitter instance. If the global
     * account object is not set, the default account is used.
     * @return authorized Twitter instance.
     */
    public Twitter getTwitter() {
        if (account==null)
            account = getDefaultAccount();

        MediaProvider mediaProvider = getMediaProvider();

        if (account!=null) {
            if (account.getServerType()== Account.Type.TWITTER) {
                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setIncludeEntitiesEnabled(true);
                cb.setJSONStoreEnabled(true);
                cb.setMediaProvider(mediaProvider.toString());
                cb.setHttpConnectionTimeout(60*1000);
                cb.setHttpReadTimeout(240*1000); // 4 min
                cb.setHttpRetryCount(3);
                Configuration conf = cb.build();
                OAuthAuthorization auth = new OAuthAuthorization(conf);
                auth.setOAuthAccessToken(new AccessToken(account.getAccessTokenKey(), account.getAccessTokenSecret()));
                auth.setOAuthConsumer(Tokens.consumerKey, Tokens.consumerSecret);
                Twitter twitterInstance = new TwitterFactory(conf).getInstance(auth);
                return twitterInstance;
            }
            else if (account.getServerType()== Account.Type.IDENTICA || account.getServerType()== Account.Type.STATUSNET) {
                String base = account.getServerUrl();
                base += "/api/";

                ConfigurationBuilder cb = new ConfigurationBuilder();
                cb.setRestBaseURL(base);
//                cb.setSearchBaseURL(HTTP_IDENTI_CA_API);
                cb.setOAuthAccessTokenURL(base + "oauth/access_token");
                cb.setOAuthAuthorizationURL(base + "oauth/authorize");
                cb.setOAuthRequestTokenURL(base + "oauth/request_token");
                cb.setIncludeEntitiesEnabled(true);
                cb.setJSONStoreEnabled(true);
                Configuration conf = cb.build() ;

                BasicAuthorization auth = new BasicAuthorization(account.getName(),account.getPassword());
                Twitter twitterInstance = new TwitterFactory(conf).getInstance(auth);
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
        twitterInstance.setOAuthConsumer(Tokens.consumerKey, Tokens.consumerSecret);
        RequestToken requestToken = twitterInstance.getOAuthRequestToken();
        Editor editor = preferences.edit();
        editor.putString("requestToken", requestToken.getToken());
        editor.putString("requestTokenSecret", requestToken.getTokenSecret());
        editor.commit();

        return requestToken;
	}

    /**
     * Get an auth token the classical OAuth way
     *
     * @param pin Pin obtained from Twitter needed to create the oauth tokens
     * @return the newly created account
     * @throws Exception
     */
	public Account generateAccountWithOauth(String pin) throws Exception{
        Twitter twitterInstance = new TwitterFactory().getInstance();
        twitterInstance.setOAuthConsumer(Tokens.consumerKey, Tokens.consumerSecret);
        RequestToken requestToken = getRequestToken(false); // twitterInstance.getOAuthRequestToken();
		AccessToken accessToken = twitterInstance.getOAuthAccessToken(requestToken, pin);

        int newId = tweetDB.getNewAccountId();
        Account account = new Account(newId,accessToken.getScreenName(),accessToken.getToken(),accessToken.getTokenSecret(),null, Account.Type.TWITTER,true);

	    tweetDB.insertOrUpdateAccount(account);

	    return account;
	}

    /**
     * Generate an account the xAuth way for Twitter. This only works if especially enabled by Twitter
     * This also works for identi.ca and generic status.net instances.
     *
     * @param username Username to get the token for
     * @param password password of that user and service
     * @param serviceType service to use. Currently supported are twitter and identi.ca
     * @param makeDefault  @throws Exception If the server can not be reached or the credentials are not valid
     * @param baseUrl Base url of the server to connect to (mostly applies to generic status.net instances)
     * @return id of the account
     * @throws Exception when anything goes wrong (e.g wrong username/password etc.)
     */
    public Account generateAccountWithXauth(String username, String password, Account.Type serviceType, boolean makeDefault, String baseUrl) throws Exception {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        Twitter twitterInstance;
        Account account = null;

        if (serviceType== Account.Type.TWITTER) {
            cb.setOAuthConsumerKey(Tokens.consumerKey);
            cb.setOAuthConsumerSecret(Tokens.consumerSecret);
            cb.setIncludeEntitiesEnabled(true);
            cb.setJSONStoreEnabled(true);
            Configuration conf = cb.build() ;
            BasicAuthorization auth = new BasicAuthorization(username,password);
            twitterInstance = new TwitterFactory(conf).getInstance(auth);
            AccessToken accessToken = twitterInstance.getOAuthAccessToken();
            int newId = tweetDB.getNewAccountId();
            account = new Account(newId,username,accessToken.getToken(),accessToken.getTokenSecret(),null, Account.Type.TWITTER,makeDefault);
            // TODO determine account id via db sequence?
            tweetDB.insertOrUpdateAccount(account);
            if (makeDefault)
                tweetDB.setDefaultAccount(newId);

        }
        else if (serviceType== Account.Type.IDENTICA || serviceType == Account.Type.STATUSNET) {

            Log.i("TwitterHelper","generateAccount with baseUrl="+baseUrl);

            cb.setRestBaseURL(baseUrl + "api/");
            cb.setOAuthAccessTokenURL(baseUrl + "api/oauth/access_token");
            cb.setOAuthAuthorizationURL(baseUrl + "api/oauth/authorize");
            cb.setOAuthRequestTokenURL(baseUrl + "api/oauth/request_token");
            cb.setJSONStoreEnabled(true);
            Configuration conf = cb.build() ;
            TwitterFactory twitterFactory = new TwitterFactory(conf);
            BasicAuthorization auth = new BasicAuthorization(username,password);
            twitterInstance = twitterFactory.getInstance(auth);
            // Trigger a fetch to validate the credentials
            Paging paging = new Paging();
            paging.setCount(1);
            twitterInstance.getHomeTimeline(paging);


            // TODO determine account id via db sequence?
            int newId = tweetDB.getNewAccountId();
            account = new Account(newId,username,baseUrl, serviceType,makeDefault,password);
            tweetDB.insertOrUpdateAccount(account);
            if (makeDefault)
                tweetDB.setDefaultAccount(newId);

        }

        return account;

    }

	public UpdateResponse updateStatus(UpdateRequest request) {
        UpdateResponse updateResponse = new UpdateResponse(request.updateType,request.statusUpdate);
        updateResponse.setPicturePath(request.picturePath);
		Log.i("TwitterHelper", "Sending update: " + request.statusUpdate);
		try {
			twitter.updateStatus(request.statusUpdate);

            String sentMessage;
            if (account.isStatusNet())
                sentMessage = context.getString(R.string.dent_sent);
            else
                sentMessage = context.getString((R.string.tweet_sent));

            updateResponse.setMessage(sentMessage);
            updateResponse.setSuccess();
		} catch (TwitterException e) {
            updateResponse.setMessage( e.getLocalizedMessage());
            updateResponse.setFailure();
            updateResponse.setStatusCode(e.getStatusCode());
            Log.e("TH::updateStatus","Update failed",e);
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
            response.setStatusCode(e.getStatusCode());
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
            status = getStatusById(status.getId(),null, true, false, false); // no list id, don't persist
            updateStatus(status); // explicitly update in DB - we know it is there.
			updateResponse.setSuccess();
            updateResponse.setMessage("(Un)favorite set");
		} catch (TwitterException e) {
            updateResponse.setFailure();
            updateResponse.setMessage(e.getLocalizedMessage());
            updateResponse.setStatusCode(e.getStatusCode());
		}
        updateResponse.setStatus(status);
        return updateResponse;
	}


    public UpdateResponse direct(UpdateRequest request) {
        UpdateResponse updateResponse = new UpdateResponse(request.updateType, (Status) null); // TODO
        try {
            twitter.sendDirectMessage((int)request.id,request.message);
            updateResponse.setSuccess();
            String msg = context.getString(R.string.direct_message_sent);
            updateResponse.setMessage(msg);
        } catch (TwitterException e) {
            updateResponse.setFailure();
            updateResponse.setMessage(e.getLocalizedMessage());
            updateResponse.setOrigMessage(request.message);
            updateResponse.setStatusCode(e.getStatusCode());
        }
        return updateResponse;
    }

	public MetaList<Status> getUserList(Paging paging, int listId, boolean fromDbOnly, int unreadCount) {

        List<Status> statuses;
        if (!fromDbOnly) {
            try {
                statuses = twitter.getUserListStatuses( listId, paging);
                int size = statuses.size();
                Log.i("getUserList","Got " + size + " statuses from Twitter");

                persistStatus(statuses,listId);

            } catch (TwitterException e) {
                statuses = new ArrayList<Status>();

                System.err.println("Got exception: " + e.getMessage() );
                if (e.getCause()!=null)
                    System.err.println("   " + e.getCause().getMessage());
            }
        } else
            statuses = new ArrayList<Status>();

        int numOriginal = statuses.size();
        int filled = fillUpStatusesFromDB(listId, statuses, unreadCount);
        Log.i("getUserList","Now we have " + statuses.size());

        MetaList<Status> metaList = new MetaList<Status>(statuses,numOriginal,filled);
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
     *
     * @param listId The list for which tweets are fetched
     * @param statuses The list of incoming statuses to fill up
     * @param unreadCount Number of unread messages the use has in the list. Provides a minimum to fetch. Set to -1 to ignore.
     * @return number of added statuses
     */
    private int fillUpStatusesFromDB(int listId, List<Status> statuses, int unreadCount) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int minOld = Integer.valueOf(preferences.getString("minOldTweets","5"));
        int maxOld = Integer.valueOf(preferences.getString("maxOldTweets","10"));
        if (unreadCount>-1 && unreadCount > maxOld)
            maxOld=unreadCount+3; // Fetch some 'context' too. Also prevents ArrayIndexOOB exceptions later

        int size = statuses.size();
        if (size==0)
            statuses.addAll(getStatuesFromDb(-1,maxOld,listId));
        else {
            int num = (size+minOld< maxOld) ? maxOld-size : minOld;
            statuses.addAll(getStatuesFromDb(statuses.get(size-1).getId(),num,listId));
        }
        int size2 = statuses.size();

        int i = size2 - size;
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
     *
     * @param statusId Id of the status to fetch
     * @param list_id id of the timeline
     * @param directOnly If true only call out to the server. Otherwise try to look up in local db.
     * @param alsoPersist Should the fetched status be persisted? Required directOnly = false and no db hit.
     * @param fromDbOnly If true, do not call out to the server.
     * @return the obtained Status
     */
    public Status getStatusById(long statusId, Long list_id, boolean directOnly, boolean alsoPersist, boolean fromDbOnly) {
        Status status = null;

        if (!directOnly) {
            String obj  = tweetDB.getStatusObjectById(accountId, statusId, list_id);
            if (obj!=null) {
                status = materializeStatus(obj);
                if (status!=null)
                    return status;
            }
        }

        if (fromDbOnly)
            return null;

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
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return status;
    }

    public List<Status> getThreadForStatus(long startid) {
        List<String> jsons = tweetDB.getThreadForStatus(accountId, startid);
        List<Status> ret = new ArrayList<Status>(jsons.size());
        for (String json : jsons) {
            Status status = materializeStatus(json);
            if (!ret.contains(status)) // Filter duplicates, as we search over all timelines
                ret.add(status);
        }

        Collections.sort(ret,new Comparator<Status>() {
            @Override
            public int compare(Status status1, Status status2) {
                return status2.getCreatedAt().compareTo(status1.getCreatedAt());
            }
        });
        return ret;
    }

    public List<Status> getRepliesToStatus(long statusId) {
        List<Status> ret = new ArrayList<Status>();
        List<String> replies = tweetDB.getReplies(accountId, statusId);
        for (String reply : replies) {
            Status status = materializeStatus(reply);
            ret.add(status);
        }
        return ret;
    }

    /**
     * Search the status table for statuses of the current user
     * that match the passed query entry. If the query starts with
     * 'from:', statuses from the passed user are looked up
     * @param query Query [from:user |�searchTerm ]
     * @return Statuses found or an empty list.
     */
    public List<Status> searchStatues(String query) {
        Log.i("searchStatuses", "Query= " + query);

        String what=null;
        if (query.contains(":")) {
            int pos = query.indexOf(':');
            what = query.substring(0, pos);
            query = query.substring(pos +1);
        }

        List<String> jsons = tweetDB.searchStatuses(accountId, query);
        List<Status> ret = new ArrayList<Status>(jsons.size());
        String qtl = query.toLowerCase();
        for (String s : jsons ) {
            Status st = materializeStatus(s);
            if (what!=null) {
                if ("from".equalsIgnoreCase(what)) {  // Todo limit options
                    if (st.getUser().getName().contains(qtl) && !ret.contains(st))
                        ret.add(st);
                    if (st.getUser().getScreenName().contains(qtl) && !ret.contains(st))
                        ret.add(st);
                }
            } else {
                if (st.getText().toLowerCase().contains(qtl) && !ret.contains(st))
                   ret.add(st);
            }
        }

        return ret;
    }


    /**
     * Retrieve a User object for the passed userId.
     * @param userId Id of the user to look up
     * @param cachedOnly If true, only a cached version of the object or null is returned. Otherwise a request to the server is made
     * @return User object or null, if it can not be obtained.
     */
    public User getUserById(long userId, boolean cachedOnly) {
        User user = null;
        boolean existing = false;

        try {
            String userJson = tweetDB.getUserById(accountId, userId);
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
                tweetDB.insertUser(accountId, userId,userJson, user.getScreenName());
            else
                tweetDB.updateUser(accountId, userId,userJson);

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
            String userJson = tweetDB.getUserByName(accountId, screenName);
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
                tweetDB.insertUser(accountId, user.getId(),userJson, user.getScreenName());
            else
                tweetDB.updateUser(accountId, user.getId(),userJson);

        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
        }
        return user;
    }


    /**
     * Send a request to follow or unfollow a user
     * @param userId Id of the user to follow
     * @param doFollow If true, send a follow request; unfollow otherwise.
     */
    public void followUnfollowUser(long userId, boolean doFollow ) throws TwitterException {
        if (doFollow)
            twitter.createFriendship(userId);
        else
            twitter.destroyFriendship(userId);

    }

    /**
     * Add a user to some of our user lists
     * @param userId User to add
     * @param listId ids of the list to put the user on
     */
    public void addUserToLists(long userId, int listId) throws TwitterException {
        twitter.createUserListMember(listId, userId);
    }

    /**
     * Remove a user to some of our user lists
     * @param userId User to add
     * @param listId ids of the list to put the user on
     */
    public void removeUserFromList(long userId, int listId) throws TwitterException {
        twitter.destroyUserListMember(listId, userId);
    }

    public List<Status> getUserTweets(Long userId) {

        Paging paging = new Paging();
        paging.setCount(30);
        List<Status> ret;
        try {
            ret = twitter.getUserTimeline(userId, paging);
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return Collections.emptyList();
        }
        return ret;
    }

    public void persistStatus(Collection<Status> statuses, long list_id) {


        List<ContentValues> values = new ArrayList<ContentValues>(statuses.size());
        long now = System.currentTimeMillis();
        List<String> urls = new ArrayList<String>();
        for (Status status : statuses) {
            String json = DataObjectFactory.getRawJSON(status);
            ContentValues cv = new ContentValues(6);
            cv.put("ID", status.getId());
            cv.put("I_REP_TO", status.getInReplyToStatusId());
            cv.put("LIST_ID", list_id);
            cv.put("ACCOUNT_ID",accountId);
            cv.put("STATUS", json);
            cv.put("ctime", now);
            values.add(cv);
//            urls.addAll(parseUrls(status.getText()));
/*
            if (status.getURLEntities()!=null) {
                for (URLEntity ue: status.getURLEntities()) {
                    urls.add(ue.getExpandedURL().toString());
                }
            }
*/
        }
        tweetDB.storeValues(TweetDB.TABLE_STATUSES,values);
        Thread urlFetchThread = new Thread(new ExpandUrlRunner(context,urls));
        urlFetchThread.start();

    }

    private List<String> parseUrls(String text) {
        List<String> ret = new ArrayList<String>();

        String[] tokens = text.split(" ");
        for (String token : tokens) {
            if (token.startsWith("http://t.co")) {
                ret.add(token);
            }
        }
        return ret;
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
     */
    private void updateStatus(Status status){

        // Serialize and then store in DB
        String json = DataObjectFactory.getRawJSON(status);
        tweetDB.updateStatus(accountId, status.getId(), json);
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
     * Upload a picture to a remote picture service like yfrog and post it to twitter.
     *
     * @param fileName Path on file system to the picture
     * @param message Message of the update
     * @return Url where this was stored on the remote service or null on error
     */
    public String postPicture(String fileName, String message) {

        try {
            File file = new File(fileName);
            MediaProvider mProvider =getMediaProvider();

            String accessTokenToken = account.getAccessTokenKey();
            String accessTokenSecret = account.getAccessTokenSecret();

            if (accessTokenSecret == null || accessTokenToken == null) {
                BugSenseHandler.sendEvent("postPicture: Token was null for account " + account);
                return null;
            }

            Properties props = new Properties();
            props.put(PropertyConfiguration.MEDIA_PROVIDER,mProvider);
            props.put(PropertyConfiguration.OAUTH_ACCESS_TOKEN,accessTokenToken);
            props.put(PropertyConfiguration.OAUTH_ACCESS_TOKEN_SECRET,accessTokenSecret);
            props.put(PropertyConfiguration.OAUTH_CONSUMER_KEY, Tokens.consumerKey);
            props.put(PropertyConfiguration.OAUTH_CONSUMER_SECRET, Tokens.consumerSecret);
            props.put(PropertyConfiguration.HTTP_READ_TIMEOUT,240*1000); // 4mins
            Configuration conf = new PropertyConfiguration(props);

            ImageUploadFactory factory = new ImageUploadFactory(conf);
            ImageUpload upload = factory.getInstance(mProvider);
            String url;
            url = upload.upload(file,message);
            return url;
        } catch (TwitterException e) {
            BugSenseHandler.sendExceptionMessage("postPicture", getMediaProvider().name().toString(), e);
        }
        return null;
    }

    MediaProvider getMediaProvider() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String provider = preferences.getString("pictureService","yfrog");

        MediaProvider mProvider ;
        if (provider.equals("yfrog"))
            mProvider = MediaProvider.YFROG;
        else if (provider.equals("twitpic"))
            mProvider = MediaProvider.TWITPIC;
        else if (provider.equals("twitter"))
            mProvider = MediaProvider.TWITTER;
        else
            throw new IllegalArgumentException("Picture provider " + provider + " unknown");

        return mProvider;
    }

    /**
     * Determine if the current user is following the passed one
     * @param userId id of the user to verify
     * @return true if we are following, false otherwise
     */
    public boolean areWeFollowing(Long userId) {

        try {
            long myId = twitter.getId();
            Relationship rel = twitter.showFriendship(myId,userId);
            return rel.isSourceFollowingTarget();
        } catch (TwitterException e) {
            e.printStackTrace();  // TODO: Customise this generated block
            return false;
        }
    }

    /**
     * Reports the passed user as spammer
     * @param userId id of the user
     */
    public void reportAsSpammer(long userId) throws TwitterException {
        twitter.reportSpam(userId);
    }

    public List<SavedSearch> getSavedSearchesFromServer() {

        List<SavedSearch> searches;
        try {
            searches = twitter.getSavedSearches();
        } catch (TwitterException e) {
            searches = Collections.emptyList();
        }

        return searches;  // TODO: Customise this generated block
    }

    public List<SavedSearch> getSavedSearchesFromDb() {
        List<String> jsons = tweetDB.getSavedSearches(accountId);
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
        List<String> jsons = tweetDB.getUsers(accountId);
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

    public MetaList<Status> getSavedSearchesTweets(int searchId, boolean fromDbOnly, Paging paging) {

        List<SavedSearch> searches = getSavedSearchesFromDb();
        for (SavedSearch search : searches) {
            if (search.getId()==searchId) {
                String queryString = search.getQuery();
                Query query = new Query(queryString); // TODO paging - probably not needed as default is 15
                   // TODO set some restriction like language or such
                try {
                    QueryResult queryResult = twitter.search(query);
                    List<Status> tweets = queryResult.getTweets();
                    MetaList metaList = new MetaList(tweets,tweets.size(),0);
                    return metaList;
                } catch (TwitterException e) {
                    e.printStackTrace();  // TODO: Customise this generated block
                    return new MetaList<Status>();
                }
            }
        }

        return new MetaList<Status>();
    }

    public void persistSavedSearch(SavedSearch search) {
        String json = DataObjectFactory.getRawJSON(search);

        tweetDB.storeSavedSearch(accountId, search.getName(),search.getQuery(),search.getId(),json);
    }

    public void markStatusesAsOld(Set<Long> ids) {
        tweetDB.addReadIds(accountId,ids);
    }

    public List<Long> getReadIds(List<Long> idsToCheck) {
        return tweetDB.getReads(accountId,idsToCheck);
    }

    public void deleteStatus(long id) throws TwitterException {
        twitter.destroyStatus(id);

        // TODO remove from tweetdb see also OneTweetActivity.deleteStatus()
    }
}