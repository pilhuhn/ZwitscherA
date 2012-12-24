package de.bsd.zwitscher;

import java.util.HashSet;
import java.util.Set;

/**
 * Keep state about which user images are currently
 * fetched to remove duplicate http requests for images
 * while scrolling through the list
 *
 * @author Heiko W. Rupp
 */
public class PicHelperState {
    private static PicHelperState ourInstance = new PicHelperState();

    public static PicHelperState getInstance() {
        return ourInstance;
    }

    private final Set<String> userInSync = new HashSet<String>();

    private PicHelperState() {
    }

    public void setSyncing(String user) {
        synchronized (userInSync) {
            userInSync.add(user);
        }
//        System.out.println("++ set syncing " + user);
    }

    public boolean isSyncing(String user) {
        boolean contains;
        synchronized (userInSync) {
            contains = userInSync.contains(user);
        }
//        System.out.println("++ is syncing " + user + ": " + contains);
        return contains;
    }

    public void syncDone(String user) {
//        System.out.println("++ sync done " + user);
        synchronized (userInSync) {
            userInSync.remove(user);
        }
    }
}
