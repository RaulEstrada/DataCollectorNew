package distance.delahoz.usf.fallpreventiondistance.callbacks;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.util.Log;

import java.util.Arrays;

import distance.delahoz.usf.fallpreventiondistance.ImageCollectorService;

/*the following is a callback object for the states of the camera device*/
public class CameraStateCallback extends CameraDevice.StateCallback {
    private final String TAG = getClass().getName();
    private ImageCollectorService cameraService;

    public CameraStateCallback(ImageCollectorService cameraService) {
        this.cameraService = cameraService;
    }

    @Override
    public void onOpened(CameraDevice camera) {
        Log.i(TAG, "onOpened");
        //sets the cameraDevice variable to the opened camera
        cameraService.setCameraDevice(camera);
            /*the following  gives a surface to the images captured by the camera.
            * this is important so we can display the images later. We pass a dummy surface we use while the
            * camera is auto-adjusting or warming up, and the read camera surface to use later */
        try {
            cameraService.getCameraDevice().createCaptureSession(Arrays.asList(cameraService.getImageReader().getSurface()),
                    cameraService.getCameraCaptureSessionStateCallback(), cameraService.getmBackgroundHandler());
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
            cameraService.stopBackgroundThread();
        }
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
        Log.i(TAG, "onDisconnected");
    }

    @Override
    public void onError(CameraDevice camera, int error) {
        Log.e(TAG, "onError");
    }

        /* onOpened, onDisconnected, and onError are states the camera
        * device can be in, similar to the states of an app*/
}
