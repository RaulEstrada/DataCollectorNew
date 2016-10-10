package com.yuengdelahoz.datacollector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.R.bool;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

public class Background_Service extends Service implements SensorEventListener {
	/**
	 * Target we publish for clients to send messages to IncomingHandler.
	 */
	final Messenger mMessenger = new Messenger(new IncomingHandler());
	Thread snd;

	/** Command to the service to display a message */
	static final int MSG_SAY_HELLO = 1;
	static final int MSG_SENSOR_REGISTER = 2;
	static final int MSG_SENSOR_UNREGISTER = 3;

	public Boolean started;
	private SoundMeter sndm;
	private Handler mHandler = new Handler();
	private static final int POLL_INTERVAL = 300;
	private String Data[];
	private SensorEvent event;
	private SensorManager SM;
	private ResultReceiver receptor;
	private Bundle bundle;
	private mTask task;
	private Timer timer_1;
	private Timer timer_2;
	private String dirName;
	private String fileName;
	private mTask task_1;
	private mTask task_2;
	private boolean flag = false;
	private float[] rotationMatrix = new float[16];
	private BufferedWriter output;
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i("Service State", "onCreate()");
		SM = (SensorManager) getSystemService(SENSOR_SERVICE);
		timer_1 = new Timer();
		timer_2 = new Timer();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.i("Service State", "onStartCommand()");

		Data = new String[] { "0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0",
				"0.0" };
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i("Service State", "onDestroy()");

