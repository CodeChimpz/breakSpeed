package com.example.myapplication.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.constants.Constants
import com.example.myapplication.entities.DataElement
import com.github.mikephil.charting.data.Entry

class AudioRecorderService constructor(val context: Context, val activity: MainActivity) {
    private lateinit var audioRecord: AudioRecord

    //RecorderData
    private var bufferSize: Int = 0

    //Track data
    private var startTime: Long? = null
    private var currentTime: Long? = null
    private var isRecording = false

    //recorder data
    private var recordedTrack: ArrayList<DataElement> = arrayListOf()

    init {
        val sampleRate = Constants.AUDIO_SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        bufferSize = AudioRecord.getMinBufferSize(
            sampleRate, channelConfig, audioFormat
        ) / Constants.AUDIO_MIN_BUFFER_DIV
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("App-Log-Permission", "checkSelfPermission FAILED")
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1
            )
        } else {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startRecording(
        setTimeout: Long,
        cbPeak: (timeData: TimeData, elementData: ElementData) -> Unit,
        cbAfter: (recordedTrack: ArrayList<DataElement>) -> Unit
    ) {
        Log.v("App-Log", "Start Recording")
        //start recoding and register cb
        if (bufferSize <= 0) {
            Log.v("App-Log", "$bufferSize Buffer Size too small")
            return
        }
        recordedTrack = arrayListOf()
        isRecording = true
        audioRecord.startRecording()
        startTime = System.currentTimeMillis()
        var totalTime: Long = 0
        val buffer = ShortArray(bufferSize)
        Log.v("App-Log", "$totalTime $setTimeout : Start Looping Read Data")
        while (totalTime < setTimeout) {
            Log.v("App-Log", ".")
            totalTime = recordLoop(buffer, cbPeak)
        }
        stopRecording()
        cbAfter(recordedTrack)
    }

    private fun recordLoop(
        buffer: ShortArray,
        cbPeak: (timeData: TimeData, elementData: ElementData) -> Unit
    ): Long {
        val read = audioRecord.read(buffer, 0, bufferSize)
        read.let {
            // Analyze audio samples and find volume peaks
            val maxAmplitude: Short = buffer.maxOrNull() ?: 0
            val timeNow = System.currentTimeMillis()
            val totalTime = timeNow - startTime!!
            // Add to data with timestamp
            val newElement = DataElement(maxAmplitude, totalTime)
            recordedTrack.add(newElement)
            cbPeak(
                TimeData(totalTime.toFloat()),
                ElementData(maxAmplitude.toFloat())
            )
            return totalTime
        }
    }

    data class TimeData(
        val totalTime: Float
    ) {}

    data class ElementData(
        val peak: Float
    ) {}

    fun stopRecording() {
        Log.v("App-Log", "Stopped Recording")
        isRecording = false
        audioRecord.stop()
        audioRecord.release()
    }
}