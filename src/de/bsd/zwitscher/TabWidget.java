package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.account.AccountNavigationListener;
import de.bsd.zwitscher.account.AccountStuffActivity;
import de.bsd.zwitscher.account.LoginActivity;
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

/**
 * Activity that creates the Tab bar and starts the various
 * activities on the tabs. Also hosts the main menu.
 */
@Deprecated
public class TabWidget extends TabActivity  {

    static final String LIST_ID = "list_id";
    private TabHost tabHost;
    ProgressBar pg;
    TextView titleTextBox;
    private int accountId;
    private Account account;
    private AbstractListActivity listActivity;
    private List<Account> accountList;
    private Menu menu;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        Log.i("TabWidget","onCreate");

        account = AccountHolder.getInstance(this).getAccount();

        // Account should be non-null. If it is null, no default account is available,
        // so user did not go through the login procedure
        if (account==null) {
            // Still null -> initial login failed
            Intent i = new Intent().setClass(this, LoginActivity.class);
            startActivity(i);
            finish();
            return;
        }

        accountId = account.getId();
        Log.i("TabWidget","Account=" + account);


        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.tabs);
        ActionBar actionBar = getActionBar();

        getAccountNames(); // Initialize accountList
        // We want the account list in the action bar for easy switching
        if (accountList.size()>1) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

            SpinnerAdapter accountSpinnerAdapter = getAccountSpinnerAdapter();
            // We need a separate class for the callback, as othewise we would pull in the ActionBar class
            // and would thus not work on Android 2.2
            actionBar.setListNavigationCallbacks(accountSpinnerAdapter, new AccountNavigationListener(this, accountList, account));
            // Don't show the title, as the account list already shows that data
            actionBar.setDisplayShowTitleEnabled(false);

            for (int i = 0; i< accountList.size(); i++) {
                if (accountList.get(i).equals(account)) {
                    actionBar.setSelectedNavigationItem(i);
                }
            }
        }
        else {
            getActionBar().setTitle(account.getAccountIdentifier());
        }



        setupTabs();

		tabHost.setCurrentTab(0); // Home tab, tabs start at 0

        new InitialSyncTask(getApplicationContext()).execute(accountId);
	}

    protected void onResume() {
        super.onResume();

        Log.i("TabWidget","onResume");
        Account tmp = AccountHolder.getInstance(this).getAccount();
        if (!tmp.equals(account)) {
            // New account, so re-setup tabs
            tabHost.clearAllTabs();
            account = tmp;
            setupTabs();
        }
        Log.i("TabWidget","Account=" + account);
        if (titleTextBox!=null)
            titleTextBox.setText(account.getAccountIdentifier());
    }

    private void setupTabs() {
        Resources res = getResources();
        tabHost = getTabHost();
        Intent homeIntent = new Intent().setClass(this,TweetListActivity.class);
        homeIntent.putExtra(LIST_ID, 0);

        String tmp = getString(R.string.home_timeline);
        TabHost.TabSpec homeSpec = tabHost.newTabSpec("tmp")
                .setIndicator(tmp, res.getDrawable(R.drawable.ic_tab_home))
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


        if (account.getServerType()== Account.Type.TWITTER) {
            tmp = getString(R.string.list);
            Intent listsIntent = new Intent().setClass(this,ListOfListsActivity.class);
            listsIntent.putExtra("list",0);
            homeSpec = tabHost.newTabSpec("lists")
                    .setIndicator(tmp,res.getDrawable(R.drawable.ic_tab_list))
                    .setContent(listsIntent);
            tabHost.addTab(homeSpec);
        }

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


        if (account.getServerType()== Account.Type.TWITTER) {

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
        MenuItem item = menu.findItem(R.id.ProgressBar);
        pg = (ProgressBar) item.getActionView();
        pg.setVisibility(ProgressBar.INVISIBLE);

        this.menu = menu;

        return true;
	}

    public void setInnerActivity(AbstractListActivity listActivity) {

        this.listActivity = listActivity;
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
//	  		syncLists();
            new SyncSLTask(this).execute();
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
            TweetDB tmpDb = TweetDB.getInstance(getApplicationContext());
            List<Account> allAccounts = tmpDb.getAccountsForSelection(false);
            for (Account a : allAccounts)
                System.out.println(a);
            break;
        /// below are Honeycomb ActionBar items
        case R.id.refresh:
            // forward to the inner list's reload/referesh
            if (listActivity!=null)
                listActivity.reload(null);
            break;
        case R.id.send:
            i = new Intent(this,NewTweetActivity.class);
            startActivity(i);
            break;
        case R.id.to_top:
            if (listActivity!=null)
                listActivity.scrollToTop(null);
            break;
        case R.id.menu_feedback:
            send_feedback();
            break;
        case R.id.menu_goto_user:
            displayUserInfo();
            break;
        case R.id.menu_default_search:
            onSearchRequested();
            break;
	    default:
            System.out.println("Unknown option " + item.toString());
	        return super.onOptionsItemSelected(item);
	    }
	    return true;
	}

    private void displayUserInfo() {
        final Dialog dialog = new Dialog(this);
        dialog.setTitle(R.string.goto_user);
        dialog.setContentView(R.layout.edit_text_dialog);
        final EditText text = (EditText) dialog.findViewById(R.id.dialog_edit_text);
        Button okButton = (Button) dialog.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = text.getText().toString();
                Intent i = new Intent(TabWidget.this, UserDetailActivity.class);
                i.putExtra("userName", user);
                startActivity(i);
                dialog.dismiss();
            }
        });
        Button cancelButton = (Button) dialog.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    private void send_feedback() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        String text = "Device: " + Build.MODEL + "\n" + "OS-Version: " + Build.VERSION.RELEASE + "\n\n";
        i.putExtra(Intent.EXTRA_TEXT,text);
        i.putExtra(Intent.EXTRA_SUBJECT, "Zwitscher Feedback");
        i.putExtra(Intent.EXTRA_EMAIL,new String[] {"hwr@pilhuhn.de"});

        startActivity(i);

    }

    ////////////// OnNavigationListener and Spinnerssetup - mostly copied from AccountStuffActivity. TODO unite that again
    public SpinnerAdapter getAccountSpinnerAdapter() {
        List<String> data = getAccountNames();

        SpinnerAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,data);

        return adapter;
    }

    private List<String> getAccountNames() {
        TweetDB tdb = TweetDB.getInstance(getApplicationContext());
        accountList = tdb.getAccountsForSelection(false);
        List<String> data = new ArrayList<String>(accountList.size());
        for (Account account : accountList) {
            String identifier = account.getAccountIdentifier();
            data.add(identifier);
        }
        return data;
    }

    void showHideAbMenuItems(boolean show) {

        if (menu!=null) {
            menu.findItem(R.id.to_top).setVisible(show);
            menu.findItem(R.id.refresh).setVisible(show);
        }
}

    private class SyncSLTask extends AsyncTask<Void,Void,Void> {

        private Context context;

        private SyncSLTask(Context context) {
            this.context = context;
        }

        ProgressDialog dialog;
        protected void onPostExecute(Void aVoid) {
            dialog.hide();
            dialog.cancel();

        }

        protected void onPreExecute() {
            dialog = new ProgressDialog(context);
            dialog.setIndeterminate(true);
            dialog.setTitle("Syncing...");
            dialog.setCancelable(false);
            dialog.show();

        }

        protected Void doInBackground(Void... voids) {
            syncLists();
            return null;
        }
    }
	/**
	 * Synchronize lists between what is available in the db
	 * and on twitter.
	 */
	private void syncLists() {
		TwitterHelper th = new TwitterHelper(this, account);
        TweetDB tdb = TweetDB.getInstance(getApplicationContext());
        if (account.getServerType()== Account.Type.TWITTER) {
            List<UserList> userLists = th.getUserListsFromServer();
            List<ZUserList> storedLists = tdb.getLists(accountId);
            List<Integer> storedListIds = new ArrayList<Integer>(storedLists.size());


/* TODO re-implement
            // Check for lists to add
            for (UserList userList : userLists) {
                if (!storedListIds.contains(userList.getId())) {
                    tdb.addList(accountId, userList.getName(),userList.getId(), userList.getUser().getScreenName());
                }
            }
            // check for outdated lists and remove them
            for (Entry<Integer, Pair<String, String>> entry : storedLists.entrySet()) {
                Integer id = entry.getKey();
                boolean found = false;
                for (UserList userList2 : userLists) {
                    if (userList2.getId() == id) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    tdb.removeList(id, account.getId());
                }
            }
            syncSearches(th,tdb);
*/
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
                tdb.deleteSearch(account.getId(), search.getId());
            }
        }

    }



    private void resetLastRead() {
        TweetDB tb = TweetDB.getInstance(getApplicationContext());
        tb.resetLastRead();
    }

    private void cleanTweetDB() {
        TweetDB tb = TweetDB.getInstance(getApplicationContext());
        tb.cleanTweetDB();
    }

    private void cleanImages() {
        PicHelper ph = new PicHelper();
        long now = System.currentTimeMillis();
        ph.cleanup(now);
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

            TweetDB tdb = TweetDB.getInstance(context);
            if (tdb.getLists(accountId).size()==0 && tdb.getSavedSearches(account.getId()).size()==0)
                syncLists();

            return null;
        }
    }
}
