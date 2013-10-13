package de.bsd.zwitscher;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.account.AccountNavigationListener;
import de.bsd.zwitscher.account.AccountStuffActivity;
import de.bsd.zwitscher.account.LoginActivity;
import de.bsd.zwitscher.helper.CleanupTask;
import de.bsd.zwitscher.helper.FetchTimelinesService;

import java.util.ArrayList;
import java.util.List;

/**
 * New main activity that takes care of the main UI.
 *
 * @author Heiko W. Rupp
 */
public class MainActivity extends Activity {

    private ListView mDrawerList;
    private Account account;
    private int accountId;
    private List<Account> accountList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ProgressBar progressItem;
    private Menu menu;
    private int listId;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.main_drawer_layout);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

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
        Log.i("TabWidget", "Account=" + account);


        ActionBar actionBar = getActionBar();

        // So that the actionbar toggle thingy can work
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        getAccountNames(); // Initialize accountList
        // We want the account list in the action bar for easy switching
        if (accountList.size()>1) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

            SpinnerAdapter accountSpinnerAdapter = getAccountSpinnerAdapter();
            actionBar.setListNavigationCallbacks(accountSpinnerAdapter,
                    new AccountNavigationListener(this, accountList, account));
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

        final CharSequence mTitle  = account.getAccountIdentifier();
        final CharSequence mDrawerTitle  = "Select...";
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);


        // Set up the drawer TODO needs to be synchronized with account change
        TwitterHelper th = new TwitterHelper(this,account);
        TweetDB tdb = TweetDB.getInstance(this);
        List<ZUserList> lists = tdb.getLists(account.getId());
        List<ZUserList> items = new ArrayList<ZUserList>(lists.size()+3);
        items.addAll(ZUserList.generateDefaults(account));
        items.addAll(lists);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new DrawerLineAdapter(this, items, account));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

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

        ////////////// OnNavigationListener and Spinnerssetup - mostly copied from AccountStuffActivity. TODO unite that again
    public SpinnerAdapter getAccountSpinnerAdapter() {
        List<String> data = getAccountNames();

        SpinnerAdapter adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,data);

        return adapter;
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            ZUserList zul = (ZUserList) ((ListView)parent).getAdapter().getItem(position);
            listId = zul.listId;
            selectItem(zul);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(ZUserList zul) {
        // Create a new fragment and specify the planet to show based on position
        TweetListFragment fragment = new TweetListFragment();
        fragment.setAccount(account);
        fragment.setListId(zul.listId);

        Bundle args = new Bundle();
//        args.putInt(PlanetFragment.ARG_PLANET_NUMBER, position);
        fragment.setArguments(args);

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                       .replace(R.id.content_frame, fragment)
                       .commit();

        // Highlight the selected item, update the title, and close the drawer
//        mDrawer.setItemChecked(position, true);
//        setTitle(mPlanetTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem item = menu.findItem(R.id.ProgressBar);
        progressItem = (ProgressBar) item.getActionView();
        progressItem.setVisibility(ProgressBar.INVISIBLE);

        this.menu = menu;

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
   //	  		syncLists();
//               new SyncSLTask(this).execute();
   	  		break;
           case R.id.DevelResetLastRead:
//               resetLastRead();
               break;
           case R.id.DevelCleanTweets:
//               cleanTweetDB();
               break;
           case R.id.DevelCleanImages:
//               cleanImages();
               break;
           case R.id.AccountStuff:
               i = new Intent(this, AccountStuffActivity.class);
               startActivity(i);
               break;
           case R.id.helpMenu:
               i = new Intent(MainActivity.this, HelpActivity.class); // TODO Fragment
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
//               if (listActivity!=null)
//                   listActivity.reload(null);
               Intent fetcher = new Intent(this, FetchTimelinesService.class);
               fetcher.putExtra("account",account);
               fetcher.putExtra("listIds",new int[] { listId});
               startService(fetcher);
               break;
           case R.id.send:
               i = new Intent(this,NewTweetActivity.class);
               startActivity(i);
               break;
           case R.id.to_top:
//               if (listActivity!=null)
//                   listActivity.scrollToTop(null);
               break;
           case R.id.menu_feedback:
//               send_feedback();
               break;
           case R.id.menu_goto_user:
//               displayUserInfo();
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


}