package de.bsd.zwitscher;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.bsd.zwitscher.helper.FlushQueueTask;
import de.bsd.zwitscher.helper.MetaList;
import de.bsd.zwitscher.helper.NetworkHelper;
import twitter4j.*;

/**
 * Show the list of tweets.
 * To unify things a bit, we introduce pseudo list ids for timelines that are not lists:
 * <ul>
 * <li>0 : home/friends timeline</li>
 * <li>-1 : mentions </li>
 * <li>-2 : direct </li>
 * <li>&gt;0 : saved search</li>
 * </ul>
 * @author Heiko W. Rupp
 */
public class TweetListActivity extends AbstractListActivity implements AbsListView.OnScrollListener,
        OnItemClickListener, OnItemLongClickListener {

    List<Status> statuses;
    List<DirectMessage> directs;
    List<Tweet> tweets;
    int list_id;
    ListView lv;
    int newMentions=0;
    private int newDirects=0;
    Long userId=null;
    int userListId = -1;
    private Pattern filterPattern;
    int unreadCount = -1;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity theParent = getParent();
        if ((!(theParent instanceof TabWidget)) && (android.os.Build.VERSION.SDK_INT<11)) {
            // We have no enclosing TabWidget, so we need to request the custom title
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        }

        // Set the layout of the list activity
        if (Build.VERSION.SDK_INT<11)
            setContentView(R.layout.tweet_list_layout);
        else
            setContentView(R.layout.tweet_list_layout_honeycomb);


        // Get the windows progress bar from the enclosing TabWidget
        if ((!(theParent instanceof TabWidget)) && (android.os.Build.VERSION.SDK_INT<11)) {
            // We have no enclosing TabWidget, so we need our window here
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
            progressBar = (ProgressBar) findViewById(R.id.title_progress_bar);
            titleTextBox = (TextView) findViewById(R.id.title_msg_box);


            ImageButton imageButton = (ImageButton) findViewById(R.id.back_button);
            imageButton.setVisibility(View.VISIBLE);
        }
        lv = getListView();
        lv.setOnScrollListener(this);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this); // Directly got to reply

        Bundle intentInfo = getIntent().getExtras();
        if (intentInfo==null) {
            list_id = 0;
        } else {
            userListId = intentInfo.getInt("userListid");
            list_id = intentInfo.getInt(TabWidget.LIST_ID);
            if (intentInfo.containsKey("userId")) {
                // Display tweets of a single user
                userId = intentInfo.getLong("userId");
                // This is a one off list. So don't offer the reload button
                ImageButton tweet_list_reload_button = (ImageButton) findViewById(R.id.tweet_list_reload_button);
                if (tweet_list_reload_button!=null)
                    tweet_list_reload_button.setVisibility(View.INVISIBLE);
            }
            if (intentInfo.containsKey("unreadCount")) {
                unreadCount = intentInfo.getInt("unreadCount");
            }
        }

        boolean fromDbOnly = tdb.getLastRead(account.getId(), list_id) != -1;
        fillListViewFromTimeline(fromDbOnly); // Only get tweets from db to speed things up at start
    }

    @Override
    public void onResume() {
     	super.onResume();

        lv = getListView();
        lv.setOnScrollListener(this);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this); // Directly got to reply

        String msg = setupFilter();
        if (msg!=null)
            Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }

    /**
     * Handle click on a list item which triggers the detail view of that item
     * @param parent Parent view
     * @param view Clicked view
     * @param position Position in the list that was clicked
     * @param id row Id of the item that was clicked
     */
    public void onItemClick(AdapterView<?> parent, View view,
            int position, long id) {

        if (statuses!=null) {
         Intent i = new Intent(this,OneTweetActivity.class);
         i.putExtra(getString(R.string.status), statuses.get(position));
         startActivity(i);
        }
        else if (directs!=null) {
           Intent i = new Intent(this, NewTweetActivity.class);
           i.putExtra("user",directs.get(position).getSender());
           i.putExtra("op",getString(R.string.direct));
           startActivity(i);
        } else {
            // Tweets; TODO
        }
    }

    /**
     * Handle a long click on a list item - by directly jumping into reply mode
     * @param parent Parent view
     * @param view Clicked view
     * @param position Position in the List that was clicked
     * @param id row id of the item that was clicked
     * @return true as the click was consumed
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        Log.i("TLA","Long click, pos=" + position + ",id="+id);
        Intent i = new Intent(this, NewTweetActivity.class);
        if (statuses!=null) {
           i.putExtra(getString(R.string.status), statuses.get(position));
           i.putExtra("op",getString(R.string.reply));
           startActivity(i);
        }
        else if (directs!=null) {
         i.putExtra("user",directs.get(position).getSender());
         i.putExtra("op",getString(R.string.direct));
         startActivity(i);
        } else {
            // Tweets TODO
        }


        return true; // We've consumed the long click
    }


    /**
     * Retrieve a list of statuses. Depending on listId, this is taken from
     * different sources:
     * <ul>
     * <li>0 : home timeline</li>
     * <li>-1 : mentions</li>
     * <li>>0 : User list</li>
     * </ul>
     * This method may trigger a network call if fromDbOnly is false.
     * The filter if not null is a regular expression, that if matches filters the
     * tweet.
     *
     *
     *
     * @param fromDbOnly If true only statuses already in the DB are returned
     * @param listId Id of the list / timeline to fetch (see above)
     * @param updateStatusList Should the currently displayed list be updated?
     * @return List of status items along with some counts
     */
	private MetaList<Status> getTimlinesFromTwitter(boolean fromDbOnly, int listId,
                                                    boolean updateStatusList) {
		Paging paging = new Paging();

		MetaList<Status> myStatuses;


    	long last = tdb.getLastRead(account.getId(), listId);
    	if (last>0 )//&& !Debug.isDebuggerConnected())
    		paging.sinceId(last).setCount(200);
        else
            paging.setCount(50); // 50 Tweets if we don't have the timeline yet

        switch (listId) {
        case 0:
            // Home time line
        	myStatuses = th.getTimeline(paging,listId, fromDbOnly);

        	break;
        case -1:
        	myStatuses = th.getTimeline(paging, listId, fromDbOnly);
        	break;
        case -2:
            // see below at getDirectsFromTwitter
            myStatuses = new MetaList<Status>();
            break;
        case -3:
            myStatuses = th.getTimeline(paging,listId,fromDbOnly);
            break;
        case -4:
            myStatuses = th.getTimeline(paging,listId,fromDbOnly);
            break;
        default:
        	myStatuses = th.getUserList(paging,listId, fromDbOnly, unreadCount);
            if (unreadCount>-1) {
                List<Status> list = myStatuses.getList();
                    last = list.get(unreadCount).getId();
            }

        	break;
        }

        // Update the 'since' id in the database
    	if (myStatuses.getList().size()>0) {
    		long newLast = myStatuses.getList().get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
    		tdb.updateOrInsertLastRead(account.getId(), listId, newLast);
    	}

        MetaList<Status> metaList;
        if (updateStatusList) {
            statuses = new ArrayList<Status>();
            List<Status> data = new ArrayList<Status>(myStatuses.getList().size());
            if (filterPattern==null) {
                setupFilter(); // TODO report errors?
            }
            for (Status status : myStatuses.getList()) {
                boolean shouldFilter = matchesFilter(status);
                if (shouldFilter) {
                    Log.i("TweetListActivity::filter, filtered ",status.getUser().getScreenName() + " - " + status.getText());
                } else {
                    data.add(status);
                    statuses.add(status);
                }
            }
            metaList = new MetaList<Status>(data,myStatuses.getNumOriginal(),myStatuses.getNumAdded());
        }
        else {
            metaList = new MetaList<Status>(new ArrayList<Status>(),0,0);
        }

        metaList.oldLast = last;

        return metaList;
	}

    /**
     * Does the passed status match the filter pattern?
     * This method checks the status text and the expanded url entities
     * @param status Status to check
     * @return True if the filter expression matches, false otherwise
     */
    private boolean matchesFilter(Status status) {
        boolean shouldFilter = false;
        if (filterPattern != null) {
            Matcher m = filterPattern.matcher(status.getText());
            if (m.matches())
                shouldFilter = true;

            if (status.getURLEntities() != null) {
                for (URLEntity ue : status.getURLEntities()) {
                    URL expUrl = ue.getExpandedURL();
                    if (expUrl !=null && expUrl.toString()!=null ) {
                        m = filterPattern.matcher(expUrl.toString());
                        if (m.matches())
                            shouldFilter = true;
                    }
                }
            }
        }
        return shouldFilter;
    }

    private MetaList<DirectMessage> getDirectsFromTwitter(boolean fromDbOnly) {
        MetaList<DirectMessage> messages;


        long last = tdb.getLastRead(account.getId(), -2);
        Paging paging = new Paging();
        if (last>-1)
            paging.setSinceId(last);


        messages = th.getDirectMessages(fromDbOnly, paging);
        directs = messages.getList();

        return messages;
    }

    private MetaList<Tweet> getSavedSearchFromTwitter(int searchId, boolean fromDbOnly) {
        MetaList<Tweet> messages;

        Paging paging = new Paging();
        paging.setCount(20);

        messages = th.getSavedSearchesTweets(searchId, fromDbOnly,paging);

        tweets = messages.getList();

        return messages;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        Activity theParent = getParent();
        if ((!(theParent instanceof TabWidget) && Build.VERSION.SDK_INT>=11)) {

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_activity_menu_honey,menu);

            ActionBar actionBar = this.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            return true;
        }
        else if (theParent instanceof TabWidget) {
            return theParent.onCreateOptionsMenu(menu);
        }

        return false;

    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.refresh:
                reload(null);
                break;
            case R.id.to_top:
                scrollToTop(null);
                break;
            case R.id.send:
                post(null);
                break;
            default:
                Log.i(getClass().getName(),"Unknown item " + item);

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called from the reload button
     * @param v view that was pressed
     */
    @SuppressWarnings("unused")
    public void reload(View v) {
        fillListViewFromTimeline(false);
        // Now check and process items that were created while we were offline
        new FlushQueueTask(this, account).execute();

        NetworkHelper networkHelper = new NetworkHelper(this);
        if (list_id == 0 &&  networkHelper.mayReloadAdditional()) {
            new GetTimeLineTask(this,-1,false, 1).execute(false);
            new GetTimeLineTask(this,-2,false, 2).execute(false);
        }
    }


    private void fillListViewFromTimeline(boolean fromDbOnly) {
    	new GetTimeLineTask(this,list_id,true, 0).execute(fromDbOnly);
    }

    public void onScrollStateChanged(AbsListView absListView, int i) {
        // nothing to do for us
    }

    @SuppressWarnings("unchecked")
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = /* maybe add a padding */
            firstVisible + visibleCount >= totalCount-1;


