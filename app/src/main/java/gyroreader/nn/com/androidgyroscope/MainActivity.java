package gyroreader.nn.com.androidgyroscope;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
    private FloatingActionButton startBtn;
    private FloatingActionButton stopBtn;
    private TextView magneticView;
    private TextView acceleroView;
    private TextView orientationView;

    public void startService() {
        Log.d("RAUL", "startService()");
        stopBtn.setVisibility(View.VISIBLE);
        startBtn.setVisibility(View.GONE);
        if (!mBound) {
            Log.d("RAUL", "startService: Not bound!");
            return;
        }
        Log.d("RAUL", "startService: Bound");
        // Create and send a message to the service, using a supported 'what'
        // value
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
            Log.d("RAUL", "onServiceConnected");
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
        Log.d("RAUL", "onBindService()");
        if (!mBound) {
            Log.d("RAUL", "onBindService: not mBound");
            Intent intent_from_trial_selector = getIntent();
            Bundle bundle = intent_from_trial_selector.getExtras();
            File dir = getAlbumStorageDir("testGyroscope");
            String filename = "test";
            String[] patient = new String[]{dir.getName(), filename};
            receptor = new Receiver(null);
            Intent intent = new Intent(getApplicationContext(), SensorBackgroundService.class);
            intent.putExtra("Receiver", receptor);
            intent.putExtra("Content", patient);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    static public File getAlbumStorageDir(String albumName) {
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

    public class Receiver extends ResultReceiver {

        @Override
        protected void onReceiveResult(int resultCode, final Bundle resultData) {
            // TODO Auto-generated method stub

            if (resultCode == 100) {
                data = resultData.getFloatArray("Array");
                Log.d("RAUL ON RECEIVE", data.toString());
                runOnUiThread(updateGUI);
            }

        }

        public Receiver(Handler handler) {
            super(handler);
            // TODO Auto-generated constructor stub
        }

    }

    // Runnables
    private Runnable updateGUI = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            /*accelerationView.setText(data[0]);
            orientationView.setText(data[1]);
            gyroscopeView.setText(data[2]);
            proximityView.setText(data[3]);
            gravityView.setText(data[4]);
            magneticfieldView.setText(data[5]);
            lineaccview.setText(data[6]);
            rotvector.setText(data[7]);
            soundlevelView.setText(data[8]);*/
            paintSensorData(data[0], data[1], data[2], acceleroView);
            paintSensorData(data[3], data[4], data[5], magneticView);
            paintSensorData(data[6], data[7], data[8], orientationView);
            Log.d("RAUL", data[0] + ", " + data[1] + ", " + data[2]);
        }
    };

    private void paintSensorData(float val1, float val2, float val3, TextView view) {
        view.setText(String.format("%.3f, %.3f, %.3f", val1, val2, val3));
    }
}
