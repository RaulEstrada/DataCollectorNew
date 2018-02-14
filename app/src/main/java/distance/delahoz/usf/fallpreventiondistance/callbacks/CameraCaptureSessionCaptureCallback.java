package distance.delahoz.usf.fallpreventiondistance.callbacks;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

import distance.delahoz.usf.fallpreventiondistance.ImageCollectorService;

public class CameraCaptureSessionCaptureCallback extends CameraCaptureSession.CaptureCallback {
    private int m = 0;
    private ImageCollectorService cameraService;
    private final String TAG = getClass().getName();

    public CameraCaptureSessionCaptureCallback(ImageCollectorService cameraService) {
        this.cameraService = cameraService;
    }

    @Override
    public void onCaptureProgressed( CameraCaptureSession session,
                                     CaptureRequest request,
                                     CaptureResult partialResult) {
    }

    @Override
    public void onCaptureCompleted( CameraCaptureSession session,
                                    CaptureRequest request,
                                    TotalCaptureResult result) {
        m++;
    }
}
