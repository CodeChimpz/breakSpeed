package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.myapplication.R.id.button
import com.example.myapplication.R.id.textView
import com.example.myapplication.R.layout.activity_main

class DataElement constructor(peak: Short, timeLong: Long) {
    var peak: Short? = peak
    var timeLong: Long? = timeLong
}

class MainActivity : ComponentActivity() {
    private var audioRecord: AudioRecord? = null
    private var textView: TextView? = null
    private var result: String? = null
    private val recordTime: Long = 10000
    private var startTime: Long? = null
    private var isRecording = false
    private var recordedTrack: ArrayList<DataElement>? = arrayListOf()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(/* layoutResID = */ activity_main
        )
        val btnCenter = findViewById<Button>(button)
        textView = findViewById<TextView>(R.id.textView)
        btnCenter.setOnClickListener {
            onMainButtonPress()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecording() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.v("App-Log", "checkSelfPermission FAILED")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        recordedTrack = arrayListOf()
        startTime = System.currentTimeMillis()
        isRecording = true
        var totalTime: Long = 0
        // Start a separate thread to read and analyze audio samples
        Thread {
            val buffer = ShortArray(bufferSize)
            while (totalTime < recordTime) {
                val read = audioRecord?.read(buffer, 0, bufferSize)
                read?.let {
                    // Analyze audio samples and find volume peaks
                    val maxAmplitude: Short = buffer.maxOrNull() ?: 0
                    val timeNow = System.currentTimeMillis()
                    totalTime = timeNow - startTime!!
                    // Add to data with timestamp
                    val newElement = DataElement(maxAmplitude, timeNow)
                    recordedTrack?.add(newElement)
                    Log.v(
                        "App-Log",
                        "${newElement.peak} : ${newElement.timeLong} : ${recordedTrack?.size}"
                    )
                }
            }
            stopRecording()
        }.start()
        Log.v("App-Log", "Waiting for update")
        while (true) {
            if (result != null) {
                textView?.text = result
            }
        }
    }

    private fun stopRecording() {
        Log.v("App-Log", "stopRecording")
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        analyzeResults()
    }

    private fun analyzeResults() {
        Log.v("App-Log", "analyzeResults")
        val sortedTrack = recordedTrack?.sortedWith(compareBy { it.peak })?.reversed()
        val firstHit = sortedTrack?.get(1)
        Log.v("App-Log", "first peak ${firstHit?.peak}")
        val secondHit = sortedTrack?.get(0)
        Log.v("App-Log", "second peak ${secondHit?.peak}")
        val timeDiff = secondHit?.timeLong?.minus(firstHit?.timeLong!!)
        Log.v("App-Log", timeDiff.toString())
        //TODO:
        result = "" + timeDiff + ""
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onMainButtonPress() {
        Log.v("App-Log", "startRecording")
        startRecording()
    }


}
