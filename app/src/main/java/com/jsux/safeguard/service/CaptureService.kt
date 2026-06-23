package com.jsux.safeguard.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import java.io.File
import java.util.concurrent.Executors

class CaptureService : LifecycleService() {

    private var imageCapture: ImageCapture? = null
    private val targetDir = "/sdcard/SAFEGUARD"

    override fun onCreate() {
        super.onCreate()
        
        // Ensure directory exists via root
        runRootCommand("mkdir -p $targetDir")

        val channelId = "safeguard_service"
        val channel = NotificationChannel(channelId, "Safeguard Active", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        
        startForeground(1, NotificationCompat.Builder(this, channelId)
            .setContentTitle("Safeguard")
            .setContentText("Monitoring...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startCamera()
        return START_NOT_STICKY
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)
                takePhoto()
            } catch (e: Exception) { stopSelf() }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val tempFile = File(cacheDir, "temp_img_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

        imageCapture?.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val finalPath = "$targetDir/SG_${System.currentTimeMillis()}.jpg"
                // Attempt to move via Root
                runRootCommand("mv ${tempFile.absolutePath} $finalPath")
                stopSelf()
            }
            override fun onError(exc: ImageCaptureException) { stopSelf() }
        })
    }

    private fun runRootCommand(command: String) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = process.outputStream
            os.write((command + "\n").toByteArray())
            os.write("exit\n".toByteArray())
            os.flush()
            process.waitFor()
        } catch (e: Exception) { e.printStackTrace() }
    }
}

