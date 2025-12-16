package com.example.utaipeieat

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {
    val tag: String = "FCMToken"
    override fun onNewToken(token: String) {
        Log.d(tag, "Device token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(tag, "From: ${message.from}")
        // Check if message contains a data payload.
        if (message.data.isNotEmpty()) {
            Log.d(tag, "Message data payload: ${message.data}")
        }
        // Check if message contains a notification payload.
        message.notification?.let {
            Log.d(tag, "Message Notification Body: ${it.body}")
        }
    }
}