		if (flag) {
			UnregisterSensors();
			sndm.stop();
			task_1.cancel();
			task_2.cancel();
		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	/**
	 * When binding to the service, we return an interface to our messenger for
	 * sending messages to the service.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("Service State", "onBind()");
		String[] content = intent.getStringArrayExtra("Content");
		dirName = content[0];
		fileName = content[1];

		Data = new String[] { "0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0",
				"0.0", "0.0" };

		receptor = intent.getParcelableExtra("Receiver");
		sndm = new SoundMeter();
		mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		bundle = new Bundle();
		Toast.makeText(getApplicationContext(), "Binding...",
				Toast.LENGTH_SHORT).show();
		return mMessenger.getBinder();

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		
			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				/*
				 * Get the Measures the acceleration force in m/s2 that is
				 * applied to a device on all three physical axes (x, y, and z),
				 * including the force of gravity.
				 */
				float xA = event.values[0];
				float yA = event.values[1];
				float zA = event.values[2];

				String accelerationOut = String.format("%.3f,%.3f,%.3f", xA,
						yA, zA);
				Data[0] = accelerationOut;
				break;

			case Sensor.TYPE_ORIENTATION:
				// Measures degrees of rotation that a device makes around all
				// three physical axes (x, y, z)
				float xO = event.values[0];
				float yO = event.values[1];
				float zO = event.values[2];

				String orientationOut = String.format("%.3f,%.3f,%.3f", xO, yO,
						zO);
				Data[1] = orientationOut;
				break;

			case Sensor.TYPE_GYROSCOPE:
				// Measures a device's rate of rotation in rad/s around each of
				// the three physical axes (x, y, and z)
				float xG = event.values[0];
				float yG = event.values[1];
				float zG = event.values[2];

				String gyroscopeOut = String.format("%.3f,%.3f,%.3f", xG, yG,
						zG);
				Data[2] = gyroscopeOut;
				break;

			case Sensor.TYPE_PROXIMITY:
				// Measures the proximity of an object in cm relative to the
				// view screen of a device.
				float distance = event.values[0];

				String proximityOut = String.format("%.2f", distance);
				Data[3] = proximityOut;
				break;

			case Sensor.TYPE_GRAVITY:
				// Measures the ambient light level (illumination) in lx.
				float xGR = event.values[0];
				float yGR = event.values[1];
				float zGR = event.values[2];

				String gravityOut = String.format("%.3f,%.3f,%.3f", xGR, yGR,
						zGR);
				Data[4] = gravityOut;
				break;

			case Sensor.TYPE_MAGNETIC_FIELD:
				// Measures the ambient geomagnetic field for all three physical
				// axes (x, y, z) in mircoT.
				float xM = event.values[0];
				float yM = event.values[1];
				float zM = event.values[2];

				String magneticfiedlOut = String.format("%.3f,%.3f,%.3f", xM,
						yM, zM);
				Data[5] = magneticfiedlOut;
				break;
			case Sensor.TYPE_LINEAR_ACCELERATION:
				float xLA = event.values[0];
				float yLA = event.values[1];
				float zLA = event.values[2];
				String linaccelout = String.format("%.3f,%.3f,%.3f", xLA, yLA,
						zLA);
				Data[6] = linaccelout;
				break;

			case Sensor.TYPE_ROTATION_VECTOR:
				SensorManager.getRotationMatrixFromVector(rotationMatrix,
						event.values);
				float xRV = event.values[0];
				float yRV = event.values[1];
				float zRV = event.values[2];
				String rotvectout = String.format("%.3f,%.3f,%.3f", xRV, yRV,
						zRV);
				Data[7] = rotvectout;
				break;

			

		}

	}

	private void updateUI() {
		// TODO Auto-generated method stub
		bundle = new Bundle();
		bundle.putStringArray("Array", Data);
		receptor.send(100, bundle);

	}

	private Runnable mPollTask = new Runnable() {
		public void run() {
			double amp = sndm.getAmplitude();
			String amps_ = String.format("%.3f", amp);

			Data[8] = amps_;
			mHandler.postDelayed(mPollTask, POLL_INTERVAL);
		}

	};
	

	public void RegisterSensors() {
		// Register sensors listeners
		SM.registerListener(this,
				SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this, SM.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this, SM.getDefaultSensor(Sensor.TYPE_PROXIMITY),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this, SM.getDefaultSensor(Sensor.TYPE_GRAVITY),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this,
				SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this,
				SM.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
				SensorManager.SENSOR_DELAY_FASTEST);
		SM.registerListener(this,
				SM.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_FASTEST);
		// Start audio recorder
		sndm.start();

	}

	private void UnregisterSensors() {
		SM.unregisterListener((SensorEventListener) this,
				SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
		SM.unregisterListener((SensorEventListener) this,
				SM.getDefaultSensor(Sensor.TYPE_ORIENTATION));
		SM.unregisterListener(this, SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		SM.unregisterListener(this, SM.getDefaultSensor(Sensor.TYPE_PROXIMITY));
		SM.unregisterListener(this, SM.getDefaultSensor(Sensor.TYPE_GRAVITY));
		SM.unregisterListener(this,
				SM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
		SM.unregisterListener(this,
				SM.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
		SM.unregisterListener(this,
				SM.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
		// Stop audio recorder
		sndm.stop();

	}

	/**
	 * Handler of incoming messages from clients.
	 */
	class IncomingHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case MSG_SENSOR_REGISTER:
				RegisterSensors();
				task_1 = new mTask(0);
				task_2 = new mTask(1);
				timer_1.scheduleAtFixedRate(task_1, 100, 100); // Timer to
																// update UI
				timer_2.scheduleAtFixedRate(task_2, 1000, 10); // Timer to write
																// to file
				flag = true;
				break;
			case MSG_SENSOR_UNREGISTER:
				UnregisterSensors();
				task_1.cancel();
				task_2.cancel();
				flag = false;
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private class mTask extends TimerTask {
		private int task;

		mTask(int t) {
			task = t;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (task == 0) {
				updateUI();

			} else {
				mSaveData(dirName, fileName);
			}

		}

	}

	public File getAlbumStorageDir(String albumName) {
		// Get the directory for the user's public pictures directory.
		File file = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
				albumName);
		if (!file.mkdirs()) {
			Log.e("Error", "Directory not created");
		}
		return file;
	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	private void mSaveData(String DirectoryName, String FileName) {
		Date date = new Date();
		String value = "";

		for (int i = 0; i < 9; i++) {
			value = value + Data[i] + ",";
		}
		for (int i = 0; i < 16; i++) {
			value = value + rotationMatrix[i] + ",";
		}

		try {
			File dir = getAlbumStorageDir(DirectoryName);
			File file = new File(dir, FileName);
			FileWriter writer = new FileWriter(file, true);
			output = new BufferedWriter(writer);
			output.append(value + date.getTime() + "\n");
			output.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
