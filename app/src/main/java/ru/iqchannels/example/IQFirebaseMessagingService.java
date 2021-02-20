package ru.iqchannels.example;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ru.iqchannels.sdk.app.IQChannels;

public class IQFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "iqchannels";

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        IQChannels.instance().setPushToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "Message received: " + remoteMessage);
        super.onMessageReceived(remoteMessage);
    }
}
