package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

/**
 * Show the list of tweets.
 * To unify things a bit, we introduce pseudo list ids for timelines that are not lists:
 * <ul>
 * <li>0 : home/friends timeline</li>
 * <li>-1 : mentions </li>
 * <li>-2 : direct </li>
 * </ul>
 * @author Heiko W. Rupp
 */
public class TweetListActivity extends ListActivity implements AbsListView.OnScrollListener {

    List<Status> statuses;
    Bundle intentInfo;
    TweetListActivity thisActivity;
    ProgressBar pg;
    TextView titleTextBox;
    int list_id;
    TweetDB tdb;
    TwitterHelper th;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.tweet_list_layout);

        intentInfo = getIntent().getExtras();
        thisActivity = this;
        // Get the windows progress bar from the enclosing TabWidget
        // TODO parent is not the TabWidget in case of user lists
        Activity theParent = getParent();
        if (theParent instanceof TabWidget) {
            TabWidget parent = (TabWidget) theParent;
            pg = parent.pg;
            titleTextBox = parent.titleTextBox;
        }
        else {
            ImageButton imageButton = (ImageButton) findViewById(R.id.back_button);
            imageButton.setVisibility(View.VISIBLE);

        }
        tdb = new TweetDB(this);
        th = new TwitterHelper(thisActivity);


        intentInfo = getIntent().getExtras();
        if (intentInfo==null) {
            list_id = 0;
        } else {
            list_id = intentInfo.getInt(TabWidget.LIST_ID);
        }

        boolean fromDbOnly = tdb.getLastRead(list_id)!=-1 ? true : false;
        fillListViewFromTimeline(fromDbOnly); // Only get tweets from db to speed things up at start
    }

    @Override
    public void onResume() {

    	super.onResume();


        // Get the windows progress bar from the enclosing TabWidget
        // TODO parent is not necessarily the TabWidget
//        TabWidget parent = (TabWidget) this.getParent();
//        pg = parent.pg;

		ListView lv = getListView();
        lv.setOnScrollListener(this);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Intent i = new Intent(parent.getContext(),OneTweetActivity.class);
				i.putExtra(getString(R.string.status), statuses.get(position));
				startActivity(i);

			}
		});
		lv.setOnItemLongClickListener(new OnItemLongClickListener() { // directly go to reply.

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Log.i("TLA","Long click, pos=" + position + ",id="+id);
				Intent i = new Intent(parent.getContext(), NewTweetActivity.class);
				i.putExtra(getString(R.string.status), statuses.get(position));
				i.putExtra("op",getString(R.string.reply));
				startActivity(i);

				return true; // We've consumed the long click
			}
		});

    }

	private List<Status> getTimlinesFromTwitter(boolean fromDbOnly) {
		Paging paging = new Paging().count(100);

		List<Status> myStatuses = new ArrayList<Status>();


    	long last = tdb.getLastRead(list_id);
    	if (last>0 )//&& !Debug.isDebuggerConnected())
    		paging.sinceId(last);

        switch (list_id) {
        case 0:
        	myStatuses = th.getTimeline(paging,list_id, fromDbOnly);
        	break;
        case -1:
        	myStatuses = th.getTimeline(paging, list_id, fromDbOnly);
        	break;
        case -2:
            // TODO directs
            break;
        default:
        	myStatuses = th.getUserList(paging,list_id, fromDbOnly);
        	break;
        }

        // Update the 'since' id in the database
    	if (myStatuses.size()>0) {
    		last = myStatuses.get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
    		tdb.updateOrInsertLastRead(list_id, last);
    	}

    	statuses = new ArrayList<Status>();
		List<Status> data = new ArrayList<Status>(myStatuses.size());
        String filter = getFilter();
		for (Status status : myStatuses) {
			User user = status.getUser();
			String item ="";
			if ((filter==null) || (filter!= null && !status.getText().matches(filter))) {
				data.add(status);
				statuses.add(status);
			} else {
				Log.i("TweetListActivity::filter",status.getUser().getScreenName() + " - " + status.getText());

			}
		}
		return data;
	}

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

    	if (item!=null && item.getItemId() == R.id.reload_item) {
            fillListViewFromTimeline(false);
    		return true;
    	}

    	return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Called from the reload button
     * @param v
     */
    @SuppressWarnings("unused")
    public void reload(View v) {
        fillListViewFromTimeline(false);
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
     * Called from the post button
     * @param v
     */
    @SuppressWarnings("unused")
    public void post(View v) {
        Intent i = new Intent(this, NewTweetActivity.class);
        startActivity(i);
    }

    private void fillListViewFromTimeline(boolean fromDbOnly) {
    	new GetTimeLineTask().execute(fromDbOnly);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        // TODO: Customise this generated block
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = /* maybe add a padding */
            firstVisible + visibleCount >= totalCount-1;

        ListAdapter adapter = absListView.getAdapter();
        if(loadMore) {
            Log.d("onScroll:","loadMore f=" + firstVisible + ", vc=" + visibleCount + ", tc=" +totalCount);
            if (adapter instanceof StatusAdapter) {
                StatusAdapter sta = (StatusAdapter) adapter;
                if (totalCount>0) {
                    Status last = (Status) sta.getItem(totalCount-1);

                    TwitterHelper th = new TwitterHelper(thisActivity);
                    List<Status> newStatuses = th.getStatuesFromDb(last.getId(),4,list_id);

                    int i = 0;
                    for (Status status : newStatuses ) {
                        sta.insert(status,totalCount+i);
                        statuses.add(status);
                        i++;
                    }
                }
            }

        }
    }

    private class GetTimeLineTask extends AsyncTask<Boolean, Void, List<Status>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (pg!=null)
                pg.setVisibility(ProgressBar.VISIBLE);
            if(titleTextBox!=null)
                titleTextBox.setText("Getting tweets...");
        }


		@Override
		protected List<twitter4j.Status> doInBackground(Boolean... params) {
            boolean fromDbOnly = params[0];
	        List<twitter4j.Status> data;
            data = getTimlinesFromTwitter(fromDbOnly);
	        return data;
		}

		@Override
		protected void onPostExecute(List<twitter4j.Status> result) {
	        setListAdapter(new StatusAdapter<twitter4j.Status>(thisActivity, R.layout.list_item, result));
            if (pg!=null)
                pg.setVisibility(ProgressBar.INVISIBLE);
            if (titleTextBox!=null)
                titleTextBox.setText("");
	        getListView().requestLayout();
		}
    }

    // ".*(http://4sq.com/|http://shz.am/).*"
    private String getFilter() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String exp = prefs.getString("filter",null);
        if (exp==null)
            return null;
        String ret=".*(" + exp.replaceAll(",","|") + ").*";

        Log.i("TweetListActivity::getFilter()","Filter is " + ret);
        return ret;
    }

}
