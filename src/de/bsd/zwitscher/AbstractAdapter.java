package de.bsd.zwitscher;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.bsd.zwitscher.helper.PicHelper;
import de.bsd.zwitscher.helper.UserImageView;

/**
 * Common base for the Adapters
 * @author Heiko W. Rupp
 */
class AbstractAdapter<T> extends ArrayAdapter<T> {

    List<T> items;
    PicHelper ph;
    final Context extContext;
    final LayoutInflater inflater;


    AbstractAdapter(Context context, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);

        extContext = context;
        items = objects;
        ph = new PicHelper();
        inflater = (LayoutInflater) extContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    static class ViewHolder {
        UserImageView iv;
        TextView statusText;
        TextView userInfo;
        TextView timeClientInfo;
    }
}
