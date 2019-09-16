package ru.iqchannels.sdk.http;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import ru.iqchannels.sdk.rels.Rels;
import ru.iqchannels.sdk.schema.ChatEvent;
import ru.iqchannels.sdk.schema.ChatEventQuery;
import ru.iqchannels.sdk.schema.ChatMessage;
import ru.iqchannels.sdk.schema.ChatMessageForm;
import ru.iqchannels.sdk.schema.Client;
import ru.iqchannels.sdk.schema.ClientAuth;
import ru.iqchannels.sdk.schema.ClientAuthRequest;
import ru.iqchannels.sdk.schema.ClientIntegrationAuthRequest;
import ru.iqchannels.sdk.schema.ClientSignupRequest;
import ru.iqchannels.sdk.schema.MaxIdQuery;
import ru.iqchannels.sdk.schema.PushTokenInput;
import ru.iqchannels.sdk.schema.RateRequest;
import ru.iqchannels.sdk.schema.RatingInput;
import ru.iqchannels.sdk.schema.RelationMap;
import ru.iqchannels.sdk.schema.Response;
import ru.iqchannels.sdk.schema.UploadedFile;

import static ru.iqchannels.sdk.app.Preconditions.checkNotNull;

public class HttpClient {
    private static final AtomicInteger threadCounter = new AtomicInteger();
    private static final String TAG = "iqchannels.http";

    private final String address;
    private final Gson gson;
    private final Rels rels;
    private final ExecutorService executor;
    private volatile String token;  // Can be changed by the application main thread.

    public HttpClient(@NonNull String address, @NonNull Rels rels) {
        address = checkNotNull(address, "null address");
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }

        this.address = address;
        this.rels = rels;
        this.gson = new Gson();
        this.executor = Executors.newCachedThreadPool(new ThreadFactory() {
            @SuppressLint("DefaultLocale")
            @Override
            public Thread newThread(@NonNull Runnable runnable) {
                String pkg = HttpClient.class.getPackage().getName();
                Thread thread = new Thread(runnable);
                thread.setName(String.format("%s-%d", pkg, threadCounter.incrementAndGet()));
                return thread;
            }
        });
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void clearToken() {
        this.token = null;
    }

    private URL requestUrl(String path) throws MalformedURLException {
        return new URL(String.format("%s/public/api/v1%s", this.address, path));
    }

    // Clients

