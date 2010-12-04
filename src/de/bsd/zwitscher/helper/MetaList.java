package de.bsd.zwitscher.helper;


import java.util.ArrayList;
import java.util.List;

/**
 * List of stuff + some metadata
 *
 * @author Heiko W. Rupp
 */
public class MetaList<T> {

    /** The actual list */
    List<T> list = new ArrayList<T>();
    /** Number of original (= received from server) items */
    int numOriginal;
    /** Number of items filled up */
    int numAdded;

    /**
     * Create a new empty MetaList with the list initialized
     */
    public MetaList() {
    }

    /**
     * Create a new MetaList from the parameters passed
     * @param list The list to store
     * @param numOriginal Number of received items
     * @param numAdded Number of items added from DB
     */
    public MetaList(List<T> list, int numOriginal, int numAdded) {
        this.list = list;
        this.numOriginal = numOriginal;
        this.numAdded = numAdded;
    }

    public List<T> getList() {
        return list;
    }

    public int getNumOriginal() {
        return numOriginal;
    }

    public int getNumAdded() {
        return numAdded;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("MetaList");
        sb.append("{listSize=").append(list.size());
        sb.append(", numOriginal=").append(numOriginal);
        sb.append(", numAdded=").append(numAdded);
        sb.append('}');
        return sb.toString();
    }
}
