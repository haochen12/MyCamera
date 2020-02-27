package com.haochen.cameratest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextureView mTextureView;
    private CameraManager mCameraManager;
    private String mCameraId;
    private Size mPreviewSize;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraDevice cameraDevice;
    private ImageReader mImageReader;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.texture_view);
        Button takePic = findViewById(R.id.take_photo);
        takePic.setOnClickListener(this);
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo:
                takePic();
                break;
            case R.id.take_record:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    private void openCamera() {
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
                return;
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "error message" + e.getReason());
        }

    }

    private void setCameraSetting(int width, int height) {
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
        try {
            //遍历所有摄像头
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                //默认打开后置摄像头
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                //根据TextureView的尺寸设置预览尺寸
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraId = cameraId;
                break;
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "error message" + e.getReason());
        }
    }

    private void takePic() {
        try {
            final CaptureRequest.Builder mBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mBuilder.addTarget(mImageReader.getSurface());
            mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED);
            mBuilder.set(CaptureRequest.JPEG_ORIENTATION, getRequestedOrientation());
            cameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(mBuilder.build(), null, null);
                        startPreview();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.e(TAG, "error message" + e.getReason());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "error message" + e.getReason());
        }
    }

    ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image mImage = reader.acquireNextImage();
            ImageSaver mImageSaver = new ImageSaver(mImage);
            mImageSaver.run();
            mImage.close();
        }
    };

    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setCameraSetting(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mCameraCaptureSession.close();

            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "open");
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "disconnected");
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(TAG, "error");
        }
    };


    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mTextureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface mSurface = new Surface(mSurfaceTexture);
        try {
            mCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mSurface);

            cameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
//                    mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE,CaptureRequest.FLASH_MODE_TORCH);
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.e(TAG, "error message" + e.getReason());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    cameraDevice.close();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "error message" + e.getReason());
            cameraDevice.close();
        }
    }


    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
        }
    };

    public static class ImageSaver implements Runnable {
        private Image mImage;

        public ImageSaver(Image image) {
            mImage = image;
        }

        @Override
        public void run() {

            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            File mImageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/" + System.currentTimeMillis() + ".jpg");
            if (!mImageFile.exists()) {
                try {
                    mImageFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "error message" + e.getCause());
                }
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mImageFile);
                fos.write(data, 0, data.length);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImageFile = null;
                if (fos != null) {
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}

