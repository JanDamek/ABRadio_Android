package cz.digitalscope.abradio.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.Constanty;

public class ContactActivity extends Activity implements OnClickListener {

    GoogleAnalyticsTracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.contactview);

        // Start the tracker in manual dispatch mode...
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.startNewSession(Constanty.GA_CODE, 30, this);
        tracker.trackPageView(this.getLocalClassName());
        tracker.dispatch();

        ((ImageButton) findViewById(R.id.kontaktujte_nas_btn)).setOnClickListener(this);
        ((Button) findViewById(R.id.digitalscope_button)).setOnClickListener(this);
        ((Button) findViewById(R.id.limemedia_button)).setOnClickListener(this);
        ((Button) findViewById(R.id.zavrit_btn_contant)).setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        String uri;
        Intent intent;
        switch (v.getId()) {
            case R.id.kontaktujte_nas_btn:
                final Intent email_Intent = new Intent(android.content.Intent.ACTION_SEND);
                email_Intent.setType("text/plain");
                email_Intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{Constanty.napiste_nam_mail});
                email_Intent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.mail_predmet));
                email_Intent.putExtra(android.content.Intent.EXTRA_TEXT, getResources().getString(R.string.mail_body));
                try {
                    ContactActivity.this.startActivity(android.content.Intent.createChooser(email_Intent, getResources().getString(R.string.mail_zaslat)));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(ContactActivity.this, getResources().getString(R.string.mail_chyba), Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.digitalscope_button:
                uri = Constanty.digitakscope_url;
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
                break;

            case R.id.limemedia_button:
                uri = Constanty.limemedia_url;
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
                break;

            case R.id.zavrit_btn_contant:
                finish();
                break;

            default:
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
