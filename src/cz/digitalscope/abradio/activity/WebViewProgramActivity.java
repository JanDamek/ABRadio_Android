package cz.digitalscope.abradio.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import org.json.JSONException;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.Constanty;
import cz.digitalscope.abradio.service.PlaybackService;

public class WebViewProgramActivity extends Activity implements OnClickListener {

    private WebView webview;
    private ServiceConnection conn;
    GoogleAnalyticsTracker tracker;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.webview_program);

        // Start the tracker in manual dispatch mode...
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.startNewSession(Constanty.GA_CODE, 30, this);
        tracker.trackPageView(this.getLocalClassName());
        tracker.dispatch();

        webview = (WebView) findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);

        final Activity activity = this;
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                activity.setProgress(progress);
                ProgressBar pr = (ProgressBar) findViewById(R.id.progres_web);
                if (progress < 100) {
                    pr.setProgress(progress * 100);
                    pr.setVisibility(View.VISIBLE);
                } else
                    pr.setVisibility(View.INVISIBLE);
            }
        });
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                Toast.makeText(activity, "Chyba! " + description,
                        Toast.LENGTH_SHORT).show();
            }
        });

        ((Button) findViewById(R.id.refresh_btn)).setOnClickListener(this);
        ((Button) findViewById(R.id.back_btn_program)).setOnClickListener(this);

        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        conn = new ServiceConnection() {
            private PlaybackService player;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                player = ((PlaybackService.ListenBinder) service).getService();
                Log.w("ABRadio", "CONNECT web view program");
                if (player != null && player.aktRadio != null) {
                    try {
                        String programURL = player.aktRadio
                                .getString("program");
                        if (programURL != "") {
                            webview.loadUrl(programURL);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT web view program");
                player = null;
            }
        };

        getBaseContext().getApplicationContext().startService(serviceIntent);
        getBaseContext().getApplicationContext().bindService(serviceIntent,
                conn, 0);
    }

    @Override
    public void onDestroy() {
        getBaseContext().getApplicationContext().unbindService(conn);
        super.onDestroy();
        tracker.stopSession();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next_btn:
                webview.goForward();
                tracker.trackEvent("Clicks", // Category
                        "Next", // Action
                        "webViewProgram", // Label
                        6); // Value
                tracker.dispatch();

                break;

            case R.id.back_btn:
                webview.goBack();
                tracker.trackEvent("Clicks", // Category
                        "Back", // Action
                        "webViewProgram", // Label
                        7); // Value
                tracker.dispatch();
                break;

            case R.id.refresh_btn:
                webview.reload();
                tracker.trackEvent("Clicks", // Category
                        "Refresh", // Action
                        "webViewProgram", // Label
                        8); // Value
                tracker.dispatch();
                break;

            case R.id.back_btn_program:
                tracker.trackEvent("Clicks", // Category
                        "Close/Back", // Action
                        "webViewProgram", // Label
                        8); // Value
                tracker.dispatch();
                finish();
        }
    }

}
