/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.http;

import android.annotation.SuppressLint;
import android.util.StringBuilderPrinter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import ru.iqchannels.sdk.Log;
import ru.iqchannels.sdk.lib.InternalIO;
import ru.iqchannels.sdk.schema.ChatException;
import ru.iqchannels.sdk.schema.Relations;
import ru.iqchannels.sdk.schema.Response;
import ru.iqchannels.sdk.schema.ResponseError;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/01/2017.
 */

public class HttpRequest {
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int POST_READ_TIMEOUT_MILLIS = 15_000;
    private static final int SSE_READ_TIMEOUT_MILLIS = 120_000;
    private static final String TAG = "iqchannels.http";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final URL url;
    private final String token;
    private final Gson gson;
    private final ExecutorService executor;

    // Guarded by synchronized.
    private boolean closed;
    private HttpURLConnection conn;

    HttpRequest() {
        url = null;
        token = null;
        gson = null;
        executor = null;
        conn = null;
    }

    HttpRequest(URL url, String token, Gson gson, ExecutorService executor) {
        this.url = url;
        this.token = token;
        this.gson = gson;
        this.executor = executor;
    }

    public void cancel() {
        if (executor == null) {
            return;
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                closeConnection();
            }
        });
    }

    private synchronized void closeConnection() {
        closed = true;
        if (conn != null) {
            conn.disconnect();
        }
    }

    private synchronized HttpURLConnection openConnection() throws IOException {
        if (closed) {
            return null;
        }
        if (conn != null) {
            return conn;
        }

        assert url != null;
        conn = (HttpURLConnection) url.openConnection();
        return conn;
    }

    @SuppressLint("DefaultLocale")
    <T> void postJSON(@Nullable Object body,
                      @Nullable TypeToken<Response<T>> resultType,
                      @NonNull HttpCallback<Response<T>> callback) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection();
            if (conn == null) {
                return;
            }

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            if (token != null) {
                conn.setRequestProperty("Authorization", String.format("Client %s", token));
            }

            conn.setReadTimeout(POST_READ_TIMEOUT_MILLIS);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setDoInput(true);
            Log.d(TAG, String.format("POST: %s", url));

            // Write a body if present.
            if (body != null) {
                conn.setDoOutput(true);
                String json = gson.toJson(body);
                byte[] bytes = json.getBytes(UTF8);
                conn.setRequestProperty("Content-Length", String.format("%d", bytes.length));

                BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
                try {
                    out.write(bytes);
                    out.flush();
                } finally {
                    out.close();
                }
            }

            // Get a status code.
            int status = conn.getResponseCode();
            String statusText = conn.getResponseMessage();
            if ((status / 100) != 2) {
                throw new HttpException(statusText);
            }

            // Assert an application/json response.
            String ctype = conn.getContentType();
            if (ctype == null || !ctype.contains("application/json")) {
                throw new HttpException(String.format("Unsupported response content type '%s'", ctype));
            }

            // Read a response when not void.
            final Response<T> result;
            int clength = conn.getContentLength();
            if (resultType == null) {
                result = new Response<>();
                result.OK = true;
                result.Result = null;
                result.Rels = new Relations();

            } else {
                if (clength == 0) {
                    throw new HttpException("Empty server response");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                try {
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {
                        builder.append(line).append('\n');
                    }

                    result = gson.fromJson(builder.toString(), resultType.getType());
                    // result = gson.fromJson(reader, resultType.getType());
                } finally {
                    reader.close();
                }
            }
            Log.d(TAG, String.format("POST %d %s %db", status, url, clength));

            if (result.OK) {
                callback.onResult(result);
                return;
            }

            ResponseError error = result.Error;
            if (error == null) {
                callback.onException(ChatException.unknown());
                return;
            }

            callback.onException(new ChatException(error.Code, error.Text));

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    <T> void multipart(
            @Nullable Map<String, String> params,
            @Nullable Map<String, HttpFile> files,
            @Nullable TypeToken<Response<T>> resultType,
            @NonNull HttpCallback<Response<T>> callback,
            @Nullable final HttpProgressCallback progressCallback) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection();
            if (conn == null) {
                return;
            }

            String boundary = generateMultipartBoundary();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    String.format("multipart/form-data;boundary=%s", boundary));
            if (token != null) {
                conn.setRequestProperty("Authorization", String.format("Client %s", token));
            }

            conn.setReadTimeout(POST_READ_TIMEOUT_MILLIS);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Write a multipart body.
            byte[] body = generateMultipartBody(boundary, params, files).toByteArray();
            conn.setRequestProperty("Content-Length", String.format("%d", body.length));

            OutputStream out = conn.getOutputStream();
            try {
                InternalIO.copy(body, out, new InternalIO.ProgressCallback() {
                    @Override
                    public void onProgress(int progress) {
                        if (progressCallback != null) {
                            progressCallback.onProgress(progress);
                        }
                    }
                });
            } finally {
                out.close();
            }

            // Get a status code.
            int status = conn.getResponseCode();
            String statusText = conn.getResponseMessage();

            if ((status / 100) != 2) {
                HttpException exception = new HttpException(statusText);
                exception.code = status;

                throw exception;
            }

            // Assert an application/json response.
            String ctype = conn.getContentType();
            if (ctype == null || !ctype.contains("application/json")) {
                throw new HttpException(String.format("Unsupported response content type '%s'", ctype));
            }

            // Read a response when not void.
            final Response<T> result;
            int clength = conn.getContentLength();
            if (resultType == null) {
                result = new Response<>();
                result.OK = true;
                result.Result = null;
                result.Rels = new Relations();

            } else {
                if (clength == 0) {
                    throw new HttpException("Empty server response");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                try {
                    result = gson.fromJson(reader, resultType.getType());
                } finally {
                    reader.close();
                }
            }
            Log.d(TAG, String.format("POST %d %s %db", status, url, clength));

            if (result.OK) {
                callback.onResult(result);
                return;
            }

            ResponseError error = result.Error;
            if (error == null) {
                callback.onException(ChatException.unknown());
                return;
            }

            callback.onException(new ChatException(error.Code, error.Text));

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String generateMultipartBoundary() {
        String uuid = UUID.randomUUID().toString();
        return String.format("-----------iqchannels-boundary-%s", uuid);
    }

    private ByteArrayOutputStream generateMultipartBody(
            String boundary,
            Map<String, String> params,
            Map<String, HttpFile> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (String key : params.keySet()) {
            String value = params.get(key);
            out.write(String.format("--%s\r\n", boundary).getBytes(UTF8));
            out.write(String.format(
                    "Content-Disposition: form-data; name=\"%s\"\r\n\r\n", key)
                    .getBytes(UTF8));
            out.write(value.getBytes(UTF8));
            out.write("\r\n".getBytes(UTF8));
        }

        for (String key : files.keySet()) {
            HttpFile httpFile = files.get(key);
            out.write(String.format("--%s\r\n", boundary).getBytes(UTF8));
            out.write(String.format(
                    "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n",
                    key, httpFile.file.getName())
                    .getBytes(UTF8));
            out.write(String.format("Content-Type: %s\r\n\r\n", httpFile.mimeType).getBytes(UTF8));

            FileInputStream in = new FileInputStream(httpFile.file);
            try {
                InternalIO.copy(in, out);
            } finally {
                in.close();
            }

            out.write("\r\n".getBytes(UTF8));
        }

        out.write(String.format("--%s--\r\n", boundary).getBytes(UTF8));
        return out;
    }

    <T> void sse(@NonNull TypeToken<Response<T>> eventType,
                 @NonNull HttpSseListener<Response<T>> listener) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openConnection();
            if (conn == null) {
                return;
            }

            conn.setRequestMethod("GET");
            if (token != null) {
                conn.setRequestProperty("Authorization", String.format("Client %s", token));
            }
            conn.setReadTimeout(SSE_READ_TIMEOUT_MILLIS);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            Log.d(TAG, String.format("SSE %s", url));

            // Get a status code.
            int status = conn.getResponseCode();
            String statusText = conn.getResponseMessage();
            if ((status / 100) != 2) {
                throw new HttpException(statusText);
            }

            // Assert a text/event-stream response.
            String ctype = conn.getContentType();
            if (ctype == null || !ctype.equals("text/event-stream")) {
                throw new HttpException(String.format("Unsupported response content type '%s'", ctype));
            }

            // Read an event stream.
            Log.d(TAG, String.format("SSE connected to %s", url));
            listener.onConnected();

            HttpSseReader reader = null;
            try {
                reader = new HttpSseReader(new BufferedReader(new InputStreamReader(conn.getInputStream())));
                while (true) {
                    HttpSseEvent sseEvent = reader.readEvent();
                    if (sseEvent == null) {
                        break;
                    }

                    assert gson != null;
                    Response<T> event = gson.fromJson(sseEvent.data, eventType.getType());
                    listener.onEvent(event);
                }
            } finally {
                if (reader != null) {
                    //noinspection ThrowFromFinallyBlock
                    reader.close();
                }
            }

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
