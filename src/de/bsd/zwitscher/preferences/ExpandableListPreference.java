package de.bsd.zwitscher.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import de.bsd.zwitscher.R;

/**
 * A preference that allows to add/remove values
 * @author Heiko W. Rupp
 */
public class ExpandableListPreference extends DialogPreference implements TextView.OnEditorActionListener,
        AdapterView.OnItemLongClickListener{

    EditText inputField;
    List<String> items = new ArrayList<String>();
    ListView listView;
    ArrayAdapter<String> arrayAdapter;
    String key ;


    public ExpandableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(false); // we are persisting
        setDialogLayoutResource(R.layout.expandable_list_preference);

        key = getKey();


    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);    // Passed view is the expandable_list_preference layout.
        inputField = (EditText) view.findViewById(R.id.elp_input);
        inputField.setOnEditorActionListener(this);

        listView = (ListView) view.findViewById(R.id.elp_list);
        // TODO get persisted values and fill into items
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        if (sp.contains(key)) {
            String val = sp.getString(key,"");
            items.clear();
            items.addAll(Arrays.asList(val.split(","))); // TODO escaping?
        }
        arrayAdapter = new ArrayAdapter<String>(getContext(),R.layout.expandable_list_item,items);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemLongClickListener(this);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // User has clicked ok, but not return - take the contents of the input
        // field.
        String s = inputField.getText().toString();
        if (!items.contains(s) && !s.equals("") && !s.equals('\n')) // For whatever reason we are called twice.
                items.add(s);

        if (items.size()>0) {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            SharedPreferences.Editor editor = sp.edit();

            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = items.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if (iter.hasNext())
                    sb.append(",");
            }
            editor.putString(key,sb.toString());
            editor.commit();
        }

    }



    /**
     * Called when the dialog is about to be displayed
     * @param v
     * @param actionId
     * @param event
     * @return
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_NULL || actionId== EditorInfo.IME_ACTION_SEND){
            String s = inputField.getText().toString();
            if (!items.contains(s) && !s.equals("") && !s.equals('\n')) // For whatever reason we are called twice.
                items.add(s);
            inputField.setText("");
            // refresh list
            arrayAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

        items.remove(position);
        arrayAdapter.notifyDataSetChanged();
        return true;

    }
}
