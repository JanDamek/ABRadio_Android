package eu.damek.abradio.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import org.json.JSONObject;

import eu.damek.abradio.R;
import eu.damek.abradio.common.AbRadioExpandableListAdapter;
import eu.damek.abradio.common.Constants;
import eu.damek.abradio.service.PlaybackService;


public class ABradioActivity extends Activity implements OnChildClickListener{

    ExpandableListView el;

    private BroadcastReceiver changeReceiver = new PlaybackChangeReceiver();
    private BroadcastReceiver updateReceiver = new PlaybackUpdateReceiver();
    private BroadcastReceiver closeReceiver = new PlaybackCloseReceiver();

    private AbRadioExpandableListAdapter mAdapter;
//    private AutoCompleteTextView hledani;
//    private Button search_btn;

    private ServiceConnection conn;
    public static PlaybackService player;

    public void attachToPlaybackService() {
        Intent serviceIntent = new Intent(getBaseContext(),
                PlaybackService.class);
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                player = ((PlaybackService.ListenBinder) service).getService();
                Log.w("ABRadio", "CONNECT home view");
                mAdapter.player = player;

//                LayoutInflater inflater = ABradioActivity.this
//                        .getLayoutInflater();
//                View header = inflater.inflate(R.layout.search_layout, null);
//                el.addHeaderView(header);

//                search_btn = (Button) findViewById(R.id.hledat_btn);
//                search_btn.setOnClickListener(ABradioActivity.this);
//
//                hledani = (AutoCompleteTextView) findViewById(R.id.search_edit_text);
//                hledani.setOnEditorActionListener(ABradioActivity.this);
//                hledani.setOnClickListener(ABradioActivity.this);
//                hledani.setOnFocusChangeListener(ABradioActivity.this);
//
//                ImageView img = (ImageView) findViewById(R.id.clear_search);
//                img.setOnClickListener(ABradioActivity.this);

                showAll();

                el.requestFocus();

//                if (hledani.isFocused()) {
//                    InputMethodManager inputManager = (InputMethodManager) ABradioActivity.this
//                            .getSystemService(Context.INPUT_METHOD_SERVICE);
//
//                    inputManager.hideSoftInputFromWindow(
//                            hledani.getWindowToken(),
//                            InputMethodManager.HIDE_NOT_ALWAYS);
//
//                    mAdapter.notifyDataSetChanged();
//                }

//                search_btn.requestFocus();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.w("ABRadio", "DISCONNECT home view");
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("session", "Session");
        prefs.edit().commit();

    }

    private void showAll() {
        if (player != null && player.categories() != null) {
            mAdapter.context = this;
            ExpandableListView listView = (ExpandableListView) findViewById(R.id.list);

            listView.addFooterView(player.getBanner(this));

            listView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();

            listView.setOnChildClickListener(this);

//            hledani.setText(player.search_text());
        }
    }

    @Override
    public void onDestroy() {
        getBaseContext().unregisterReceiver(closeReceiver);
        getBaseContext().unregisterReceiver(updateReceiver);
        getBaseContext().unregisterReceiver(changeReceiver);
        getBaseContext().getApplicationContext().unbindService(conn);
        super.onDestroy();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.home);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mAdapter = new AbRadioExpandableListAdapter(this);
        el = (ExpandableListView) findViewById(R.id.list);
        el.setClickable(true);
        el.setFocusable(true);
        el.setFocusableInTouchMode(true);

        attachToPlaybackService();
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v,
                                int groupPosition, int childPosition, long id) {

        if (mAdapter != null && player != null) {
            JSONObject radio = mAdapter.getChild(groupPosition, childPosition);
            player.aktRadio = radio;
            player.play();

            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        }

        return true;

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    class PlaybackChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    class PlaybackUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    class PlaybackCloseReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {

//            case R.id.search_edit_text:
//                getWindow().setSoftInputMode(
//                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//                break;

//            case R.id.clear_search:
//                hledani.setText("");

//            case R.id.hledat_btn:
//                is_hide_soft_key = true;
//                search();
//                break;

//        }
//    }

//    @Override
//    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//        onClick(v);
//        search();
//        return true;
//    }

//    private void search() {
//        if (hledani.isFocused()) {
//            player.setSearch_text(hledani.getText().toString());
//            InputMethodManager inputManager = (InputMethodManager) this
//                    .getSystemService(Context.INPUT_METHOD_SERVICE);
//            inputManager.hideSoftInputFromWindow(this.getCurrentFocus()
//                    .getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
//
//            mAdapter.notifyDataSetChanged();
//            // ((ExpandableListView)
//            // findViewById(R.id.list)).invalidateViews();
//        }
//
//    }

//    @Override
//    public void onFocusChange(View v, boolean hasFocus) {
//        if (is_hide_soft_key) {
//            InputMethodManager inputManager = (InputMethodManager) ABradioActivity.this
//                    .getSystemService(Context.INPUT_METHOD_SERVICE);
//
//            inputManager.hideSoftInputFromWindow(hledani.getWindowToken(),
//                    InputMethodManager.HIDE_NOT_ALWAYS);
//
//        }
//
//    }

}