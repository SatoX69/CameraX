package com.jsux.safeguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CaptureService : LifecycleService() {

    private var previousBrightness = "100"
    private var previousMode = "1" // 1 = auto, 0 = manual

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        executeSafeguardProtocol()
    }

    private fun executeSafeguardProtocol() {
        // 1. Save current brightness settings and max out via Root
        saveAndMaxBrightness()

        // 2. Set up a 1x1 pixel invisible overlay for CameraX to bind to
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            1, 1,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val previewView = PreviewView(this)
        windowManager.addView(previewView, params)

        // 3. Initialize CameraX and take the picture
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)

                val outputDirectory = getExternalFilesDir(null)
                val photoFile = File(outputDirectory, "Intruder_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            cleanup(windowManager, previewView)
                        }
                        override fun onError(exc: ImageCaptureException) {
                            cleanup(windowManager, previewView)
                        }
                    }
                )
            } catch (e: Exception) {
                cleanup(windowManager, previewView)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun saveAndMaxBrightness() {
        try {
            // Read current states
            val getBrightness = Runtime.getRuntime().exec("su -c settings get system screen_brightness")
            previousBrightness = getBrightness.inputStream.bufferedReader().readLine() ?: "100"
            
            val getMode = Runtime.getRuntime().exec("su -c settings get system screen_brightness_mode")
            previousMode = getMode.inputStream.bufferedReader().readLine() ?: "1"

            // Disable auto-brightness and set to max (255)
            Runtime.getRuntime().exec("su -c settings put system screen_brightness_mode 0").waitFor()
            Runtime.getRuntime().exec("su -c settings put system screen_brightness 255").waitFor()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun restoreBrightness() {
        try {
            Runtime.getRuntime().exec("su -c settings put system screen_brightness $previousBrightness").waitFor()
            Runtime.getRuntime().exec("su -c settings put system screen_brightness_mode $previousMode").waitFor()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun cleanup(windowManager: WindowManager, view: PreviewView) {
        restoreBrightness()
        windowManager.removeView(view)
        stopSelf()
    }

    private fun createNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("safeguard_channel", "Safeguard Service", NotificationManager.IMPORTANCE_MIN)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, "safeguard_channel")
            .setContentTitle("Safeguard active")
            .setSmallIcon(android.R.drawable.ic_secure)
            .build()
    }
}

