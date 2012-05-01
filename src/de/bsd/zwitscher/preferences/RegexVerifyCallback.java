package de.bsd.zwitscher.preferences;


import android.util.Log;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Verifier callback that checks for valid Rexexp patterns. Called from
 * ExpandableListPreference.onDialogClosed if defined in preferences.xml
 *
 * @author Heiko W. Rupp
 * @see ExpandableListPreference#onDialogClosed(boolean)
 */
public class RegexVerifyCallback implements VerifyCallback {

    @Override
    public boolean verify(String entry) {
        try {
            Pattern.compile(entry);
            return true;
        }
        catch (PatternSyntaxException e) {
            Log.w("RegexVerifyCallback","Pattern [" + entry + "] was invalid: " + e.getLocalizedMessage());
            return false;
        }
    }
}
