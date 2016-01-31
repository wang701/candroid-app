package com.example.yang.candroid;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.util.Log;
import android.app.Service;
import android.os.IBinder;
import android.content.Intent;
import android.content.Context;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;

import org.apache.commons.io.input.TeeInputStream;
import de.greenrobot.event.EventBus;
import static android.os.Environment.getExternalStorageDirectory;

import org.isoblue.can.CanSocket;
import org.isoblue.can.CanSocketJ1939;
import org.isoblue.can.CanSocketJ1939.J1939Message;

public class MainActivity extends Activity {
	private ArrayAdapter<String> mLog;
	private boolean mToggleState;
	private Intent mIt;
	private EventBus bus = EventBus.getDefault();
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		bus.register(this);
    }
	
	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		ToggleButton tB = (ToggleButton) findViewById(R.id.toggleButton);

		if (savedInstanceState != null) {
			savedInstanceState.putBoolean("tb_state", tB.isChecked());
		/*	savedInstanceState.putInt("log_size", mLog.getCount());
			int i;
			String[] saveLog = new String[mLog.getCount()];
			for (i = 0; i < mLog.getCount(); i++) {
				saveLog[i] = mLog.getItem(i);
			}
			savedInstanceState.putStringArray("log_data", saveLog);
        	if (mMsgLoggerTask != null && mMsgLoggerTask.getStatus() != AsyncTask.Status.FINISHED) {
            	mMsgLoggerTask.cancel(true);
        	}
		*/
		}

		super.onSaveInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
  		super.onRestoreInstanceState(savedInstanceState);
		
	/*	int resLogSize = savedInstanceState.getInt("log_size");
		String[] resLog = new String[resLogSize];
		resLog = savedInstanceState.getStringArray("log_data");
	 	int i;
		mLog = new ArrayAdapter<String>(this, R.layout.message);
		for (i = 0; i < resLogSize; i++) {
			mLog.add(resLog[i]);
		}
		ListView lV = (ListView) findViewById(R.id.mylist);
		lV.setAdapter(mLog);
    */
		ToggleButton tB = (ToggleButton) findViewById(R.id.toggleButton);
		mToggleState = savedInstanceState.getBoolean("tb_state");
		tB.setChecked(mToggleState);
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
            case R.id.action_delete_log:
                return true;
            case R.id.action_email_log:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleOnOff(View view) throws IOException {
        ToggleButton toggleButton = (ToggleButton) view;

        if(toggleButton.isChecked()){
			mLog = new ArrayAdapter<String>(this, R.layout.message);
			ListView msgList = (ListView) findViewById(R.id.mylist);
			msgList.setAdapter(mLog);

			mIt = new Intent(this, CandroidLog.class);
			startService(mIt);

        } else {
			stopService(mIt);
		}
    }

	public void onEvent(J1939MsgEvent event){
		Toast.makeText(this, "event triggered", Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDestroy() {
		bus.unregister(this);
		super.onDestroy();
	}
}
