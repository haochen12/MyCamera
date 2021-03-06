package com.haochen.mycamera.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.haochen.mycamera.R;
import com.haochen.mycamera.utile.CameraUtil;
import com.haochen.mycamera.utile.ImageReaderUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private AutoFitTextureView mTextureView;
    private CameraManager mCameraManager;
    private Size mPreviewSize;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String mCameraId;
    boolean bCameraFace = false;
    private int mWidth;
    private int mHeight;

    Button takeRecord;

    private MediaRecorder mMediaRecorder;

    private boolean isRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.texture_view);
        initListener();
        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mMediaRecorder = new MediaRecorder();
    }

    private void initListener() {
        Button takePic = findViewById(R.id.take_photo);
        takePic.setOnClickListener(this);
        Button btnChange = findViewById(R.id.btn_change_camera_facing);
        btnChange.setOnClickListener(this);
        takeRecord = findViewById(R.id.take_record);
        takeRecord.setOnClickListener(this);
        Button takeRecordScreen = findViewById(R.id.take_record_screen);
        takeRecordScreen.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    void setupMediaRecorder() {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);

        mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory() + "/DCIM/" + System.currentTimeMillis() + ".mp4");

        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setAudioEncodingBitRate(10000000);

        mMediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "error:" + e.getCause());
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.take_photo:
                takePic();
                break;
            case R.id.btn_change_camera_facing:
                closeCamera();
                if (bCameraFace) {
                    bCameraFace = false;
                } else {
                    bCameraFace = true;
                }
                setCameraSetting(mWidth, mHeight);
                openCamera();
                break;

            case R.id.take_record:
                if (isRecord) {
                    closeRecord();
                    takeRecord.setText("开始");
                    startPreview();
                } else {
                    startRecord();
                }

                break;

            case R.id.take_record_screen:
                break;
        }
    }

    void closeRecord() {
        mMediaRecorder.stop();
        isRecord = false;
    }

    void startRecord() {
        setupMediaRecorder();
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mCaptureRequestBuilder.addTarget(mSurface);
            mCaptureRequestBuilder.addTarget(mMediaRecorder.getSurface());

            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mMediaRecorder.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback, null);
                        mMediaRecorder.start();
                        isRecord = true;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                takeRecord.setText("暂停");
                            }
                        });
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "error message" + e.getReason());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "开启摄像头失败", Toast.LENGTH_SHORT).show();
                    closeCamera();
                    Log.e(TAG, "开启摄像头失败");
                }
            }, null);
        } catch (CameraAccessException e) {
            CameraUtil.closeCameraDevice(mCameraDevice);
            Log.e(TAG, "error message" + e.getReason());
        }
    }

    private void openCamera() {
        try {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO}, 0);
                return;
            }
            mCameraManager.openCamera(mCameraId, mCameraStateCallback, null);

        } catch (CameraAccessException e) {
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
                if (characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_BACK) && !bCameraFace) {
                    mCameraId = cameraId;
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    //根据TextureView的尺寸设置预览尺寸
                    mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);

                } else if (characteristics.get(CameraCharacteristics.LENS_FACING).equals(CameraCharacteristics.LENS_FACING_FRONT) && bCameraFace) {
                    mCameraId = cameraId;
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    //根据TextureView的尺寸设置预览尺寸
                    mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                }

                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                Log.i(TAG, "cameraId" + cameraId);
                //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "error message" + e.getReason());
        }
    }

    private void takePic() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_STATE_FOCUSED_LOCKED);
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getRequestedOrientation());
            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(mCaptureRequestBuilder.build(), null, null);
                        session.stopRepeating();
                        session.abortCaptures();
                        startPreview();
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "error message" + e.getReason());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    session.close();
                    mCameraDevice.close();
                    Log.e(TAG, "fail to create session");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "error message" + e.getReason());
        }
    }

    ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image mImage = reader.acquireNextImage();
            ImageReaderUtil.savePicByCustomPath(mImage, "");
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
            mWidth = width;
            mHeight = height;
            setCameraSetting(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            closeCamera();
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
    SurfaceTexture mSurfaceTexture;
    Surface mSurface;
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "open");
            mCameraDevice = camera;
            mSurfaceTexture = mTextureView.getSurfaceTexture();
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mSurface = new Surface(mSurfaceTexture);
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "disconnected");
            mCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "error");
        }
    };

    private void startPreview() {
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getRequestedOrientation());
            mCaptureRequestBuilder.addTarget(mSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback, null);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "error message" + e.getReason());
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getApplicationContext(), "开启摄像头失败", Toast.LENGTH_SHORT).show();
                    closeCamera();
                    Log.e(TAG, "开启摄像头失败");
                }
            }, null);
        } catch (CameraAccessException e) {
            CameraUtil.closeCameraDevice(mCameraDevice);
            Log.e(TAG, "error message" + e.getReason());
        }
    }

    private void closeCamera() {
        CameraUtil.closeCameraCaptureSession(mCameraCaptureSession);
        CameraUtil.closeCameraDevice(mCameraDevice);
        ImageReaderUtil.closeImageReader(mImageReader);
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
}

