package ru.iqchannels.example;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import ru.iqchannels.sdk.app.IQChannels;

public class AppFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "iqchannels";

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + token);
        IQChannels.instance().setPushToken(token);
    }
}
