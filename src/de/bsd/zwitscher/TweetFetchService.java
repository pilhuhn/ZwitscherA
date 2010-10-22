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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import twitter4j.Paging;
import twitter4j.Status;

import java.util.List;

/**
 * TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class TweetFetchService extends Service {

    private boolean isRunning = false;
    private boolean shouldRun = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // Not needed for a local service
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);    // TODO: Customise this generated block

        if (isRunning) {
            Log.d("TFS::onStart","already running, do nothing");
            return;
        }
        isRunning=true;
        Thread t = new Thread(new Fetcher());
            t.start();
    }

    @Override
    public void onDestroy() {
        Log.d("TFS::onDestroy","Shutting down");
        shouldRun=false;
        isRunning=false;
    }

    private class Fetcher implements Runnable {

        TweetDB tdb = new TweetDB(getApplicationContext());
        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         * <p/>
         * The general contract of the method <code>run</code> is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            while (shouldRun) {
                Log.d("TFS:Fetcher:run","About to fetch");
                TwitterHelper th = new TwitterHelper(getApplicationContext());
                Paging p = new Paging();
                long lastRead = getLastRead("home");
                p.setSinceId(lastRead);
                List<Status> status = th.getTimeline(p,R.string.home_timeline,false, false);
                Log.d("TFS:Fetcher:run","Got "+ status.size() + " entries");
                if (status.size()>0) {
                    long last = status.get(0).getId(); // assumption is that twitter sends the newest (=highest id) first
                    tdb.updateOrInsertLastRead("home", last);
                }


                try {
                    Thread.sleep(1000*60*5);
                } catch (InterruptedException e) {
                    shouldRun = false;
                }
            }
            Log.d("TFS:Fetcher:run","Left the run loop");
        }

        private long getLastRead(String name) {
            return tdb.getLastRead(name);
        }
    }
}
