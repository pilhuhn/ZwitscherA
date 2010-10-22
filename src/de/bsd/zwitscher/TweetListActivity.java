package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.User;

/**
 * Show the list of tweets
 * @author Heiko W. Rupp
 */
public class TweetListActivity extends ListActivity {

    List<Status> statuses;
    Bundle intentInfo;
    TweetListActivity thisActivity;
    ProgressBar pg;
    TextView titleTextBox;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        intentInfo = getIntent().getExtras();
        thisActivity = this;
        // Get the windows progress bar from the enclosing TabWidget
        TabWidget parent = (TabWidget) this.getParent();
        pg = parent.pg;
        titleTextBox = parent.titleTextBox;

        fillListViewFromTimeline(true); // Only get tweets from db to speed things up at start
    }

    @Override
    public void onResume() {

    	super.onResume();
        intentInfo = getIntent().getExtras();

        // Get the windows progress bar from the enclosing TabWidget
        TabWidget parent = (TabWidget) this.getParent();
        pg = parent.pg;

		ListView lv = getListView();

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

	private List<Status> getTimlinesFromTwitter(int timeline, int listId, String specialName, boolean fromDbOnly) {
		TwitterHelper th = new TwitterHelper(getApplicationContext());
		Paging paging = new Paging().count(100);
		TweetDB tdb = new TweetDB(this);
		List<Status> myStatus = new ArrayList<Status>();

		// special name is set for lists - this is the list name
		// Now fake it for other timelines
        switch (timeline) {
        case R.string.home_timeline:
        	specialName = "home";
        	break;
        case R.string.mentions:
        	specialName = "mentions";
        	break;
        }

    	long last = tdb.getLastRead(specialName);
    	if (last!=0 )//&& !Debug.isDebuggerConnected())
    		paging.sinceId(last);

        switch (timeline) {
        case R.string.home_timeline:
        	myStatus = th.getTimeline(paging,R.string.home_timeline, fromDbOnly);
        	break;
        case R.string.mentions:
        	myStatus = th.getTimeline(paging, R.string.mentions, fromDbOnly);
        	break;
        case R.string.list:
        	myStatus = th.getUserList(paging,listId, fromDbOnly);
        	break;
        }

        // Update the 'since' id in the database
    	if (myStatus.size()>0) {
    		last = myStatus.get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
    		tdb.updateOrInsertLastRead(specialName, last);
    	}

    	statuses = new ArrayList<Status>();
		List<Status> data = new ArrayList<Status>(myStatus.size());
        String filter = getFilter();
		for (Status status : myStatus) {
			User user = status.getUser();
			String item ="";
			if ((filter==null) || (filter!= null && !status.getText().matches(filter))) {
//				item += user.getName() +  " (" + user.getScreenName() + "): " + status.getText();
				data.add(status);
				statuses.add(status);
			} else {
				Log.i("TweetListActivity::filter",status.getUser().getScreenName() + " - " + status.getText());

			}
		}
		if (statuses.size()==0) { // No (new) tweet found
			//data.add(">>  Sorry, no tweets currently available, try later << ");
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

    private void fillListViewFromTimeline(boolean fromDbOnly) {
    	new GetTimeLineTask().execute(new Boolean[]{fromDbOnly});
    }

    private class GetTimeLineTask extends AsyncTask<Boolean, Void, List<Status>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pg.setVisibility(ProgressBar.VISIBLE);
            titleTextBox.setText("Getting tweets...");
        }


		@Override
		protected List<twitter4j.Status> doInBackground(Boolean... params) {
            boolean fromDbOnly = params[0];
	        List<twitter4j.Status> data;
	        if (intentInfo==null)
	            data = getTimlinesFromTwitter(R.string.home_timeline,0, null, fromDbOnly);
	        else {
	        	String timelineString = intentInfo.getString("timeline");
				if (timelineString!=null && timelineString.contains("mentions")) {
	        		data = getTimlinesFromTwitter(R.string.mentions, 0, null, fromDbOnly);
	        	} else {

		            String listName = intentInfo.getString("listName");
		            int id = intentInfo.getInt("id");
		            data = getTimlinesFromTwitter(R.string.list,id, listName, fromDbOnly);
	        	}
	        }
	        return data;
		}

		@Override
		protected void onPostExecute(List<twitter4j.Status> result) {
	        setListAdapter(new StatusAdapter<twitter4j.Status>(thisActivity, R.layout.list_item, result));
            pg.setVisibility(ProgressBar.INVISIBLE);
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

    /**
     * Adapter for individual list rows of
     * the TweetList
     *
     * @author Heiko W. Rupp
     */
    private class StatusAdapter<T extends Status> extends ArrayAdapter<Status> {

        private static final String STRONG = "<b>";
        private static final String STRONG_END = "</b>";
        private List<Status> items;
        PicHelper ph;

        public StatusAdapter(Context context, int textViewResourceId, List<Status> objects) {
            super(context, textViewResourceId, objects);
            items = objects;
            ph = new PicHelper();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView; //= super.getView(position, convertView, parent);
            if (view==null) {
                LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                view = li.inflate(R.layout.list_item,null);
            }

            if (position %2 == 0)
                view.setBackgroundColor(Color.BLACK);
            else
                view.setBackgroundColor(Color.DKGRAY);

            Status status = items.get(position);

            ImageView iv = (ImageView) view.findViewById(R.id.ListImageView);
            TextView tv = (TextView) view.findViewById(R.id.ListTextView);
            TextView uv = (TextView) view.findViewById(R.id.ListUserView);

            Bitmap bi;
            String userName ;
            if (status.getRetweetedStatus()==null) {
                bi = ph.getBitMapForUserFromFile(status.getUser());
                userName = STRONG + status.getUser().getName() + STRONG_END;
                if (status.getInReplyToScreenName()!=null) {
                    userName += " in reply to " + STRONG + status.getInReplyToScreenName() + STRONG_END;
                }
            }
            else {
                bi = ph.getBitMapForUserFromFile(status.getRetweetedStatus().getUser());
                userName = STRONG + status.getRetweetedStatus().getUser().getName() + STRONG +
                        " retweeted by " + STRONG + status.getUser().getName() + STRONG_END;
            }

            if (bi!=null)
                iv.setImageBitmap(bi);
            else {
                // underlying view seems to be reused, so default image is not loaded when bi==null
                iv.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.user_unknown));
            }
            uv.setText(Html.fromHtml(userName));
            tv.setText(status.getText());
         //   tv.setTextColor(Color.YELLOW);
            return view;
        }
    }
}
