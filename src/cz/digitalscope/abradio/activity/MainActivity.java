package cz.digitalscope.abradio.activity;

import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TabHost;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.Constanty;
import cz.digitalscope.abradio.service.PlaybackService;

public class MainActivity extends TabActivity implements
        OnCheckedChangeListener, OnClickListener {

    GoogleAnalyticsTracker tracker;

    // Define 1 tab host that will host four tabs
    private TabHost tabHost;

    // Define the intents to be activated when user want to change the tab
    private Intent tab1Intent;
    private Intent tab2Intent;
    private Intent tab3Intent;
    private Intent tab4Intent;
    // private Intent tab5Intent;

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
                    Builder dlg = new AlertDialog.Builder(MainActivity.this);
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
                    ((Button) findViewById(R.id.button1))
                            .setVisibility(View.VISIBLE);
                } else {
                    ((Button) findViewById(R.id.button1))
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Set the layout of this activity. The tabs are defined there.
        setContentView(R.layout.tabs);

        // Start the tracker in manual dispatch mode...
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.startNewSession(Constanty.GA_CODE, 30, this);
        tracker.trackPageView(this.getLocalClassName());
        tracker.dispatch();

        // Initialize intents, Change it in your app with your Class names.
        this.tab1Intent = new Intent(this, ABradioActivity.class); // Intent
        // triger
        // when
        // users
        // clicks on
        // first tab
        this.tab2Intent = new Intent(this, OblibeneActivity.class);// Intent
        // triger
        // when
        // users
        // clicks on
        // second
        // tab
        this.tab3Intent = new Intent(this, ListenersActivity.class);// Intent
        // triger
        // when users
        // clicks on
        // third tab
        this.tab4Intent = new Intent(this, WebViewActivity.class);// Intent
        // triger
        // when
        // users
        // clicks on
        // fourth
        // tab
        // this.tab5Intent
        // = new
        // Intent(this,
        // ABRadioPlayerActivity.class);

        // Initialize the radio buttons that represent tab changer button
        // This buttons are defined in the layout file main.xml in the layout
        // folder
        ((RadioButton) findViewById(R.id.rb1)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.rb2)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.rb3)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.rb4)).setOnCheckedChangeListener(this);

        ((Button) findViewById(R.id.button1)).setOnClickListener(this);

        // Now setup the TabHost, get TabHost From the activity
        this.tabHost = getTabHost();

        // Now initialize the tabs
        // Look at buildTabs() method bellow for information about parameters
        this.tabHost.addTab(buildTabs("first_tab", R.string.tab_1_title,
                R.drawable.place_unselected, this.tab1Intent));

        this.tabHost.addTab(buildTabs("second_tab", R.string.tab_2_title,
                R.drawable.favorite_unselected, this.tab2Intent));

        this.tabHost.addTab(buildTabs("third_tab", R.string.tab_3_title,
                R.drawable.group_unselected, this.tab3Intent));

        this.tabHost.addTab(buildTabs("fourth_tab", R.string.tab_4_title,
                R.drawable.user_unselected, this.tab4Intent));

        // this.tabHost.addTab(buildTabs("player", R.string.tab_4_title,
        // R.drawable.user_unselected, this.tab5Intent));

        ((Button) findViewById(R.id.button1)).setVisibility(View.INVISIBLE);

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
                tracker.trackEvent("Clicks", // Category
                        "MenuSetting", // Action
                        "Main", // Label
                        5); // Value
                tracker.dispatch();
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
                case R.id.rb4:
                    this.tabHost.setCurrentTabByTag("fourth_tab");
                    break;
            }

            if (player != null && player.isPlaying()) {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.VISIBLE);
            } else {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        // this.tabHost.setCurrentTab(-1);
        //
        // RadioButton rb = (RadioButton) findViewById(R.id.rb5);
        // rb.setChecked(true);
        //
        // ((Button) findViewById(R.id.button1)).setVisibility(View.INVISIBLE);
        //
        // this.tabHost.setCurrentTabByTag("player");
        Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
        startActivity(intent);
    }

    class PlaybackChangeReceiverMain extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!((RadioButton) findViewById(R.id.rb5)).isChecked()
                    && player != null && player.isPlaying()) {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.VISIBLE);
            } else {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.INVISIBLE);
            }
        }
    }

    class PlaybackUpdateReceiverMain extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (player != null && player.isPlaying()) {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.VISIBLE);
            } else {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.INVISIBLE);
            }
        }
    }

    class PlaybackCloseReceiverMain extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (player != null && player.isPlaying()) {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.VISIBLE);
            } else {
                ((Button) findViewById(R.id.button1))
                        .setVisibility(View.INVISIBLE);
            }
        }
    }
}