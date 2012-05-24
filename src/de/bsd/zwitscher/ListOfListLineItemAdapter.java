package de.bsd.zwitscher;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter for items of the list_of_list_line_item layout
 * Items are name of the list + count of unread items
 */
class ListOfListLineItemAdapter extends ArrayAdapter<Pair<String,Integer>> {

    List<Pair<String,Integer>> items;
    Context extContext;
    LayoutInflater inflater;


    ListOfListLineItemAdapter(Context context, int textViewResourceId, List<Pair<String,Integer>> objects) {
        super(context, textViewResourceId, objects);
        extContext = context;
        inflater = (LayoutInflater) extContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        items = objects;

    }

    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        // Use ViewHolder pattern to only inflate once
        if (convertView ==null) {
            convertView = inflater.inflate(R.layout.list_of_list_line_item,parent,false);

            viewHolder = new ViewHolder();
            viewHolder.countView = (TextView) convertView.findViewById(R.id.count);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (position %2 == 0)
            convertView.setBackgroundColor(Color.BLACK);
        else
            convertView.setBackgroundColor(Color.DKGRAY);


        Pair<String,Integer> response = items.get(position);
        viewHolder.nameView.setText(response.first);
        if (response.second!=null && response.second>0)
            viewHolder.countView.setText(""+response.second);
        else
            viewHolder.countView.setText("");

        return convertView;
    }

    void setCountForItem(int item,int count) {

        if (item > items.size()) {
            Log.e("ListoIfListLineAdapter", "req item " + item + " > size: " + items.size() );
            return;
        }

        Pair<String,Integer> pair = items.get(item);
        Pair<String,Integer> p2 = new Pair<String,Integer>(pair.first,count);
        items.set(item,p2);

    }

    public int getUnreadCountForPosition(int position) {
        Pair<String,Integer> item = items.get(position);
        return item.second;
    }

    static class ViewHolder {
        TextView nameView;
        TextView countView;
    }
}