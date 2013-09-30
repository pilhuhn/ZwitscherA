package de.bsd.zwitscher.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * Helper that deals with network availability
 *
 * @author Heiko W. Rupp
 */
public class NetworkHelper {

    private ConnectivityManager cManager;
    private SharedPreferences preferences;

    public NetworkHelper(Context context) {
        cManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean mayDownloadImages() {

        String networkConfig = preferences.getString("networkConfig","1");
        boolean whenRoaming = preferences.getBoolean("roaming", false);

        return downLoadOk(networkConfig, whenRoaming);
    }

    public boolean mayReloadAdditional() {

        String networkConfig = preferences.getString("extraload_networkConfig","1");
        boolean whenRoaming = preferences.getBoolean("extraload_roaming", false);

        return downLoadOk(networkConfig, whenRoaming);
    }

    private boolean downLoadOk(String networkConfig, boolean whenRoaming) {
        NetworkInfo info = cManager.getActiveNetworkInfo();
        if (info==null) {
            Log.i("NetworkHelper", "Can't get info about active network, blocking down load");
            return false;
        }
        int type = info.getType();
        int subType = info.getSubtype();

        int configType;
        try {
            configType = Integer.parseInt(networkConfig);
        } catch (NumberFormatException e) {
            Log.w("NetworkHelper",e.getMessage());
            configType = 6;
        }
        if (configType==6) // Never
            return false;

        if (type== ConnectivityManager.TYPE_MOBILE) {
            if (info.isRoaming() && !whenRoaming) {
                Log.d("NetworkHelper","Phone is roaming, but download disabled ");
                return false;
            }

            switch (configType) {
                case 1: return true;  // Always
                case 2: return false; // WiFi necessary - but this is a mobile network
                case 3: return subType >= TelephonyManager.NETWORK_TYPE_UMTS;
                case 4: return subType >= TelephonyManager.NETWORK_TYPE_EDGE;
                case 5: return subType >= TelephonyManager.NETWORK_TYPE_GPRS;
                case 6: return false; // Never
                default:
                    Log.w("NetworkHelper","Unknown config type: " + configType);
                    return false;
            }

        } else if (type==ConnectivityManager.TYPE_WIFI && configType<6) {
            return true; // Wifi is 'better' than mobile and 'Never' was caught earlier
        } else {
            Log.i("NetworkHelper","Unknown connectivity type: " + info.getTypeName());
            return false;
        }
    }

    public boolean isOnline() {
        NetworkInfo info = cManager.getActiveNetworkInfo();

        return info != null;

    }
}
