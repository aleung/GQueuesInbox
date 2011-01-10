package leoliang.gqueuesinbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Note: Background data setting is ignored, always use data service when it's available.
 */
public class NetworkReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "GQueuesInbox.NetworkReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isNetworkDown = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        if (isNetworkDown) {
            Log.d(LOG_TAG, "onReceive: Network NOT connected, stopping SendService");
            context.stopService(new Intent(context, SendService.class));
        } else {
            Log.d(LOG_TAG, "onReceive: Network connected, starting SendService");
            context.startService(new Intent(context, SendService.class));
        }
    }

}
