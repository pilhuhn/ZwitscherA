package de.bsd.zwitscher;

import android.app.ListActivity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Just display a Conversation ..
 *
 * @author Heiko W. Rupp
 */
public class ThreadListActivity extends ListActivity {

    ProgressBar pg;
    TextView titleTextBox;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        // Set the layout of the list activity
        setContentView(R.layout.tweet_list_layout);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
        pg = (ProgressBar) findViewById(R.id.title_progress_bar);
        titleTextBox = (TextView) findViewById(R.id.title_msg_box);


        ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
        backButton.setVisibility(View.VISIBLE);
        // Disable the reload button
        ImageButton reloadButton = (ImageButton) findViewById(R.id.tweet_list_reload_button);
        reloadButton.setVisibility(View.GONE);

        Intent i = getIntent();

        new GetConversationTask().execute(i);

    }



    void fillConversation(Intent i) {

        List<Status> result = new ArrayList<Status>();

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


    private class GetConversationTask extends AsyncTask<Intent,Void,Void> {

        @Override
        protected Void doInBackground(Intent... params) {
            Intent i = params[0];
            fillConversation(i);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pg.setVisibility(ProgressBar.INVISIBLE);
            titleTextBox.setText("");
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            pg.setVisibility(ProgressBar.VISIBLE);
            String s = getString(R.string.getting_conversation)+ "...";
            titleTextBox.setText(s);

        }
    }
}
