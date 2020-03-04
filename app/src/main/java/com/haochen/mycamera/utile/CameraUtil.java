package com.haochen.mycamera.utile;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.os.Environment;
import android.text.TextUtils;

import java.util.Objects;

public class CameraUtil {
    public static void closeCameraDevice(CameraDevice device) {
        if (Objects.nonNull(device)) {
            device.close();
            device = null;
        }
    }

    public static void closeCameraCaptureSession(CameraCaptureSession mCameraCaptureSession) {
        if (Objects.nonNull(mCameraCaptureSession)) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }
}
