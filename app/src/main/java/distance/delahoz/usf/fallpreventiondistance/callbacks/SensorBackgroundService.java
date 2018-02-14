package distance.delahoz.usf.fallpreventiondistance.callbacks;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Service that gathers data from the accelerometer and the magnetic field sensors. From this
 * raw data, it computes the Android inclination matrix I and the rotation matrix R. From the
 * rotation matrix R, it computes the Android orientation.
 * This service updates the UI with a delay and period between executions of 100ms, and writes the
 * data to a file stored in the Android device with a delay of 1,000ms and a period between executions
 * of 10ms
 */
public class SensorBackgroundService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    /* The result returned by the service is an array with the following values:
        0. Acceleration on the x-axis (accelerometer val[0])
        1. Acceleration on the y-axis (accelerometer val[1])
        2. Acceleration on the z-axis (accelerometer val[2])
        3. Ambient magnetic field on the x-axis (magnetic field val[0])
        4. Ambient magnetic field on the y-axis (magnetic field val[1])
        5. Ambient magnetic field on the z-axis (magnetic field val[2])
        6. Azimuth, or angle of rotation around the z-axis. Value in degrees
        7. Pitch, or angle of rotation around the x-axis. Value in degrees
        8. Roll, or angle of rotation around the y-axis. Value in degrees
     */
    private float[] sensorValues = new float[] { 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f};
    private Handler mHandler = new Handler();
    private static final int POLL_INTERVAL = 300;
    private Bundle bundle;
    private ResultReceiver receptor;
    public static final int MSG_SENSOR_REGISTER = 2;
    public static final int MSG_SENSOR_UNREGISTER = 3;
    private mTask task_updateUI;
    private Timer timer_updateUI;
    private boolean flag = false;
    // Values of the acceleration force on all three axis of the device. Needed to compute device orientation
    private float[] gravity;
    // Values of the magnetic field on all three axis of the device. Needed to compute device orientation
    private float[] geomagnetic;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public void onCreate() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        timer_updateUI = new Timer();
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
        }
    }

    /**
     * When binding to the service, we return an interface to our messenger for
     * sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("Service State", "onBind()");
        receptor = intent.getParcelableExtra("Receiver");
        mHandler.postDelayed(mPollTask, POLL_INTERVAL);
        bundle = new Bundle();
        Toast.makeText(getApplicationContext(), "Binding...",
                Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    @Override
    /**
     * When the service receives a sensor event that comes from the accelerometer or the magnetic field,
     * it adds the values received to the sensorValues array that periodically sends to the UI and saves in
     * a data file on the phone. if the service has the values for both the gravity and magnetic field,
     * it tries to compute the orientation angles on all three axis.
     */
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
        tryToComputeOrientation();
    }

    /**
     * If the service has the values for both the gravity and magnetic field,
     * it computes the rotation matrix R transforming a vector from the device coordinate system to the
     * world's coordinate system. From the rotation matrix R, we compute the orientation and obtain the
     * different angles of rotation around the z-, x- and y-axis in radians. The Pitch (rotation around
     * the x-axis) is used to compute the distance when the device is in portrait mode. The Roll (rotation
     * around the y-axis) is used to compute the distance when the device is in landscape mode.
     */
    private void tryToComputeOrientation() {
        if (gravity != null && geomagnetic != null) {
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
                    gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                sensorValues[6] = (float) Math.toDegrees(orientation[0]);
                sensorValues[7] = (float) Math.toDegrees(orientation[1]);
                sensorValues[8] = (float) Math.toDegrees(orientation[2]);
            } else {
                Log.e("SENSOR BACKGROUND", "Could not compute orientation");
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
                    timer_updateUI.scheduleAtFixedRate(task_updateUI, 100, 100); // Timer to update UI
                    flag = true;
                    break;
                case MSG_SENSOR_UNREGISTER:
                    UnregisterSensors();
                    task_updateUI.cancel();
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
            if (task == 0) {
                updateUI();
            }
        }
    }

    private void updateUI() {
        bundle = new Bundle();
        bundle.putFloatArray("Array", sensorValues);
        receptor.send(100, bundle);

    }
}
