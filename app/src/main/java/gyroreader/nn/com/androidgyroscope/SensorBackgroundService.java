package gyroreader.nn.com.androidgyroscope;

import android.app.Service;
import android.content.Context;
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
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class SensorBackgroundService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private float[] sensorValues = new float[] { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
    private Handler mHandler = new Handler();
    private static final int POLL_INTERVAL = 300;
    private Bundle bundle;
    private ResultReceiver receptor;
    static final int MSG_SENSOR_REGISTER = 2;
    static final int MSG_SENSOR_UNREGISTER = 3;
    private mTask task_updateUI;
    private mTask task_saveDataToFile;
    private Timer timer_updateUI;
    private Timer timer_saveDataToFile;
    private boolean flag = false;
    private String dirName;
    private String fileName;
    private float[] gravity;
    private float[] geomagnetic;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        timer_updateUI = new Timer();
        timer_saveDataToFile = new Timer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i("Service State", "onStartCommand()");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Service State", "onDestroy()");
        if (flag) {
            UnregisterSensors();
            task_updateUI.cancel();
            task_saveDataToFile.cancel();
        }
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
        receptor = intent.getParcelableExtra("Receiver");
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
                sensorValues[0] = event.values[0];
                sensorValues[1] = event.values[1];
                sensorValues[2] = event.values[2];
                gravity = event.values;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorValues[3] = event.values[0];
                sensorValues[4] = event.values[1];
                sensorValues[5] = event.values[2];
                geomagnetic = event.values;
                break;
        }
        if (gravity != null && geomagnetic != null) {
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
                    gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                sensorValues[6] = (float)Math.toDegrees(orientation[0]);
                sensorValues[7] = (float)Math.toDegrees(orientation[1]);
                sensorValues[8] = (float)Math.toDegrees(orientation[2]);
            } else {
                Log.e("RAUL TEST", "Could not compute rotationmatrix");
            }
        }
    }

    private Runnable mPollTask = new Runnable() {
        public void run() {
            mHandler.postDelayed(mPollTask, POLL_INTERVAL);
        }

    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void RegisterSensors() {
        // Register sensors listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void UnregisterSensors() {
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
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
                    task_updateUI = new mTask(0);
                    task_saveDataToFile = new mTask(1);
                    timer_updateUI.scheduleAtFixedRate(task_updateUI, 100, 100); // Timer to
                    // update UI
                    timer_saveDataToFile.scheduleAtFixedRate(task_saveDataToFile, 1000, 10); // Timer to write
                    // to file
                    flag = true;
                    break;
                case MSG_SENSOR_UNREGISTER:
                    UnregisterSensors();
                    task_updateUI.cancel();
                    task_saveDataToFile.cancel();
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

    private void mSaveData(String DirectoryName, String FileName) {
        Date date = new Date();
        String value = "";

        for (int i = 0; i < sensorValues.length; i++) {
            value = value + sensorValues[i] + ",";
        }
        try {
            File dir = getAlbumStorageDir(DirectoryName);
            File file = new File(dir, FileName);
            FileWriter writer = new FileWriter(file, true);
            BufferedWriter output = new BufferedWriter(writer);
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

    private void updateUI() {
        // TODO Auto-generated method stub
        bundle = new Bundle();
        bundle.putFloatArray("Array", sensorValues);
        receptor.send(100, bundle);

    }
}
