package eu.damek.abradio.service;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import eu.damek.abradio.R;
import eu.damek.abradio.activity.MainActivity;
import eu.damek.abradio.common.Communicator;
import eu.damek.abradio.common.Constants;
import eu.damek.abradio.common.DownloadImage;
import eu.damek.abradio.icecast.IcyStreamMeta;

public class PlaybackService extends Service implements OnErrorListener, OnBufferingUpdateListener,
		OnInfoListener, OnPreparedListener, OnVideoSizeChangedListener,
		SharedPreferences.OnSharedPreferenceChangeListener {

	private boolean playerStarted;

	private boolean isInMetaRead = false;
	
	public boolean aniState = false;

	private static final String LOG_TAG = PlaybackService.class.getName();

	public boolean isInLoadImages = false;

	public ProgressBar ani = null;
	public ProgressBar ani_pl = null;
	private int ani_cnt = 0;

	public NotificationManager notificationManager;
	private static final int NOTIFICATION_ID = 1;

	private int bindCount = 0;

	private TelephonyManager telephonyManager;
	private PhoneStateListener listener;
	private boolean isPausedInCall = false;
	private Intent lastChangeBroadcast;

	private MediaPlayer player;
	private Handler uiHandler;
	private JSONArray category = null;
	public Integer catCount = 0;

	public HashMap<String, Drawable> imageCache;
	public HashMap<String, JSONObject> oblibene;
	public HashMap<String, JSONObject> listeners;

	public JSONObject aktRadio = null;
	private String playURL = "";
	public String artist = "";
	public String title = "";

	private Timer timedMeta = null;
	private final int TIMER_DELAY = 1000;

	private String _int_search_text = "";
	private JSONArray filtered_category;

	private AsyncTask<URL, Integer, IcyStreamMeta> metatask = null;

	private Integer _sleep = 0;

	private boolean _sleepRuning;

	private Timer sleepTimer;
	private SharedPreferences _prefs;

	public View getBanner(Activity owner) {
		AdView adView = new AdView(owner);
		adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
		adView.setAdUnitId(getText(R.string.addsence_banned).toString());
		AdRequest request = new AdRequest.Builder()
				.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
				.build();
		LayoutInflater mInflater = (LayoutInflater) owner
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View convertView = mInflater.inflate(R.layout.banner_layout, null);
		LinearLayout layout = (LinearLayout) convertView
				.findViewById(R.id.banner_loayout);
		// Add the adView to it
		layout.addView(adView);
		adView.loadAd(request);
		return convertView;
	}

	private JSONArray loadFromServer() {
		AsyncTask.execute(new Runnable() {
			@Override
			public void run() {
				String data = new Communicator().executeHttpGet(Constants.DataURL);
				if (data != null)
					try {
						JSONObject json = new JSONObject(data);
						category = json.getJSONArray("categories");
					} catch (JSONException e) {
						e.printStackTrace();
					}
			}
		});
		return category;
	}

	public JSONArray categories() {
		if (category == null) {
			category = new JSONArray();
		}

		if (category.length() == 0) {
			loadFromServer();
		}
		return category;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		_prefs = PlaybackService.this.getSharedPreferences(
				Constants.SETTING_NAME, 0);
		_prefs.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(_prefs, null);

		imageCache = new HashMap<>();

		FileInputStream fOut;
		ObjectInputStream osw;
		JSONArray tmp;

		try {
			fOut = openFileInput("oblibene.dat");
			try {
				osw = new ObjectInputStream(fOut);
				try {
					String json = "";
					while (osw.available() > 0) {
						json += osw.readChar();
					}
					if (!json.equalsIgnoreCase("")) {
						tmp = new JSONArray(json);
					} else
						tmp = new JSONArray();
				} catch (JSONException e) {
					Toast.makeText(this,
							getResources().getString(R.string.chyba_uloz_obl),
							Toast.LENGTH_LONG).show();
					e.printStackTrace();
					tmp = new JSONArray();
				}
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
				tmp = new JSONArray();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			tmp = new JSONArray();
		}
		oblibene = new HashMap<>();
		for (int i = 0; i < tmp.length(); i++) {
			JSONObject data;
			try {
				data = tmp.getJSONObject(i);
				String id = data.getString("id");
				oblibene.put(id, data);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		try {
			fOut = openFileInput("listeners.dat");
			try {
				osw = new ObjectInputStream(fOut);
				try {
					String json = "";
					while (osw.available() > 0) {
						json += osw.readChar();
					}
					if (!json.equalsIgnoreCase("")) {
						tmp = new JSONArray(json);
					} else
						tmp = new JSONArray();
				} catch (JSONException e) {
					Toast.makeText(this,
							getResources().getString(R.string.chyba_uloz_list),
							Toast.LENGTH_LONG).show();
					e.printStackTrace();
					tmp = new JSONArray();
				}
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
				tmp = new JSONArray();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			tmp = new JSONArray();
		}
		listeners = new HashMap<>();
		for (int i = 0; i < tmp.length(); i++) {
			JSONObject data;
			try {
				data = tmp.getJSONObject(i);
				String id = data.getString("id");
				listeners.put(id, data);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Log.w(LOG_TAG, "Playback service created");

		if (telephonyManager == null) {
			telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			// Create a PhoneStateListener to watch for offhook and idle events
			listener = new PhoneStateListener() {
				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					switch (state) {
					case TelephonyManager.CALL_STATE_OFFHOOK:
					case TelephonyManager.CALL_STATE_RINGING:
						// Phone going offhook or ringing, pause the player.
						if (isPlaying()) {
							pause();
							isPausedInCall = true;
						}
						break;
					case TelephonyManager.CALL_STATE_IDLE:
						// Phone idle. Rewind a couple of seconds and start
						// playing.
						if (isPausedInCall) {
							play();
							isPausedInCall = false;
						}
						break;
					}
				}
			};

			// Register the listener with the telephony manager.
			telephonyManager.listen(listener,
					PhoneStateListener.LISTEN_CALL_STATE);
		}
		if (uiHandler == null) {
			uiHandler = new Handler();
		}

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				savedata();
			}
		};

		// ulozeni dat kazdych 10min
		new Timer().scheduleAtFixedRate(task, 3600 * TIMER_DELAY,
				3600 * TIMER_DELAY);
	}

	@SuppressLint("WorldReadableFiles")
	public void savedata() {
		FileOutputStream fOut;
		ObjectOutputStream osw;
		JSONArray tmp = new JSONArray();
		for (int i = 0; i < oblibene.size(); i++) {
			tmp.put(oblibene.get(oblibene.keySet().toArray()[i]));
		}
		try {
			fOut = openFileOutput("oblibene.dat", MODE_PRIVATE);
			try {
				osw = new ObjectOutputStream(fOut);
				osw.writeChars(tmp.toString());
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		tmp = new JSONArray();
		for (int i = 0; i < listeners.size(); i++) {
			tmp.put(listeners.get(listeners.keySet().toArray()[i]));
		}
		try {
			fOut = openFileOutput("listeners.dat", MODE_PRIVATE);
			try {
				osw = new ObjectOutputStream(fOut);
				osw.writeChars(tmp.toString());
				osw.flush();
				osw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		bindCount++;
		Log.d(LOG_TAG, "Bound PlaybackService, count " + bindCount);
		return new ListenBinder();
	}

	@Override
	public boolean onUnbind(Intent arg0) {
		bindCount--;
		Log.d(LOG_TAG, "Unbinding PlaybackService, count " + bindCount);
		if (!isPlaying() && bindCount == 0) {
			Log.w(LOG_TAG, "Will stop self");
			stopSelf();
		} else {
			Log.d(LOG_TAG, "Will not stop self");
		}
		return false;
	}

	public boolean isPlaying() {
		if (player != null) {
			return playerStarted || player.isPlaying();
		} else
			return playerStarted;
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		return cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isConnectedOrConnecting();
	}

	public static boolean isConnected(Context context) {
		WifiManager wifi = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		return wifi != null && (wifi.getConnectionInfo().getSSID() != null && wifi
				.isWifiEnabled());
	}

	public void play() {
		Boolean connected = isConnected(this);
		Boolean seti = _prefs.getBoolean("only_wifi", false);
		if (aktRadio != null && (connected || !seti)) {
			stop();

			setAni(View.VISIBLE);

			try {
				JSONArray streams = aktRadio.getJSONArray("streams");
				JSONObject stream = streams.getJSONObject(0);

				player = new MediaPlayer();
				player.setAudioStreamType(AudioManager.STREAM_MUSIC);
				player.setOnInfoListener(this);
				player.setOnErrorListener(this);
				player.setOnPreparedListener(this);
				player.setOnBufferingUpdateListener(this);
				player.setOnVideoSizeChangedListener(this);
				
				try {
					playURL = stream.getString("url");
					player.setDataSource(playURL);
					
					Log.d(LOG_TAG, "Start stream:" + playURL);
				} catch (IllegalArgumentException e) {
					setAni(View.INVISIBLE);
					Toast.makeText(
							this,
							getResources().getString(
									R.string.chyba_start_stream)
									+ playURL, Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (IllegalStateException e) {
					setAni(View.INVISIBLE);
					Toast.makeText(
							this,
							getResources().getString(R.string.chyba_str)
									+ playURL, Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (IOException e) {
					setAni(View.INVISIBLE);
					Toast.makeText(
							this,
							getResources().getString(R.string.chyba_nac_str)
									+ playURL, Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				try {
					try {
						if (!isInMetaRead) {
							isInMetaRead = true;
							metatask = new MetadataTask().execute(new URL(
									playURL));
						}
					} catch (MalformedURLException e) {
						isInMetaRead = false;
						setAni(View.INVISIBLE);
						Toast.makeText(
								this,
								getResources().getString(
										R.string.chyba_meta_str)
										+ playURL, Toast.LENGTH_LONG).show();
						e.printStackTrace();
					}
					player.prepareAsync();
					playerStarted = true;
					prepareNotify();

					// doplneni do listeners spustene radio
					addListeners();

					playerStarted = true;

					final Context c = this;
					TimerTask task = new TimerTask() {
						@Override
						public void run() {
							try {
								if (!isInMetaRead) {
									isInMetaRead = true;
									metatask = new MetadataTask()
											.execute(new URL(playURL));
								}
							} catch (MalformedURLException e) {
								isInMetaRead = false;
								setAni(View.INVISIBLE);
								Toast.makeText(
										c,
										getResources().getString(
												R.string.chyba_meta_str)
												+ playURL, Toast.LENGTH_LONG).show();
								e.printStackTrace();
							}
						}
					};

					timedMeta = new Timer();
					timedMeta.scheduleAtFixedRate(task, 20 * TIMER_DELAY,
							10 * TIMER_DELAY);

				} catch (IllegalStateException e) {
					setAni(View.INVISIBLE);
					Toast.makeText(this,
							getResources().getString(R.string.chyba_state), Toast.LENGTH_LONG)
							.show();
					e.printStackTrace();
				}
			} catch (JSONException e1) {
				setAni(View.INVISIBLE);
				Toast.makeText(this,
						getResources().getString(R.string.err_start), Toast.LENGTH_LONG)
						.show();
				e1.printStackTrace();
			}
		} else {
			if (aktRadio == null) {
				setAni(View.INVISIBLE);
				Toast.makeText(this, "Chybne radio", Toast.LENGTH_LONG).show();
			} else if (_prefs.getBoolean("only_wifi", true)
					|| isConnected(this)) {
				Toast.makeText(
						this,
						getResources().getString(R.string.only_wifi_seting_str),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void setAni(int visible) {
		if (visible == View.VISIBLE) {
			ani_cnt++;
			aniState = true;
			if (ani != null)
				ani.setVisibility(View.VISIBLE);
			if (ani_pl != null)
				ani_pl.setVisibility(View.VISIBLE);
		} else {
			ani_cnt--;
			if (ani_cnt <= 0) {
				aniState = false;
				if (ani != null)
					ani.setVisibility(View.INVISIBLE);
				if (ani_pl != null)
					ani_pl.setVisibility(View.INVISIBLE);
				ani_cnt = 0;
			}
		}
	}

	public void pause() {
		Log.d(LOG_TAG, "pause");
		stop();
	}

	public void stop() {
		Log.d(LOG_TAG, "stop");
		if (player != null) {
			player.stop();
			timedMeta.cancel();
			player.setOnBufferingUpdateListener(null);
			player.setOnErrorListener(null);
			player.setOnInfoListener(null);
			player.setOnPreparedListener(null);
			playerStarted = false;
			player.release();
			player = null;
			playURL = "";
			if (metatask != null && metatask.getStatus() == Status.RUNNING) {
				metatask.cancel(true);
			}
		}
		cleanup();
	}

	@Override
	public void onDestroy() {

		super.onDestroy();
		stop();
		Log.w(LOG_TAG, "Service exiting");

		stop();

		telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
	}

	public class ListenBinder extends Binder {

		public PlaybackService getService() {
			return PlaybackService.this;
		}
	}

	private void prepareNotify() {
		if (player != null) {
			CharSequence contentText = "";
			if (player.isPlaying()) {
				contentText = artist + " " + title;
			} else
				try {
					contentText = aktRadio.getString("name") + " - "
							+ aktRadio.getString("description");
				} catch (JSONException e) {
					Toast.makeText(this,
							getResources().getString(R.string.err_notify), Toast.LENGTH_LONG)
							.show();
					e.printStackTrace();
				}
			long when = System.currentTimeMillis();
			final Notification.Builder builder = new Notification.Builder(this.getApplicationContext());

			builder.setSmallIcon(R.drawable.icon);
			builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.notification_large));

			CharSequence title = getString(R.string.app_name);

			Intent notificationIntent = new Intent(this, MainActivity.class);
			notificationIntent.setAction(Intent.ACTION_VIEW);
			notificationIntent.addCategory(Intent.CATEGORY_DEFAULT);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			PendingIntent contentIntent = PendingIntent.getActivity(this.getApplicationContext(), 0,
					notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			builder.setContentIntent(contentIntent);
			builder.setWhen(when);
			builder.setContentText(title);
			builder.setContentText(contentText);
			Notification notif = builder.getNotification();
			notificationIntent.setFlags(Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
			notificationManager.notify(NOTIFICATION_ID, notif);

			// Change broadcasts are sticky, so when a new receiver
			// connects, it
			// will
			// have the data without polling.
			if (lastChangeBroadcast != null) {
				getApplicationContext().removeStickyBroadcast(
						lastChangeBroadcast);
			}
			lastChangeBroadcast = new Intent(Constants.SERVICE_CHANGE_NAME);
			lastChangeBroadcast.putExtra(Constants.EXTRA_TITLE,
					getString(R.string.app_name));
			getApplicationContext().sendStickyBroadcast(lastChangeBroadcast);
		}
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		
		if (Build.VERSION.SDK_INT >= 16 && what==MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -110) {
		      // Ignorer, da Samsung Galaxy SIII sender denne fejl (onError(1) -110) men i øvrigt spiller fint videre!
		      return true;
		    }
		
		Toast.makeText(
				this,
				getResources().getString(R.string.err_stream) + " onError("
						+ what + ", " + extra + ")", Toast.LENGTH_LONG).show();

		Log.e(LOG_TAG, "onError(" + what + ", " + extra + ")");
		stop();

		cleanup();

		setAni(View.INVISIBLE);

		return false;
	}

	@Override
	public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
		Log.w(LOG_TAG, "onInfo(" + arg1 + ", " + arg2 + ")");
		if (arg1 == MediaPlayer.MEDIA_INFO_METADATA_UPDATE) {
			try {
				if (!isInMetaRead)
					isInMetaRead = true;
				metatask = new MetadataTask().execute(new URL(playURL));
			} catch (MalformedURLException e) {
				isInMetaRead = false;
				e.printStackTrace();
			}
		} else if (arg1 == 701) {
			setAni(View.VISIBLE);
		} else if (arg1 == 702 || arg1 == 703) {
			setAni(View.INVISIBLE);
		}
		return true;
	}

	/**
	 * Remove all intents and notifications about the last media.
	 */
	private void cleanup() {
		artist = "";
		title = "";
		notificationManager.cancel(NOTIFICATION_ID);
		if (lastChangeBroadcast != null) {
			getApplicationContext().removeStickyBroadcast(lastChangeBroadcast);
		}
		getApplicationContext().sendBroadcast(
				new Intent(Constants.SERVICE_CLOSE_NAME));
		setAni(View.INVISIBLE);

		lastChangeBroadcast = new Intent(Constants.SERVICE_CHANGE_NAME);
		lastChangeBroadcast.putExtra(Constants.EXTRA_TITLE,
				getString(R.string.app_name));
		getApplicationContext().sendStickyBroadcast(lastChangeBroadcast);

		setAni(View.INVISIBLE);
	}

	public Drawable getImage(String imageURL, final ImageView img,
			boolean cachable) {

		Drawable image;
		if ((image = imageCache.get(imageURL)) == null) {
			new DownloadImage(img, this, cachable).execute(imageURL);
			return getResources().getDrawable(R.drawable.vodoznak);
		} else
			return image;
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		Log.d(LOG_TAG,"Nabufrovano, spusteni streramu");
		arg0.start();
		setAni(View.INVISIBLE);
	}

	protected class MetadataTask extends AsyncTask<URL, Integer, IcyStreamMeta> {
		IcyStreamMeta streamMeta;

		@Override
		protected IcyStreamMeta doInBackground(URL... urls) {
			isInMetaRead = true;
			streamMeta = new IcyStreamMeta(urls[0]);
			streamMeta.refreshMeta();
			return streamMeta;
		}

		@Override
		protected void onPostExecute(IcyStreamMeta result) {
			isInMetaRead = false;
			AsyncTask.execute(new Runnable() {
				@Override
				public void run() {
					String a = streamMeta.getArtist();
					String t = streamMeta.getTitle();
					if (!artist.equalsIgnoreCase(a) || !title.equalsIgnoreCase(t)) {
						artist = a;
						title = t;
						if (isPlaying()) {
							prepareNotify();
						}
					}
				}
			});
		}

	}

	public Boolean isFavorite() {
		boolean ret = false;
		if (aktRadio != null && oblibene != null) {
			try {
				String id = aktRadio.getString("id");
				ret = oblibene.containsKey(id);
			} catch (JSONException e) {
				Toast.makeText(this,
						getResources().getString(R.string.err_set_fav), Toast.LENGTH_LONG)
						.show();
				e.printStackTrace();
				ret = false;
			}
		}
		return ret;
	}

	public void togleFovorite() {
		String id;
		try {
			id = aktRadio.getString("id");
			if (isFavorite()) {
				oblibene.remove(id);
			} else {
				oblibene.put(id, aktRadio);
			}
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		savedata();
		// odeslat message ze je nove favorite
		lastChangeBroadcast = new Intent(Constants.SERVICE_CHANGE_NAME);
		lastChangeBroadcast.putExtra(Constants.EXTRA_TITLE,
				getString(R.string.app_name));
		getApplicationContext().sendStickyBroadcast(lastChangeBroadcast);
		prepareNotify();
	}

	private void addListeners() {
		String id;
		try {
			id = aktRadio.getString("id");
			listeners.put(id, aktRadio);
		} catch (JSONException e) {
			Toast.makeText(this,
					getResources().getString(R.string.err_set_list), Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}

		savedata();
	}

	public Integer sleep() {
		return _sleep;
	}

	public void sleppToggle() {
		_sleep += 15;
		if (_sleep > 60) {
			_sleep = 0;
		}

		if (_sleepRuning) {
			if (_sleep == 0) {
				_sleepRuning = false;
				sleepTimer.cancel();
			}
		} else if (_sleep > 0) {
			startSleep();
		}
	}

	private void startSleep() {
		_sleepRuning = true;

		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				_sleep--;
				if (_sleep == 0) {
					stopSleep();
				}
				lastChangeBroadcast = new Intent(Constants.SERVICE_CHANGE_NAME);
				lastChangeBroadcast.putExtra(Constants.EXTRA_TITLE,
						getString(R.string.app_name));
				getApplicationContext()
						.sendStickyBroadcast(lastChangeBroadcast);
			}
		};

		sleepTimer = new Timer();
		sleepTimer
				.scheduleAtFixedRate(task, 60 * TIMER_DELAY, 60 * TIMER_DELAY);
	}

	private void stopSleep() {
		_sleepRuning = false;
		if (isPlaying()) {
			stop();
		}
		sleepTimer.cancel();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
		if (arg1!=null && arg1.equalsIgnoreCase("no_dimmer")) {
			lastChangeBroadcast = new Intent(Constants.SERVICE_CHANGE_NAME);
			lastChangeBroadcast.putExtra(Constants.EXTRA_TITLE,
					getString(R.string.app_name));
			getApplicationContext().sendStickyBroadcast(lastChangeBroadcast);
		}
	}

	public SharedPreferences preference() {
		return _prefs;
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		// slepa funkce, moznost zobrazeni stavu bufferu
		
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
		// slepa funkce, pouze pro video
		
	}
}
