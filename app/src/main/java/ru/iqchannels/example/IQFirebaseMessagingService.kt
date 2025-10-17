package ru.iqchannels.example

import ru.iqchannels.sdk.IQLog
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.iqchannels.sdk.app.IQChannels

class IQFirebaseMessagingService : FirebaseMessagingService() {

	companion object {
		private const val TAG = "iqchannels"
	}

	override fun onNewToken(token: String) {
		IQLog.d(TAG, "Refreshed token: $token")
		IQChannels.setPushToken(token)
	}

	override fun onMessageReceived(remoteMessage: RemoteMessage) {
		IQLog.d(TAG, "Message received: $remoteMessage")
		super.onMessageReceived(remoteMessage)
	}
}
