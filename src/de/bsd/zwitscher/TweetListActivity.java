package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import android.widget.Toast;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.FlushQueueTask;
import de.bsd.zwitscher.helper.MetaList;
import de.bsd.zwitscher.helper.NetworkHelper;
import de.bsd.zwitscher.other.TweetMarkerSync;
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
@Deprecated
public class TweetListActivity extends AbstractListActivity implements AbsListView.OnScrollListener,
        OnItemClickListener, OnItemLongClickListener {

    List<Status> statuses;
    List<DirectMessage> directs;
    List<Status> tweets;
    int list_id;
    ListView lv;
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
        if (!(theParent instanceof TabWidget)) {
            // We have no enclosing TabWidget, so we need to request progress thingy
            requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }

        // Set the layout of the list activity
        setContentView(R.layout.tweet_list_layout);

        lv = getListView();
        lv.setItemsCanFocus(false);
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

        // Only get tweets from db to speed things up at start
        boolean fromDbOnly = tdb.getLastRead(account.getId(), list_id) != -1;

        // Check if we are switching accounts and the user wants to load messages from remote
        AccountHolder accountHolder = AccountHolder.getInstance(this);
        if (accountHolder.isSwitchingAccounts()) {
            accountHolder.setSwitchingAccounts(false); // reset flag

            // Ok, we are switching. Now check preferences
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean loadOnAccountChange = preferences.getBoolean("load_on_account_switch",false);

            if (loadOnAccountChange) { // preferences say ok to load
                fromDbOnly = false;
            }
        }

        // Start loading messages to be displayed
        fillListViewFromTimeline(fromDbOnly);
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
     * for general items or a reply for a direct message
     * @param parent Parent view
     * @param view Clicked view
     * @param position Position in the list that was clicked
     * @param id row Id of the item that was clicked
     */
    public void onItemClick(AdapterView<?> parent, View view,
            int position, long id) {

        if (statuses!=null) {
         Intent i = new Intent(this,OneTweetActivity.class);
            if (statuses.size()>=position) {
                Status value = statuses.get(position);
                i.putExtra("status", value); // NON-NLS
                startActivity(i);
            }
            else {
                Toast.makeText(this,"Item at position " + position + " not found. Please refresh list",Toast.LENGTH_LONG);
            }

        }
        else if (directs!=null) {
           Intent i = new Intent(this, NewTweetActivity.class);
           i.putExtra("user",directs.get(position).getSender());
           i.putExtra("op","Directs");
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
                if (list.size()<=unreadCount)
                    unreadCount = list.size()-1;
                if (unreadCount > -1)
                    last = list.get(unreadCount).getId();
            }

        	break;
        }

        long newLast=-1;
        // Update the 'since' id in the database
    	if (myStatuses.getList().size()>0) {
    		newLast = myStatuses.getList().get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
    		tdb.updateOrInsertLastRead(account.getId(), listId, newLast);
    	}

        // Sync with TweetMarker

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean doTweetMarkerSync = prefs.getBoolean("tweetmarker_sync",false);

        long newLast2=-1;
        if (doTweetMarkerSync && listId>=0 && !account.isStatusNet() && !fromDbOnly) {
            if (listId==0)
                newLast2 = TweetMarkerSync.syncFromTweetMarker("timeline", account.getName());
            else
                newLast2 = TweetMarkerSync.syncFromTweetMarker("lists."+listId, account.getName());

            if (newLast2>newLast) {
                tdb.updateOrInsertLastRead(account.getId(), listId, newLast2);
            } else {
                if (listId==0)
                    TweetMarkerSync.syncToTweetMarker("timeline",newLast,account.getName(),th.getOAuth());
                else
                    TweetMarkerSync.syncToTweetMarker("lists."+listId,newLast,account.getName(),th.getOAuth());
            }
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

        if (newLast2>last) {
            metaList.oldLast=newLast2;
            // the read status from remote is newer than the last read locally, so lets mark those in between as read
            Set<Long> ids = new HashSet<Long>(statuses.size());
            for (Status s : statuses) {
                long id = s.getId();
                if (id>last) {
//                    th.markStatusAsOld(id);
                    ids.add(id);
                }
            }
            th.markStatusesAsOld(ids);
        }
        else {
            metaList.oldLast = last;
        }

        for (Status status:metaList.getList()) {
            AccountHolder accountHolder = AccountHolder.getInstance(this);
            accountHolder.addUserName(status.getUser().getScreenName());
            if (status.getHashtagEntities()!=null) {
                for (HashtagEntity hte : status.getHashtagEntities()) {
                    accountHolder.addHashTag(hte.getText());
                }
            }
        }

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
                    String expUrl = ue.getExpandedURL();
                    if (expUrl != null) {
                        m = filterPattern.matcher(expUrl);
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

    private MetaList<Status> getSavedSearchFromTwitter(int searchId, boolean fromDbOnly) {
        MetaList<Status> messages;

        Paging paging = new Paging();
        paging.setCount(20);

        messages = th.getSavedSearchesTweets(searchId, fromDbOnly,paging);

        tweets = messages.getList();

        return messages;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        Activity theParent = getParent();
        if (!(theParent instanceof TabWidget)) {

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_activity_menu_honey,menu);

            ActionBar actionBar = this.getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            return true;
        }
        else {
            return theParent.onCreateOptionsMenu(menu);
        }
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
                Log.i("TweetListActivity","Unknown item " + item);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (getListAdapter() instanceof StatusAdapter) {
            StatusAdapter adapter = (StatusAdapter) getListAdapter();
            th.markStatusesAsOld(adapter.newOlds);
        }
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

        // TODO this is not really executed in parallel and seems to block the update of the
        // TODO main timeline
        // TODO the main timeline updates quicky and terminates when teh spinner thingy hides
        // TODO but if the next code is also executed, the timeline is only refreshed after
        // TODO this is all done
        // TODO this may be a fight for the tweets table ?

        NetworkHelper networkHelper = new NetworkHelper(this);
        if (list_id == 0 &&  networkHelper.mayReloadAdditional()) {
            System.out.println("### triggering GTLT -1");
            new GetTimeLineTask(this,-1,false, 5).execute(false);
            System.out.println("### triggering GTLT -2");
            new GetTimeLineTask(this,-2,false, 3).execute(false);
            System.out.println("### triggering done");
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


        // TODO if this is the very first load of a timeline, loadMore will not reveal
        // new items and just create load -- in this case skip it
        // TODO introduce a flag for this. Can perhaps be populated from the
        // knowledge of the stored last read id of the list

//        Log.d("onScroll:","loadMore= " + loadMore + " f=" + firstVisible + ", vc=" + visibleCount + ", tc=" +totalCount);
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
                        if (messages.size()>0) {
                            for (DirectMessage direct : messages) {
                                sta.insert(direct, totalCount + i);
                                directs.add(direct);
                                i++;
                            }
                            sta.notifyDataSetChanged();
                        }
                    } else if (item instanceof Status) {
                        Status last = (Status) item;
                        if (statuses==null)
                            statuses = new ArrayList<Status>();

                        List<Status> newStatuses = th.getStatuesFromDb(last.getId(),7,list_id);
                        // TODO add checking for old
                        if (newStatuses.size()>0) {
                            List<Long> readIds = obtainReadIds(newStatuses);
                            sta.readIds.addAll(readIds);
                            for (Status status : newStatuses ) {
                                if (!matchesFilter(status)) {
                                    sta.insert(status, totalCount + i);
                                    statuses.add(status);
                                    i++;
                                }
                            }
                            sta.notifyDataSetChanged();
                        }
                    }
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
            String s = getString(R.string.getting_statuses, account.getStatusType());
            if (updateListAdapter) {
                if (progressBar!=null) {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            }
            if(titleTextBox!=null) {
                titleTextBox.setText(s);
            }
            ActionBar ab = getActionBar();
            if (ab!=null) {
                ab.setSubtitle(s);
            }

            if (getParent()!=null) {
                getParent().setProgressBarIndeterminateVisibility(true);
            }
            else {
                // No parent tab bar when used on a list
                setProgressBarIndeterminateVisibility(true);
            }
        }


		@Override
        @SuppressWarnings("unchecked")
		protected MetaList doInBackground(Boolean... params) {

            fromDbOnly = params[0];

            if (!fromDbOnly) {
                NetworkHelper networkHelper = new NetworkHelper(context);
                if (!networkHelper.isOnline()) {
                    // User wants stuff from the server, but we are offline
                    // so "fail fast"
                    fromDbOnly=true;
                }
            }

            if (startDelaySecs>0 && !fromDbOnly) {
                try {
                    Thread.sleep(1000L*startDelaySecs);
                } catch (InterruptedException e) {
                    Log.d("GetTimeLineTask",e.getMessage());
                }
            }


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

            if (getListAdapter()!=null && getListAdapter().getCount()>0 && result.getNumAdded()==0) {

                // No new items, no need to replace the current adapter
                updateListAdapter=false;

            }


            if (updateListAdapter) {
                if (listId <-4) { // saved search
                    setListAdapter(new TweetAdapter(context, account, R.layout.tweet_list_item, result.getList()));
                }
                else if (listId == -2) { // direct messages
                    setListAdapter(new StatusAdapter(context, account, R.layout.tweet_list_item,
                                                result.getList(),0, new ArrayList<Long>()));
                }
                else { // all others
                    List<twitter4j.Status> statusList = (List<twitter4j.Status>) result.getList();
                    // Get the old adapter if it existed, get the read ids from it and persist them
                    if (getListAdapter()!=null) {
                        StatusAdapter oldOne = (StatusAdapter) getListAdapter();
                        Set<Long> newReadIds = oldOne.newOlds;
                        th.markStatusesAsOld(newReadIds);

                    }

                    List<Long> reads = obtainReadIds(statusList);
                    setListAdapter(new StatusAdapter(context, account, R.layout.tweet_list_item,
                            result.getList(), result.oldLast, reads));
                }

                if (result.getList().size()==0) {
                    Toast.makeText(context, getString(R.string.no_result), Toast.LENGTH_LONG).show();
                }
            }

            if (titleTextBox!=null)
                titleTextBox.setText(account.getAccountIdentifier());
            ActionBar ab = getActionBar();
            if (ab!=null) {
                String s=null;
                List<ZUserList> userLists = tdb.getLists(account.getId());
                for (ZUserList zul : userLists) {
                    if (zul.listId == userListId) {
                        s = zul.getDisplayName(account);
                    }
                }
                ab.setTitle(account.getAccountIdentifier());
                ab.setSubtitle(s);
            }
            if (updateListAdapter)
	            getListView().requestLayout();
            if (result.getNumOriginal()>0) { // TODO distinguish the timelines
                String tmp = context.getResources().getQuantityString(R.plurals.new_entries,result.getNumOriginal());
                String updating;
                switch (listId) {
                    case 0: updating = context.getString(R.string.home_timeline);
                        break;
                    case -1: updating = context.getString(R.string.mentions);
                        break;
                    case -2 : updating = context.getString(R.string.direct);
                        break;
                    case -3: updating = context.getString(R.string.sent);
                        break;
                    case -4: updating = context.getString(R.string.favorites);
                        break;
                    default: updating = context.getString(R.string.list);
                }
                if (listId!=-2) { // direct messages produce too many false positives
                    Toast.makeText(context,updating + ": " + tmp,Toast.LENGTH_SHORT).show();
                }
            }

            if (updateListAdapter) {
                // Only do the next if we actually did an update from twitter
                if (!fromDbOnly) {
                    Log.i("GTLTask", " scroll to " + result.getNumOriginal());
                    // TODO modify to scroll to last-read position
                    int position = result.getNumOriginal() - 1;
                    if (position>0) {
                        getListView().setSelection(position);
                    }
                }
                else if (unreadCount>1) {
                    getListView().setSelection(unreadCount-1);
                }
            }
            if (progressBar !=null) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
            }

            if (getParent()!=null) {
                getParent().setProgressBarIndeterminateVisibility(false);
            } else {
                // A list has no TabWdget as parent
                setProgressBarIndeterminateVisibility(false);
            }

		}

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if(titleTextBox!=null)
                titleTextBox.setText(updating +" "+ values[0] + "...");
            ActionBar ab = getActionBar();
            if (ab!=null)
                ab.setSubtitle(updating +" "+ values[0] + "...");
        }
    }

    private List<Long> obtainReadIds(List<Status> statusList) {
        List<Long> idsToCheck = new ArrayList<Long>(statusList.size());
        for ( Status status: statusList) {
            idsToCheck.add(status.getId());
        }
        return th.getReadIds(idsToCheck);
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
