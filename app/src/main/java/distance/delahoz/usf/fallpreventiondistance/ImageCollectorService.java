package distance.delahoz.usf.fallpreventiondistance;

import android.app.Service;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import distance.delahoz.usf.fallpreventiondistance.callbacks.CameraCaptureSessionCaptureCallback;
import distance.delahoz.usf.fallpreventiondistance.callbacks.CameraCaptureSessionStateCallback;
import distance.delahoz.usf.fallpreventiondistance.callbacks.CameraStateCallback;

/**
 * Created by Panos on 11/11/2016.
 */
/**************************************************************************************
 * Yueng. The code below allows for the continuous retrieval of frames
 * from the camera. Camera2 API by Google is used. The files are being saved as JPEG's. Also,
 * you will find something you might be unfamiliar with in this code. The unfamiliarity at hand
 * is known as... a... comment. Currently, what you are reading, is a comment. The gray lines
 * in the code, are comments.  They are used to help you understand unfamiliar code,
 * or code you have forgotten all about.
 *
 * This code will take you on a journey of self discovery, self loathing,
 * fear, anxiety, and finally, a false sense of accomplishment.
 *
 * Enjoy!
 * **********************************************************************************/
//class definition for the camera service, extending a service
public class ImageCollectorService extends Service {
    /*the following variables define the tag for logs, the camera, the camera device,
     * the session for the capture session of the camera,an image reader to handle the image,
     * a handler thread to run the service on a separate thread to not block the ui,
     * and a handler to run with the new thread*/
    private final String TAG = this.getClass().getName();
    private static final int CAMERA = CameraCharacteristics.LENS_FACING_BACK;
    private CameraDevice cameraDevice;
    private CameraCaptureSession session;
    private ImageReader imageReader;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private CameraCharacteristics mCharacteristics;
    public static final int MSG_SENSOR_REGISTER = 2;
    public static final int MSG_SENSOR_UNREGISTER = 3;
    private ResultReceiver receptor;
    private Bundle bundle;
    private int deviceWidth;
    private int deviceHeight;

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    // Callbacks
    private CameraStateCallback cameraStateCallback = new CameraStateCallback(this);
    private CameraCaptureSessionStateCallback cameraCaptureSessionStateCallback = new CameraCaptureSessionStateCallback(this);
    private CameraCaptureSessionCaptureCallback cameraCaptureSessionCaptureCallback = new CameraCaptureSessionCaptureCallback(this);
    private ImageAvailableCallback onImageAvailableListener;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private String KEY_IMAGE = "image";

