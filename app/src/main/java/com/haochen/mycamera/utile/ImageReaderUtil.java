package com.haochen.mycamera.utile;

import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.haochen.mycamera.activity.MyThreadPool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ImageReaderUtil {
    private static String TAG = ImageReaderUtil.class.getSimpleName();

    public static byte[] getByte(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        closeImage(image);
        return data;
    }

    public static void closeImageReader(ImageReader mImageReader) {
        if (Objects.nonNull(mImageReader)) {
            mImageReader.close();
        }
    }

    public static void closeImage(Image image) {
        if (Objects.nonNull(image)) {
            image.close();
        }
    }

    public static void savePicByCustomPath(Image image, String path) {
        if (TextUtils.isEmpty(path)) {
            path = Environment.getExternalStorageDirectory() + "/DCIM/" + System.currentTimeMillis() + ".jpg";
        }
        ImageSaver mImageSaver = new ImageSaver(image, path);
        MyThreadPool.getInstance().getExecutor().execute(mImageSaver);
    }

    public static class ImageSaver implements Runnable {
        private Image mImage;
        private String mPath;

        public ImageSaver(Image image, String path) {
            mImage = image;
            mPath = path;
        }

        @Override
        public void run() {
            byte[] data = getByte(mImage);
            File mImageFile = new File(mPath);
            if (!mImageFile.exists()) {
                try {
                    mImageFile.createNewFile();
                } catch (IOException e) {
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
