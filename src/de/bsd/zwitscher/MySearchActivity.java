package de.bsd.zwitscher;

import java.util.List;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import twitter4j.Status;

/**
 * Activity that shows search results
 * @author Heiko W. Rupp
 */
public class MySearchActivity extends AbstractListActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT<11) {
            setContentView(R.layout.tweet_list_layout);

            ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
            backButton.setEnabled(true);
            backButton.setVisibility(View.VISIBLE);
            ImageButton reloadButton = (ImageButton) findViewById(R.id.tweet_list_reload_button);
            reloadButton.setEnabled(false);
            reloadButton.setVisibility(View.GONE);
            ImageButton postButton = (ImageButton) findViewById(R.id.post_button);
            postButton.setEnabled(false);
            postButton.setVisibility(View.GONE);

        } else {
            setContentView(R.layout.tweet_list_layout_honeycomb);
        }

        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (Build.VERSION.SDK_INT>=11) {
                ActionBar actionBar = getActionBar();
                actionBar.setTitle(account.getAccountIdentifier());
                String queryIs = getString(R.string.query_is);
                actionBar.setSubtitle(queryIs + ": " + query);
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);

            }
            List<Status> statusList = th.searchStatues(query);
            StatusAdapter<Status> adapter = new StatusAdapter<Status>(this,account,R.layout.tweet_list_item,statusList);
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