//        Log.d("onScroll:","loadMore f=" + firstVisible + ", vc=" + visibleCount + ", tc=" +totalCount);
        if(loadMore) {
//Debug.startMethodTracing("list" + firstVisible);
            ListAdapter adapter = absListView.getAdapter();
            if (adapter instanceof StatusAdapter) {
                StatusAdapter sta = (StatusAdapter) adapter;
                if (totalCount>0) {
                    Object item = sta.getItem(totalCount - 1);
                    int i = 0;
                    if (item instanceof  DirectMessage) {
                        DirectMessage message = (DirectMessage) item;

                        List<DirectMessage> messages = th.getDirectsFromDb(message.getId(),7);
                        for (DirectMessage direct : messages) {
                            sta.insert(direct, totalCount + i);
                            directs.add(direct);
                            i++;
                        }
                    } else if (item instanceof Status) {
                        Status last = (Status) item;
                        if (statuses==null)
                            statuses = new ArrayList<Status>();

                        List<Status> newStatuses = th.getStatuesFromDb(last.getId(),7,list_id);
                        for (Status status : newStatuses ) {
                            if (!matchesFilter(status)) {
                                sta.insert(status, totalCount + i);
                                statuses.add(status);
                                i++;
                            }
                        }
                    }
                    sta.notifyDataSetChanged();
                }
            }
//Debug.stopMethodTracing();
        }
    }

    private class GetTimeLineTask extends AsyncTask<Boolean, String, MetaList> {

        boolean fromDbOnly = false;
        String updating;
        Context context;
        private int listId;
        Dialog dialog = null;
        private boolean updateListAdapter;
        private int startDelaySecs;

        /**
         * Fetch a timeline from db and/or remote. Allow to delay this a bit so that the
         * progress bar etc. from earlier fetch tasks can be displayed.
         * @param context Calling activity
         * @param listId  What timeline to fetch
         * @param updateListAdapter Should the adapter be updated - true for current tab, false for others
         * @param startDelaySecs How long to delay before the network fetch is performed.
         */
        private GetTimeLineTask(Context context, int listId, boolean updateListAdapter, int startDelaySecs) {
            this.context = context;
            this.listId = listId;
            this.updateListAdapter = updateListAdapter;
            this.startDelaySecs = startDelaySecs;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updating = context.getString(R.string.updating);
            String s = getString(R.string.getting_tweets)+ "...";
            if (updateListAdapter) {
                if (progressBar!=null)
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                else {
                    dialog = new Dialog(context);
                    dialog.setTitle(s);
                    dialog.setCancelable(false);
                    dialog.show();
                }
            }
            if(titleTextBox!=null) {
                titleTextBox.setText(s);
            }
            if (Build.VERSION.SDK_INT>=11) {
                ActionBar ab = getActionBar();
                if (ab!=null)
                    ab.setSubtitle(s);
            }
        }


		@Override
        @SuppressWarnings("unchecked")
		protected MetaList doInBackground(Boolean... params) {
            if (startDelaySecs>0) {
                try {
                    Thread.sleep(1000L*startDelaySecs);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }

            fromDbOnly = params[0];
	        MetaList data;
            if (userId!=null) {
                List<twitter4j.Status> statuses = th.getUserTweets(userId);
                data = new MetaList(statuses,statuses.size(),0);
                String user = context.getString(R.string.user);
                publishProgress(user);
            }
            else {
                String directsString = context.getString(R.string.direct);
                if (this.listId >-5 && this.listId !=-2) {
                    String updating;
                    switch (listId) {
                        case 0: updating = context.getString(R.string.home_timeline);
                            break;
                        case -1: updating = context.getString(R.string.mentions);
                            break;
                        case -3: updating = context.getString(R.string.sent);
                            break;
                        case -4: updating = context.getString(R.string.favorites);
                            break;
                        default: updating = "";
                    }
                    publishProgress(updating);
                    data = getTimlinesFromTwitter(fromDbOnly, listId, updateListAdapter);
                }
                else if (listId ==-2) {
                    publishProgress(directsString);
                    data = getDirectsFromTwitter(fromDbOnly);
                }
                else { // list id < -4 ==> saved search
                    String s = context.getString(R.string.searches);
                    publishProgress(s);
                    data = getSavedSearchFromTwitter(-listId,fromDbOnly);
                }
            }
	        return data;
		}

        @SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(MetaList result) {
            if (updateListAdapter) {
                if (listId <-4)
                    setListAdapter(new TweetAdapter(context, account, R.layout.tweet_list_item, result.getList()));
                else
                    setListAdapter(new StatusAdapter(context, account, R.layout.tweet_list_item, result.getList(), result.oldLast));

                if (result.getList().size()==0) {
                    Toast.makeText(context, "Got no result from the server", Toast.LENGTH_LONG).show();
                }
            }

            if (titleTextBox!=null)
                titleTextBox.setText(account.getAccountIdentifier());
            if (Build.VERSION.SDK_INT>=11) {
                ActionBar ab = getActionBar();
                if (ab!=null) {
                    String s=null;
                    Map<Integer,Pair<String,String>> userLists = tdb.getLists(account.getId());
                    if (userListId !=-1) {
                        Pair<String,String> nameOwnerPair = userLists.get(userListId);
                        if (nameOwnerPair!=null) {
                            String tmp;
                            if (nameOwnerPair.second.equals(account.getName()))
                                tmp = nameOwnerPair.first;
                            else
                                tmp = "@" +nameOwnerPair.second + "/" + nameOwnerPair.first;
                            s =tmp;
                        }
                    }
                    ab.setTitle(account.getAccountIdentifier());
                    ab.setSubtitle(s);
                }
            }
            if (updateListAdapter)
	            getListView().requestLayout();
            if (result.getNumOriginal()>0) {
                Toast.makeText(context,result.getNumOriginal() + " new items",Toast.LENGTH_SHORT).show();
            }
            if (newMentions>0) {
                String s = getString(R.string.new_mentions);
                Toast.makeText(context,newMentions + " " + s,Toast.LENGTH_LONG).show();
                newMentions=0;
            }
            if (newDirects>0) {
                String s = getString(R.string.new_directs);
                Toast.makeText(context,newDirects + " " + s,Toast.LENGTH_LONG).show();
                newDirects=0;
            }


            // Only do the next if we actually did an update from twitter
            if (!fromDbOnly && updateListAdapter) {
                Log.i("GTLTask", " scroll to " + result.getNumOriginal());
                getListView().setSelection(result.getNumOriginal() - 1);
            }
            if (progressBar !=null)
                progressBar.setVisibility(ProgressBar.INVISIBLE);
            if (dialog!=null)
                dialog.cancel();

		}

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if(titleTextBox!=null)
                titleTextBox.setText(updating +" "+ values[0] + "...");
            if (Build.VERSION.SDK_INT>=11) {
                ActionBar ab = getActionBar();
                if (ab!=null)
                    ab.setSubtitle(updating +" "+ values[0] + "...");
            }

        }
    }


    /**
     * Set up a filter for tweets. The entries from the filter list are joined together to e.g.
     *  ".*(http://4sq.com/|http://shz.am/).*"
     * @return Error message on issue with pattern, null otherwise
     */
    private String setupFilter() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String exp = prefs.getString("filter",null);
        if (exp==null) {
            filterPattern = null;
            return null;
        }
        String ret=".*(" + exp.replaceAll(",","|") + ").*";

        Log.i("TweetListActivity::getFilter()","Filter is " + ret);
        try {
            filterPattern = Pattern.compile(ret);
        } catch (PatternSyntaxException e) {
            String tmp = getString(R.string.invalid_filter,e.getLocalizedMessage());
            Log.e("setupFilter",tmp);
            return tmp;
        }
        return null;
    }

}
