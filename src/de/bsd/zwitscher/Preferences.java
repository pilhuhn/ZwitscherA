package de.bsd.zwitscher;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.widget.Toast;
import de.bsd.zwitscher.other.ReadItLaterStore;

public class Preferences extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

	/** Called when the activity is first created. */
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	}

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(preferences, "ril_user");
        setRilPassSummary(preferences, findPreference("ril_password"));
        if (Build.VERSION.SDK_INT>=11) {
            // The experimental UI needs v11 (Honeycomb or later)
            findPreference("newui").setEnabled(true);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Preference p = findPreference(key);
        if (key.equals("ril_user")) {
            p.setSummary(sharedPreferences.getString("ril_user",""));
        }

        if (key.equals("ril_password")) {
            setRilPassSummary(sharedPreferences, p);


            //  check if account is valid
            ReadItLaterStore store = new ReadItLaterStore(sharedPreferences.getString("ril_user",""),sharedPreferences.getString("ril_password",""));
            boolean success = store.verifyAccount();

            if (!success) {
                Toast.makeText(this,getString(R.string.account_not_valid),Toast.LENGTH_LONG).show();
                p.setSummary(R.string.invalid);
            }
        }

    }

    private void setRilPassSummary(SharedPreferences sharedPreferences, Preference p) {
        String ril_password = sharedPreferences.getString("ril_password", "");
        if (ril_password.equals(""))
            p.setSummary(R.string.unset);
        else
            p.setSummary(R.string.set);
    }
}
