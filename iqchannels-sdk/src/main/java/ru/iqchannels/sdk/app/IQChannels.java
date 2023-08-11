package ru.iqchannels.sdk.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;

import ru.iqchannels.sdk.Log;
import ru.iqchannels.sdk.http.HttpCallback;
import ru.iqchannels.sdk.http.HttpClient;
import ru.iqchannels.sdk.http.HttpProgressCallback;
import ru.iqchannels.sdk.http.HttpRequest;
import ru.iqchannels.sdk.http.HttpSseListener;
import ru.iqchannels.sdk.rels.Rels;
import ru.iqchannels.sdk.schema.ChatEvent;
import ru.iqchannels.sdk.schema.ChatEventQuery;
import ru.iqchannels.sdk.schema.ChatException;
import ru.iqchannels.sdk.schema.ChatExceptionCode;
import ru.iqchannels.sdk.schema.ChatMessage;
import ru.iqchannels.sdk.schema.ChatMessageForm;
import ru.iqchannels.sdk.schema.Client;
import ru.iqchannels.sdk.schema.ClientAuth;
import ru.iqchannels.sdk.schema.FileToken;
import ru.iqchannels.sdk.schema.MaxIdQuery;
import ru.iqchannels.sdk.schema.UploadedFile;

import static ru.iqchannels.sdk.app.Preconditions.checkNotNull;

public class IQChannels {
    private static final String TAG = "iqchannels";
    private static final String ANONYMOUS_TOKEN = "anonymous_token";

    private static IQChannels instance;

    public static synchronized IQChannels instance() {
        if (instance == null) {
            instance = new IQChannels();
        }
        return instance;
    }

    // Config and login
    @Nullable private IQChannelsConfig config;
    @Nullable private HttpClient client;
    @Nullable private Handler handler; // Always nonnull where used.
    @Nullable private SharedPreferences preferences;

    @Nullable private String token;
    @Nullable private String credentials;
    @Nullable private String signupName;

    // Auth
    @Nullable private ClientAuth auth;
    @Nullable private HttpRequest authRequest;
    private int authAttempt;
    private final Set<IQChannelsListener> listeners;

    // Push token
    private String pushToken;
    private boolean pushTokenSent;
    private int pushTokenAttempt;
    @Nullable private HttpRequest pushTokenRequest;

    // Unread
    private int unread;
    private int unreadAttempt;
    @Nullable private HttpRequest unreadRequest;
    private final Set<UnreadListener> unreadListeners;

    // Messages
    @Nullable private List<ChatMessage> messages;
    @Nullable private HttpRequest messageRequest;
    private final Set<MessagesListener> messageListeners;

    // More messages
    @Nullable private HttpRequest moreMessageRequest;
    private final Set<Callback<List<ChatMessage>>> moreMessageCallbacks;

    // Events
    private int eventsAttempt;
    @Nullable private HttpRequest eventsRequest;

    // Received queue
    private int receiveAttempt;
    private final Set<Long> receivedQueue;
    @Nullable private HttpRequest receivedRequest;

    // Read queue
    private int readAttempt;
    private final Set<Long> readQueue;
    @Nullable private HttpRequest readRequest;

    // Send queue
    private long localId;
    private int sendAttempt;
    private final List<ChatMessageForm> sendQueue;
    @Nullable private HttpRequest sendRequest;

    private IQChannels() {
        listeners = new HashSet<>();
        unreadListeners = new HashSet<>();
        messageListeners = new HashSet<>();
        moreMessageCallbacks = new HashSet<>();
        receivedQueue = new HashSet<>();
        readQueue = new HashSet<>();
        sendQueue = new ArrayList<>();
    }

    @Nullable
    public ClientAuth getAuth() {
        return auth;
    }

    @Nullable
    public HttpRequest getAuthRequest() {
        return authRequest;
    }

    public Cancellable addListener(final IQChannelsListener listener) {
        this.listeners.add(listener);
        return new Cancellable() {
            @Override
            public void cancel() {
                listeners.remove(listener);
            }
        };
    }

    private void execute(Runnable runnable) {
        if (handler != null) {
            handler.post(runnable);
        } else {
            runnable.run();
        }
    }

    public void configure(Context context, IQChannelsConfig config) {
        if (this.config != null) {
            clear();
        }

        this.handler = new Handler(context.getApplicationContext().getMainLooper());
        this.config = config;
        this.client = new HttpClient(context, config.address, new Rels(config.address));
        this.preferences = context.getApplicationContext().getSharedPreferences(
                "IQChannels", Context.MODE_PRIVATE);
    }

    public Picasso picasso(Context context) {
        if (this.client == null) {
            return Picasso.get();
        }
        return this.client.picasso();
    }

    public void signup(String name) {
        this.logout();

        this.signupName = name;
        this.signupAnonymous();
    }

    public void login(String credentials) {
        this.logout();

        this.credentials = credentials;
        auth();
    }

