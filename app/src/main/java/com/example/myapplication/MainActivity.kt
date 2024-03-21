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
import androidx.core.content.ContextCompat
import com.example.myapplication.R.id.button
import com.example.myapplication.R.layout.activity_main
import java.math.RoundingMode
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class DataElement constructor(var peak: Short, var timeLong: Long) {
}

class MainActivity : ComponentActivity() {
    //    const
    private val DEFAULT_DISTANCE_CM = 143
    private val AMPL_LOW_PEAK_THRESHOLD = 500
    private val AMPL_CHANGE_DB_THRESHOLD = 500
    private val MAX_BREAKSPEED_MS = 800
    private val MIN_BREAKSPEED_MS = 80
    private val recordingTimeout: Long = 10000

    //
    private var audioRecord: AudioRecord? = null
    private var textView: TextView? = null
    private lateinit var buttonSaved: Button
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
        buttonSaved = btnCenter
        textView = findViewById<TextView>(R.id.textView)
        btnCenter.setOnClickListener {
            buttonSaved.isClickable = false;
            Log.v("App-Log", "isClickable FALSE")
            Thread {
                onMainButtonPress()
            }.start()
            buttonSaved.isClickable = true;
            Log.v("App-Log", "isClickable TRUE")
        }
    }

    fun runOnUiThreadCb(cb: () -> Unit) {
        runOnUiThread {
            cb()
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
        ) / 4

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
        runOnUiThreadCb {
            buttonSaved.background =
                ContextCompat.getDrawable(this, R.drawable.btn_img_circular_pending)
        }
        Log.v("App-Log", "Change state")
        recordThenExecute(bufferSize, recordingTimeout) {
            analyzeResults()
        }
        runOnUiThreadCb {
            buttonSaved.background = ContextCompat.getDrawable(this, R.drawable.btn_circular)
        }
        Log.v("App-Log", "Waiting for update")
        if (result != null) {
            runOnUiThreadCb {
                textView?.text = result
            }
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
                    "App-Log-GRAPH",
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

        var secondHit: DataElement? = recordedTrack?.maxBy { it.peak }
        val secondHitIndex = recordedTrack?.indexOf(secondHit) ?: 0

//        val splitIndex = if (1 >= secondHitIndex) (AMPL_CHANGE_T_PEAKS + 1)
//        else (secondHitIndex - AMPL_CHANGE_T_PEAKS)
        val splitIndex = if (secondHitIndex > 1) {
            secondHitIndex - 1
        } else {
            0
        }
        Log.v("App-Log", "second peak ${secondHit?.peak}")
        var firstHitTemp: DataElement? = recordedTrack?.slice(0..<splitIndex)?.maxBy { it.peak }

        var timeDiffMs = getTimeDiffMs(secondHit!!, firstHitTemp!!)
        Log.v("App-Log", "Inintial timeDiffMs ${firstHitTemp.peak} $timeDiffMs")
        val peakDiff = (secondHit.peak - firstHitTemp.peak).absoluteValue
        //IF too fast = we took parts of break peak ->
        //IF too slow = hit peak > break peak ->
        //-->Find the closest valid peak that isn't too fast
        if (timeDiffMs >= MAX_BREAKSPEED_MS || timeDiffMs <= MIN_BREAKSPEED_MS || peakDiff >= AMPL_CHANGE_DB_THRESHOLD) {
            Log.v(
                "App-Log",
                "Reevaluating TOO_SLOW -> ${timeDiffMs >= MAX_BREAKSPEED_MS} TOO_FAST -> ${timeDiffMs <= MIN_BREAKSPEED_MS}" +
                        " PEAK_DIFF ${peakDiff >= AMPL_CHANGE_DB_THRESHOLD}"

            )
            var firstHitTempIndex: Int = recordedTrack?.indexOf(firstHitTemp) ?: 0
            val sortedGraph = recordedTrack?.sortedBy { it.peak }?.reversed()
            var i = 1
            var peakLow: Boolean = false
            while ((timeDiffMs >= MAX_BREAKSPEED_MS || timeDiffMs <= MIN_BREAKSPEED_MS || !peakLow ) && i < recordedTrack?.size!!) {
                firstHitTemp = sortedGraph?.get(i)
                firstHitTempIndex = recordedTrack?.indexOf(firstHitTemp) ?: 0
                peakLow = if (timeDiffMs <= MIN_BREAKSPEED_MS) false else checkLowPeak(
                    firstHitTempIndex,
                    secondHitIndex
                )
                timeDiffMs = getTimeDiffMs(secondHit, firstHitTemp!!)
                Log.v("App-Log", "Index relative $i ${firstHitTemp.peak} $timeDiffMs $peakLow")
                i++
            }
        }
        val firstHit = firstHitTemp
        Log.v("App-Log", "first peak ${firstHit?.peak}")
        Log.v("App-Log", "second peak ${secondHit.peak}")
        val timeDiffS = (timeDiffMs.toDouble() / 1000).absoluteValue
        Log.v("App-Log", "timeDiffMs $timeDiffMs $timeDiffS")
        if (timeDiffS <= 0) {
            return
        }
        val velocity = (DEFAULT_DISTANCE_CM / timeDiffS) / 100 * 2.24
        val velocityRounded = velocity.toBigDecimal().setScale(2, RoundingMode.UP)
        //TODO:
        result = "" + velocityRounded + "mph"
        Log.v("App-Log", "velocityRounded $velocityRounded $velocity ")
    }

    fun getTimeDiffMs(secondHit: DataElement, firstHit: DataElement): Long {
        return secondHit.timeLong.minus(firstHit.timeLong).absoluteValue ?: 0
    }

    fun checkLowPeak(first: Int, second: Int): Boolean {
        val max = max(first, second)
        val min = min(first, second)
        val minPeak = recordedTrack?.slice(min..max)?.minBy { it.peak }
        val firstPeak = recordedTrack?.get(first)
        return firstPeak?.peak!! - minPeak?.peak!! > AMPL_LOW_PEAK_THRESHOLD
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
