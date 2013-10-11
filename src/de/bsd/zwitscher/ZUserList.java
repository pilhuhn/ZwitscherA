package de.bsd.zwitscher;

import de.bsd.zwitscher.account.Account;

import java.util.ArrayList;
import java.util.List;

/**
 * A user list inside Zwitscher
 *
 * @author Heiko W. Rupp
 */
public class ZUserList {

    String listName;
    int listId;
    String ownerName;
    int unreadCount;

    public ZUserList(int listId, String listName, String ownerName) {
        this.listName = listName;
        this.listId = listId;
        this.ownerName = ownerName;
    }


    public String getDisplayName(Account account) {
        String listname;
        if (account.getName().equals(ownerName)) {
            listname = listName;
        }
        else {
            listname = "@" + ownerName + "/" + listName;
        }

        return listname;
    }

    public boolean matches(String name) {
        if (name.startsWith("@") && name.equals("@"+ownerName +"/" +listName)) {
            return true;
        }
        else if (!name.startsWith("@") && name.equals(listName)) {
            return true;
        }
        return false;
    }

    public static List<ZUserList> generateDefaults(Account account) {
        List<ZUserList> list = new ArrayList<ZUserList>(4);
        list.add(new ZUserList(0, "Home", account.getName()));
        list.add(new ZUserList(-1, "Mentions", account.getName()));
        list.add(new ZUserList(-3, "Sent", account.getName()));
        list.add(new ZUserList(-4, "Favorites", account.getName()));

        return list;
    }


        @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ZUserList zUserList = (ZUserList) o;

        if (listId != zUserList.listId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return listId;
    }
}
