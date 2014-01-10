package cz.digitalscope.abradio.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import cz.digitalscope.abradio.R;
import cz.digitalscope.abradio.common.Constanty;

public class NastaveniActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    GoogleAnalyticsTracker tracker;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Start the tracker in manual dispatch mode...
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.startNewSession(Constanty.GA_CODE, 30, this);
        tracker.trackPageView(this.getLocalClassName());
        tracker.dispatch();

        getPreferenceManager().setSharedPreferencesName(
                Constanty.SETTING_NAME);
        addPreferencesFromResource(R.xml.setting);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
                this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
        tracker.stopSession();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
    }
}