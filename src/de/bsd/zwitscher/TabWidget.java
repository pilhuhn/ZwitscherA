package de.bsd.zwitscher;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bsd.zwitscher.helper.PicHelper;
import twitter4j.SavedSearch;
import twitter4j.UserList;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import twitter4j.json.DataObjectFactory;

public class TabWidget extends TabActivity {

    static final String LIST_ID = "list_id";
    TabHost tabHost;
	TabHost.TabSpec homeSpec;
    ProgressBar pg;
    TextView titleTextBox;
    ComponentName sComponentName;
    int accountId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent().setClass(this,TweetFetchService.class);
        sComponentName = startService(serviceIntent);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.tabs);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        titleTextBox = (TextView) findViewById(R.id.title_msg_box);

        accountId = 0; // TODO select correct account

		Resources res = getResources();
		tabHost = getTabHost();
		Intent homeIntent = new Intent().setClass(this,TweetListActivity.class);
        homeIntent.putExtra(LIST_ID, 0);

        String tmp = getString(R.string.home_timeline);
        homeSpec = tabHost.newTabSpec("tmp")
				.setIndicator(tmp,res.getDrawable(R.drawable.ic_tab_home))
				.setContent(homeIntent);
		tabHost.addTab(homeSpec);

		Intent mentionsIntent = new Intent().setClass(this,TweetListActivity.class);
		mentionsIntent.putExtra(LIST_ID, -1);

        tmp= getString(R.string.mentions);
		homeSpec = tabHost.newTabSpec("mentions")
				.setIndicator(tmp,res.getDrawable(R.drawable.ic_tab_mention))
				.setContent(mentionsIntent);
		tabHost.addTab(homeSpec);

        tmp = getString(R.string.direct);
        Intent directIntent = new Intent().setClass(this,TweetListActivity.class);
        directIntent.putExtra(LIST_ID, -2);
		homeSpec = tabHost.newTabSpec("directs")
				.setIndicator(tmp,res.getDrawable(R.drawable.ic_tab_direct))
				.setContent(directIntent);
		tabHost.addTab(homeSpec);

        tmp = getString(R.string.list);
        Intent listsIntent = new Intent().setClass(this,ListOfListsActivity.class);
        listsIntent.putExtra("list",0);
        homeSpec = tabHost.newTabSpec("lists")
                .setIndicator(tmp,res.getDrawable(R.drawable.ic_tab_list))
                .setContent(listsIntent);
        tabHost.addTab(homeSpec);


        Intent searchIntent = new Intent().setClass(this,ListOfListsActivity.class);
        searchIntent.putExtra("list",1);
        tmp = getString(R.string.searches);
        homeSpec = tabHost.newTabSpec("searches")
                .setIndicator(tmp,res.getDrawable(R.drawable.ic_tab_search))
                .setContent(searchIntent);
        tabHost.addTab(homeSpec);

		tabHost.setCurrentTab(0); // Home tab, tabs start at 0

        new InitialSyncTask(this).execute(accountId);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		 MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.main_menu, menu);
		    return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.preferences:
	    	i = new Intent(TabWidget.this, Preferences.class);
			startActivity(i);
			break;
	    case R.id.reloadLists:
	  		syncLists();
	  		break;
        case R.id.DevelResetLastRead:
            resetLastRead();
            break;
        case R.id.DevelCleanTweets:
            cleanTweetDB();
            break;
        case R.id.DevelCleanImages:
            cleanImages();
            break;
        case R.id.DevelStopServices:
            Intent serviceIntent = new Intent().setClass(this,TweetFetchService.class);
            stopService(serviceIntent);
            break;
        case R.id.helpMenu:
            i = new Intent(TabWidget.this, HelpActivity.class);
            startActivity(i);
            break;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	    return true;
	}

	/**
	 * Synchronize lists between what is available in the db
	 * and on twitter.
	 */
	private void syncLists() {
		TwitterHelper th = new TwitterHelper(getApplicationContext());
        TweetDB tdb = new TweetDB(this,accountId);
		List<UserList> userLists = th.getUserLists();
		Map<String,Integer> storedLists = tdb.getLists();
		// Check for lists to add
		for (UserList userList : userLists) {
			if (!storedLists.containsValue(userList.getId())) {
				tdb.addList(userList.getName(),userList.getId(), DataObjectFactory.getRawJSON(userList));
			}
		}
		// check for outdated lists and remove them
		for (Entry<String, Integer> entry : storedLists.entrySet()) {
			Integer id = entry.getValue();
			boolean found = false;
			for (UserList userList2 : userLists) {
				if (userList2.getId() == id) {
					found = true;
					break;
				}
			}
			if (!found) {
				tdb.removeList(id);
			}
		}

        syncSearches(th,tdb);
	}

    private void syncSearches(TwitterHelper th, TweetDB tdb) {
        List<SavedSearch> searches = th.getSavedSearchesFromServer();
        List<SavedSearch> storedSearches = th.getSavedSearchesFromDb();

        for (SavedSearch search : searches) {
            if (!storedSearches.contains(search)) {
                th.persistSavedSearch(search);
            }
        }

        for (SavedSearch search : storedSearches) {
            if (!searches.contains(search)) {
                tdb.deleteSearch(search.getId());
            }
        }

    }



    private void resetLastRead() {
        TweetDB tb = new TweetDB(this,accountId);
        tb.resetLastRead();
    }

    private void cleanTweetDB() {
        TweetDB tb = new TweetDB(this,accountId);
        tb.cleanTweets();
    }

    private void cleanImages() {
        PicHelper ph = new PicHelper();
        ph.cleanup();
    }

    /**
     * Helper class that triggers syncing of lists and searches
     * at start when both are empty.
     */
    private class InitialSyncTask extends AsyncTask<Integer,Void,Void> {

        private Context context;

        private InitialSyncTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(Integer... params) {
            int accountId = params[0];

            TweetDB tdb = new TweetDB(context,accountId);
            if (tdb.getLists().size()==0 && tdb.getSavedSearches().size()==0)
                syncLists();

            return null;
        }
    }
}
