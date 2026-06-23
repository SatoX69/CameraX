package com.jsux.safeguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jsux.safeguard.service.CaptureService

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Camera permission is required for the application to function.", Toast.LENGTH_LONG).show()
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
            
            // Persist the URI for the AdminReceiver to use later
            getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("SAVED_URI_KEY", uri.toString())
                .apply()
                
            findViewById<TextView>(R.id.statusText).text = "Storage Linked Successfully"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        findViewById<Button>(R.id.btnSelectDir).setOnClickListener {
            directoryPickerLauncher.launch(null)
        }

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val prefs = getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
            val uriString = prefs.getString("SAVED_URI_KEY", null)
            
            if (uriString != null) {
                val intent = Intent(this, CaptureService::class.java).apply {
                    putExtra("SAVE_URI", uriString)
                }
                ContextCompat.startForegroundService(this, intent)
                Toast.makeText(this, "Capture sequence initiated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select a directory first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

