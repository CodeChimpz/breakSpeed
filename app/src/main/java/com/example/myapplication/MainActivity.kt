package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R.drawable.viz_simple
import com.example.myapplication.R.id.button
import com.example.myapplication.R.layout.activity_main
import com.example.myapplication.buttons.MainButton
import com.example.myapplication.constants.Constants
import com.example.myapplication.util.Ball
import com.example.myapplication.util.DataElement
import com.example.myapplication.util.Table
import java.math.RoundingMode
import kotlin.math.absoluteValue


class MainActivity : ComponentActivity() {
    private val ball: Ball = Ball()
    private val table: Table = Table()

    //
    private var result: String? = null
    private var startTime: Long? = null
    private var currentTime: Long? = null
    private var isRecording = false
    private var recordedTrack: ArrayList<DataElement>? = arrayListOf()

    //
    private var audioRecord: AudioRecord? = null
    private var textView: TextView? = null
    private var vizualSimple: View? = null
    private var vizualSimpleSize: Int? = null
    private lateinit var mainButton: MainButton

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(/* layoutResID = */ activity_main)
        val btnCenter = findViewById<Button>(button)
        mainButton = MainButton(btnCenter, this, this)
        textView = findViewById<TextView>(R.id.textView)
        vizualSimple = findViewById<View>(R.id.simpleViz)
        vizualSimpleSize = vizualSimple?.layoutParams?.width
        mainButton.setOnClickListener(cb = {
            Thread {
                Log.v("App-Log", "startRecording")
                startRecording()
            }.start()
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startRecording() {
        val sampleRate = Constants.AUDIO_SAMPLE_RATE
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            audioFormat
        ) / Constants.AUDIO_MIN_BUFFER_DIV
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("App-Log-Permission", "checkSelfPermission FAILED")
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
        //start recoding and register cb
        recordThenExecute(bufferSize, Constants.DEFAULT_RECORDING_TIMEOUT) {
            analyzeResults()
        }
        //render results
        if (result != null) {
            runOnUiThread {
                textView?.text = result
            }
        }
        mainButton.deactivateView()
    }


    private fun stopRecording() {
        Log.v("App-Log", "stopRecording")
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
    }

    private fun recordThenExecute(bufferSize: Int, setTimeout: Long, cb: () -> Unit = {}) {
        audioRecord?.startRecording()
        recordedTrack = arrayListOf()
        startTime = System.currentTimeMillis()
        isRecording = true
        runOnUiThread {
            vizualSimple?.background = ContextCompat.getDrawable(this, viz_simple)
        }
        var totalTime: Long = 0
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

                //UI UPDATE
                mainButton.updateButtonViewProgressDefault((totalTime.toDouble() / setTimeout.toDouble() * 100).toInt())
                runOnUiThread {
                    visualiseSimple(maxAmplitude.toDouble())
                }
            }
        }
        stopRecording()
        runOnUiThread {
            vizualSimple?.background = null
        }
        cb()
    }

    private fun visualiseSimple(peak: Double) {
        val MAX_WIDTH: Double = 10000.0
        val layoutParams = vizualSimple?.layoutParams
        layoutParams?.width = (peak / MAX_WIDTH * vizualSimpleSize!!).toInt()
        if (layoutParams?.width!! > 0) {
            vizualSimple?.layoutParams = layoutParams
        }
    }

    private data class PeaksData(val breakPointMs: Float, val firstPointMs: Float) {}

    private fun analyzeResults(): Long {
        Log.v("App-Log", "analyzeResults")
        val secondHit: DataElement? = recordedTrack?.maxBy { it.peak }
        val secondHitIndex = recordedTrack?.indexOf(secondHit) ?: 0
        val splitIndex = if (secondHitIndex > 1) {
            secondHitIndex - 1
        } else {
            0
        }
        Log.v("App-Log", "second peak ${secondHit?.peak}")
        var firstHitTemp: DataElement? = recordedTrack?.slice(0..<splitIndex)?.maxBy { it.peak }
        var timeDiffMs = Calculation.getTimeDiffMs(secondHit!!, firstHitTemp!!)
        Log.v("App-Log", "Inintial timeDiffMs ${firstHitTemp.peak} $timeDiffMs")
        val peakDiff = (secondHit.peak - firstHitTemp.peak).absoluteValue
        //IF too fast = we took parts of break peak ->
        //IF too slow = hit peak > break peak ->
        //-->Find the closest valid peak that isn't too fast
        if (timeDiffMs >= Constants.DEFAULT_MAX_BREAKSPEED_MS || timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS || peakDiff >= Constants.DEFAULT_AMPL_CHANGE_DB_THRESHOLD) {
            Log.v(
                "App-Log",
                "Reevaluating TOO_SLOW -> ${timeDiffMs >= Constants.DEFAULT_MAX_BREAKSPEED_MS} TOO_FAST -> ${timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS}" +
                        " PEAK_DIFF ${peakDiff >= Constants.DEFAULT_AMPL_CHANGE_DB_THRESHOLD}"

            )
//            var firstHitTempIndex: Int = recordedTrack?.indexOf(firstHitTemp) ?: 0
            var firstHitTempIndex: Int
            val sortedGraph = recordedTrack?.sortedBy { it.peak }?.reversed()
            var i = 1
            var peakLow = false
            while ((timeDiffMs >= Constants.DEFAULT_MAX_BREAKSPEED_MS || timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS || !peakLow) && i < recordedTrack?.size!!) {
                firstHitTemp = sortedGraph?.get(i)
                firstHitTempIndex = recordedTrack?.indexOf(firstHitTemp) ?: 0
                peakLow =
                    if (timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS) false else Calculation.checkLowPeak(
                        firstHitTempIndex,
                        secondHitIndex,
                        recordedTrack!!,
                        Constants.DEFAULT_AMPL_LOW_PEAK_THRESHOLD
                    )
                timeDiffMs = Calculation.getTimeDiffMs(secondHit, firstHitTemp!!)
//                Log.v("App-Log", "Index relative $i ${firstHitTemp.peak} $timeDiffMs $peakLow")
                i++
            }
        }
        val firstHit = firstHitTemp
        val timeDiffS = (timeDiffMs.toDouble() / 1000).absoluteValue
        Log.v("App-Log", "first peak ${firstHit?.peak}")
        Log.v("App-Log", "second peak ${secondHit.peak}")
        return timeDiffMs

    }

    private fun getResults(timeDiffS: Long) {
        if (timeDiffS <= 0) {
            return
        }
        val S = Calculation.getDistance(ball, table)
        val velocity = (S / timeDiffS) / 100 * 2.24
        val velocityRounded = velocity.toBigDecimal().setScale(1, RoundingMode.UP)
        //TODO:
        result = "" + velocityRounded + "mph"
        Log.v("App-Log", "velocityRounded $velocityRounded $velocity ")
    }


}
