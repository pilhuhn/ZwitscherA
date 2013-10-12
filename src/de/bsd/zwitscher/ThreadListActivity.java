package de.bsd.zwitscher;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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

    private ProgressBar pg;
    private TextView titleTextBox;
    private Context thisActivity;
    private List<Status> statuses;
    private Account account;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        thisActivity = this;

        setContentView(R.layout.tweet_list_layout);


        account = AccountHolder.getInstance(this).getAccount();
        // Pull the start id ( = id of the calling s
        Intent i = getIntent();
        Bundle b = i.getExtras();
        long startId = 0;
        if (b!=null)
            startId = b.getLong("startId");

        new GetConversationTask().execute(startId);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (getListAdapter() instanceof StatusAdapter) {
            StatusAdapter adapter = (StatusAdapter) getListAdapter();
            TwitterHelper th = new TwitterHelper(this,account);
            th.markStatusesAsOld(adapter.newOlds);
        }
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.thread_activity_menu,menu);
        pg = (ProgressBar) menu.findItem(R.id.ProgressBar).getActionView();

        ActionBar actionBar = this.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.to_top:
                getListView().setSelection(0);
                break;
            default:
                Log.e("ThreadListActivity", "Unknown item " + item.getItemId());

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle click on a list item which triggers the detail view of that item
     * @param l Parent view
     * @param v Clicked view
     * @param position Position in the list that was clicked
     * @param id Id of the item clicked
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
     * @param v Button that was clicked
     */
    @SuppressWarnings("unused")
    public void done(View v) {
        finish();
    }

    /**
     * Scrolls to top, called from the ToTop button
     * @param v Button that was clicked
     */
    @SuppressWarnings("unused")
    public void scrollToTop(View v) {
        getListView().setSelection(0);
    }

    /**
     * Called from the post button
     * @param v Button that was clicked
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

            List<twitter4j.Status> result ;

            TwitterHelper th = new TwitterHelper(ThreadListActivity.this, account);
            result = th.getThreadForStatus(i);
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if (pg!=null)
                pg.setVisibility(ProgressBar.VISIBLE);
            if (titleTextBox!=null) {
                String s = getString(R.string.getting_conversation)+ "...";
                titleTextBox.setText(s);
            }

        }

        @Override
        protected void onPostExecute(List<twitter4j.Status> statusList) {
            if (pg!=null)
                pg.setVisibility(ProgressBar.INVISIBLE);
            if (titleTextBox!=null)
                titleTextBox.setText("");

            List<Long> readIds = obtainReadIds(statusList);
            setListAdapter(new StatusAdapter<twitter4j.Status>(thisActivity, account, R.layout.tweet_list_item, statusList,0, readIds));
            statuses = statusList;

            ListView lv = getListView();
            lv.requestLayout();
        }
    }

    // TODO move to common helper, as this is copied from TweetListActivity
    private List<Long> obtainReadIds(List<Status> statusList) {
        TwitterHelper th = new TwitterHelper(ThreadListActivity.this, account);
        List<Long> idsToCheck = new ArrayList<Long>(statusList.size());
        for ( Status status: statusList) {
            idsToCheck.add(status.getId());
        }
        return th.getReadIds(idsToCheck);
    }

}
