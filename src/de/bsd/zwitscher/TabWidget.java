package de.bsd.zwitscher;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.account.AccountStuffActivity;
import de.bsd.zwitscher.helper.CleanupTask;
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

/**
 * Activity that creates the Tab bar and starts the various
 * activities on the tabs. Also hosts the main menu.
 */
public class TabWidget extends TabActivity {

    static final String LIST_ID = "list_id";
    static final long SEVEN_DAYS = 7 * 86400 * 1000L;
    TabHost tabHost;
	TabHost.TabSpec homeSpec;
    ProgressBar pg;
    TextView titleTextBox;
    int accountId;
    Account account;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Log.i("TabWidget","onCreate");
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.tabs);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        titleTextBox = (TextView) findViewById(R.id.title_msg_box);

        account = AccountHolder.getInstance().getAccount();
        accountId = account.getId();
        Log.i("TabWidget","Account=" + account);

        setupTabs();

		tabHost.setCurrentTab(0); // Home tab, tabs start at 0

        new InitialSyncTask(this).execute(accountId);
	}

    protected void onResume() {
        super.onResume();

        Log.i("TabWidget","onResume");
        Account tmp = AccountHolder.getInstance().getAccount();
        if (!tmp.equals(account)) {
            // New account, so re-setup tabs
            tabHost.clearAllTabs();
            account = tmp;
            setupTabs();
        }
        Log.i("TabWidget","Account=" + account);
        titleTextBox.setText(account.getAccountIdentifier());
    }

    private void setupTabs() {
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
                .setIndicator(tmp, res.getDrawable(R.drawable.ic_tab_mention))
                .setContent(mentionsIntent);
        tabHost.addTab(homeSpec);

        tmp = getString(R.string.direct);
        Intent directIntent = new Intent().setClass(this,TweetListActivity.class);
        directIntent.putExtra(LIST_ID, -2);
        homeSpec = tabHost.newTabSpec("directs")
                .setIndicator(tmp, res.getDrawable(R.drawable.ic_tab_direct))
                .setContent(directIntent);
        tabHost.addTab(homeSpec);

        tmp = getString(R.string.sent);
        Intent sentIntent = new Intent().setClass(this,TweetListActivity.class);
        sentIntent.putExtra(LIST_ID, -3);
        homeSpec = tabHost.newTabSpec("sent")
                .setIndicator(tmp, res.getDrawable(R.drawable.ic_tab_sent)) 
                .setContent(sentIntent);
        tabHost.addTab(homeSpec);

        tmp = getString(R.string.favorites);
        Intent favsIntent = new Intent().setClass(this,TweetListActivity.class);
        favsIntent.putExtra(LIST_ID, -4);
        homeSpec = tabHost.newTabSpec("favs")
                .setIndicator(tmp, res.getDrawable(R.drawable.ic_tab_favorite))
                .setContent(favsIntent);
        tabHost.addTab(homeSpec);

        if (account.getServerType().equalsIgnoreCase("twitter")) {
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
                    .setIndicator(tmp, res.getDrawable(R.drawable.ic_tab_search))
                    .setContent(searchIntent);
            tabHost.addTab(homeSpec);
        }
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
	    	i = new Intent(this, Preferences.class);
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
        case R.id.AccountStuff:
            i = new Intent(this, AccountStuffActivity.class);
            startActivity(i);
            break;
        case R.id.helpMenu:
            i = new Intent(TabWidget.this, HelpActivity.class);
            startActivity(i);
            break;
        case R.id.menu_cleanTweets:
            new CleanupTask(this).execute();
            break;
        case R.id.DevelDumpAccounts:
            TweetDB tmpDb = new TweetDB(this,-1);
            List<Account> allAccounts = tmpDb.getAccountsForSelection();
            for (Account a : allAccounts)
                System.out.println(a);
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
		TwitterHelper th = new TwitterHelper(this, account);
        TweetDB tdb = new TweetDB(this,accountId);
        if (account.getServerType().equalsIgnoreCase("twitter")) {
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
        tb.cleanTweetDB();
    }

    private void cleanImages() {
        PicHelper ph = new PicHelper();
        long now = System.currentTimeMillis();
        ph.cleanup(now - SEVEN_DAYS);
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
