package de.bsd.zwitscher;

import java.util.List;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
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



        Intent intent = getIntent();

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            List<Status> statusList = th.searchStatues(query);
            StatusAdapter<Status> adapter = new StatusAdapter<Status>(this,account,R.layout.tweet_list_item,statusList);
            getListView().setAdapter(adapter);
        }
    }


    @Override
    public void reload(View v) {
        // Nothing to do for now.
    }
}