package com.example.yang.candroid;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.util.Log;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.IOException;
import java.util.ArrayList;

import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;
import org.isoblue.can.CanSocketJ1939.Filter;

public class MainActivity extends Activity {
	private CanSocketJ1939 mSocket;
	private J1939Message mMsg;
	private MsgLoggerTask mMsgLoggerTask;
	private MsgAdapter mLog;
	private FilterDialogFragment mFilterDialog;
	private WarningDialogFragment mWarningDialog;
	private FragmentManager mFm = getFragmentManager();
	private ListView mMsgList;
	private ListView mFilterList;
	private RequestQueue mQueue;
	private ConfigGetRequest mConfigReq;
	private RegisterPostRequest mRegisterReq;
	private boolean mIsCandroidServiceRunning;
	private boolean mSaveFiltered = false;
	public static Filter mFilter;
	public static MsgAdapter mFilterItems;
	public static ArrayList<Filter> mFilters = new ArrayList<Filter>();
	private static final String CAN_INTERFACE = "can0";
	private static final String TAG = "CandroidActivity";
	private static final String msgFilter = "Adding new filter(s) will stop " +
			"current logging, do you wish to continue?";
	private static final String msgStop = "Stop logging and Candroid Service?";
	private static final String msgLogOpt = "Change log options will stop " +
			"current logging, do you wish to continue?";
	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	private GoogleApiClient client;
	//private final String token = "E620jPvYjmu_8h3jI2vhPiGSgXIEM43kZImNB7_p";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "in onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initCandroid();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(broadcastReceiver,
				new IntentFilter(CandroidService.BROADCAST_ACTION));
	}

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		Log.d(TAG, "in onSaveInstanceState()");
		String[] values = mFilterItems.getValues();
		savedInstanceState.putStringArray("filtersList", values);
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		Log.d(TAG, "in onRestoreInstanceState()");
		String[] values = savedInstanceState.getStringArray("filtersList");
		if (values != null) {
			mFilterItems.addArray(values);
		}
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "in onDestroy()");
		if (mMsgLoggerTask != null && mSocket != null) {
			stopTask();
			closeCanSocket();
			Log.d(TAG, "socket closed, task stopped");
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "in onPause()");
		unregisterReceiver(broadcastReceiver);
		super.onPause();
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "in onStart()");
		if (isServiceRunning(CandroidService.class)) {
			ToggleButton b = (ToggleButton) findViewById(R.id.streamToggle);
			b.setChecked(true);
			onStreamGo();
		}
		super.onStart();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.connect();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app deep link URI is correct.
				Uri.parse("android-app://com.example.yang.candroid/http/host/path")
		);
		AppIndex.AppIndexApi.start(client, viewAction);
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "in onStop()");
		if (mMsgLoggerTask != null && mSocket != null) {
			stopTask();
			closeCanSocket();
			Log.d(TAG, "socket closed, task stopped");
		}
		super.onStop();
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		Action viewAction = Action.newAction(
				Action.TYPE_VIEW, // TODO: choose an action type.
				"Main Page", // TODO: Define a title for the content shown.
				// TODO: If you have web page content that matches this app activity's content,
				// make sure this auto-generated web page URL is correct.
				// Otherwise, set the URL to null.
				Uri.parse("http://host/path"),
				// TODO: Make sure this auto-generated app deep link URI is correct.
				Uri.parse("android-app://com.example.yang.candroid/http/host/path")
		);
		AppIndex.AppIndexApi.end(client, viewAction);
		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		client.disconnect();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.save_option).setChecked(mSaveFiltered);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case R.id.add_filters:
				if (isServiceRunning(CandroidService.class)) {
					mWarningDialog.mWarningMsg = msgFilter;
					mWarningDialog.show(mFm, "warning");
				} else {
					mFilterDialog.show(mFm, "filter");
				}
				return true;
			case R.id.save_option:
				if (isServiceRunning(CandroidService.class)) {
					mWarningDialog.mWarningMsg = msgLogOpt;
					mWarningDialog.show(mFm, "warning");
				}
				if (item.isChecked()) {
					mSaveFiltered = false;
					item.setChecked(false);
				} else {
					mSaveFiltered = true;
					item.setChecked(true);
				}
				return true;
			case R.id.view_old_logs:
				Uri selectedUri = Uri.parse(Environment
						.getExternalStorageDirectory() + "/Log/");
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(selectedUri, "resource/folder");
				if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
					startActivity(intent);
				} else {
					Toast.makeText(this, "No file manager app found",
							Toast.LENGTH_LONG).show();
				}
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/* callback for streamToggle */
	public void toggleListener(View view) throws IOException {
		ToggleButton b = (ToggleButton) view;
		if (b.isChecked()) {
			onStreamGo();
		} else {
			mIsCandroidServiceRunning =
					isServiceRunning(CandroidService.class);
			if (mIsCandroidServiceRunning) {
				mWarningDialog.mWarningMsg = msgStop;
				mWarningDialog.show(mFm, "warning");
			}
		}
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, "in OnReceive()");
			renderOldView(intent);
		}
	};

	private void renderOldView(Intent intent) {
		Bundle b = intent.getBundleExtra("serviceBundle");
		mSaveFiltered = b.getBoolean("saveOption");
		mFilters = (ArrayList<Filter>) b.getSerializable("filters");
		ArrayList<String> filterStrList = new ArrayList<String>();
		for (Filter f : mFilters) {
			filterStrList.add("Filtering on " + f.toString());
		}
		String[] filterStr = new String[filterStrList.size()];
		filterStr = filterStrList.toArray(filterStr);
		if (mFilterItems.isEmpty()) {
			mFilterItems.addArray(filterStr);
		}
	}

	public void initCandroid() {
		Log.d(TAG, "initializing parameters in initCandroid()");
		mLog = new MsgAdapter(this, 100);
		mFilterItems = new MsgAdapter(this, 20);
		mMsgList = (ListView) findViewById(R.id.msglist);
		mFilterList = (ListView) findViewById(R.id.filterlist);
		mMsgList.setAdapter(mLog);
		mFilterList.setAdapter(mFilterItems);
		mFilterDialog = new FilterDialogFragment();
		mWarningDialog = new WarningDialogFragment();
		mQueue = Volley.newRequestQueue(getApplicationContext());
		mConfigReq = new ConfigGetRequest(new Response.Listener<ConfigGetRequest.OADAConfiguration>() {
			@Override
			public void onResponse(ConfigGetRequest.OADAConfiguration wellKnown) {
				RegisterPostRequest(wellKnown);
			}
		});
	}

	/* callback for adding new filters */
	public void onAddNewFilter() {
		onStreamStop();
		mFilterItems.clear();
	}

	/* callback for stop everything */
	public void onStreamStop() {
		stopTask();
		closeCanSocket();
		stopForegroundService();
		ToggleButton b = (ToggleButton) findViewById(R.id.streamToggle);
		b.setChecked(false);
	}

	/* callback for starting the logger */
	public void onStreamGo() {
		mQueue.add(mConfigReq);
		mQueue.addRequestFinishedListener();
		setupCanSocket();
		startTask();
		Log.d(TAG, "isServiceRunning: " +
				isServiceRunning(CandroidService.class));
		startForegroundService();
	}

	private boolean isServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager)
				getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service :
				manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void setupCanSocket() {
		try {
			mSocket = new CanSocketJ1939(CAN_INTERFACE);
			mSocket.setPromisc();
			mSocket.setTimestamp();
			mSocket.setfilter(mFilters);
		} catch (IOException e) {
			Log.e(TAG, "socket creation on " + CAN_INTERFACE + " failed");
		}
	}

	private void closeCanSocket() {
		if (mSocket != null) {
			try {
				mSocket.close();
				mSocket = null;
			} catch (IOException e) {
				Log.e(TAG, "cannot close socket");
			}
		}
	}

	private void startTask() {
		Log.d(TAG, "in startTask(), start AsyncTask");
		mMsgLoggerTask = new MsgLoggerTask();
		mMsgLoggerTask.execute(mSocket);
	}

	private void stopTask() {
		Log.d(TAG, "in stopTask(), cancel AsyncTask");
		mMsgLoggerTask.cancel(true);
		SystemClock.sleep(100);
		mMsgLoggerTask = null;
	}

	private void startForegroundService() {
		Intent startForegroundIntent = new Intent(
				CandroidService.FOREGROUND_START);
		startForegroundIntent.putExtra("save_option", mSaveFiltered);
		startForegroundIntent.putExtra("filter_list", mFilters);
		startForegroundIntent.setClass(
				MainActivity.this, CandroidService.class);
		startService(startForegroundIntent);
	}

	private void stopForegroundService() {
		Intent stopForegroundIntent = new Intent(
				CandroidService.FOREGROUND_STOP);
		stopForegroundIntent.setClass(
				MainActivity.this, CandroidService.class);
		stopService(stopForegroundIntent);
	}

	private class MsgLoggerTask extends AsyncTask
			<CanSocketJ1939, J1939Message, Void> {
		@Override
		protected Void doInBackground(CanSocketJ1939... socket) {
			try {
				while (!isCancelled()) {
					if (socket[0] != null) {
						if (socket[0].select(1) == 0) {
							mMsg = socket[0].recvMsg();
							publishProgress(mMsg);
						}
						if (isCancelled()) {
							break;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		protected void onProgressUpdate(J1939Message... msg) {
			mLog.add(msg[0].toString());
/*			mQueue.add(new MessagePostRequest(token,
			"http://128.46.213.95:3000/bookmarks/candroid",
			msg[0].toString()));
*/
		}

		protected void onPostExecute(Void Result) {
			// Do nothing
		}
	}
}
