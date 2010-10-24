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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import twitter4j.Status;

import java.util.ArrayList;
import java.util.List;

/**
 * Just display a Conversation ..
 *
 * @author Heiko W. Rupp
 */
public class ThreadListActivity extends ListActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<Status> result = new ArrayList<Status>();

        Intent i = getIntent();
        Bundle b = i.getExtras();
        long id = 0;
        if (b!=null)
            id = b.getLong("startId");

        TwitterHelper th = new TwitterHelper(this);

        Status status = th.getStatusById(id,null) ;
        while (status!=null) {
            result.add(status);

            long inReplyToStatusId = status.getInReplyToStatusId();
            if (inReplyToStatusId!=-1)
                status = th.getStatusById(inReplyToStatusId,null);
            else
                status=null;
        }

        setListAdapter(new StatusAdapter<twitter4j.Status>(this, R.layout.list_item, result));

        ListView lv = getListView();
        lv.requestLayout();

    }

}
