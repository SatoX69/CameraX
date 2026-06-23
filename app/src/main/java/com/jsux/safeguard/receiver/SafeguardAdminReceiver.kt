package com.jsux.safeguard

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class SafeguardAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("failed_attempts", 0) + 1
        prefs.edit().putInt("failed_attempts", attempts).apply()

        // Act every 2 failed attempts
        if (attempts % 2 == 0) {
            val serviceIntent = Intent(context, CaptureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        // Reset counter on successful unlock
        context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
            .edit().putInt("failed_attempts", 0).apply()
    }
}

