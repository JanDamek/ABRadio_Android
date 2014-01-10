package cz.digitalscope.abradio.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
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

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.Constanty;

public class WebViewActivity extends Activity implements OnClickListener {

    private WebView webview;
    GoogleAnalyticsTracker tracker;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.webview);

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
                activity.setProgress(progress * 100);
                ProgressBar pr = (ProgressBar) findViewById(R.id.progres_web);
                if (progress < 100) {
                    pr.setProgress(progress);
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

        ((Button) findViewById(R.id.back_btn)).setOnClickListener(this);
        ((Button) findViewById(R.id.refresh_btn)).setOnClickListener(this);
        ((Button) findViewById(R.id.next_btn)).setOnClickListener(this);

        webview.loadUrl("http://www.abradio.cz/");

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.next_btn:
                webview.goForward();
                tracker.trackEvent(
                        "Clicks",  // Category
                        "Next",  // Action
                        "webView", // Label
                        1);       // Value
                tracker.dispatch();
                break;

            case R.id.back_btn:
                webview.goBack();
                tracker.trackEvent(
                        "Clicks",  // Category
                        "Back",  // Action
                        "webView", // Label
                        2);       // Value
                tracker.dispatch();
                break;

            case R.id.refresh_btn:
                webview.reload();
                tracker.trackEvent(
                        "Clicks",  // Category
                        "Refresh",  // Action
                        "webView", // Label
                        3);       // Value
                tracker.dispatch();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the tracker when it is no longer needed.
        tracker.stopSession();
    }

}