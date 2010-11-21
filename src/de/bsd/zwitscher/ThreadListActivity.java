package de.bsd.zwitscher;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Just display a Conversation ..
 *
 * @author Heiko W. Rupp
 */
public class ThreadListActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout of the list activity
        setContentView(R.layout.tweet_list_layout);
        ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
        backButton.setVisibility(View.VISIBLE);
        // Disable the reload button
        ImageButton reloadButton = (ImageButton) findViewById(R.id.tweet_list_reload_button);
        reloadButton.setVisibility(View.GONE);


        List<Status> result = new ArrayList<Status>();

        Intent i = getIntent();
        Bundle b = i.getExtras();
        long id = 0;
        if (b!=null)
            id = b.getLong("startId");

        TwitterHelper th = new TwitterHelper(this);

        Status status = th.getStatusById(id,null, false, true) ;
        while (status!=null) {
            List<Status> replies = th.getRepliesToStatus(status.getId());
            for (Status reply : replies) {
                if (!result.contains(reply))
                    result.add(reply);
            }
            result.add(status);

            long inReplyToStatusId = status.getInReplyToStatusId();
            if (inReplyToStatusId!=-1)
                status = th.getStatusById(inReplyToStatusId,null, false, true);
            else
                status=null;
        }

        setListAdapter(new StatusAdapter<twitter4j.Status>(this, R.layout.tweet_list_item, result));

        ListView lv = getListView();
        lv.requestLayout();

    }

    /**
     * Called from the Back button
     * @param v
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

    /**
     * Scrolls to top, called from the ToTop button
     * @param v
     */
    @SuppressWarnings("unused")
    public void scrollToTop(View v) {
        getListView().setSelection(0);
    }

    /**
     * Called from the post button
     * @param v
     */
    @SuppressWarnings("unused")
    public void post(View v) {
        Intent i = new Intent(this, NewTweetActivity.class);
        startActivity(i);
    }

}