    public HttpRequest clientsMe(@NonNull final HttpCallback<Client> callback) {
        String path = "/clients/me";
        TypeToken<Response<Client>> type = new TypeToken<Response<Client>>() {};
        return this.post(path, null, type, new HttpCallback<Response<Client>>() {
            @Override
            public void onResult(Response<Client> response) {
                RelationMap map = rels.map(response.Rels);
                Client client = response.Result;
                rels.client(client, map);
                callback.onResult(client);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    public HttpRequest clientsSignup(
            @NonNull String name,
            @NonNull String channel,
            @NonNull final HttpCallback<ClientAuth> callback) {
        checkNotNull(name, "null name");

        String path = "/clients/signup";
        ClientSignupRequest req = new ClientSignupRequest(name, channel);
        TypeToken<Response<ClientAuth>> type = new TypeToken<Response<ClientAuth>>() {};
        return this.post(path, req, type, new HttpCallback<Response<ClientAuth>>() {
            @Override
            public void onResult(Response<ClientAuth> response) {
                RelationMap map = rels.map(response.Rels);
                ClientAuth auth = response.Result;
                rels.clientAuth(auth, map);
                callback.onResult(auth);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    public HttpRequest clientsAuth(
            @NonNull String token,
            @NonNull final HttpCallback<ClientAuth> callback) {
        checkNotNull(token, "null token");

        String path = "/clients/auth";
        ClientAuthRequest req = new ClientAuthRequest(token);
        TypeToken<Response<ClientAuth>> type = new TypeToken<Response<ClientAuth>>() {};
        return this.post(path, req, type, new HttpCallback<Response<ClientAuth>>() {
            @Override
            public void onResult(Response<ClientAuth> response) {
                RelationMap map = rels.map(response.Rels);
                ClientAuth auth = response.Result;
                rels.clientAuth(auth, map);
                callback.onResult(auth);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    public HttpRequest clientsIntegrationAuth(
            @NonNull String credentials,
            @NonNull final HttpCallback<ClientAuth> callback) {
        checkNotNull(credentials, "null credentials");

        String path = "/clients/integration_auth";
        ClientIntegrationAuthRequest req = new ClientIntegrationAuthRequest(credentials);
        TypeToken<Response<ClientAuth>> type = new TypeToken<Response<ClientAuth>>() {};
        return this.post(path, req, type, new HttpCallback<Response<ClientAuth>>() {
            @Override
            public void onResult(Response<ClientAuth> response) {
                RelationMap map = rels.map(response.Rels);
                ClientAuth auth = response.Result;
                rels.clientAuth(auth, map);
                callback.onResult(auth);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    // Push token

    public HttpRequest pushChannelFCM(
            @NonNull String channel,
            @NonNull String token,
            @NonNull final HttpCallback<Void> callback) {
        checkNotNull(channel, "null channel");
        checkNotNull(token, "null push token");

        String path = "/push/channel/fcm/" +channel;
        PushTokenInput input = new PushTokenInput(token);
        return this.post(path, input, null, new HttpCallback<Response<Object>>() {
            @Override
            public void onResult(Response<Object> result) {
                callback.onResult(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    // Channel chat messages

    public HttpRequest chatsChannelTyping(
            @NonNull String channel,
            @NonNull final HttpCallback<Void> callback) {
        checkNotNull(channel, "null channel");

        String path = "/chats/channel/typing/" + channel;
        return this.post(path, null, null, new HttpCallback<Response<Object>>() {
            @Override
            public void onResult(Response<Object> result) {
                callback.onResult(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    public HttpRequest chatsChannelMessages(
            @NonNull String channel,
            @NonNull MaxIdQuery query,
            @NonNull final HttpCallback<List<ChatMessage>> callback) {
        checkNotNull(channel, "null channel");
        checkNotNull(query, "null form");

        String path = "/chats/channel/messages/" + channel;
        TypeToken<Response<List<ChatMessage>>> type = new TypeToken<Response<List<ChatMessage>>>() {};

        return this.post(path, query, type, new HttpCallback<Response<List<ChatMessage>>>() {
            @Override
            public void onResult(Response<List<ChatMessage>> response) {
                RelationMap map = rels.map(response.Rels);
                List<ChatMessage> messages = response.Result;
                rels.chatMessages(messages, map);
                callback.onResult(messages);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    public HttpRequest chatsChannelSend(
            @NonNull String channel,
            @NonNull ChatMessageForm form,
            @NonNull final HttpCallback<Void> callback) {
        checkNotNull(channel, "null channel");
        checkNotNull(form, "null form");

        String path = "/chats/channel/send/" + channel;

        return this.post(path, form, null, new HttpCallback<Response<Object>>() {
            @Override
            public void onResult(Response<Object> result) {
                callback.onResult(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    // Channel chat events

    @SuppressLint("DefaultLocale")
    public HttpRequest chatsChannelEvents(
            @NonNull String channel,
            @NonNull ChatEventQuery query,
            @NonNull final HttpSseListener<List<ChatEvent>> listener) {
        checkNotNull(channel, "null channel");
        checkNotNull(query, "null query");

        String path = "/sse/chats/channel/events/" + channel;
        if (query.LastEventId != null) {
            path = String.format("%s?LastEventId=%d", path, query.LastEventId);
        }
        if (query.Limit != null) {
            if (path.contains("?")) {
                path = path + "&";
            } else {
                path = path + "?";
            }
            path = String.format("%sLimit=%d", path, query.Limit);
        }

        TypeToken<Response<List<ChatEvent>>> type = new TypeToken<Response<List<ChatEvent>>>() {};
        return this.sse(path, type, new HttpSseListener<Response<List<ChatEvent>>>() {
            @Override
            public void onConnected() {
                listener.onConnected();
            }

            @Override
            public void onEvent(Response<List<ChatEvent>> event) {
                RelationMap map = rels.map(event.Rels);
                List<ChatEvent> events = event.Result;
                rels.chatEvents(events, map);
                listener.onEvent(events);
            }

            @Override
            public void onException(Exception e) {
                listener.onException(e);
            }
        });
    }

    public HttpRequest chatsChannelUnread(
            @NonNull String channel,
            @NonNull final HttpSseListener<Integer> listener) {
        checkNotNull(channel, "null channel");

        String path = String.format("/sse/chats/channel/unread/%s", channel);
        TypeToken<Response<Integer>> resultType = new TypeToken<Response<Integer>>() {};
        return this.sse(path, resultType, new HttpSseListener<Response<Integer>>() {
            @Override
            public void onConnected() {
                listener.onConnected();
            }

            @Override
            public void onEvent(Response<Integer> event) {
                listener.onEvent(event.Result);
            }

            @Override
            public void onException(Exception e) {
                listener.onException(e);
            }
        });
    }

    // Chat messages

    public HttpRequest chatsMessagesReceived(
            @NonNull List<Long> messageIds,
            @NonNull final HttpCallback<Void> callback) {
        checkNotNull(messageIds, "null messageIds");

        String path = "/chats/messages/received";
        return this.post(path, messageIds, null, new HttpCallback<Response<Object>>() {
            @Override
            public void onResult(Response<Object> result) {
                callback.onResult(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    public HttpRequest chatsMessagesRead(
            @NonNull List<Long> messageIds,
            @NonNull final HttpCallback<Void> callback) {
        checkNotNull(messageIds, "null messageIds");

        String path = "/chats/messages/read";
        return this.post(path, messageIds, null, new HttpCallback<Response<Object>>() {
            @Override
            public void onResult(Response<Object> result) {
                callback.onResult(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    // Files

    public HttpRequest filesUpload(
            @NonNull File file,
            @Nullable String mimeType,
            @NonNull final HttpCallback<UploadedFile> callback,
            @Nullable final HttpProgressCallback progressCallback) {
        checkNotNull(file, "null file");

        Map<String, String> params = new HashMap<>();
        if (mimeType != null && mimeType.startsWith("image/")) {
            params.put("Type", "image");
        } else {
            params.put("Type", "file");
        }

        Map<String, HttpFile> files = new HashMap<>();
        files.put("File", new HttpFile(mimeType, file));

        String path = "/files/upload";
        TypeToken<Response<UploadedFile>> resultType = new TypeToken<Response<UploadedFile>>() {};
        return this.multipart(path, params, files, resultType, new HttpCallback<Response<UploadedFile>>() {
            @Override
            public void onResult(Response<UploadedFile> response) {
                RelationMap map = rels.map(response.Rels);
                UploadedFile file = response.Result;
                rels.file(file, map);
                callback.onResult(file);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        }, progressCallback);
    }

    // Ratings

    public HttpRequest ratingsRate(
            long ratingId,
            int value,
            @NonNull final HttpCallback<Void> callback) {

        String path = "/ratings/rate";
        RateRequest req = new RateRequest(ratingId, value);

        return this.post(path, req, null, new HttpCallback<Response<Object>>() {
            @Override
            public void onResult(Response<Object> result) {
                callback.onResult(null);
            }

            @Override
            public void onException(Exception exception) {
                callback.onException(exception);
            }
        });
    }

    // POST JSON

    private <T> HttpRequest post(
            @NonNull String path,
            @Nullable final Object body,
            @Nullable final TypeToken<Response<T>> responseType,
            @NonNull final HttpCallback<Response<T>> callback) {
        checkNotNull(path, "null path");
        checkNotNull(callback, "null callback");

        final URL url;
        try {
            url = requestUrl(path);
        } catch (MalformedURLException e) {
            Log.e(TAG, String.format("POST exception, path=%s, exc=%s", path, e));

            callback.onException(e);
            return new HttpRequest();
        }

        final HttpRequest request = new HttpRequest(url, this.token, this.gson, this.executor);
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    request.postJSON(body, responseType, callback);

                } catch (InterruptedIOException e) {
                    Log.d(TAG, String.format("POST cancelled, url=%s", url));
                    callback.onException(e);

                } catch (Exception e) {
                    Log.e(TAG, String.format("POST exception, url=%s, exc=%s", url, e));
                    callback.onException(e);
                }
            }
        });
        return request;
    }

    private <T> HttpRequest multipart(
            @NonNull String path,
            @Nullable final Map<String, String> params,
            @Nullable final Map<String, HttpFile> files,
            @Nullable final TypeToken<Response<T>> resultType,
            @NonNull final HttpCallback<Response<T>> callback,
            @Nullable final HttpProgressCallback progressCallback) {
        checkNotNull(path, "null path");
        checkNotNull(callback, "null callback");

        final URL url;
        try {
            url = requestUrl(path);
        } catch (MalformedURLException e) {
            Log.e(TAG, String.format("Multipart exception, path=%s, exc=%s", path, e));

            callback.onException(e);
            return new HttpRequest();
        }

        final HttpRequest request = new HttpRequest(url, this.token, this.gson, this.executor);
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    request.multipart(params, files, resultType, callback, progressCallback);

                } catch (InterruptedIOException e) {
                    Log.d(TAG, String.format("Multipart cancelled, url=%s", url));
                    callback.onException(e);

                } catch (Exception e) {
                    Log.e(TAG, String.format("Multipart exception, url=%s, exc=%s", url, e));
                    callback.onException(e);
                }
            }
        });
        return request;
    }

    private <T> HttpRequest sse(
            @NonNull final String path,
            @NonNull final TypeToken<Response<T>> eventType,
            @NonNull final HttpSseListener<Response<T>> listener) {
        checkNotNull(path, "null path");
        checkNotNull(eventType, "null resultType");
        checkNotNull(listener, "null callback");

        final URL url;
        try {
            url = requestUrl(path);
        } catch (MalformedURLException e) {
            Log.e(TAG, String.format("SSE: exception, path=%s, exc=%s", path, e));
            listener.onException(e);
            return new HttpRequest();
        }

        final HttpRequest request = new HttpRequest(url, this.token, this.gson, this.executor);
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    request.sse(eventType, listener);
                } catch (Exception e) {
                    Log.e(TAG, String.format("SSE: exception, url=%s, exc=%s", url, e));
                    listener.onException(e);
                }
            }
        });
        return request;
    }
}
