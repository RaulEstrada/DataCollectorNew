package com.yuengdelahoz.datacollector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Data_Collection extends Activity{

	/** Messenger for communicating with the service. */
	Messenger msnService = null;

	/** Receiver for getting data from service */
	Receiver receptor;

	/** Flag indicating whether we have called bind on the service. */
	private boolean mBound;
	
	/** Flag indicating whether we have started the service. */
	private boolean isStarted;

	// Service

	// Buttons
	private Button startButton;

	// Views
	private TextView accelerationView;
	private TextView orientationView;
	private TextView gyroscopeView;
	private TextView magneticfieldView;
	private TextView proximityView;
	private TextView gravityView;
	private TextView soundlevelView;
	private TextView lineaccview;
	private TextView rotvector;
	private TextView viewUI;

	// Threads
	Thread updater = null;
	int count;

	// other
	private String[] data;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sensor_display);
		

		mBound = false;
		isStarted=false;

		// Find Buttons
		startButton = (Button) findViewById(R.id.StartButton);
		startButton.setText("Start");

		// Find Views
		accelerationView = (TextView) findViewById(R.id.acelerationView);
		orientationView = (TextView) findViewById(R.id.orientationView);
		gyroscopeView = (TextView) findViewById(R.id.gyroscopeView);
		magneticfieldView = (TextView) findViewById(R.id.magneticfieldView);
		proximityView = (TextView) findViewById(R.id.proximityView);
		gravityView = (TextView) findViewById(R.id.gravityview);
		soundlevelView = (TextView) findViewById(R.id.soundlevelview);
		lineaccview = (TextView) findViewById(R.id.lineaccview);
		rotvector = (TextView) findViewById(R.id.rotationvector);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.user_data, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.i("UI State", "onStart()");
		//Bind to remote service
		onBindService();

	}

	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i("UI State", "onRestart()");

	}

	@Override
	protected void onResume() {
		super.onResume();
		// Register sensors listeners
		Log.i("UI State", "onResume()");

	}

	/** Stop the updates when Activity is paused */
	@Override
	protected void onPause() {
		super.onPause();
		Log.i("UI State", "onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.i("UI State", "onStop()");

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unbind from the service
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		Log.i("UI State", "onDestroy()");

	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the object we can use to
			// interact with the service. We are communicating with the
			// service using a Messenger, so here we get a client-side
			// representation of that from the raw IBinder object.
			msnService = new Messenger(service);
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			msnService = null;
			mBound = false;
		}
	};

	public void start(View view) {
		if (startButton.getText() == "Start") {
			startButton.setText("Stop");
			if (!mBound)
				return;
			// Create and send a message to the service, using a supported 'what'
			// value
			Message msg = Message.obtain(null, Background_Service.MSG_SENSOR_REGISTER, 0, 0);
			try {
				msnService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		} else {
			startButton.setText("Start");
			Message msg = Message.obtain(null, Background_Service.MSG_SENSOR_UNREGISTER, 0, 0);
			try {
				msnService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
	}

	// Runnables
	private Runnable updateGUI = new Runnable() {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			accelerationView.setText(data[0]);
			orientationView.setText(data[1]);
			gyroscopeView.setText(data[2]);
			proximityView.setText(data[3]);
			gravityView.setText(data[4]);
			magneticfieldView.setText(data[5]);
			lineaccview.setText(data[6]);
			rotvector.setText(data[7]);
			soundlevelView.setText(data[8]);
		}
	};

	private String[] userData;

	void onBindService() {
		if (!mBound) {
			Intent intent_from_trial_selector = getIntent();
			Bundle bundle = intent_from_trial_selector.getExtras();
			userData= bundle.getStringArray("UserData");
			String dir = userData[6];
			String filename = userData[7];
			String[] patient = new String[]{dir,filename};
			receptor = new Receiver(null);
			Intent intent = new Intent(getApplicationContext(), Background_Service.class);
			intent.putExtra("Receiver", receptor);
			intent.putExtra("Content", patient);
			bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}
	}

	public class Receiver extends ResultReceiver {

		@Override
		protected void onReceiveResult(int resultCode, final Bundle resultData) {
			// TODO Auto-generated method stub

			if (resultCode == 100) {
				data = resultData.getStringArray("Array");
				runOnUiThread(updateGUI);
			}

		}

		public Receiver(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}

	}
	
	private void onStartService (){
		if(!isStarted){
			Intent intent_from_trial_selector = getIntent();
			Bundle bundle = intent_from_trial_selector.getExtras();
			userData= bundle.getStringArray("UserData");
			String dir = userData[6];
			String filename = userData[7];
			String[] patient = new String[]{dir,filename};
			receptor = new Receiver(null);
			Intent intent = new Intent(getApplicationContext(), Background_Service.class);
			intent.putExtra("Receiver", receptor);
			intent.putExtra("Content", patient);
			startService(intent);
		}
		
	}



}
