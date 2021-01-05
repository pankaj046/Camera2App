package sharma.pankaj.cameraapp.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import sharma.pankaj.cameraapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String TAG = "LogC";
    private Size previewsize;
    private Size jpegSizes[] = null;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater()); /*setContentView(R.layout.activity_main);*/
        View view = binding.getRoot();
        setContentView(view);
        init();
    }

    private void init() {
        binding.cameraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    public void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String camerId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(camerId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewsize = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please Allow Permissions", Toast.LENGTH_SHORT).show();
            } else {
                manager.openCamera(camerId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        startCamera();
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {

                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {

                    }
                }, null);
            }

        } catch (Exception e) {
            Log.e(TAG, "startCamera: " + e);
        }
    }

    void startCamera() {
        if (cameraDevice == null || !binding.cameraView.isAvailable() || previewsize == null) {
            return;
        }

        SurfaceTexture texture = binding.cameraView.getSurfaceTexture();
        if (texture == null) {
            return;
        }

        texture.setDefaultBufferSize(previewsize.getWidth(), previewsize.getHeight());
        Surface surface = new Surface(texture);

        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (Exception e) {
            Log.e(TAG, "startCamera: " + e);
        }
        previewBuilder.addTarget(surface);
        try {
            HandlerThread thread = new HandlerThread("changed Preview");
            thread.start();
            Handler handler = new Handler(thread.getLooper());
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    previewSession = session;
                    getChangedPreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, handler);
        } catch (Exception e) {
            Log.e(TAG, "startCamera: " + e);
        }
    }


    void getChangedPreview() {
        if (cameraDevice == null) {
            return;
        }
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("changed Preview");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        try {
            previewSession.setRepeatingRequest(previewBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                private void process(CaptureResult result) {
                    Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
                    Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
                    if (faces != null && mode != null)
                        Log.e("tag", "faces : " + faces.length + " , mode : " + mode);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                    process(partialResult);
                }

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    process(result);
                }
            }, handler);
        } catch (Exception e) {
            Log.e(TAG, "startCamera: " + e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
//        getChangedPreview();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraDevice != null) {
            cameraDevice.close();
        }
    }

    private boolean isPermissionAllowed() {
        final boolean[] isAllowed = {false};
        Dexter.withContext(MainActivity.this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    isAllowed[0] = true;
                } else {
                    isAllowed[0] = false;
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

            }
        }).check();

        return isAllowed[0];
    }
}