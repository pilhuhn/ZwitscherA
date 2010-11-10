package de.bsd.zwitscher;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import twitter4j.Status;
import twitter4j.UserList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class ListOfListsActivity extends ListActivity {

    TwitterHelper th;
    TweetDB tdb;
    Set<Map.Entry<String, Integer>> userListsEntries;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        th = new TwitterHelper(this);
        tdb = new TweetDB(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

    List<String> result = new ArrayList<String>();

        userListsEntries = tdb.getLists().entrySet();
  		for (Map.Entry<String, Integer> userList : userListsEntries) {
            result.add(userList.getKey());
        }
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, result));

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

        String text = (String) getListView().getItemAtPosition(position);

        int listId = -1 ;
        for (Map.Entry<String,Integer> userList : userListsEntries) {
            if (userList.getKey().equals(text))
                listId = userList.getValue();
        }

        if (listId!=-1) {
            Intent intent = new Intent().setClass(this,TweetListActivity.class);
            intent.putExtra(TabWidget.LIST_ID, listId);

            startActivity(intent);
        }
    }
}