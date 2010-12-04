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
import android.view.View;
import android.view.Window;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.bsd.zwitscher.helper.MetaList;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Status;

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
public class TweetListActivity extends ListActivity implements AbsListView.OnScrollListener,
        OnItemClickListener, OnItemLongClickListener {

    List<Status> statuses;
    List<DirectMessage> directs;
    Bundle intentInfo;
    TweetListActivity thisActivity;
    ProgressBar pg;
    TextView titleTextBox;
    int list_id;
    TweetDB tdb;
    TwitterHelper th;
    ListView lv;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = this;

        Activity theParent = getParent();
        if (!(theParent instanceof TabWidget)) {
            // We have no enclosing TabWidget, so we need to request the custom title
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        }

        // Set the layout of the list activity
        setContentView(R.layout.tweet_list_layout);


        // Get the windows progress bar from the enclosing TabWidget
        if (theParent instanceof TabWidget) {
            TabWidget parent = (TabWidget) theParent;
            pg = parent.pg;
            titleTextBox = parent.titleTextBox;
        }
        else {
            // We have no enclosing TabWidget, so we need our window here
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
            pg = (ProgressBar) findViewById(R.id.title_progress_bar);
            titleTextBox = (TextView) findViewById(R.id.title_msg_box);


            ImageButton imageButton = (ImageButton) findViewById(R.id.back_button);
            imageButton.setVisibility(View.VISIBLE);

        }
        tdb = new TweetDB(this,0); // TODO set correct account
        th = new TwitterHelper(this);
        lv = getListView();
        lv.setOnScrollListener(this);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this); // Directly got to reply


        intentInfo = getIntent().getExtras();
        if (intentInfo==null) {
            list_id = 0;
        } else {
            list_id = intentInfo.getInt(TabWidget.LIST_ID);
        }

        boolean fromDbOnly = tdb.getLastRead(list_id) != -1;
        fillListViewFromTimeline(fromDbOnly); // Only get tweets from db to speed things up at start
    }

    @Override
    public void onResume() {
     	super.onResume();

        lv = getListView();
        lv.setOnScrollListener(this);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this); // Directly got to reply

    }

    /**
     * Handle click on a list item which triggers the detail view of that item
     * @param parent Parent view
     * @param view Clicked view
     * @param position Position in the list that was clicked
     * @param id
     */
    public void onItemClick(AdapterView<?> parent, View view,
            int position, long id) {

        if (statuses!=null) {
         Intent i = new Intent(parent.getContext(),OneTweetActivity.class);
         i.putExtra(getString(R.string.status), statuses.get(position));
         startActivity(i);
        }
        else {
           Intent i = new Intent(parent.getContext(), NewTweetActivity.class);
           i.putExtra("user",directs.get(position).getSender());
           i.putExtra("op",getString(R.string.direct));
           startActivity(i);
        }


    }

    /**
     * Handle a long click on a list item - by directly jumping into reply mode
     * @param parent Parent view
     * @param view Clicked view
     * @param position Position in the List that was clicked
     * @param id
     * @return true as the click was consumed
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        Log.i("TLA","Long click, pos=" + position + ",id="+id);
        Intent i = new Intent(parent.getContext(), NewTweetActivity.class);
        if (statuses!=null) {
           i.putExtra(getString(R.string.status), statuses.get(position));
           i.putExtra("op",getString(R.string.reply));
        }
        else if (directs!=null) {
         i.putExtra("user",directs.get(position).getSender());
         i.putExtra("op",getString(R.string.direct));
        }
        startActivity(i);

        return true; // We've consumed the long click
    }


    /**
     * Retrieve a list of statuses. Depending on list_id, this is taken from
     * different sources:
     * <ul>
     * <li>0 : home timeline</li>
     * <li>-1 : mentions</li>
     * <li>>0 : User list</li>
     * </ul>
     * This method may trigger a network call if fromDbOnly is false.
     * @param fromDbOnly If true only statuses already in the DB are returned
     * @return List of status items along with some counts
     */
	private MetaList<Status> getTimlinesFromTwitter(boolean fromDbOnly) {
		Paging paging = new Paging().count(100);

		MetaList<Status> myStatuses;


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
            // see below at getDirectsFromTwitter
            myStatuses = new MetaList<Status>();
            break;
        default:
        	myStatuses = th.getUserList(paging,list_id, fromDbOnly);
        	break;
        }

        // Update the 'since' id in the database
    	if (myStatuses.getList().size()>0) {
    		last = myStatuses.getList().get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
    		tdb.updateOrInsertLastRead(list_id, last);
    	}

    	statuses = new ArrayList<Status>();
		List<Status> data = new ArrayList<Status>(myStatuses.getList().size());
        String filter = getFilter();
		for (Status status : myStatuses.getList()) {
			if ((filter==null) || (filter!= null && !status.getText().matches(filter))) {
				data.add(status);
				statuses.add(status);
			} else {
				Log.i("TweetListActivity::filter",status.getUser().getScreenName() + " - " + status.getText());

			}
		}

        MetaList<Status> metaList = new MetaList<Status>(data,myStatuses.getNumOriginal(),myStatuses.getNumAdded());

		return metaList;
	}

    private MetaList getDirectsFromTwitter(boolean fromDbOnly) {
        MetaList<DirectMessage> messages;


        long last = tdb.getLastRead(-2);
        Paging paging = new Paging();
        if (last>-1)
         paging.setSinceId(last);


        messages = th.getDirectMessages(fromDbOnly, paging);
        directs = messages.getList();

        return messages;
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

    private void fillListViewFromTimeline(boolean fromDbOnly) {
    	new GetTimeLineTask().execute(fromDbOnly);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        // nothing to do for us
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = /* maybe add a padding */
            firstVisible + visibleCount >= totalCount-1;

        ListAdapter adapter = absListView.getAdapter();
        Log.d("onScroll:","loadMore f=" + firstVisible + ", vc=" + visibleCount + ", tc=" +totalCount);
        if(loadMore) {
            if (adapter instanceof StatusAdapter) {
                StatusAdapter sta = (StatusAdapter) adapter;
                if (totalCount>0) {
                    if (sta.getItem(totalCount-1) instanceof  DirectMessage) // TODO directs
                        return;

                    Status last = (Status) sta.getItem(totalCount-1);

                    TwitterHelper th = new TwitterHelper(thisActivity);
                    List<Status> newStatuses = th.getStatuesFromDb(last.getId(),7,list_id);

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

    private class GetTimeLineTask extends AsyncTask<Boolean, Void, MetaList> {

        boolean fromDbOnly = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (pg!=null)
                pg.setVisibility(ProgressBar.VISIBLE);
            if(titleTextBox!=null) {
                String s = getString(R.string.getting_tweets)+ "...";
                titleTextBox.setText(s);
            }
        }


		@Override
		protected MetaList<twitter4j.Status> doInBackground(Boolean... params) {
            fromDbOnly = params[0];
	        MetaList data;
            if (list_id!=-2)
                data = getTimlinesFromTwitter(fromDbOnly);
            else
                data = getDirectsFromTwitter(fromDbOnly);
            Log.i("GTLTask", "got " + data.toString());
	        return data;
		}

		@Override
		protected void onPostExecute(MetaList result) {
	        setListAdapter(new StatusAdapter(thisActivity, R.layout.tweet_list_item, result.getList()));
            if (pg!=null)
                pg.setVisibility(ProgressBar.INVISIBLE);
            if (titleTextBox!=null)
                titleTextBox.setText("");
	        getListView().requestLayout();

            // Only do the next if we actually did an update from twitter
            if (!fromDbOnly) {
                Log.i("GTLTask"," scroll to " + result.getNumOriginal());
                getListView().setSelection(result.getNumOriginal()-1);
            }
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
