package eu.damek.abradio.activity;

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

import org.json.JSONException;

import eu.damek.abradio.R;
import eu.damek.abradio.common.ArtWork;
import eu.damek.abradio.common.Constants;
import eu.damek.abradio.service.PlaybackService;

public class PlayerActivity extends Activity implements OnClickListener,
        OnSeekBarChangeListener {

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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.player);

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
        sleepTime.setText(R.string.zeroMinuts);

        artImg.setImageDrawable(getResources().getDrawable(R.drawable.vodoznak));

        attachToPlaybackService();

        findViewById(R.id.playBtn).setOnClickListener(this);
        findViewById(R.id.stopBtn).setOnClickListener(this);
        findViewById(R.id.sleepBtn).setOnClickListener(this);
        findViewById(R.id.back_player).setOnClickListener(this);
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
            if (sleepTime != null) {
                sleepTime.setText(player.sleep().toString() + getString(R.string.mins_add_to_text));
            }
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent_prg;
        if (player != null)
            switch (v.getId()) {

                case R.id.playBtn:
                    player.play();
                    break;

                case R.id.stopBtn:
                    player.stop();
                    break;

                case R.id.favBtn:
                    player.togleFovorite();
                    if (player.isFavorite()) {
                        favBtn.setBackgroundResource(R.drawable.fav_remove_touch);
                    } else {
                        favBtn.setBackgroundResource(R.drawable.fav_add_touch);
                    }
                    break;

                case R.id.back_player:
                    finish();
                    break;

                case R.id.sleepBtn:
                    player.sleppToggle();
                    sleepTime.setText(player.sleep().toString() + getText(R.string.mins_add_to_text));
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
                if (!artwork.equalsIgnoreCase("") && player.isPlaying()) {
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
            volumeTrack.setProgress(volumeTrack.getProgress() - 1);
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC,
                    volumeTrack.getProgress(), 0);
            Log.d("PlayerABRadio", "Volume down");
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeTrack.setProgress(volumeTrack.getProgress() + 1);
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC,
                    volumeTrack.getProgress(), 0);
            Log.d("PlayerABRadio", "Volume up");
            return true;
        }
        return false;
    }

}
