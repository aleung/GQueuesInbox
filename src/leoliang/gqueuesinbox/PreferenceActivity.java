package leoliang.gqueuesinbox;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;

public class PreferenceActivity extends android.preference.PreferenceActivity {

    private void clearNotification() {
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mgr.cancel(Constants.NOTIFICATION_INCORRECT_KEY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onPause() {
        super.onPause();
        startService(new Intent(this, SendService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        clearNotification();
    }
}
