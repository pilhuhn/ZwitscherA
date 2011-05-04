package de.bsd.zwitscher;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
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
import de.bsd.zwitscher.helper.NetworkHelper;
import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.Tweet;

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
public class TweetListActivity extends AbstractListActivity implements AbsListView.OnScrollListener,
        OnItemClickListener, OnItemLongClickListener {

    List<Status> statuses;
    List<DirectMessage> directs;
    List<Tweet> tweets;
    Bundle intentInfo;
    int list_id;
    ListView lv;
    int newMentions=0;
    private int newDirects=0;
    Integer userId=null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity theParent = getParent();
        if (!(theParent instanceof TabWidget)) {
            // We have no enclosing TabWidget, so we need to request the custom title
            requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        }

        // Set the layout of the list activity
        setContentView(R.layout.tweet_list_layout);


        // Get the windows progress bar from the enclosing TabWidget
        if (!(theParent instanceof TabWidget)) {
            // We have no enclosing TabWidget, so we need our window here
            getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,R.layout.window_title);
            pg = (ProgressBar) findViewById(R.id.title_progress_bar);
            titleTextBox = (TextView) findViewById(R.id.title_msg_box);


            ImageButton imageButton = (ImageButton) findViewById(R.id.back_button);
            imageButton.setVisibility(View.VISIBLE);

        }
        lv = getListView();
        lv.setOnScrollListener(this);
		lv.setOnItemClickListener(this);
		lv.setOnItemLongClickListener(this); // Directly got to reply


        intentInfo = getIntent().getExtras();
        if (intentInfo==null) {
            list_id = 0;
        } else {
            list_id = intentInfo.getInt(TabWidget.LIST_ID);
            if (intentInfo.containsKey("userId")) {
                // Display tweets of a single user
                userId = intentInfo.getInt("userId");
                // This is a one off list. So don't offer the reload button
                ImageButton tweet_list_reload_button = (ImageButton) findViewById(R.id.tweet_list_reload_button);
                tweet_list_reload_button.setVisibility(View.INVISIBLE);
            }
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
         Intent i = new Intent(this,OneTweetActivity.class);
         i.putExtra("account",account);
         i.putExtra(getString(R.string.status), statuses.get(position));
         startActivity(i);
        }
        else if (directs!=null) {
           Intent i = new Intent(this, NewTweetActivity.class);
           i.putExtra("account",account);
           i.putExtra("user",directs.get(position).getSender());
           i.putExtra("op",getString(R.string.direct));
           startActivity(i);
        } else {
            // Tweets; TODO
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
    public boolean onItemLongClick(AdapterView<?> parent, View view,
            int position, long id) {
        Log.i("TLA","Long click, pos=" + position + ",id="+id);
        Intent i = new Intent(this, NewTweetActivity.class);
        i.putExtra("account",account);
        if (statuses!=null) {
           i.putExtra(getString(R.string.status), statuses.get(position));
           i.putExtra("op",getString(R.string.reply));
           startActivity(i);
        }
        else if (directs!=null) {
         i.putExtra("user",directs.get(position).getSender());
         i.putExtra("op",getString(R.string.direct));
         startActivity(i);
        } else {
            // Tweets TODO
        }


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
     *
     * @param fromDbOnly If true only statuses already in the DB are returned
     * @param filter
     * @return List of status items along with some counts
     */
	private MetaList<Status> getTimlinesFromTwitter(boolean fromDbOnly, String filter) {
		Paging paging = new Paging();

		MetaList<Status> myStatuses;


    	long last = tdb.getLastRead(list_id);
    	if (last>0 )//&& !Debug.isDebuggerConnected())
    		paging.sinceId(last);
        else
            paging.setCount(50); // 50 Tweets if we don't have the timeline yet

        switch (list_id) {
        case 0:
            // Home time line
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

    private MetaList<DirectMessage> getDirectsFromTwitter(boolean fromDbOnly) {
        MetaList<DirectMessage> messages;


        long last = tdb.getLastRead(-2);
        Paging paging = new Paging();
        if (last>-1)
            paging.setSinceId(last);


        messages = th.getDirectMessages(fromDbOnly, paging);
        directs = messages.getList();

        return messages;
    }

    private MetaList<Tweet> getSavedSearchFromTwitter(int searchId, boolean fromDbOnly) {
        MetaList<Tweet> messages;

        Paging paging = new Paging();
        paging.setCount(20);

        messages = th.getSavedSearchesTweets(searchId, fromDbOnly,paging);

        tweets = messages.getList();

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


    private void fillListViewFromTimeline(boolean fromDbOnly) {
    	new GetTimeLineTask(this).execute(fromDbOnly);
    }

    public void onScrollStateChanged(AbsListView absListView, int i) {
        // nothing to do for us
    }

    @SuppressWarnings("unchecked")
    public void onScroll(AbsListView absListView, int firstVisible, int visibleCount, int totalCount) {

        boolean loadMore = /* maybe add a padding */
            firstVisible + visibleCount >= totalCount-1;


//        Log.d("onScroll:","loadMore f=" + firstVisible + ", vc=" + visibleCount + ", tc=" +totalCount);
        if(loadMore) {
//Debug.startMethodTracing("list" + firstVisible);
            ListAdapter adapter = absListView.getAdapter();
            if (adapter instanceof StatusAdapter) {
                StatusAdapter sta = (StatusAdapter) adapter;
                if (totalCount>0) {
                    Object item = sta.getItem(totalCount - 1);
                    int i = 0;
                    if (item instanceof  DirectMessage) {
                        DirectMessage message = (DirectMessage) item;

                        List<DirectMessage> messages = th.getDirectsFromDb(message.getId(),7);
                        for (DirectMessage direct : messages) {
                            sta.insert(direct, totalCount + i);
                            directs.add(direct);
                            i++;
                        }
                    } else if (item instanceof Status) {
                        Status last = (Status) item;
                        if (statuses==null)
                            statuses = new ArrayList<Status>();

                        List<Status> newStatuses = th.getStatuesFromDb(last.getId(),7,list_id);
                        for (Status status : newStatuses ) {
                            sta.insert(status, totalCount + i);
                            statuses.add(status);
                            i++;
                        }
                    }
                    sta.notifyDataSetChanged();
                }
            }
//Debug.stopMethodTracing();
        }
    }

    private class GetTimeLineTask extends AsyncTask<Boolean, String, MetaList> {

        boolean fromDbOnly = false;
        String updating;
        Context context;

        private GetTimeLineTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            updating = context.getString(R.string.updating);
            if (pg!=null)
                pg.setVisibility(ProgressBar.VISIBLE);
            if(titleTextBox!=null) {
                String s = getString(R.string.getting_tweets)+ "...";
                titleTextBox.setText(s);
            }
        }


		@Override
		protected MetaList doInBackground(Boolean... params) {
            fromDbOnly = params[0];
	        MetaList data;
            if (userId!=null) {
                List<twitter4j.Status> statuses = th.getUserTweets(userId);
                data = new MetaList(statuses,statuses.size(),0);
                String user = context.getString(R.string.user);
                publishProgress(user);
            }
            else {
                String directsString = context.getString(R.string.direct);
                if (list_id>-2) {
                    String mentionsString = context.getString(R.string.mentions);
                    String homeString = context.getString(R.string.home_timeline);
                    if (list_id<=0)
                        publishProgress(list_id==-1? mentionsString : homeString);
                    else
                        publishProgress("");
                    String filter = getFilter();
                    data = getTimlinesFromTwitter(fromDbOnly, filter);
                    // Also check for mentions + directs (if allowed in prefs)
                    NetworkHelper networkHelper = new NetworkHelper(context);

                    if (list_id<=0 && !fromDbOnly && networkHelper.mayReloadAdditional()) { // TODO make this block nicer
                        publishProgress(mentionsString);
                        long mentionLast = tdb.getLastRead(-1);
                        Paging paging;
                        paging = new Paging().count(100);

                        if (mentionLast>0)
                            paging.setSinceId(mentionLast);
                        MetaList<twitter4j.Status> mentions = th.getTimeline(paging,-1,false);
                        newMentions = mentions.getNumOriginal();
                        if (mentions.getList().size()>0) {
                            long id = mentions.getList().get(0).getId();
                            tdb.updateOrInsertLastRead(-1,id);
                        }

                        if (list_id==0) { // Fetch directs only if original list was homes
                            publishProgress(directsString);
                            MetaList<DirectMessage> directs = getDirectsFromTwitter(false);
                            newDirects = directs.getNumOriginal();
                            if (directs.getList().size()>0) {
                                long id = mentions.getList().get(0).getId();
                                tdb.updateOrInsertLastRead(-2,id);
                            }
                        }

                    }
                }
                else if (list_id==-2) {
                    publishProgress(directsString);
                    data = getDirectsFromTwitter(fromDbOnly);
                }
                else { // list id < -2 ==> saved search
                    String s = context.getString(R.string.searches);
                    publishProgress(s);
                    data = getSavedSearchFromTwitter(-list_id,fromDbOnly);
                }
            }
	        return data;
		}

        @SuppressWarnings("unchecked")
		@Override
		protected void onPostExecute(MetaList result) {
            if (list_id<-2)
	            setListAdapter(new TweetAdapter(context, account, R.layout.tweet_list_item, result.getList()));
            else
	            setListAdapter(new StatusAdapter(context, account, R.layout.tweet_list_item, result.getList()));

            if (result.getList().size()==0) {
                Toast.makeText(context,"Got no result from the server",Toast.LENGTH_LONG).show();
            }

            if (pg!=null)
                pg.setVisibility(ProgressBar.INVISIBLE);
            if (titleTextBox!=null)
                titleTextBox.setText(account.getAccountIdentifier());
	        getListView().requestLayout();
            if (newMentions>0) {
                String s = getString(R.string.new_mentions);
                Toast.makeText(context,newMentions + " " + s,Toast.LENGTH_LONG).show();
                newMentions=0;
            }
            if (newDirects>0) {
                String s = getString(R.string.new_directs);
                Toast.makeText(context,newDirects + " " + s,Toast.LENGTH_LONG).show();
                newDirects=0;
            }

            // Only do the next if we actually did an update from twitter
            if (!fromDbOnly) {
                Log.i("GTLTask", " scroll to " + result.getNumOriginal());
                getListView().setSelection(result.getNumOriginal() - 1);
            }
		}

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            titleTextBox.setText(updating +" "+ values[0] + "...");
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
