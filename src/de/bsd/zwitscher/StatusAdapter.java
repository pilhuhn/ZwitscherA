/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.bsd.zwitscher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import twitter4j.Status;

import java.text.DateFormat;
import java.util.List;

/**
 * Adapter for individual list rows of
 * the TweetList
 *
 * @author Heiko W. Rupp
 */
class StatusAdapter<T extends Status> extends ArrayAdapter<Status> {

    private static final String STRONG = "<b>";
    private static final String STRONG_END = "</b>";
    private List<Status> items;
    PicHelper ph;
    TwitterHelper th;
    private Context extContext;

    public StatusAdapter(Context context, int textViewResourceId, List<Status> objects) {
        super(context, textViewResourceId, objects);
        extContext = context;
        items = objects;
        ph = new PicHelper();
        th = new TwitterHelper(context);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView; //= super.getView(position, convertView, parent);
        if (view==null) {
            LayoutInflater li = (LayoutInflater) extContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = li.inflate(R.layout.list_item,null);
        }

        if (position %2 == 0)
            view.setBackgroundColor(Color.BLACK);
        else
            view.setBackgroundColor(Color.DKGRAY);

        Status status = items.get(position);

        ImageView iv = (ImageView) view.findViewById(R.id.ListImageView);
        TextView statusText = (TextView) view.findViewById(R.id.ListTextView);
        TextView userInfo = (TextView) view.findViewById(R.id.ListUserView);
        TextView timeClientInfo = (TextView) view.findViewById(R.id.ListTimeView);

        Bitmap bi;
        String userName ;
        if (status.getRetweetedStatus()==null) {
            bi = ph.getBitMapForUserFromFile(status.getUser());
            userName = STRONG + status.getUser().getName() + STRONG_END;
            if (status.getInReplyToScreenName()!=null) {
                userName += " in reply to " + STRONG + status.getInReplyToScreenName() + STRONG_END;
            }
        }
        else {
            bi = ph.getBitMapForUserFromFile(status.getRetweetedStatus().getUser());
            userName = STRONG + status.getRetweetedStatus().getUser().getName() + STRONG_END +
                    " retweeted by " + STRONG + status.getUser().getName() + STRONG_END;
        }

        if (bi!=null) {
            bi = ph.decorate(bi,extContext,status.isFavorited(),status.getRetweetedStatus()!=null);
            iv.setImageBitmap(bi);
        }
        else {
            // underlying view seems to be reused, so default image is not loaded when bi==null
            iv.setImageBitmap(BitmapFactory.decodeResource(extContext.getResources(), R.drawable.user_unknown));
        }
        userInfo.setText(Html.fromHtml(userName));
        statusText.setText(status.getText());

        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        String text = th.getStatusDate(status) + " via " + status.getSource();
        timeClientInfo.setText(Html.fromHtml(text));
        return view;
    }
}
