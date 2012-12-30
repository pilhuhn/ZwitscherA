package de.bsd.zwitscher;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.FetchTimelinesService;

import java.util.ArrayList;
import java.util.List;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class MainActivity extends Activity implements ActionBar.OnNavigationListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        setContentView(R.layout.main);

        ActionBar actionBar = getActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS|ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
/*
        List<String> accounts = new ArrayList<String>();
        accounts.add("pilhuhn@twitter");
        accounts.add("pilhuhn@identi.ca");
        accounts.add("rhq_project@twitter");
        SpinnerAdapter accountAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accounts);
        actionBar.setListNavigationCallbacks(accountAdapter,this);
*/

        ActionBar.Tab tab = actionBar.newTab();
        tab.setText(R.string.home_timeline);
        tab.setTabListener(new MainTabListener<TweetListFragment>(this,"home",TweetListFragment.class));
        actionBar.addTab(tab);

        tab = actionBar.newTab();
        tab.setText(R.string.mentions);
        tab.setTabListener(new MainTabListener<TweetListFragment>(this,"mentions",TweetListFragment.class));
        actionBar.addTab(tab);

        tab = actionBar.newTab();
        tab.setText(R.string.direct);
        tab.setTabListener(new MainTabListener<TweetListFragment>(this,"directs",TweetListFragment.class));
        actionBar.addTab(tab);

        tab = actionBar.newTab();
        tab.setText(R.string.list);
        tab.setTabListener(new MainTabListener<TweetListFragment>(this,"lists",TweetListFragment.class)); // TODO list of lists fragment
        // TODO add listener
        actionBar.addTab(tab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main_menu, menu);
        ActionBar actionBar = getActionBar();
//        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

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

            case R.id.refresh:
                i = new Intent(this, FetchTimelinesService.class);
                i.putExtra("listIds", new int[]{0,1,3});
                i.putExtra("account",AccountHolder.getInstance().getAccount());
                startService(i);
                break;

           // TODO add other menu options
           }

        return true;
    }
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        return false;  // TODO: Customise this generated block
    }

}