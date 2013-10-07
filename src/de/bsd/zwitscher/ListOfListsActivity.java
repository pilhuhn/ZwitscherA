package de.bsd.zwitscher;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import de.bsd.zwitscher.helper.CaseInsensitivePairComparator;
import de.bsd.zwitscher.helper.MetaList;
import de.bsd.zwitscher.helper.NetworkHelper;
import twitter4j.Paging;
import twitter4j.SavedSearch;

import java.util.*;

/**
 * Display the list of user lists
 *
 * @author Heiko W. Rupp
 */
@Deprecated
public class ListOfListsActivity extends AbstractListActivity {

    List<ZUserList> userListsEntries;
    int mode;
    ListOfListLineItemAdapter adapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = getIntent().getIntExtra("list",0);

        setContentView(R.layout.tweet_list_layout);
        // TODO enable/disable reload button


    }


    @Override
    protected void onResume() {
        super.onResume();

        setupAdapter();

    }

    private void setupAdapter() {
        List<Pair<String,Integer>> result = new ArrayList<Pair<String, Integer>>();

        if (mode==0) {
            // Display users lists
            userListsEntries = tdb.getLists(account.getId());
            for (ZUserList zul : userListsEntries) {

                String listname = zul.getDisplayName(account);
                int count=tdb.getUnreadCount(account.getId(),zul.listId);

                Pair<String,Integer> pair = new Pair<String,Integer>(listname,count);
                result.add(pair);
            }
            if (result.isEmpty()) {
                String s = getString(R.string.please_sync_lists);
                Pair<String,Integer> pair = new Pair<String, Integer>(s,0);
                result.add(pair);
            }
        }
        else if (mode==1) { // Saved searches
            List<SavedSearch> searches = th.getSavedSearchesFromDb();
            for (SavedSearch search : searches) {
                Pair<String,Integer> pair = new Pair<String, Integer>(search.getName(),0);
                result.add(pair);
            }

            if (result.isEmpty()) {
                String s = getString(R.string.no_searches_found);
                Pair<String,Integer> pair = new Pair<String, Integer>(s,0);
                result.add(pair);
            }
        }
        else
            throw new IllegalArgumentException("Unknown mode " + mode);

        Collections.sort(result, new CaseInsensitivePairComparator());
        adapter = new ListOfListLineItemAdapter(this, R.layout.list_of_list_line_item, result);
        setListAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    /**
     * This method will be called when an item in the list is selected.
     * Subclasses should override. Subclasses can call
     * getListView().getItemAtPosition(position) if they need to access the
     * data associated with the selected item.
     *
     * @param l        The ListView where the click happened
     * @param v        The view that was clicked within the ListView
     * @param position The position of the view in the list
     * @param id       The row id of the item that was clicked
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        String text = ((Pair<String,Integer>) getListView().getItemAtPosition(position)).first;

        if (mode==0) { // User list
            int listId = -1 ;
            String ownerName = account.getName();
            for (ZUserList zul : userListsEntries) {
                if (zul.matches(text)) {
                    listId = zul.listId;
                    ownerName = zul.ownerName;
                }
            }

            if (listId!=-1) {

                int count = adapter.getUnreadCountForPosition(position);
                adapter.setCountForItem(position,0);
                adapter.notifyDataSetChanged();
                getListView().requestLayout();

                Intent intent = new Intent().setClass(this,TweetListActivity.class);
                intent.putExtra(TabWidget.LIST_ID, listId);
                intent.putExtra("userListid",listId);
                intent.putExtra("userListOwner",ownerName);
                intent.putExtra("unreadCount",count);

                startActivityForResult(intent, listId);
            }
        } else if (mode ==1) {
            List<SavedSearch> searches = th.getSavedSearchesFromDb();
            for (SavedSearch search : searches) {
                if (text.equals(search.getName())) {
                    Intent intent = new Intent().setClass(this,TweetListActivity.class);
                    intent.putExtra(TabWidget.LIST_ID, (-search.getId()));
                    intent.putExtra("listName",text);

                    startActivity(intent);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Mark the list as read when we return from it.
        // request code = listId
        tdb.markAllRead(requestCode, account.getId());
        // Request re-layouting the list of lists, as the count has changed (just to be sure)
        if (adapter==null) {
            // Rare case, can happen when the user was in a list, then left for a different
            // apps, the system pages out the activity and on return it gets re-created without
            // an adapter.
            setupAdapter();
        }
        adapter.notifyDataSetChanged();
        getListView().requestLayout();

    }

    /**
     * Trigger updating the user lists. This is only
     * supported for lists and not for searches (those are not persisted)
     * @param v View that was pressed
     */
    @Override
    public void reload(View v) {
        if (mode==0) {
            new SyncAllListsTask(this).execute();
        }
    }

    class SyncAllListsTask extends AsyncTask<Void,Object,Void> {

        Context context;
        String updating;

        SyncAllListsTask(Context context) {
            this.context = context;
            updating = context.getString(R.string.updating);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setProgressBarVisibility(true);
            if (progressBar !=null)
                progressBar.setVisibility(ProgressBar.VISIBLE);

            getParent().setProgressBarIndeterminateVisibility(true);
        }


        @Override
        protected Void doInBackground(Void... params) {

            NetworkHelper networkHelper = new NetworkHelper(context);

            if (networkHelper.isOnline()) {
                for (ZUserList zul : userListsEntries) {

                    publishProgress(zul.listName);

                    Paging paging = new Paging();
                    paging.setCount(100);

                    int listId = zul.listId;
                    long lastFetched = tdb.getLastFetched(account.getId(), listId);
                    if (lastFetched>0)
                        paging.setSinceId(lastFetched);
                    MetaList<twitter4j.Status> list = th.getUserList(paging, listId, false, -1);
                    long newOnes = list.getNumOriginal();
                    if (newOnes>0) {
                        long maxId = list.getList().get(0).getId();
                        tdb.updateOrInsertLastFetched(account.getId(), listId, maxId);
                    }
                }
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            setupAdapter();

            setProgressBarVisibility(false);
            if (progressBar !=null)
                progressBar.setVisibility(ProgressBar.INVISIBLE);
            if (titleTextBox!=null)
                titleTextBox.setText("");

            ActionBar ab = getActionBar();
            if (ab==null && parent!=null)
                ab=parent.getActionBar();
            if (ab!=null) {
                ab.setTitle(account.getAccountIdentifier());
                ab.setSubtitle(null);
            }
            getParent().setProgressBarIndeterminateVisibility(false);

        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);

            String list = (String) values[0];

            String text = updating + " " + list + "...";
            if (titleTextBox!=null) {
                titleTextBox.setText(text);
            }

            ActionBar ab = getActionBar();
            if (ab==null && parent!=null)
                ab=parent.getActionBar();
            if (ab!=null) {
                ab.setTitle(R.string.updating);
                ab.setSubtitle(list);
            }
        }
    }
}
