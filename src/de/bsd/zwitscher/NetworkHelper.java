package de.bsd.zwitscher;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;


/**
 * TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class NetworkHelper {

    private Context context;
    ConnectivityManager cManager;
    SharedPreferences preferences;
    TelephonyManager telephonyManager;

    public NetworkHelper(Context context) {
        this.context = context;
        cManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public boolean mayDownloadImages() {

        String networkConfig = preferences.getString("networkConfig","1");
        boolean whenRoaming = preferences.getBoolean("roaming", false);

        NetworkInfo info = cManager.getActiveNetworkInfo();
        if (info==null) {
            Log.i("NetworkHelper","Can't get info about active network, blocking image load");
            return false;
        }
        int type = info.getType();
        int subType = info.getSubtype();
        /*
        11-05 21:26:00.359: INFO/NetworkHelper(329): Current network is MOBILE:UMTS
        11-05 21:26:00.359: INFO/NetworkHelper(329): Current network is 0:3
        Edge = 2
        Hsdpa = 8
        */

        Log.i("NetworkHelper","Current network is " + info.getTypeName() + ":" + info.getSubtypeName());
        Log.i("NetworkHelper","Current network is " + type + ":" + info.getSubtype());

        int configType = Integer.parseInt(networkConfig);
        if (configType==5) // Never
            return false;
        if (configType==1)
            return true; // Always


        if (type==ConnectivityManager.TYPE_MOBILE) {
            if (info.isRoaming() && !whenRoaming) {
                Log.d("NetworkHelper","Phone is roaming, but download disabled ");
                return false;
            }

            switch (configType) {
                case 1: return true;
                case 2: return subType > TelephonyManager.NETWORK_TYPE_UMTS;
                case 3: return subType >= TelephonyManager.NETWORK_TYPE_EDGE;
                case 4: return subType >= TelephonyManager.NETWORK_TYPE_GPRS;
                case 5: return false; // Never
                default:
                    Log.w("NetworkHelper","Unknown config type: " + configType);
                    return false;
            }



        } else if (type==ConnectivityManager.TYPE_WIFI) {
            return true; // Wifi is 'better' than mobile and 'Never' was caught earlier
        } else {
            Log.i("NetworkHelper","Unknown connectivity type: " + info.getTypeName());
            return false;
        }

     //   return true; // TODO fix
    }

    public boolean isOnline() {
        NetworkInfo info = cManager.getActiveNetworkInfo();

        if (info==null)
            return false;

        return true;
    }
}
