package de.bsd.zwitscher.helper;

import android.util.Pair;

import java.util.Comparator;

/**
 * Comparator that does case insensitive comaring of two Pair&lt;String,?&gt;
 */
public class CaseInsensitivePairComparator implements Comparator<Pair<String,?>>
{
    @Override
    public int compare(Pair<String, ?> stringPair, Pair<String, ?> stringPair1) {
        return stringPair.first.compareToIgnoreCase(stringPair1.first);
    }
}
