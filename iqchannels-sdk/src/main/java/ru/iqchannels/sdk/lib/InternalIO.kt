/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.lib;

import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 27/04/2017.
 */

public class InternalIO {
    public static interface ProgressCallback {
        void onProgress(int progress);
    }

    private InternalIO() {}

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static void copy(byte[] src, OutputStream dst) throws IOException {
        copy(src, dst, null);
    }

    public static void copy(byte[] src, OutputStream dst,
                            @Nullable ProgressCallback callback) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(src);

        int n;
        int total = 0;
        int progress = 0;

        byte[] buf = new byte[16 * 1024];
        while ((n = input.read(buf)) > -1) {
            dst.write(buf, 0, n);
            dst.flush();

            total += n;
            int newProgress = (total * 100) / src.length;
            if (newProgress != progress) {
                progress = newProgress;
                if (callback != null) {
                    callback.onProgress(progress);
                }
            }
        }
    }

    public static void copy(InputStream src, OutputStream dst) throws IOException {
        int n;

        byte[] buf = new byte[16 * 1024];
        while ((n = src.read(buf)) > -1) {
            dst.write(buf, 0, n);
            dst.flush();
        }
    }
}