    public void loginAnonymous() {
        this.logout();

        this.authAnonymous();

        this.token = this.preferences.getString(ANONYMOUS_TOKEN, null);
        if (this.token == null || this.token.isEmpty()) {
            this.signup("");
        } else {
            this.auth();
        }
    }

    public void logout() {
        clear();

        this.credentials = null;
        this.token = null;

        Log.i(TAG, "Logout");
    }

    public void logoutAnonymous() {
        this.logout();

        if (this.preferences == null) {
            return;
        }

        SharedPreferences.Editor editor = this.preferences.edit();
        editor.remove(ANONYMOUS_TOKEN);
        editor.apply();

        Log.i(TAG, "Logout anonymous");
    }

    private void clear() {
        clearAuth();
        clearPushTokenState();
        clearUnread();
        clearMessages();
        clearMoreMessages();
        clearEvents();
        clearReceived();
        clearRead();
        clearSend();

        Log.d(TAG, "Cleared");
    }

    // Auth

    private void clearAuth() {
        if (authRequest != null) {
            authRequest.cancel();
        }

        auth = null;
        authRequest = null;
        if (client != null) {
            client.clearToken();
        }

        this.signupName = null;
        Log.d(TAG, "Cleared auth");
    }

    private void auth() {
        if (auth != null) {
            return;
        }
        if (authRequest != null) {
            return;
        }
        if (client == null) {
            return;
        }
        if (credentials == null && token == null) {
            return;
        }

        HttpCallback<ClientAuth> callback = new HttpCallback<ClientAuth>() {
            @Override
            public void onResult(final ClientAuth result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        authComplete(result);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        authException(exception);
                    }
                });
            }
        };

        authAttempt++;
        if (credentials != null) {
            assert this.config != null;
            String channel = this.config.channel;
            authRequest = this.client.clientsIntegrationAuth(credentials, channel, callback);
        } else {
            authRequest = this.client.clientsAuth(token, callback);
        }
        Log.i(TAG, String.format("Authenticating, attempt=%d", authAttempt));

        for (final IQChannelsListener listener: this.listeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.authenticating();
                }
            });
        }
    }

    private void authException(final Exception exception) {
        if (authRequest == null) {
            return;
        }
        authRequest = null;
        if (credentials == null) {
            Log.e(TAG, String.format("Failed to auth, exc=%s", exception));
            return;
        }

        for (final IQChannelsListener listener: this.listeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.authFailed(exception);
                }
            });
        }

        assert handler != null;
        int delaySec = Retry.delaySeconds(authAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                auth();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to auth, will retry in %d seconds, exc=%s",
                delaySec, exception));
    }

    private void authComplete(final ClientAuth auth) {
        if (auth.Client == null || auth.Session == null || auth.Session.Token == null) {
            authException(new ChatException(ChatExceptionCode.INVALID, "Invalid client auth"));
            return;
        }
        if (authRequest == null) {
            return;
        }
        authRequest = null;
        assert this.client != null;

        this.auth = auth;
        this.authAttempt = 0;
        this.client.setToken(auth.Session.Token);

        Log.i(TAG, String.format("Authenticated, clientId=%d, sessionId=%d",
                auth.Client.Id, auth.Session.Id));

        for (final IQChannelsListener listener: listeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.authComplete(auth);
                }
            });
        }

        sendPushToken();
        loadMessages();
        listenToUnread();
    }

    // Anonymous auth

    private void authAnonymous() {
        if (auth != null) {
            return;
        }
        if (authRequest != null) {
            return;
        }
        if (client == null) {
            return;
        }

        this.token = this.preferences.getString(ANONYMOUS_TOKEN, null);
        if (token == null || token.isEmpty()) {
            this.signupAnonymous();
            return;
        }

        HttpCallback<ClientAuth> callback = new HttpCallback<ClientAuth>() {
            @Override
            public void onResult(final ClientAuth result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        authComplete(result);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        authAnonymousException(exception);
                    }
                });
            }
        };

        authAttempt++;
        authRequest = this.client.clientsAuth(token, callback);
        Log.i(TAG, String.format("Authenticating anonymous, attempt=%d", authAttempt));

        for (final IQChannelsListener listener: this.listeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.authenticating();
                }
            });
        }
    }

    private void authAnonymousException(final Exception exception) {
        if (authRequest == null) {
            return;
        }
        authRequest = null;
        if (credentials == null) {
            Log.e(TAG, String.format("Failed to auth, exc=%s", exception));
            return;
        }

        for (final IQChannelsListener listener: this.listeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.authFailed(exception);
                }
            });
        }

        if (exception instanceof ChatException) {
            ChatException exc = (ChatException) exception;
            ChatExceptionCode code = exc.getCode();

            if (code == ChatExceptionCode.UNAUTHORIZED) {
                Log.e(TAG, "Failed to auth, invalid anonymous token");

                assert this.preferences != null;
                SharedPreferences.Editor editor = this.preferences.edit();
                editor.remove(ANONYMOUS_TOKEN);
                editor.apply();
                this.token = null;

                this.signupAnonymous();
                return;
            }
        }

        assert handler != null;
        int delaySec = Retry.delaySeconds(authAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                auth();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to auth, will retry in %d seconds, exc=%s",
                delaySec, exception));
    }

    // Signup anonymous

    private void signupAnonymous() {
        if (client == null) {
            return;
        }

        this.logout();
        assert this.config != null;

        String name = this.signupName;
        String channel = this.config.channel;

        this.authRequest = this.client.clientsSignup(name, channel, new HttpCallback<ClientAuth>() {
            @Override
            public void onResult(final ClientAuth result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        signupComplete(result);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        signupException(exception);
                    }
                });
            }
        });

        Log.i(TAG, "Signing up anonymous client");
    }

    private void signupComplete(final ClientAuth auth) {
        if (auth.Client == null || auth.Session == null || auth.Session.Token == null) {
            signupException(new ChatException(ChatExceptionCode.INVALID, "Invalid client auth"));
            return;
        }
        if (this.authRequest == null) {
            return;
        }

        assert this.preferences != null;
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putString(ANONYMOUS_TOKEN, auth.Session.Token);
        editor.apply();
        Log.i(TAG, String.format("Signed up anonymous client, clientId=%d", auth.Client.Id));

        authComplete(auth);
    }

    private void signupException(final Exception exception) {
        if (authRequest == null) {
            return;
        }
        authRequest = null;
        Log.e(TAG, String.format("Failed to sign up anonymous client, exc=%s", exception));

        for (final IQChannelsListener listener : listeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.authFailed(exception);
                }
            });
        }

        assert handler != null;
        int delaySec = Retry.delaySeconds(authAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                signupAnonymous();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to signup, will retry in %d seconds, exc=%s",
                delaySec, exception));
    }

    // Push token

    public void setPushToken(String token) {
        if (token != null && pushToken != null && token.equals(pushToken)) {
            return;
        }

        this.pushToken = token;
        this.pushTokenSent = false;
        if (this.pushTokenRequest != null) {
            this.pushTokenRequest.cancel();
            this.pushTokenRequest = null;
        }

        this.sendPushToken();
    }

    private void clearPushTokenState() {
        if (pushTokenRequest != null) {
            pushTokenRequest.cancel();
        }

        pushTokenSent = false;
        pushTokenAttempt = 0;
        pushTokenRequest = null;
        Log.d(TAG, "Cleared push token state");
    }

    private void sendPushToken() {
        if (auth == null) {
            return;
        }
        if (pushToken == null) {
            return;
        }
        if (pushTokenSent) {
            return;
        }
        if (pushTokenRequest != null) {
            return;
        }

        assert client != null;
        assert config != null;

        pushTokenAttempt++;
        pushTokenRequest = client.pushChannelFCM(config.channel, pushToken, new HttpCallback<Void>() {
            @Override
            public void onResult(Void result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        onSentPushToken();
                    }
                });
            }

            @Override
            public void onException(final Exception e) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        onFailedToSendPushToken(e);
                    }
                });
            }
        });
        Log.i(TAG, String.format("Sending a push token, attempt=%d", pushTokenAttempt));
    }

    private void onSentPushToken() {
        if (pushTokenRequest == null) {
            return;
        }

        pushTokenRequest = null;
        pushTokenSent = true;
        Log.i(TAG, "Sent a push token");
    }

    private void onFailedToSendPushToken(Exception e) {
        if (pushTokenRequest == null) {
            return;
        }

        pushTokenRequest = null;

        assert handler != null;
        int delaySec = Retry.delaySeconds(pushTokenAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendPushToken();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format(
                "Failed to send a push token, will retyr in %ds, exc=%s",
                delaySec, e));
    }

    // Unread

    public Cancellable addUnreadListener(final UnreadListener listener) {
        checkNotNull(listener, "null listener");
        unreadListeners.add(listener);
        listenToUnread();
        Log.d(TAG, String.format("Added an unread listener %s", listener));

        final int copy = unread;
        execute(new Runnable() {
            @Override
            public void run() {
                listener.unreadChanged(copy);
            }
        });

        return new Cancellable() {
            @Override
            public void cancel() {
                unreadListeners.remove(listener);
                Log.d(TAG, String.format("Removed an unread listener %s", listener));

                clearUnreadWhenNoListeners();
            }
        };
    }

    private void clearUnread() {
        if (unreadRequest != null) {
            unreadRequest.cancel();
        }

        unread = 0;
        unreadAttempt = 0;
        unreadRequest = null;

        for (final UnreadListener listener : unreadListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.unreadChanged(0);
                }
            });
        }
        Log.d(TAG, "Cleared unread");
    }

    private void clearUnreadWhenNoListeners() {
        if (unreadListeners.isEmpty()) {
            clearUnread();
        }
    }

    private void listenToUnread() {
        if (auth == null) {
            return;
        }
        if (unreadRequest != null) {
            return;
        }
        if (unreadListeners.isEmpty()) {
            return;
        }

        assert client != null;
        assert config != null;

        unreadAttempt++;
        unreadRequest = client.chatsChannelUnread(config.channel, new HttpSseListener<Integer>() {
            @Override
            public void onConnected() {
            }

            @Override
            public void onEvent(final Integer event) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        unreadReceived(event);
                    }
                });
            }

            @Override
            public void onException(final Exception e) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        unreadException(e);
                    }
                });
            }
        });
        Log.i(TAG, String.format("Listening to unread notifications, attempt=%d", unreadAttempt));
    }

    private void unreadException(Exception e) {
        if (unreadRequest == null) {
            return;
        }
        unreadRequest = null;

        if (auth == null) {
            Log.i(TAG, String.format("Failed to listen to unread notifications, exc=%s", e));
            return;
        }

        assert handler != null;
        int delaySec = Retry.delaySeconds(unreadAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                listenToUnread();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format(
                "Failed to listen to unread notifications, will retry in %ds, exc=%s",
                delaySec, e));
    }

    private void unreadReceived(Integer unread) {
        if (unreadRequest == null) {
            return;
        }

        this.unread = unread == null ? 0 : unread;
        unreadAttempt = 0;
        Log.i(TAG, String.format("Received an unread notification, unread=%d", this.unread));

        final int copy = this.unread;
        for (final UnreadListener listener : unreadListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.unreadChanged(copy);
                }
            });
        }
    }

    // Messages

    public Cancellable loadMessages(final MessagesListener listener) {
        checkNotNull(listener, "null listener");
        messageListeners.add(listener);
        Log.d(TAG, String.format("Added a messages listener %s", listener));

        if (messages != null) {
            listenToEvents();

            final List<ChatMessage> copy = new ArrayList<>(messages);
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messagesLoaded(copy);
                }
            });
        } else {
            loadMessages();
        }

        return new Cancellable() {
            @Override
            public void cancel() {
                messageListeners.remove(listener);
                Log.d(TAG, String.format("Removed a messages listener %s", listener));
                cancelLoadMessagesWhenNoListeners();
            }
        };
    }

    private void cancelLoadMessagesWhenNoListeners() {
        if (!messageListeners.isEmpty()) {
            return;
        }

        if (messageRequest != null) {
            messageRequest.cancel();
        }
        clearEvents();
    }

    private void clearMessages() {
        if (messageRequest != null) {
            messageRequest.cancel();
        }

        for (MessagesListener listener : messageListeners) {
            listener.messagesCleared();
        }

        messages = null;
        messageRequest = null;
        Log.d(TAG, "Cleared messages");
    }

    private void loadMessages() {
        if (messages != null) {
            return;
        }
        if (messageRequest != null) {
            return;
        }

        if (auth == null) {
            return;
        }
        if (messageListeners.isEmpty()) {
            return;
        }
        assert client != null;
        assert config != null;

        MaxIdQuery query = new MaxIdQuery();
        messageRequest = client.chatsChannelMessages(config.channel, query, new HttpCallback<List<ChatMessage>>() {
            @Override
            public void onResult(final List<ChatMessage> messages) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        messagesLoaded(messages);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        messagesException(exception);
                    }
                });
            }
        });
        Log.i(TAG, "Loading messages");
    }

    private void messagesException(final Exception exception) {
        if (messageRequest == null) {
            return;
        }
        messageRequest = null;
        Log.e(TAG, String.format("Failed to load messages, exc=%s", exception));

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messagesException(exception);
                }
            });
        }
        messageListeners.clear();
    }

    private void messagesLoaded(List<ChatMessage> messages) {
        if (messageRequest == null) {
            return;
        }

        messageRequest = null;
        setMessages(messages);
        Log.i(TAG, String.format("Loaded messages, size=%d", messages.size()));

        assert this.messages != null;
        final List<ChatMessage> copy = new ArrayList<>(this.messages);
        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messagesLoaded(copy);
                }
            });
        }

        listenToEvents();
    }

    // More messages

    private void clearMoreMessages() {
        if (moreMessageRequest != null) {
            moreMessageRequest.cancel();
        }

        final CancellationException exc = new CancellationException();
        for (final Callback callback : moreMessageCallbacks) {
            execute(new Runnable() {
                @Override
                public void run() {
                    callback.onException(exc);
                }
            });
        }
        moreMessageCallbacks.clear();
    }

    public Cancellable loadMoreMessages(final Callback<List<ChatMessage>> callback) {
        moreMessageCallbacks.add(callback);
        loadMoreMessages();

        return new Cancellable() {
            @Override
            public void cancel() {
                moreMessageCallbacks.remove(callback);
                cancelLoadMoreMessagesWhenNoCallbacks();
            }
        };
    }

    private void cancelLoadMoreMessagesWhenNoCallbacks() {
        if (moreMessageCallbacks.isEmpty()) {
            clearMoreMessages();
        }
    }

    private void loadMoreMessages() {
        if (auth == null || messages == null) {
            for (final Callback<List<ChatMessage>> callback : moreMessageCallbacks) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(null);
                    }
                });
            }
            moreMessageCallbacks.clear();
            return;
        }
        if (moreMessageRequest != null) {
            return;
        }

        MaxIdQuery query = new MaxIdQuery();
        for (ChatMessage message : messages) {
            if (message.Id > 0) {
                query.MaxId = message.Id;
                break;
            }
        }

        assert client != null;
        assert config != null;
        moreMessageRequest = client.chatsChannelMessages(config.channel, query, new HttpCallback<List<ChatMessage>>() {
            @Override
            public void onResult(final List<ChatMessage> messages) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        moreMessagesLoaded(messages);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        moreMessagesException(exception);
                    }
                });
            }
        });
        Log.i(TAG, String.format("Loading more messages, maxId=%s", query.MaxId));
    }

    private void moreMessagesException(final Exception exception) {
        if (moreMessageRequest == null) {
            return;
        }
        moreMessageRequest = null;
        Log.e(TAG, String.format("Failed to load more messages, exc=%s", exception));

        for (final Callback<List<ChatMessage>> callback : moreMessageCallbacks) {
            execute(new Runnable() {
                @Override
                public void run() {
                    callback.onException(exception);
                }
            });
        }
        moreMessageCallbacks.clear();
    }

    private void moreMessagesLoaded(List<ChatMessage> moreMessages) {
        if (moreMessageRequest == null) {
            return;
        }
        moreMessageRequest = null;
        assert messages != null;

        final List<ChatMessage> newMessages = prependMessages(moreMessages);
        Log.i(TAG, String.format("Loaded more messages, count=%d, total=%d",
                moreMessages.size(), messages.size()));

        // Notify the callbacks.
        for (final Callback<List<ChatMessage>> callback : moreMessageCallbacks) {
            execute(new Runnable() {
                @Override
                public void run() {
                    callback.onResult(newMessages);
                }
            });
        }
        moreMessageCallbacks.clear();
    }

    // Events

    private void clearEvents() {
        if (eventsRequest != null) {
            eventsRequest.cancel();
        }

        eventsAttempt = 0;
        eventsRequest = null;
        Log.d(TAG, "Cleared events");
    }

    private void listenToEvents() {
        if (eventsRequest != null) {
            return;
        }

        if (auth == null) {
            return;
        }
        if (messages == null) {
            return;
        }

        ChatEventQuery query = new ChatEventQuery();
        for (ChatMessage message : messages) {
            if (message.EventId == null) {
                continue;
            }
            if (query.LastEventId == null) {
                query.LastEventId = message.EventId;
                continue;
            }
            if (query.LastEventId < message.EventId) {
                query.LastEventId = message.EventId;
            }
        }

        assert client != null;
        assert config != null;

        eventsAttempt++;
        eventsRequest = client.chatsChannelEvents(config.channel, query, new HttpSseListener<List<ChatEvent>>() {
            @Override
            public void onConnected() {}

            @Override
            public void onEvent(final List<ChatEvent> events) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        eventsReceived(events);
                    }
                });
            }

            @Override
            public void onException(final Exception e) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        eventsException(e);
                    }
                });
            }
        });
    }

    private void eventsException(Exception e) {
        if (eventsRequest == null) {
            return;
        }
        eventsRequest = null;

        if (auth == null) {
            Log.i(TAG, String.format("Failed to listen to events, exc=%s", e));
            return;
        }

        assert handler != null;
        int delaySec = Retry.delaySeconds(eventsAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                listenToEvents();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to listen to events, will retry in %d seconds, exc=%s",
                delaySec, e));
    }

    private void eventsReceived(List<ChatEvent> events) {
        if (eventsRequest == null) {
            return;
        }

        eventsAttempt = 0;
        Log.i(TAG, String.format("Received chat events, count=%d", events.size()));

        applyEvents(events);
    }

    // Received message ids

    private void clearReceived() {
        if (receivedRequest != null) {
            receivedRequest.cancel();
        }

        receiveAttempt = 0;
        receivedQueue.clear();
        receivedRequest = null;
        Log.d(TAG, "Cleared received queue");
    }

    private void enqueueReceived(ChatMessage message) {
        if (message.My) {
            return;
        }
        if (message.Received) {
            return;
        }

        receivedQueue.add(message.Id);
        sendReceived();
    }

    private void sendReceived() {
        if (auth == null) {
            return;
        }
        if (receivedQueue.isEmpty()) {
            return;
        }
        if (receivedRequest != null) {
            return;
        }

        final List<Long> messageIds = new ArrayList<>(receivedQueue);
        receivedQueue.clear();
        receiveAttempt++;

        assert client != null;
        receivedRequest = client.chatsMessagesReceived(messageIds, new HttpCallback<Void>() {
            @Override
            public void onResult(Void result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        sentReceivedMessageIds(messageIds);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        sendReceivedMessageIdsException(exception, messageIds);
                    }
                });
            }
        });
        Log.i(TAG, String.format("Sending received message ids, count=%d", messageIds.size()));
    }

    private void sendReceivedMessageIdsException(Exception exception, List<Long> messageIds) {
        if (receivedRequest == null) {
            return;
        }
        receivedRequest = null;

        if (auth == null) {
            Log.i(TAG, "Failed to send received message ids");
            return;
        }

        receivedQueue.addAll(messageIds);

        assert handler != null;
        int delaySec = Retry.delaySeconds(receiveAttempt);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendReceived();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to send received message ids, will retry in %d seconds, exc=%s",
                delaySec, exception));
    }

    private void sentReceivedMessageIds(List<Long> messageIds) {
        if (receivedRequest == null) {
            return;
        }

        receivedRequest = null;
        receiveAttempt = 0;

        Log.i(TAG, String.format("Sent received message ids, count=%d", messageIds.size()));
        sendReceived();
    }

    // Send read message ids

    public void markAsRead(ChatMessage message) {
        if (message.My) {
            return;
        }
        if (message.Read) {
            return;
        }

        readQueue.add(message.Id);
        sendRead();
    }

    private void clearRead() {
        if (readRequest != null) {
            readRequest.cancel();
        }

        readAttempt = 0;
        readRequest = null;
        readQueue.clear();
        Log.d(TAG, "Cleared read queue");
    }

    private void sendRead() {
        if (auth == null) {
            return;
        }
        if (readQueue.isEmpty()) {
            return;
        }
        if (readRequest != null) {
            return;
        }

        final List<Long> messageIds = new ArrayList<>(readQueue);
        readQueue.clear();
        readAttempt++;

        assert client != null;
        readRequest = client.chatsMessagesRead(messageIds, new HttpCallback<Void>() {
            @Override
            public void onResult(Void result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        sentReadMessageIds(messageIds);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        sendReadMessageIdsException(exception, messageIds);
                    }
                });
            }
        });
        Log.i(TAG, String.format("Sending read message ids, count=%d", messageIds.size()));
    }

    private void sendReadMessageIdsException(Exception exception, List<Long> messageIds) {
        if (readRequest == null) {
            return;
        }
        readRequest = null;

        if (auth == null) {
            Log.i(TAG, "Failed to send read message ids");
            return;
        }

        readQueue.addAll(messageIds);

        int delaySec = Retry.delaySeconds(readAttempt);
        assert handler != null;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendRead();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to send read message ids, will retry in %d seconds, exc=%s",
                delaySec, exception));
    }

    private void sentReadMessageIds(List<Long> messageIds) {
        if (readRequest == null) {
            return;
        }

        readRequest = null;
        readAttempt = 0;

        Log.i(TAG, String.format("Sent read message ids, count=%d", messageIds.size()));
        sendRead();
    }

    // Send

    public void send(String text) {
        if (text == null) {
            return;
        }
        if (text.isEmpty()) {
            return;
        }
        if (auth == null) {
            return;
        }

        long localId = nextLocalId();
        final ChatMessage message = new ChatMessage(auth.Client, localId);
        message.Sending = true;

        assert messages != null;
        messages.add(message);

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageSent(message);
                }
            });
        }

        ChatMessageForm form = ChatMessageForm.text(localId, text);
        sendQueue.add(form);
        Log.i(TAG, String.format("Enqueued an outgoing message, localId=%d", localId));
        send();
    }

    public void sendFile(File file) {
        if (file == null) {
            return;
        }
        if (!file.exists()) {
            return;
        }
        if (auth == null) {
            return;
        }

        long localId = nextLocalId();
        final ChatMessage message = new ChatMessage(auth.Client, localId, file);
        assert messages != null;
        messages.add(message);

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageSent(message);
                }
            });
        }

        sendFile(message);
    }

    public void sendFile(final ChatMessage message) {
        if (auth == null) {
            return;
        }
        if (message.UploadRequest != null) {
            return;
        }

        File file = message.Upload;
        if (file == null) {
            message.UploadExc = new Exception("File is not found");
            return;
        }
        if (!file.exists()) {
            message.UploadExc = new Exception("File does not exist");
            return;
        }

        String mimetype = "";
        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (ext != null) {
            mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }

        message.Sending = true;
        message.UploadExc = null;
        message.UploadRequest = this.client.filesUpload(file, mimetype, new HttpCallback<UploadedFile>() {
            @Override
            public void onResult(final UploadedFile uploadedFile) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        if (message.UploadRequest == null) {
                            return;
                        }

                        message.Upload = null;
                        message.UploadExc = null;
                        message.UploadRequest = null;
                        message.UploadProgress = 100;
                        message.File = uploadedFile;
                        Log.i(TAG, String.format("sendFile: Uploaded a file, fileId=%s", uploadedFile.Id));

                        ChatMessageForm form = ChatMessageForm.file(localId, uploadedFile.Id);
                        sendQueue.add(form);
                        Log.i(TAG, String.format("Enqueued an outgoing message, localId=%d", localId));
                        send();

                        for (final MessagesListener listener : messageListeners) {
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    listener.messageUploaded(message);
                                }
                            });
                        }
                    }
                });
            }

            @Override
            public void onException(final Exception e) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        if (message.UploadRequest == null) {
                            return;
                        }

                        message.Sending = false;
                        message.UploadExc = e;
                        message.UploadProgress = 0;
                        message.UploadRequest = null;
                        Log.e(TAG, String.format("sendFile: Failed to upload a file, e=%s", e));

                        for (final MessagesListener listener : messageListeners) {
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    listener.messageUpdated(message);
                                }
                            });
                        }
                    }
                });
            }
        }, new HttpProgressCallback() {
            @Override
            public void onProgress(final int progress) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        message.UploadProgress = progress;

                        for (final MessagesListener listener : messageListeners) {
                            execute(new Runnable() {
                                @Override
                                public void run() {
                                    listener.messageUpdated(message);
                                }
                            });
                        }
                    }
                });
            }
        });

        Log.i(TAG, String.format("Uploading a file, messageLocalId=%d, filename=%s",
                localId, file.getName()));

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageUpdated(message);
                }
            });
        }
    }

    public void cancelUpload(final ChatMessage message) {
        if (auth == null) {
            return;
        }
        if (message.Upload == null) {
            return;
        }

        assert messages != null;
        messages.remove(message);
        if (message.UploadRequest != null) {
            message.UploadRequest.cancel();
        }

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageCancelled(message);
                }
            });
        }
    }

    private void clearSend() {
        if (sendRequest != null) {
            sendRequest.cancel();
        }

        sendAttempt = 0;
        sendQueue.clear();
        sendRequest = null;
        Log.d(TAG, "Cleared send queue");
    }

    private long nextLocalId() {
        long localId = new Date().getTime();
        if (localId < this.localId) {
            localId = this.localId + 1;
        }

        this.localId = localId;
        return localId;
    }

    private void send() {
        if (auth == null) {
            return;
        }
        if (sendQueue.isEmpty()) {
            return;
        }
        if (sendRequest != null) {
            return;
        }

        assert client != null;
        assert config != null;

        final ChatMessageForm form = sendQueue.remove(0);
        sendAttempt++;
        sendRequest = client.chatsChannelSend(config.channel, form, new HttpCallback<Void>() {
            @Override
            public void onResult(Void result) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        sent(form);
                    }
                });
            }

            @Override
            public void onException(final Exception exception) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        sendException(exception, form);
                    }
                });
            }
        });
        Log.i(TAG, String.format("Sending a message, localId=%d", form.LocalId));
    }

    private void sendException(Exception exception, ChatMessageForm form) {
        if (sendRequest == null) {
            return;
        }
        sendRequest = null;

        if (auth == null) {
            Log.i(TAG, String.format("Failed to send a message, exc=%s", exception));
            return;
        }

        sendQueue.add(0, form);

        int delaySec = Retry.delaySeconds(sendAttempt);
        assert handler != null;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                send();
            }
        }, delaySec * 1000);
        Log.e(TAG, String.format("Failed to send a message, will retry in %d seconds, exc=%s",
                delaySec, exception));
    }

    private void sent(ChatMessageForm form) {
        if (sendRequest == null) {
            return;
        }

        sendRequest = null;
        sendAttempt = 0;

        Log.i(TAG, String.format("Sent a message, localId=%d", form.LocalId));
        send();
    }

    // File url

    public void filesUrl(@NonNull String fileId, HttpCallback<String> callback) {
        if (auth == null) {
            return;
        }
        if (client == null) {
            return;
        }

        client.filesUrl(fileId, callback);
    }

    // Ratings

    public void ratingsRate(long ratingId, int value) {
        if (auth == null) {
            return;
        }

        assert client != null;
        assert config != null;

        client.ratingsRate(ratingId, value, new HttpCallback<Void>() {
            @Override
            public void onResult(Void result) { }

            @Override
            public void onException(Exception exception) {}
        });

        Log.i(TAG, String.format("Sent rating, ratingId=%d, value=%d", ratingId, value));
    }

    // Data

    private void setMessages(@NonNull List<ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);

        for (ChatMessage message : messages) {
            enqueueReceived(message);
        }
    }

    private List<ChatMessage> prependMessages(List<ChatMessage> messages) {
        List<ChatMessage> newMessages = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            if (getMessageById(message.Id) == null) {
                newMessages.add(message);
            }
        }

        assert this.messages != null;
        this.messages.addAll(0, newMessages);

        for (ChatMessage message : newMessages) {
            enqueueReceived(message);
        }
        return newMessages;
    }

    @Nullable
    private ChatMessage getMessageById(long messageId) {
        if (messages == null) {
            return null;
        }

        for (ChatMessage message : messages) {
            if (message.Id == messageId) {
                return message;
            }
        }
        return null;
    }

    @Nullable
    private ChatMessage getMyMessageByLocalId(long localId) {
        if (messages == null) {
            return null;
        }

        for (ChatMessage message : messages) {
            if (message.My && message.LocalId == localId) {
                return message;
            }
        }
        return null;
    }

    private void applyEvents(List<ChatEvent> events) {
        for (ChatEvent event : events) {
            applyEvent(event);
        }
    }

    private void applyEvent(ChatEvent event) {
        switch (event.Type) {
            case MESSAGE_CREATED:
                messageCreated(event);
                break;
            case MESSAGE_DELETED:
                messageDeleted(event);
                break;
            case MESSAGE_RECEIVED:
                messageReceived(event);
                break;
            case MESSAGE_READ:
                messageRead(event);
                break;
            case TYPING:
                messageTyping(event);
                break;
            default:
                Log.i(TAG, String.format("applyEvent: %s", event.Type) );
        }
    }

    private void messageCreated(ChatEvent event) {
        final ChatMessage message = event.Message;
        if (message == null) {
            return;
        }

        if (message.My) {
            final ChatMessage existing = getMyMessageByLocalId(message.LocalId);
            if (existing != null) {

                existing.Id = message.Id;
                existing.EventId = message.EventId;
                existing.Payload = message.Payload;
                existing.Text = message.Text;
                existing.FileId = message.FileId;
                existing.File = message.File;
                existing.Client = message.Client;
                existing.Sending = false;

                Log.i(TAG, String.format("Received a message confirmation, localId=%d",
                        message.LocalId));

                for (final MessagesListener listener : messageListeners) {
                    execute(new Runnable() {
                        @Override
                        public void run() {
                            listener.messageUpdated(existing);
                        }
                    });
                }
                return;
            }
        }

        assert messages != null;
        messages.add(message);
        Log.i(TAG, String.format("Received a new message, messageId=%d", message.Id));

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageReceived(message);
                }
            });
        }

        enqueueReceived(message);
    }

    private void messageDeleted(ChatEvent event) {
        final List<ChatMessage> messagesToDelete = event.Messages;
        if (messagesToDelete == null || messagesToDelete.isEmpty()) {
            return;
        }

        for (ChatMessage chatMessageToDelete : messagesToDelete) {
            ChatMessage oldMessage = null;

            assert messages != null;
            for (ChatMessage message : messages) {
                if (message.Id == chatMessageToDelete.Id) {
                    oldMessage = message;
                }
            }

            if (oldMessage != null) {
                messages.remove(oldMessage);
                Log.i(TAG, String.format("Deleted message, messageId=%d", oldMessage.Id));
            }

            for (final MessagesListener listener : messageListeners) {
                execute(new Runnable() {
                    @Override
                    public void run() {
                        listener.messageDeleted(chatMessageToDelete);
                    }
                });
            }
        }
    }

    private void messageReceived(ChatEvent event) {
        if (event.MessageId == null) {
            return;
        }

        final ChatMessage message = getMessageById(event.MessageId);
        if (message == null) {
            return;
        }
        if (message.EventId != null && message.EventId >= event.Id) {
            return;
        }

        message.EventId = event.Id;
        message.Received = true;
        message.ReceivedAt = event.CreatedAt;
        Log.i(TAG, String.format("Marked a message as received, messageId=%d", message.Id));

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageUpdated(message);
                }
            });
        }
    }

    private void messageRead(ChatEvent event) {
        if (event.MessageId == null) {
            return;
        }

        final ChatMessage message = getMessageById(event.MessageId);
        if (message == null) {
            return;
        }
        if (message.EventId != null && message.EventId >= event.Id) {
            return;
        }

        message.EventId = event.Id;
        message.Read = true;
        message.ReadAt = event.CreatedAt;
        if (!message.Received) {
            message.Received = true;
            message.ReceivedAt = event.CreatedAt;
        }
        Log.i(TAG, String.format("Marked a message as read, messageId=%d", message.Id));

        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.messageUpdated(message);
                }
            });
        }
    }

    private void messageTyping(ChatEvent event) {
        for (final MessagesListener listener : messageListeners) {
            execute(new Runnable() {
                @Override
                public void run() {
                    listener.eventTyping(event);
                }
            });
        }
    }
}
