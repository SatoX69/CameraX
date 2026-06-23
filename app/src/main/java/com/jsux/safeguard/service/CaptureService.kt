package com.jsux.safeguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureService : LifecycleService() {

    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    override fun onCreate() {
        super.onCreate()
        cameraExecutor = Executors.newSingleThreadExecutor()

        val channelId = "safeguard_service"
        val channel = NotificationChannel(channelId, "Safeguard Active", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safeguard")
            .setContentText("Acquiring image...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        val uriString = intent?.getStringExtra("SAVE_URI")
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            startCamera(uri)
        } else {
            stopSelf()
        }
        
        return START_NOT_STICKY
    }

    private fun startCamera(saveUri: Uri) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
                takePhoto(saveUri)
            } catch (e: Exception) {
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(saveUri: Uri) {
        val capture = imageCapture ?: return

        val dir = DocumentFile.fromTreeUri(this, saveUri)
        val file = dir?.createFile("image/jpeg", "SG_${System.currentTimeMillis()}.jpg")
        
        if (file == null) {
            stopSelf()
            return
        }

        val outputStream = contentResolver.openOutputStream(file.uri)
        if (outputStream == null) {
            stopSelf()
            return
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    stopSelf()
                }
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    stopSelf()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

