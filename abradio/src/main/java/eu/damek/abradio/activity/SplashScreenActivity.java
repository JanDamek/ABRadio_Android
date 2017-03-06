package eu.damek.abradio.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import eu.damek.abradio.R;
import eu.damek.abradio.service.PlaybackService;

public class SplashScreenActivity extends Activity {

    /**
     * The thread to process splash screen events
     */
    private static PlaybackService player = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.startscreen);

        class WaitAndLoadImages extends AsyncTask<Integer, Integer, Integer> {

            @Override
            protected Integer doInBackground(Integer... params) {
                while (player == null) {
                    try {
                        wait(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                JSONArray cat = player.categories();
                if (player.catCount == 0 || cat.length() > player.catCount) {
                    for (int i = 0; i < cat.length(); i++) {
                        JSONObject c;
                        try {
                            c = cat.getJSONObject(i);
                            JSONArray rad;
                            try {
                                rad = c.getJSONArray("radios");
                                for (int i1 = 0; i1 < rad.length(); i1++) {
                                    JSONObject r = rad.getJSONObject(i1);
                                    String logo = r.getString("logo");
                                    URL url;
                                    try {
                                        url = new URL(logo);
                                        Object content;
                                        try {
                                            content = url.getContent();
                                            InputStream is = (InputStream) content;
                                            Drawable image = Drawable
                                                    .createFromStream(is, "src");
                                            player.imageCache.put(logo, image);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } catch (MalformedURLException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    player.catCount = cat.length();
                }
                return 0;
            }

            @Override
            protected void onPostExecute(Integer result) {
                player.isInLoadImages = false;
                Intent intent = new Intent(SplashScreenActivity.this,
                        MainActivity.class);
                startActivity(intent);
            }

        }

        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        ServiceConnection conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                player = ((PlaybackService.ListenBinder) service).getService();
                if (player.isOnline()) {
                    Log.w("ABRadio", "CONNECT splash screen");

                    if (!player.isInLoadImages) {
                        player.isInLoadImages = true;
                        new WaitAndLoadImages().execute(0);
                    }
                } else {
                    Toast.makeText(SplashScreenActivity.this, "Aplikaci nelze spustit bez aktivniho internetu.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT splash screen");
                player = null;
            }
        };
        getBaseContext().getApplicationContext().startService(serviceIntent);
        getBaseContext().getApplicationContext().bindService(serviceIntent,
                conn, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}