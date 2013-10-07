package de.bsd.zwitscher;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.bsd.zwitscher.account.Account;

import java.util.List;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class DrawerLineAdapter extends ArrayAdapter<ZUserList> {

    private final LayoutInflater inflater;
    private Context context;
    private List<ZUserList> userLists;
    private Account account;

    public DrawerLineAdapter(Context context, List<ZUserList> userLists, Account account) {
        super(context, R.layout.drawer_row, userLists);
        this.context = context;
        this.userLists = userLists;
        this.account = account;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;

        if (convertView ==null) {
            convertView = inflater.inflate(R.layout.drawer_row,parent,false);

            viewHolder = new ViewHolder();
            viewHolder.textView = (TextView) convertView.findViewById(R.id.textView1);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.imgView);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.textView.setText(userLists.get(position).getDisplayName(account));
        ImageView imageView = viewHolder.imageView;
        int listId = userLists.get(position).listId;
        switch (listId) {
            case 0: imageView.setImageResource(R.drawable.ic_tab_home); break;
            case -1: imageView.setImageResource(R.drawable.ic_tab_mention); break;
            case -2: imageView.setImageResource(R.drawable.ic_tab_mention); break;
            case -3: imageView.setImageResource(R.drawable.ic_tab_sent); break;
            case -4: imageView.setImageResource(R.drawable.ic_tab_favorite); break;
            default:
                if (listId<0) {
                    imageView.setImageResource(R.drawable.ic_tab_list);
                }
                else {
                    imageView.setImageResource(R.drawable.ic_tab_search);
                }
        }

        return convertView;
    }

    static class ViewHolder {
        TextView textView;
        ImageView imageView;
    }
}
