package distance.delahoz.usf.fallpreventiondistance;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import distance.delahoz.usf.fallpreventiondistance.callbacks.SensorBackgroundService;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA = 113;
    private boolean useCamera = false;

    /** Messenger for communicating with the service. */
    Messenger msnSensorService = null;
    Messenger msnCameraService = null;
    /** Receiver for getting data from service */
    enum ReceiverType {CAMERA, SENSOR};
    Receiver sensorReceiver;
    Receiver cameraReceiver;
    /** Flag indicating whether we have called bind on the service. */
    private boolean mBound;
    private boolean cBound;
    /** Array of float values holding the sensor data returned by the sensor background service.
     * First three values correspond to accelerometer, three next values are magnetic field sensor
     * data, and last three values are orientation angle values*/
    private float[] data;
    /** User height he/she inputs and that will be used to compute the distance */
    private double userHeight = 1f;

    private FloatingActionButton startBtn;
    private FloatingActionButton stopBtn;
    private ImageView imageView;
    private TextView orientationView;
    private TextView heightView;
    private TextView distanceView;

    private void askUserInput() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.userHeight);
        final EditText heightInput = new EditText(this);
        heightInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(heightInput);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                userHeight = Double.parseDouble(heightInput.getText().toString());
            }
        });
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("UI State", "onStart()");
        //Bind to remote service
        onBindSensorService();
        onBindCameraService();
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
            unbindService(sensorConnection);
            unbindService(cameraConnection);
            mBound = false;
            cBound = false;
        }
        Log.i("UI State", "onDestroy()");

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int [] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            useCamera = true;
            Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
            Log.i("requests", "camera request");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean camPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
        if (!camPermission){
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }

        orientationView = (TextView) findViewById(R.id.orientationText);
        heightView = (TextView) findViewById(R.id.heightText);
        distanceView = (TextView) findViewById(R.id.distanceText);
        imageView = (ImageView) findViewById(R.id.imageView);
        startBtn = (FloatingActionButton) findViewById(R.id.startBtn);
        stopBtn = (FloatingActionButton) findViewById(R.id.stopBtn);
        stopBtn.setVisibility(View.GONE);

        final Intent processImage = new Intent(this,ImageCollectorService.class);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCustomService(processImage);
            }
        });
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopCustomService(processImage);
            }
        });
        askUserInput();
    }

    public void startCustomService(Intent processImage) {
        stopBtn.setVisibility(View.VISIBLE);
        startBtn.setVisibility(View.GONE);
        if (mBound) {
            // Create and send a message to the service, using a supported 'what' value
            Message msg = Message.obtain(null, SensorBackgroundService.MSG_SENSOR_REGISTER, 0, 0);
            try {
                msnSensorService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (cBound) {
            // Create and send a message to the service, using a supported 'what' value
            Message msg = Message.obtain(null, ImageCollectorService.MSG_SENSOR_REGISTER, 0, 0);
            try {
                msnCameraService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            startService(processImage);
        }
    }

    public void stopCustomService(Intent processImage) {
        stopBtn.setVisibility(View.GONE);
        startBtn.setVisibility(View.VISIBLE);
        Message msgSensor = Message.obtain(null, SensorBackgroundService.MSG_SENSOR_UNREGISTER, 0, 0);
        try {
            msnSensorService.send(msgSensor);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Message msgCamera = Message.obtain(null, ImageCollectorService.MSG_SENSOR_UNREGISTER, 0, 0);
        try {
            msnCameraService.send(msgCamera);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        stopService(processImage);
    }

    private ServiceConnection sensorConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            msnSensorService = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            msnSensorService = null;
            mBound = false;
        }
    };

    private ServiceConnection cameraConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            msnCameraService = new Messenger(service);
            cBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            msnCameraService = null;
            cBound = false;
        }
    };

    void onBindSensorService() {
        if (!mBound) {
            sensorReceiver = new Receiver(null, ReceiverType.SENSOR);
            Intent intent = new Intent(getApplicationContext(), SensorBackgroundService.class);
            intent.putExtra("Receiver", sensorReceiver);
            bindService(intent, sensorConnection, Context.BIND_AUTO_CREATE);
        }
    }

    void onBindCameraService() {
        if (!cBound) {
            cameraReceiver = new Receiver(null, ReceiverType.CAMERA);
            Intent intent = new Intent(getApplicationContext(), ImageCollectorService.class);
            intent.putExtra("Receiver", cameraReceiver);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;
            intent.putExtra("height", height);
            intent.putExtra("width", width);
            bindService(intent, cameraConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public class Receiver extends ResultReceiver {
        private ReceiverType type;

        @Override
        protected void onReceiveResult(int resultCode, final Bundle resultData) {
            if (resultCode == 100) {
                if (type == ReceiverType.SENSOR){
                    data = resultData.getFloatArray("Array");
                    runOnUiThread(updateGUI);
                } else if (type == ReceiverType.CAMERA) {
                    byte[] imgBytes = resultData.getByteArray("capturedImg");
                    Bitmap bm = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    imageView.setImageBitmap(bm);
                }
            }
        }

        public Receiver(Handler handler, ReceiverType type) {
            super(handler);
            this.type = type;
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
            //paintSensorData(data[0], data[1], data[2], acceleroView);
            //paintSensorData(data[3], data[4], data[5], magneticView);
            //paintSensorData(data[6], data[7], data[8], orientationView);
            // When absolute value of gravity on x-axis is greater than the absolute value of gravity on
            // y-axis, then the device is in landscape mode.
            boolean landscape = Math.abs(data[0]) >= Math.abs(data[1]);
            // If landscape mode, we use the roll. Otherwise, we use the pitch
            float angle = (landscape) ? data[8] : data[7];
            angle = Math.abs(angle);
            // The inclination angle (roll or pitch) and the angle with the floor form a right angle,
            // so we can compute the angle with the floor to later use its tangent to compute the distance d
            float floorAngle = 90 - angle;
            Double distance = Double.NaN;
            if (floorAngle != 0) {
                distance = userHeight / Math.tan(Math.toRadians(floorAngle));
            }
            String orientationMsg = (landscape) ? "Landscape: " : "Portrait: ";
            orientationMsg += String.format("%.3f", floorAngle) + "º";
            orientationView.setText(orientationMsg);
            heightView.setText(String.format("%.3f", userHeight));
            distanceView.setText(String.format("%.3f", distance));
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
