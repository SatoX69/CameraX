package com.jsux.safeguard.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jsux.safeguard.service.CaptureService

class SafeguardAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("failed_attempts", 0) + 1
        prefs.edit().putInt("failed_attempts", attempts).apply()

        // Act every 2 failed attempts
        if (attempts % 2 == 0) {
            val savedUri = prefs.getString("SAVED_URI_KEY", null)
            val serviceIntent = Intent(context, CaptureService::class.java)
            
            // Pass the URI so the service knows where to save
            if (savedUri != null) {
                serviceIntent.putExtra("SAVE_URI", savedUri)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
            .edit().putInt("failed_attempts", 0).apply()
    }
}

