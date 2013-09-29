package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import twitter4j.Status;

/**
 * Activity that shows search results
 * @author Heiko W. Rupp
 */
public class MySearchActivity extends AbstractListActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tweet_list_layout);

        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            ActionBar actionBar = getActionBar();
            actionBar.setTitle(account.getAccountIdentifier());
            String queryIs = getString(R.string.query_is);
            actionBar.setSubtitle(queryIs + ": " + query);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);

            List<Status> statusList = th.searchStatues(query);
            StatusAdapter<Status> adapter = new StatusAdapter<Status>(this,account,R.layout.tweet_list_item,statusList, 0,new ArrayList<Long>());
            getListView().setAdapter(adapter);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        else
            return super.onOptionsItemSelected(item);
    }

    @Override
    public void reload(View v) {
        // Nothing to do for now.
    }
}