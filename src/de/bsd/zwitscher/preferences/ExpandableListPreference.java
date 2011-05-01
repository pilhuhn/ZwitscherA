package de.bsd.zwitscher.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
 * A preference that allows to add/remove values. Items are stored in the key
 * separated by the passed separator; default separator is comma (',').
 *
 * @attr ref separator A separator to separate the items in the preferences entry.
 *
 * @author Heiko W. Rupp
 */
public class ExpandableListPreference extends DialogPreference implements TextView.OnEditorActionListener,
        AdapterView.OnItemLongClickListener{

    EditText inputField;
    List<String> items = new ArrayList<String>();
    ListView listView;
    ArrayAdapter<String> arrayAdapter;
    String key ;
    String separator;
    boolean askBeforeDelete = true;
    private static final String DEFAULT_SEPARATOR = ",";

    public ExpandableListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        String separ = attrs.getAttributeValue(null,"separator");
        if (separ==null)
            separator = DEFAULT_SEPARATOR;
        else
            separator = separ;

        askBeforeDelete = attrs.getAttributeBooleanValue(null,"askBeforeDelete",true);

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
        // get persisted values and fill into items
        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        if (sp.contains(key)) {
            String val = sp.getString(key,"");
            items.clear();
            items.addAll(Arrays.asList(val.split(separator))); // TODO escaping?
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
        if (!items.contains(s) && !s.equals("") && !s.equals("\n")) // For whatever reason we are called twice.
                items.add(s);

        if (items.size()>0) {
            SharedPreferences sp = getPreferenceManager().getSharedPreferences();
            SharedPreferences.Editor editor = sp.edit();

            StringBuilder sb = new StringBuilder();
            Iterator<String> iter = items.iterator();
            while (iter.hasNext()) {
                sb.append(iter.next());
                if (iter.hasNext())
                    sb.append(separator);
            }
            editor.putString(key,sb.toString());
            editor.commit();
        }

    }



    /**
     * Called when the dialog is about to be displayed
     * @param v The
     * @param actionId The action that happened
     * @param event
     * @return true if we consumed the event. False otherwise.
     */
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if(actionId == EditorInfo.IME_NULL || actionId== EditorInfo.IME_ACTION_SEND){
            String s = inputField.getText().toString();
            if (!items.contains(s) && !s.equals("") && !s.equals("\n")) // For whatever reason we are called twice.
                items.add(s);
            inputField.setText("");
            // refresh list
            arrayAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    /**
     * Called on a long click on an item. Meant to delete the selected item.
     * If the global option askBeforeDelete is set, a Dialog will be shown.
     * @param parent
     * @param view
     * @param position Position of the selected item
     * @param id
     * @return true if the event has been consumed.
     */
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {


        if (askBeforeDelete) {
            Context context = getContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(context.getString(R.string.wantToRemove,items.get(position)))
                    .setCancelable(false);

            builder.setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    items.remove(position);
                    arrayAdapter.notifyDataSetChanged();

                }
            });
            builder.setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        else {
            items.remove(position);
            arrayAdapter.notifyDataSetChanged();
        }

        return true;

    }
}
