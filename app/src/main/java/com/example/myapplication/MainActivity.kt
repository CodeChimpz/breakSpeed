package com.example.myapplication

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.myapplication.R.id.button
import com.example.myapplication.R.layout.activity_main

class MainActivity : ComponentActivity() {
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var hasAudioPermission: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            /* layoutResID = */ activity_main
        )
        val btnCenter = findViewById<Button>(button)
        btnCenter.setOnClickListener {
            onMainButtonPress()
        }
    }

    //    Check if permissions were granted and save the result
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            hasAudioPermission =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!hasAudioPermission) {
                Toast.makeText(
                    this,
                    "Permission to record audio denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun onMainButtonPress() {

    }
}
