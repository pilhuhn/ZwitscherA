package de.bsd.zwitscher;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

/**
 * Show a list with multiple items where the
 * user can select some from
 *
 * @author Heiko W. Rupp
 */
public class MultiSelectListActivity extends ListActivity implements AdapterView.OnItemClickListener {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        List<String> data = bundle.getStringArrayList("data");
        String mode = bundle.getString("mode");


        if (mode.equals("single"))
            setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, data));
        else
            setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, data));


        final ListView listView = getListView();

        listView.setItemsCanFocus(false);
        if (mode.equals("single"))
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        else
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        listView.setOnItemClickListener(this);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO: Customise this generated block
        ListView listView = getListView();
        String item = (String) listView.getItemAtPosition(position);

        Intent intent = new Intent();
        intent.putExtra("data", item);
        setResult(RESULT_OK,intent);

        finish();
    }
}