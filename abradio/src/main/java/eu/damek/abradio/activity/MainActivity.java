package eu.damek.abradio.activity;

import android.app.AlertDialog.Builder;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TabHost;

import com.google.android.gms.ads.MobileAds;

import eu.damek.abradio.R;
import eu.damek.abradio.common.Constants;
import eu.damek.abradio.service.PlaybackService;

public class MainActivity extends TabActivity implements
        OnCheckedChangeListener, OnClickListener {

    // Define 1 tab host that will host four tabs
    private TabHost tabHost;

    private BroadcastReceiver changeReceiver = new PlaybackChangeReceiverMain();
    private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiverMain();
    private BroadcastReceiver closeReceiver = new PlaybackCloseReceiverMain();

    private ServiceConnection conn;
    public static PlaybackService player;

    public void attachToPlaybackService() {
        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                player = ((PlaybackService.ListenBinder) service).getService();
                if (player.categories().length() == 0) {
                    Builder dlg = new Builder(MainActivity.this);
                    dlg.setTitle(getResources().getString(R.string.chyba_title));
                    dlg.setMessage(getResources().getString(
                            R.string.nejsou_data));
                    dlg.setNegativeButton(
                            getResources().getString(R.string.stopapp_btn),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    MainActivity.this.finish();
                                }
                            });
                    dlg.show();
                }
                Log.w("ABRadio", "CONNECT main activity");

                player.ani = (ProgressBar) findViewById(R.id.ani);
                if (player.aniState) {
                    player.ani.setVisibility(View.VISIBLE);
                } else
                    player.ani.setVisibility(View.INVISIBLE);

                if (player.isPlaying()) {
                    findViewById(R.id.button1)
                            .setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.button1)
                            .setVisibility(View.INVISIBLE);
                }

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT main activity");
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-9508528448741167~4014523050");

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set the layout of this activity. The tabs are defined there.
        setContentView(R.layout.tabs);

        // Initialize intents, Change it in your app with your Class names.
        Intent tab1Intent = new Intent(this, ABradioActivity.class);
        Intent tab2Intent = new Intent(this, OblibeneActivity.class);
        Intent tab3Intent = new Intent(this, ListenersActivity.class);

        // Initialize the radio buttons that represent tab changer button
        // This buttons are defined in the layout file main.xml in the layout
        // folder
        ((RadioButton) findViewById(R.id.rb1)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.rb2)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.rb3)).setOnCheckedChangeListener(this);

        findViewById(R.id.button1).setOnClickListener(this);

        // Now setup the TabHost, get TabHost From the activity
        this.tabHost = getTabHost();

        // Now initialize the tabs
        // Look at buildTabs() method bellow for information about parameters
        this.tabHost.addTab(buildTabs("first_tab", R.string.tab_1_title,
                R.drawable.place_unselected, tab1Intent));

        this.tabHost.addTab(buildTabs("second_tab", R.string.tab_2_title,
                R.drawable.favorite_unselected, tab2Intent));

        this.tabHost.addTab(buildTabs("third_tab", R.string.tab_3_title,
                R.drawable.group_unselected, tab3Intent));


        findViewById(R.id.button1).setVisibility(View.INVISIBLE);

        attachToPlaybackService();
    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Event Handling for Individual menu item selected Identify single menu
     * item by it's id
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.setting:
                Intent intent = new Intent(MainActivity.this,
                        NastaveniActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private TabHost.TabSpec buildTabs(String tag, int resLabel, int resIcon,
                                      final Intent content) {
        return this.tabHost
                .newTabSpec(tag)
                .setIndicator(getString(resLabel),
                        getResources().getDrawable(resIcon))
                .setContent(content);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        if (isChecked) {
            switch (buttonView.getId()) {
                case R.id.rb1:
                    this.tabHost.setCurrentTabByTag("first_tab");
                    break;
                case R.id.rb2:
                    this.tabHost.setCurrentTabByTag("second_tab");
                    break;
                case R.id.rb3:
                    this.tabHost.setCurrentTabByTag("third_tab");
                    break;
            }

            if (player != null && player.isPlaying()) {
                findViewById(R.id.button1)
                        .setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.button1)
                        .setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }

    class PlaybackChangeReceiverMain extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (player != null && player.isPlaying()) {
                findViewById(R.id.button1)
                        .setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.button1)
                        .setVisibility(View.INVISIBLE);
            }
        }
    }

    class PlaybackUpdateReceiverMain extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (player != null && player.isPlaying()) {
                findViewById(R.id.button1)
                        .setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.button1)
                        .setVisibility(View.INVISIBLE);
            }
        }
    }

    class PlaybackCloseReceiverMain extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (player != null && player.isPlaying()) {
                findViewById(R.id.button1)
                        .setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.button1)
                        .setVisibility(View.INVISIBLE);
            }
        }
    }
}
