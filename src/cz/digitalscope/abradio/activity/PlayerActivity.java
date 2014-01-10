package cz.digitalscope.abradio.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import org.json.JSONException;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.ArtWork;
import cz.digitalscope.abradio.common.Constanty;
import cz.digitalscope.abradio.service.PlaybackService;

public class PlayerActivity extends Activity implements OnClickListener,
        OnSeekBarChangeListener {

    GoogleAnalyticsTracker tracker;

    /**
     * Called when the activity is first created.
     */

    private TextView description = null;
    private TextView artist = null;
    private TextView song = null;
    private ImageView artImg = null;
    private ImageButton favBtn = null;
    private ImageView logo = null;
    private TextView radioName = null;
    private TextView sleepTime = null;
    private SeekBar volumeTrack = null;

    private BroadcastReceiver changeReceiver = new PlaybackChangeReceiverPlayer();
    private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiverPlayer();
    private BroadcastReceiver closeReceiver = new PlaybackCloseReceiverPlayer();

    private ServiceConnection conn;
    private AudioManager mgr;
    public static PlaybackService player;

    public void attachToPlaybackService() {
        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                player = ((PlaybackService.ListenBinder) service).getService();
                player.ani_pl = (ProgressBar) findViewById(R.id.ani_pl);
                if (player.aniState) {
                    player.ani_pl.setVisibility(View.VISIBLE);
                } else
                    player.ani_pl.setVisibility(View.INVISIBLE);

                Log.w("ABRadio", "CONNECT player");

                ((LinearLayout) findViewById(R.id.banner_player))
                        .addView(player.getBanner(PlayerActivity.this));
                setPlayInfo();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT player");
                player.ani_pl = null;
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
        setContentView(R.layout.player);

        // Start the tracker in manual dispatch mode...
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.startNewSession(Constanty.GA_CODE, 30, this);
        tracker.trackPageView(this.getLocalClassName());
        tracker.dispatch();

        volumeTrack = (SeekBar) findViewById(R.id.volumeSeek);
        volumeTrack.setOnSeekBarChangeListener(this);
        mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volumeTrack.setMax(mgr.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volumeTrack.setProgress(mgr.getStreamVolume(AudioManager.STREAM_MUSIC));

        favBtn = (ImageButton) findViewById(R.id.favBtn);

        description = (TextView) findViewById(R.id.descriptionText);
        artist = (TextView) findViewById(R.id.artist_text);
        song = (TextView) findViewById(R.id.song_text);
        artImg = (ImageView) findViewById(R.id.artImage);
        logo = (ImageView) findViewById(R.id.radioLogoImage);
        radioName = (TextView) findViewById(R.id.radioNameText);
        sleepTime = (TextView) findViewById(R.id.sleepTimeText);

        description.setText("");
        artist.setText("");
        song.setText("");
        radioName.setText("");
        sleepTime.setText("0 min");

        artImg.setImageDrawable(getResources().getDrawable(R.drawable.vodoznak));

        attachToPlaybackService();

        ((ImageButton) findViewById(R.id.program_btn)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.playBtn)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.stopBtn)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.sleepBtn)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.back_player)).setOnClickListener(this);
        ((ImageButton) findViewById(R.id.info_contakt_btn))
                .setOnClickListener(this);
        ((ImageButton) findViewById(R.id.facebook_btn))
                .setOnClickListener(this);
        favBtn.setOnClickListener(this);

    }

    protected void setPlayInfo() {
        if (player != null) {

            if (player.preference() != null
                    && player.preference().getBoolean("no_dimmer", true)) {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
            if (player.isFavorite()) {
                favBtn.setBackgroundResource(R.drawable.fav_remove_touch);
            } else
                favBtn.setBackgroundResource(R.drawable.fav_add_touch);

            // getCurrentFocus().setKeepScreenOn(player.isPlaying());
            if (radioName != null) {
                try {
                    radioName.setText(player.aktRadio.getString("name"));
                } catch (JSONException e) {
                    radioName.setText("");
                    e.printStackTrace();
                }
            }
            if (description != null) {
                try {
                    description.setText(player.aktRadio
                            .getString("description"));
                } catch (JSONException e) {
                    description.setText("");
                    e.printStackTrace();
                }
            }
            if (logo != null) {
                String urlLogo;
                try {
                    urlLogo = player.aktRadio.getString("logo_ipad");
                    if (urlLogo.length() == 0) {
                        urlLogo = player.aktRadio.getString("logo");
                    }
                } catch (JSONException e) {
                    urlLogo = "";
                    e.printStackTrace();
                }
                if (urlLogo.length() != 0) {
                    Drawable image = player.getImage(urlLogo, logo, true);
                    logo.setImageDrawable(image);
                }
            }
            ImageButton program_btn = (ImageButton) findViewById(R.id.program_btn);
            try {
                String prg = player.aktRadio.getString("program");
                if (prg.length() == 0) {
                    program_btn.setVisibility(View.INVISIBLE);
                } else
                    program_btn.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                program_btn.setVisibility(View.INVISIBLE);
                e.printStackTrace();
            }
            if (sleepTime != null) {
                sleepTime.setText(player.sleep().toString() + " min");
            }
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent_prg;
        if (player != null)
            switch (v.getId()) {

                case R.id.playBtn:
                    tracker.trackEvent("Clicks", // Category
                            "Play", // Action
                            "Player", // Label
                            9); // Value
                    player.play();
                    tracker.dispatch();
                    break;

                case R.id.stopBtn:
                    tracker.trackEvent("Clicks", // Category
                            "Stop", // Action
                            "Player", // Label
                            10); // Value
                    player.stop();
                    tracker.dispatch();
                    break;

                case R.id.info_contakt_btn:
                    tracker.trackEvent("Clicks", // Category
                            "Info/Contakt", // Action
                            "Player", // Label
                            11); // Value
                    tracker.dispatch();
                    intent_prg = new Intent(PlayerActivity.this,
                            ContactActivity.class);
                    startActivity(intent_prg);
                    break;

                case R.id.facebook_btn:
                    tracker.trackEvent("Clicks", // Category
                            "Facebook", // Action
                            "Player", // Label
                            12); // Value
                    tracker.dispatch();
                    String uri = Constanty.facebook_url;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                    break;

                case R.id.favBtn:
                    player.togleFovorite();
                    if (player.isFavorite()) {
                        favBtn.setBackgroundResource(R.drawable.fav_remove_touch);
                        tracker.trackEvent("Clicks", // Category
                                "RemoveFav", // Action
                                "Player", // Label
                                13); // Value
                        tracker.dispatch();
                    } else {
                        favBtn.setBackgroundResource(R.drawable.fav_add_touch);
                        tracker.trackEvent("Clicks", // Category
                                "AddFav", // Action
                                "Player", // Label
                                14); // Value
                        tracker.dispatch();
                    }
                    break;

                case R.id.program_btn:
                    intent_prg = new Intent(PlayerActivity.this,
                            WebViewProgramActivity.class);
                    startActivity(intent_prg);
                    tracker.trackEvent("Clicks", // Category
                            "Program", // Action
                            "Player", // Label
                            15); // Value
                    tracker.dispatch();
                    break;

                case R.id.back_player:
                    tracker.trackEvent("Clicks", // Category
                            "Close/Back", // Action
                            "Player", // Label
                            16); // Value
                    tracker.dispatch();
                    finish();
                    break;

                case R.id.sleepBtn:
                    tracker.trackEvent("Clicks", // Category
                            "Sleep", // Action
                            "Player", // Label
                            17); // Value
                    tracker.dispatch();
                    player.sleppToggle();
                    sleepTime.setText(player.sleep().toString() + " min");
                    break;

            }

    }

    class PlaybackChangeReceiverPlayer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setPlayInfo();
            try {
                artist.setText(player.artist);
                song.setText(player.title);
                String artwork = player.aktRadio.getString("artwork");
                if (artwork != "" && player.isPlaying()) {
                    new ArtWork(player, artImg, artist, song, getResources()
                            .getDrawable(R.drawable.vodoznak)).execute(artwork);
                } else
                    artImg.setImageDrawable(getResources().getDrawable(
                            R.drawable.vodoznak));

            } catch (JSONException e) {
                artImg.setImageDrawable(getResources().getDrawable(
                        R.drawable.vodoznak));
            }
        }
    }

    class PlaybackUpdateReceiverPlayer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }

    class PlaybackCloseReceiverPlayer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if (fromUser) {
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC,
                    seekBar.getProgress(), 0);
            Log.d("PlayerABRadio", "Set Volume by seek bar");

        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mgr.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), 0);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        super.onKeyLongPress(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeTrack.setProgress(0);
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC,
                    volumeTrack.getProgress(), 0);
            Log.d("PlayerABRadio", "Volume mute");
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            tracker.trackEvent("Clicks", // Category
                    "VolumeDows", // Action
                    "Player", // Label
                    18); // Value
            tracker.dispatch();
            volumeTrack.setProgress(volumeTrack.getProgress() - 1);
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC,
                    volumeTrack.getProgress(), 0);
            Log.d("PlayerABRadio", "Volume down");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            tracker.trackEvent("Clicks", // Category
                    "VolumeUp", // Action
                    "Player", // Label
                    19); // Value
            tracker.dispatch();
            volumeTrack.setProgress(volumeTrack.getProgress() + 1);
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC,
                    volumeTrack.getProgress(), 0);
            Log.d("PlayerABRadio", "Volume up");
            return true;
        }
        return false;
    }

}
