package eu.damek.abradio.activity;

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

import eu.damek.abradio.R;
import eu.damek.abradio.common.AbRadioSimpleListAdabter;
import eu.damek.abradio.common.Constants;
import eu.damek.abradio.service.PlaybackService;

public class OblibeneActivity extends Activity {


    private BroadcastReceiver changeReceiver = new PlaybackChangeReceiverPlayerFav();
    private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiverPlayerFav();
    private BroadcastReceiver closeReceiver = new PlaybackCloseReceiverPlayerFav();

    private ServiceConnection conn;
    public static PlaybackService player;

    private void showAll() {
        ListView favList = (ListView) findViewById(R.id.favListView);

        favList.addFooterView(player.getBanner(this));

        favList.setAdapter(new AbRadioSimpleListAdabter(player, this, 0, favList));
    }

    public void attachToPlaybackService() {
        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                player = ((PlaybackService.ListenBinder) service).getService();
                Log.w("ABRadio", "CONNECT oblibene");
                showAll();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT oblibene");
                player = null;
            }
        };

        // Explicitly start the service. Don't use BIND_AUTO_CREATE, since it
        // causes an implicit service stop when the last binder is removed.
        getBaseContext().getApplicationContext().startService(serviceIntent);
        getBaseContext().getApplicationContext().bindService(serviceIntent,
                conn, 0);

        getBaseContext().registerReceiver(changeReceiver,
                new IntentFilter(Constants.SERVICE_CHANGE_NAME));
        getBaseContext().registerReceiver(updateReceiver,
                new IntentFilter(Constants.SERVICE_UPDATE_NAME));
        getBaseContext().registerReceiver(closeReceiver,
                new IntentFilter(Constants.SERVICE_CLOSE_NAME));
    }

    @Override
    public void onDestroy() {
        getBaseContext().unregisterReceiver(closeReceiver);
        getBaseContext().unregisterReceiver(updateReceiver);
        getBaseContext().unregisterReceiver(changeReceiver);
        getBaseContext().getApplicationContext().unbindService(conn);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout of this activity
        setContentView(R.layout.oblibene);

        attachToPlaybackService();
    }

    class PlaybackChangeReceiverPlayerFav extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            ((ListView) findViewById(R.id.favListView)).invalidateViews();
        }
    }

    class PlaybackUpdateReceiverPlayerFav extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    class PlaybackCloseReceiverPlayerFav extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }
}
