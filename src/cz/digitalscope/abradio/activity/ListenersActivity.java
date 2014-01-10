package cz.digitalscope.abradio.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ListView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.Constanty;
import cz.digitalscope.abradio.common.SimpleListAdabter;
import cz.digitalscope.abradio.service.PlaybackService;

public class ListenersActivity extends Activity {

    GoogleAnalyticsTracker tracker;

    private BroadcastReceiver changeReceiver = new PlaybackChangeReceiverPlayerList();
    private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiverPlayerList();
    private BroadcastReceiver closeReceiver = new PlaybackCloseReceiverPlayerList();

    private ServiceConnection conn;
    public static PlaybackService player;

    private void showAll() {
        ListView listenersList = (ListView) findViewById(R.id.listenersListView);

        listenersList.addFooterView(player.getBanner(this));

        listenersList.setAdapter(new SimpleListAdabter(player, this, 1, listenersList));
    }

    public void attachToPlaybackService() {
        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                player = ((PlaybackService.ListenBinder) service).getService();
                Log.w("ABRadio", "CONNECT poslouchane");
                showAll();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT poslouchane");
                player = null;
            }
        };

        // Explicitly start the service. Don't use BIND_AUTO_CREATE, since it
        // causes an implicit service stop when the last binder is removed.
        getBaseContext().getApplicationContext().startService(serviceIntent);
        getBaseContext().getApplicationContext().bindService(serviceIntent,
                conn, 0);

        getBaseContext().registerReceiver(changeReceiver,
                new IntentFilter(Constanty.SERVICE_CHANGE_NAME));
        getBaseContext().registerReceiver(updateReceiver,
                new IntentFilter(Constanty.SERVICE_UPDATE_NAME));
        getBaseContext().registerReceiver(closeReceiver,
                new IntentFilter(Constanty.SERVICE_CLOSE_NAME));
    }

    @Override
    public void onDestroy() {
        getBaseContext().unregisterReceiver(closeReceiver);
        getBaseContext().unregisterReceiver(updateReceiver);
        getBaseContext().unregisterReceiver(changeReceiver);
        getBaseContext().getApplicationContext().unbindService(conn);
        super.onDestroy();
        tracker.stopSession();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout of this activity
        setContentView(R.layout.listeners);

        // Start the tracker in manual dispatch mode...
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.startNewSession(Constanty.GA_CODE, 30, this);
        tracker.trackPageView(this.getLocalClassName());
        tracker.dispatch();

        attachToPlaybackService();
    }

    class PlaybackChangeReceiverPlayerList extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((ListView) findViewById(R.id.listenersListView)).invalidateViews();
        }
    }

    class PlaybackUpdateReceiverPlayerList extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    class PlaybackCloseReceiverPlayerList extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }
}
