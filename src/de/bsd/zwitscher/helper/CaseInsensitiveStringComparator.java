package de.bsd.zwitscher.helper;

import java.io.Serializable;
import java.util.Comparator;


/**
 * Comparator for strings that is case insensitive
 * @author Heiko W. Rupp
 */
public class CaseInsensitiveStringComparator implements Comparator<String>, Serializable {

   @Override
   public int compare(java.lang.String string, java.lang.String string1) {
      return string.compareToIgnoreCase(string1);
   }
}
