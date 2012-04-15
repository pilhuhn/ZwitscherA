package de.bsd.zwitscher;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import de.bsd.zwitscher.helper.CaseInsensitivePairComparator;
import de.bsd.zwitscher.helper.MetaList;
import twitter4j.Paging;
import twitter4j.SavedSearch;

import java.util.*;

/**
 * Display the list of user lists
 *
 * @author Heiko W. Rupp
 */
public class ListOfListsActivity extends AbstractListActivity {

    Set<Map.Entry<Integer,Pair<String,String>>> userListsEntries;
    int mode;
    ListOfListLineItemAdapter adapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = getIntent().getIntExtra("list",0);

        View reloadButton;
        if (Build.VERSION.SDK_INT<11) {
            setContentView(R.layout.tweet_list_layout);
            reloadButton = findViewById(R.id.tweet_list_reload_button);
            if (mode==0)
                reloadButton.setEnabled(true);
            else
                reloadButton.setEnabled(false);  // disabled for stored searches (for now), as the tweets are not persisted

        }
        else { // >= 11 -> honeycomb and later
            setContentView(R.layout.tweet_list_layout_honeycomb);
            // TODO enable/disable reload button
        }


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
            userListsEntries = tdb.getLists(account.getId()).entrySet();
            for (Map.Entry<Integer, Pair<String, String>> userList : userListsEntries) {
                Pair<String, String> nameOwnerPair = userList.getValue();
                String listname;
                if (account.getName().equals(nameOwnerPair.second))
                    listname = nameOwnerPair.first;
                else
                    listname = "@" + nameOwnerPair.second + "/" + nameOwnerPair.first;

                int count=tdb.getUnreadCount(account.getId(),userList.getKey());

                Pair<String,Integer> pair = new Pair<String,Integer>(listname,count);
                result.add(pair);
            }
            if (result.isEmpty()) {
                String s = getString(R.string.please_sync_lists);
                Pair<String,Integer> pair = new Pair<String, Integer>(s,0);
                result.add(pair);
            }
        }
        else if (mode==1) {
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
            for (Map.Entry<Integer, Pair<String, String>> entry : userListsEntries) {
                Pair<String,String> nameOwnerPair = entry.getValue();
                if (text.startsWith("@") && text.equals("@"+nameOwnerPair.second +"/" +nameOwnerPair.first)) {
                    listId = entry.getKey();
                    ownerName = nameOwnerPair.second;
                }
                else if (!text.startsWith("@") && text.equals(nameOwnerPair.first)) {
                    listId = entry.getKey();
                    ownerName = nameOwnerPair.second;
                }
            }

            if (listId!=-1) {

                tdb.markAllRead(listId, account.getId());
                adapter.setCountForItem(position,0);
                adapter.notifyDataSetChanged();
                getListView().requestLayout();

                Intent intent = new Intent().setClass(this,TweetListActivity.class);
                intent.putExtra(TabWidget.LIST_ID, listId);
                intent.putExtra("userListid",listId);
                intent.putExtra("userListOwner",ownerName);

                startActivity(intent);
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

        }


        @Override
        protected Void doInBackground(Void... params) {

            for (Map.Entry<Integer, Pair<String, String>> entry : userListsEntries) {

                Pair<String, String> nameOwnerPair = entry.getValue();
                publishProgress(nameOwnerPair.first);

                Paging paging = new Paging();
                paging.setCount(100);

                int listId = entry.getKey();
                long lastFetched = tdb.getLastFetched(account.getId(), listId);
                if (lastFetched>0)
                    paging.setSinceId(lastFetched);
                MetaList<twitter4j.Status> list = th.getUserList(paging, listId, false);
                long newOnes = list.getNumOriginal();
                if (newOnes>0) {
                    long maxId = list.getList().get(0).getId();
                    tdb.updateOrInsertLastFetched(account.getId(), listId, maxId);
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

            if (Build.VERSION.SDK_INT>=11) {
                ActionBar ab = getActionBar();
                if (ab==null && parent!=null)
                    ab=parent.getActionBar();
                if (ab!=null) {
                    ab.setTitle(account.getAccountIdentifier());
                    ab.setSubtitle(null);
                }
            }

        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);

            String list = (String) values[0];

            String text = updating + " " + list + "...";
            if (titleTextBox!=null) {
                titleTextBox.setText(text);
            }

            if (Build.VERSION.SDK_INT>=11) {
                ActionBar ab = getActionBar();
                if (ab==null && parent!=null)
                    ab=parent.getActionBar();
                if (ab!=null)
                    ab.setSubtitle(text);
            }
        }
    }
}
