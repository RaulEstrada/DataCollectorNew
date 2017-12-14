package gyroreader.nn.com.androidgyroscope;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    /** Messenger for communicating with the service. */
    Messenger msnService = null;
    /** Receiver for getting data from service */
    Receiver receptor;
    private float[] data;
    /** Flag indicating whether we have called bind on the service. */
    private boolean mBound;
    /** Flag indicating whether we have started the service. */
    private boolean isStarted;
    /** Button to start reading sensor data and computing the distance */
    private FloatingActionButton startBtn;
    /** Button to stop reading sensor data and computing the distance */
    private FloatingActionButton stopBtn;
    /** View that displays the magnetic field raw sensor data for debugging purposes */
    private TextView magneticView;
    /** View that displays the accelerometer raw sensor data for debugging purposes */
    private TextView acceleroView;
    /** View that displays the orientation angles computed for debugging purposes */
    private TextView orientationView;
    /** View that displays the final results: phone position (landscape/portrait), angle of inclination
     * and distance*/
    private TextView resultsView;
    /** User height he/she inputs and that will be used to compute the distance */
    private double userHeight = 1f;

    public void startService() {
        stopBtn.setVisibility(View.VISIBLE);
        startBtn.setVisibility(View.GONE);
        if (!mBound) {
            return;
        }
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, SensorBackgroundService.MSG_SENSOR_REGISTER, 0, 0);
        try {
            msnService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopService() {
        stopBtn.setVisibility(View.GONE);
        startBtn.setVisibility(View.VISIBLE);
        Message msg = Message.obtain(null, SensorBackgroundService.MSG_SENSOR_UNREGISTER, 0, 0);
        try {
            msnService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(R.string.act_label);
        startBtn = (FloatingActionButton) findViewById(R.id.startBtn);
        stopBtn = (FloatingActionButton) findViewById(R.id.stopBtn);
        magneticView = (TextView) findViewById(R.id.magneticValues);
        acceleroView = (TextView) findViewById(R.id.accelerValues);
        orientationView = (TextView) findViewById(R.id.orientaValues);
        resultsView = (TextView) findViewById(R.id.resultValues);
        // At first, all values are initialized to 0 and displayed this way to the user
        paintSensorData(0f, 0f, 0f, magneticView);
        paintSensorData(0f, 0f, 0f, acceleroView);
        paintSensorData(0f, 0f, 0f, orientationView);
        stopBtn.setVisibility(View.GONE);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
            }
        });

        mBound = false;
        isStarted=false;
        createHeightInputDialog();
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

    private void createHeightInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View viewInflated = LayoutInflater.from(getApplication())
                .inflate(R.layout.height_dialog, null);
        builder.setView(viewInflated);
        final EditText inputHeight = (EditText) findViewById(R.id.heightInput);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                userHeight = Double.parseDouble(inputHeight.getText().toString());
            }
        });
        builder.show();
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

    void onBindService() {
        if (!mBound) {
            receptor = new Receiver(null);
            Intent intent = new Intent(getApplicationContext(), SensorBackgroundService.class);
            intent.putExtra("Receiver", receptor);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public class Receiver extends ResultReceiver {
        @Override
        protected void onReceiveResult(int resultCode, final Bundle resultData) {
            if (resultCode == 100) {
                data = resultData.getFloatArray("Array");
                runOnUiThread(updateGUI);
            }
        }

        public Receiver(Handler handler) {
            super(handler);
        }
    }

    /**Runnables. It receives the data from the accelerometer and the magnetic field sensors, as well
        as the orientation angles on all three axis. Using the gravity on the x-axis and the y-axis we can
        see if the device is in landscape or portrait mode. If the device is in portrait mode, we use the
        pitch as the inclination angle. If the device is in landscape mode, we use the roll as the inclination
        angle. With the user height and the inclination angle, we can compute the distance using the tangent
     */
    private Runnable updateGUI = new Runnable() {

        @Override
        public void run() {
            paintSensorData(data[0], data[1], data[2], acceleroView);
            paintSensorData(data[3], data[4], data[5], magneticView);
            paintSensorData(data[6], data[7], data[8], orientationView);
            // When absolute value of gravity on x-axis is greater than the absolute value of gravity on
            // y-axis, then the device is in landscape mode.
            boolean landscape = Math.abs(data[0]) >= Math.abs(data[1]);
            float angle = (landscape) ? data[8] : data[7];
            angle = Math.abs(angle);
            double distance = Math.tan(Math.toRadians(angle)) * userHeight;
            String resultString = (landscape) ? "Landscape: " : "Portrait: ";
            resultString += String.format("%.3f", angle) + "\nHeight: " + userHeight +
                    " cm.\nDistance: " + String.format("%.3f", distance) + " cm.";
            resultsView.setText(resultString);
        }
    };

    /**
     * Helper method that formats all the sensor values to have 3 decimal digits and displays them
     * in the specified view.
     */
    private void paintSensorData(float val1, float val2, float val3, TextView view) {
        view.setText(String.format("%.3f, %.3f, %.3f", val1, val2, val3));
    }
}
