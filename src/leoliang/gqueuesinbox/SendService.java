package leoliang.gqueuesinbox;

import java.net.URI;

import leoliang.gqueuesinbox.ItemRepository.Item;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Proxy;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class SendService extends Service {

    /**
     * Runnable that performs the actual sending to GQueues service
     */
    private class Sender implements Runnable {

        private volatile Thread runner;

        @Override
        public void run() {
            HttpClient httpClient = createHttpClient();
            ItemRepository repository = new ItemRepository(getApplicationContext());
            try {
                for (Item item : repository.getAll()) {
                    if (Thread.currentThread() != runner) {
                        Log.d(LOG_TAG, "Sender is stopped.");
                        return;
                    }
                    if (item.hasSent()) {
                        continue;
                    }
                    try {
                        sendItem(item, httpClient);
                        Log.v(LOG_TAG, "Item " + item.getKey() + " has been sent");
                        repository.setHasSent(item.getKey());
                    } catch (SendException e) {
                        if (e.getErrorCode() == 403) {
                            Notification notification = new Notification(android.R.drawable.stat_notify_error,
                                    "GQueueInbox send failed", System.currentTimeMillis());
                            notification.setLatestEventInfo(SendService.this, "Incorrect GQueue private key",
                                    "Press here to input private key", PendingIntent.getActivity(SendService.this, 0,
                                            new Intent(SendService.this, PreferenceActivity.class), 0));
                            NotificationManager notificationManger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notificationManger.notify(Constants.NOTIFICATION_INCORRECT_KEY, notification);
                        }
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                Log.d(LOG_TAG, "Nothing to send, stop itself.");
                stopSelf();
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        public synchronized void startThread() {
            if (runner == null) {
                runner = new Thread(this);
                runner.start();
            }
        }

        public synchronized void stopThread() {
            if (runner != null) {
                Thread moribund = runner;
                runner = null;
                moribund.interrupt();
            }
        }

    }

    private class SendException extends Exception {
        private int errorCode = 0;

        public SendException(Exception e) {
            super(e);
        }

        public SendException(int errorCode) {
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }

    }

    private static final String LOG_TAG = "GQueuesInbox.SendService";

    private Sender sender = new Sender();

    private HttpClient createHttpClient() {
        HttpClient httpClient = new DefaultHttpClient();
        String proxyHost = Proxy.getHost(getBaseContext());
        int proxyPort = Proxy.getPort(getBaseContext());
        if ((proxyHost != null) && (proxyPort > 0)) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        return httpClient;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // not supported
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "onDestroy'd");
        sender.stopThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStart(intent, startId);
        Log.v(LOG_TAG, "onStart'd");
        sender.startThread();
        return START_NOT_STICKY;
    }

    private void sendItem(ItemRepository.Item item, HttpClient httpClient) throws SendException {
        int statusCode;
        try {
            String key = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
                    "gqueues_private_key_preference", "");
            URI uri = new URI("http", null, "www.gqueues.com", -1, "/newItem/", String.format(
                    "authKey=%s&description=%s&notes=%s", key, item.getDescription(), item.getNote()), null);
            Log.v(LOG_TAG, "HTTP GET " + uri);
            HttpUriRequest request = new HttpGet(uri);
            HttpResponse response = httpClient.execute(request);
            Log.v(LOG_TAG, "Response status line: " + response.getStatusLine());
            statusCode = response.getStatusLine().getStatusCode();
            if ((statusCode == HttpStatus.SC_OK) || (statusCode == HttpStatus.SC_CREATED)
                    || (statusCode == HttpStatus.SC_ACCEPTED)) {
                return;
            }
        } catch (Exception e) {
            throw new SendException(e);
            //        } catch (ClientProtocolException e) {
            //            // TODO Auto-generated catch block
            //            e.printStackTrace();
            //        } catch (IOException e) {
            //            // TODO Auto-generated catch block
            //            e.printStackTrace();
            //        } catch (URISyntaxException e) {
            //            // TODO Auto-generated catch block
            //            e.printStackTrace();
        }
        throw new SendException(statusCode);
    }
}
