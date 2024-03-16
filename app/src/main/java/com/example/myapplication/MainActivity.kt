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

class DataElement constructor(var peak: Short, var timeLong: Long) {
}

class MainActivity : ComponentActivity() {
    //    const
    private val AMPL_CHANGE_T_MS = 100
    private val AMPL_CHANGE_T_PEAKS = 2
    private val recordingTimeout: Long = 10000

    //
    private var audioRecord: AudioRecord? = null
    private var textView: TextView? = null
    private var result: String? = null

    //    private val testTimeout: Long = 1000
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
        recordThenExecute(bufferSize, recordingTimeout) {
            analyzeResults()
        }
        Log.v("App-Log", "Waiting for update")
        if (result != null) {
            textView?.text = result
        }
    }

    private fun recordThenExecute(bufferSize: Int, setTimeout: Long, cb: () -> Unit = {}) {
        audioRecord?.startRecording()
        recordedTrack = arrayListOf()
        startTime = System.currentTimeMillis()
        isRecording = true
        var totalTime: Long = 0
//        TODO: Later
//        Start a separate thread to read and analyze audio samples
//        Thread {
        val buffer = ShortArray(bufferSize)
        while (totalTime < setTimeout) {
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
        cb()
//        }.start()
    }

    private fun stopRecording() {
        Log.v("App-Log", "stopRecording")
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
    }

    private fun analyzeResults() {
        Log.v("App-Log", "analyzeResults")
        val secondHit = recordedTrack?.maxBy { it.peak }
        val secondHitIndex = recordedTrack?.indexOf(secondHit) ?: 0
        val splitIndex = if (AMPL_CHANGE_T_PEAKS >= secondHitIndex) (AMPL_CHANGE_T_PEAKS + 1)
        else (secondHitIndex - AMPL_CHANGE_T_PEAKS)
        Log.v("App-Log", "second peak ${secondHit?.peak}")
        val firstHit = recordedTrack?.slice(0..<splitIndex)?.maxBy { it.peak }
        Log.v("App-Log", "first peak ${firstHit?.peak}")
        val timeDiff = secondHit?.timeLong?.minus(firstHit?.timeLong!!) ?: 0
        Log.v("App-Log", timeDiff.toString())
        //TODO:
        result = "" + timeDiff.div(1000) + "s"
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
