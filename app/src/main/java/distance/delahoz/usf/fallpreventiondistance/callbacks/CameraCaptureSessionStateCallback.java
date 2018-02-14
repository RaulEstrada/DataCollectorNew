package distance.delahoz.usf.fallpreventiondistance.callbacks;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.util.Log;

import distance.delahoz.usf.fallpreventiondistance.ImageCollectorService;

public class CameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
    private final String TAG = getClass().getName();
    private ImageCollectorService cameraService;

    public CameraCaptureSessionStateCallback(ImageCollectorService cameraService) {
        this.cameraService = cameraService;
    }

    @Override
    public void onConfigured(CameraCaptureSession session) {
        Log.i(TAG, "onConfigured");
            /*the following creates a session for the camera to
             * repeatedly make requests to capture an image (will go until
             * stopped by the user or app crashes) */
        cameraService.setCameraCaptureSession(session);
        try {
            // In the beginning, the capture request works with a dummy surface while the real camera is adjusting
            session.setRepeatingRequest(cameraService.createCaptureRequest(cameraService.getImageReader().getSurface()),
                    cameraService.getCameraCaptureSessionCaptureCallback(), cameraService.getmBackgroundHandler());
        } catch (CameraAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {
        Log.i(TAG, "onConfiguredFailed");
    }
}
