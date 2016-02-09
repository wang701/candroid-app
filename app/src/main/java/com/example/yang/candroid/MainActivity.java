package com.example.yang.candroid;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.content.Intent;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends Activity {
	private MsgAdapter mLog;
	private Intent mIt;
	private EventBus mMsgBus;
	private EventBus mFileBus;
	private ListView mMsgList;
	private boolean mToggleState;
	private boolean mWriteToFile;

	public FileOutEvent event = null;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		mMsgBus = EventBus.getDefault();
		mFileBus = EventBus.getDefault();
		mMsgBus.register(this);
		mLog = new MsgAdapter(this, 30);
		mMsgList = (ListView) findViewById(R.id.mylist);
    }

	@Override
	protected void onSaveInstanceState(Bundle savedInstanceState) {
		ToggleButton tB = (ToggleButton) findViewById(R.id.toggleButton);

		if (savedInstanceState != null) {
			savedInstanceState.putBoolean("tb_state", tB.isChecked());
			String[] prevMsgs = mLog.getValues();
			savedInstanceState.putStringArray("lv_msgs", prevMsgs);
			savedInstanceState.putString("intent_str", mIt.toUri(0));
			// savedInstanceState.putBoolean("write_file", mWriteToFile);
		}

		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		String[] prevMsgs = savedInstanceState.getStringArray("lv_msgs");
		mLog.addArray(prevMsgs);
		mMsgList.setAdapter(mLog);

		ToggleButton tB = (ToggleButton) findViewById(R.id.toggleButton);
		mToggleState = savedInstanceState.getBoolean("tb_state");
		tB.setChecked(mToggleState);

		String uri = savedInstanceState.getString("intent_str");
		try {
			mIt = Intent.parseUri(uri, 0);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		// mWriteToFile = savedInstanceState.getBoolean("write_file");

		super.onRestoreInstanceState(savedInstanceState);
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
            /*case R.id.action_delete_log:
                File dir = new File(Environment.getExternalStorageDirectory() + "/Log/");
                if (dir.isDirectory())
                {
                    String[] children = dir.list();
                    for (int i = 0; i < children.length; i++)
                    {
                        new File(dir, children[i]).delete();
                    }
                }
                return true;
            case R.id.action_email_log:
                return true;
			*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleOnOff(View view) throws IOException {
        ToggleButton toggleButton = (ToggleButton) view;

        if(toggleButton.isChecked()){
			mMsgList.setAdapter(mLog);
			mIt = new Intent(this, CandroidLog.class);
			startService(mIt);
			mWriteToFile = true;
			event = new FileOutEvent(mWriteToFile);
			mFileBus.post(event);
			System.out.println("post event");
        } else {
			stopService(mIt);
		}
    }

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEventMainThread(J1939MsgEvent event) {
		mLog.add(event.toString());
	}

	@Override
	protected void onDestroy() {
		mMsgBus.unregister(this);
		super.onDestroy();
	}
}
