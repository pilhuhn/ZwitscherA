package de.bsd.zwitscher.preferences;

/**
 * Interface that can be implemented for a callback from the ExpandableListPreference to
 * check if the user given values are valid or not.
 *
 * @author Heiko W. Rupp
 * @see ExpandableListPreference
 */
public interface VerifyCallback {

    boolean verify(String entry);
}
