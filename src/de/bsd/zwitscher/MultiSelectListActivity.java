package de.bsd.zwitscher;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Show a list with multiple items where the
 * user can select some from
 *
 * @author Heiko W. Rupp
 */
public class MultiSelectListActivity extends ListActivity implements AdapterView.OnItemClickListener {

    private String mode;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        List<String> data = bundle.getStringArrayList("data");
        boolean[] checked = bundle.getBooleanArray("checked");
        mode = bundle.getString("mode");

        final ListView listView = getListView();

        if (mode.equals("single")) {
            setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, data));
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setOnItemClickListener(this);
        }
        else {
            Button okButton = new Button(this);
            okButton.setText("ok");
            okButton.setEnabled(true);
            okButton.setVisibility(View.VISIBLE);
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SparseBooleanArray positions = getListView().getCheckedItemPositions();
                    List<Integer> positionsList = new ArrayList<Integer>(positions.size());
                    for (int i = 0; i < positions.size();i++) {
                        if (positions.get(i))
                            positionsList.add(i);
                    }
                    long[] items = new long[positionsList.size()];
                    for (int i = 0; i < positionsList.size(); i++) {
                        items[i]=positionsList.get(i);
                    }

                    Intent intent = prepareReturnedIntent();
                    intent.putExtra("data", items);
                    finish();
                }
            });
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.addFooterView(okButton); // Needs to be called before setAdapter

            setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_multiple_choice, data));
            for (int i = 0; i < checked.length; i++) {
                getListView().setItemChecked(i,checked[i]); // does not check
            }
        }

        listView.setItemsCanFocus(false);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView listView = getListView();
        String item = (String) listView.getItemAtPosition(position);

        Intent intent = prepareReturnedIntent();
        intent.putExtra("data", item);
        finish();
    }

    private Intent prepareReturnedIntent() {
        Intent intent = new Intent();
        intent.putExtra("mode",mode);
        setResult(RESULT_OK,intent);

        return intent;
    }
}