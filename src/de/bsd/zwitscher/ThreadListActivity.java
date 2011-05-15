package de.bsd.zwitscher;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
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
    Context thisActivity;
    List<Status> statuses;
    private Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisActivity = this;

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

        account = AccountHolder.getInstance().getAccount();
        // Pull the start id ( = id of the calling s
        Intent i = getIntent();
        Bundle b = i.getExtras();
        long startId = 0;
        if (b!=null)
            startId = b.getLong("startId");

        new GetConversationTask().execute(startId);

    }

    /**
     * Do the magic of getting the coversation starting with the status with
     * id <i>statusId</i>.
     * @param statusId Id of the status to start with
     * @return List of Status that are involved in the conversation (may not be complete).
     */
    List<Status> getConversation(long statusId) {

        List<Status> result = new ArrayList<Status>();

        TwitterHelper th = new TwitterHelper(this, account);

        Status status = th.getStatusById(statusId,null, false, true) ;
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

        return result;
    }

    /**
     * Handle click on a list item which triggers the detail view of that item
     * @param l Parent view
     * @param v Clicked view
     * @param position Position in the list that was clicked
     * @param id
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (statuses!=null) {
            Intent i = new Intent(this,OneTweetActivity.class);
            i.putExtra(getString(R.string.status), statuses.get(position));
            startActivity(i);
        }
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

    /**
     * Task to get the conversation in the background
     */
    private class GetConversationTask extends AsyncTask<Long,Void,List<twitter4j.Status>> {

        @Override
        protected List<twitter4j.Status> doInBackground(Long... params) {
            Long i = params[0];
            return getConversation(i);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pg.setVisibility(ProgressBar.VISIBLE);
            String s = getString(R.string.getting_conversation)+ "...";
            titleTextBox.setText(s);

        }

        @Override
        protected void onPostExecute(List<twitter4j.Status> statusList) {
            pg.setVisibility(ProgressBar.INVISIBLE);
            titleTextBox.setText("");

            setListAdapter(new StatusAdapter<twitter4j.Status>(thisActivity, account, R.layout.tweet_list_item, statusList));
            statuses = statusList;

            ListView lv = getListView();
            lv.requestLayout();
        }
    }
}
