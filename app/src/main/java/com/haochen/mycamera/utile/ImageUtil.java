package com.haochen.mycamera.utile;

import android.media.Image;

import java.nio.ByteBuffer;

public class ImageUtil {
    public static byte[] getByte(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }
}
