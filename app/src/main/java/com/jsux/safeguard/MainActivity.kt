package com.jsux.safeguard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jsux.safeguard.service.CaptureService

class MainActivity : AppCompatActivity() {

    private var selectedDirectoryUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedDirectoryUri = uri
            findViewById<TextView>(R.id.statusText).text = "Storage Linked"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        findViewById<Button>(R.id.btnSelectDir).setOnClickListener {
            directoryPickerLauncher.launch(null)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val uri = selectedDirectoryUri
            if (uri != null) {
                val intent = Intent(this, CaptureService::class.java).apply {
                    putExtra("SAVE_URI", uri.toString())
                }
                ContextCompat.startForegroundService(this, intent)
                Toast.makeText(this, "Capture sequence initiated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select a directory first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