    //state of the app once tapped on
    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate from camera service");
        super.onCreate();
    }

    /**
     * Return the Camera Id which matches the field CAMERA.
     */
    public String getCamera(CameraManager manager) {
        Log.i(TAG, "getCamera from camera service");
        // getCameraIdList() returns a list of currently connected camera devices
        try {
            for (String cameraId : manager.getCameraIdList()) {
                //getCameraCharacteristics() presents the capabilities of the selected camera
                mCharacteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = mCharacteristics.get(CameraCharacteristics.LENS_FACING);
                Log.i("camera2id", cameraId);
                if (cOrientation == CAMERA) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("Service State", "onBind()");
        receptor = intent.getParcelableExtra("Receiver");
        deviceHeight = intent.getIntExtra("height", 240);
        deviceWidth = intent.getIntExtra("width", 240);
        bundle = new Bundle();
        Toast.makeText(getApplicationContext(), "Binding...",
                Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }

    /* the following starts the intent for working with the main activity. if everything
    * gets setup as it should, our camera will start reading images at the below specified
    * desired format, size, and repeat interval. */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,"Current thread is " + Thread.currentThread().getName());
        Log.i(TAG, "onStartCommand from camera service");

        /*
        Instantiating callback that handles how images are processed once they are available.
        */
        onImageAvailableListener =  new ImageAvailableCallback();

        //background thread started, so we do not block ui thread
        startBackgroundThread();
        Toast.makeText(this, "Service starting", Toast.LENGTH_SHORT).show();
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            imageReader = ImageReader.newInstance(deviceWidth, deviceHeight,  ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(onImageAvailableListener,getmBackgroundHandler());
            Log.i(TAG, "onStartCommand");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraOpenCloseLock.release();
            /* this shows error because it is looking for a request made to the
            * user to allow access of the camera. this check is done in the main activity file,
            * so we can ignore the presented error.
            *
            * or not, whatever...*/
            manager.openCamera(getCamera(manager), cameraStateCallback, getmBackgroundHandler()); /** ignore error. warns about checking permissions. this happens in main*/
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    //state of the app if it is closed or has crashed
    @Override
    public void onDestroy() {
        closeCamera();
        Log.d(TAG, "Service should be dead here");
    }

    //a package of setting and outputs needed to capture an image from the camera device
    public CaptureRequest createCaptureRequest(Surface surface) {
        try {
            //"builds" a request to capture an image
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            /* from the built request to capture an image, here we get the surface
             to be used to project the image on*/
            builder.addTarget(surface);
            /** this needs to be fixed*/
            builder.set(CaptureRequest.JPEG_ORIENTATION, mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION));
            builder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            builder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //returns the "build" request made
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }

    /** Starts a background thread and its {@link Handler}. */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /** Stops the background thread and its {@link Handler}.*/
    public void stopBackgroundThread() {
        HandlerThread moribund = mBackgroundThread;
        mBackgroundThread = null;
        moribund.quitSafely();
        mBackgroundHandler = null;
    }

    /**
     * Closes the current {@link CameraDevice}.
     *
     * This method also terminates the CameraDevice and the ImageReader.
     */
    private void closeCamera() {
        Log.i(TAG,"Closing Camera");
        Toast.makeText(this, "stopping service", Toast.LENGTH_SHORT).show();
        try {
            mCameraOpenCloseLock.acquire();
            if (null != session) {
                session.stopRepeating();
                session.close();
                session = null;
            }
            Log.i(TAG,"Closing session");
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            Log.i(TAG,"Closing cameraDevice");
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
            Log.i(TAG,"Closing imageReader");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.i(TAG,"releasing lock");
            stopBackgroundThread();
            mCameraOpenCloseLock.release();
            Log.i(TAG,"Closed Camera");
        }
    }

    public CameraDevice getCameraDevice() {
        return this.cameraDevice;
    }

    public void setCameraDevice(CameraDevice cameraDevice) {
        this.cameraDevice = cameraDevice;
    }

    public ImageReader getImageReader() {
        return this.imageReader;
    }

    public CameraCaptureSessionStateCallback getCameraCaptureSessionStateCallback() {
        return this.cameraCaptureSessionStateCallback;
    }

    public CameraCaptureSession getCameraCaptureSession() {
        return this.session;
    }

    public void setCameraCaptureSession(CameraCaptureSession session) {
        this.session = session;
    }

    public Handler getmBackgroundHandler() {
        return this.mBackgroundHandler;
    }

    public CameraCaptureSessionCaptureCallback getCameraCaptureSessionCaptureCallback() {
        return this.cameraCaptureSessionCaptureCallback;
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case MSG_SENSOR_REGISTER:
                    Log.d("RAUL", "SENSOR REGISTER CAMERA!");
                    break;
                case MSG_SENSOR_UNREGISTER:
                    closeCamera();
                    Log.d("RAUL", "SENSOR UNREGISTER CAMERA!");
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    public class ImageAvailableCallback implements ImageReader.OnImageAvailableListener {
        private String TAG = getClass().getName();

        public ImageAvailableCallback() {
            Log.d(TAG,"Constructor: Processing Image");
        }

        @Override
        public void onImageAvailable(ImageReader reader) {
        /* the following variables are used to convert the data we get to bytes,
        * and re-construct them to finally create and save an image file*/
            Image img;
            //pretty self explanatory. like, c'mon now. read the line. lazy...
            img = reader.acquireLatestImage();
            /*the full code below would also have "if-else" or "else" statements
            * to check for other types of retrieved images/files */
            if (img == null) return;
            if (img.getFormat() == ImageFormat.JPEG) {
                //check if we have external storage to write to. if we do, save acquired image
                Log.d("RAUL", "IMAGE AVAILABLE!");
                ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                updateUI(bytes);
            }
            img.close();
        }

    }

    private void updateUI(byte[] bytes) {
        bundle = new Bundle();
        bundle.putByteArray("capturedImg", bytes);
        receptor.send(100, bundle);
    }
}
