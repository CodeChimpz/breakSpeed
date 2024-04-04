package com.example.myapplication

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.example.myapplication.R.id.button
import com.example.myapplication.R.layout.activity_main
import com.example.myapplication.ui.elements.buttons.MainButton
import com.example.myapplication.constants.Constants
import com.example.myapplication.constants.Constants.DEFAULT_MAX_BREAKSPEED_MS
import com.example.myapplication.entities.Ball
import com.example.myapplication.entities.DataElement
import com.example.myapplication.entities.Table
import com.example.myapplication.services.AudioRecorderService
import com.example.myapplication.services.Calculation
import com.example.myapplication.ui.LineChartService
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import java.math.RoundingMode
import kotlin.math.absoluteValue


class MainActivity : ComponentActivity() {
    //Game entities
    private val ball: Ball = Ball()
    private val table: Table = Table()

    //Util
    private lateinit var recorder: AudioRecorderService
    private val setTimeout = Constants.DEFAULT_RECORDING_TIMEOUT

    //UI
    private var result: String? = null
    private lateinit var textView: TextView
    private lateinit var mainButton: MainButton
    private lateinit var chart: LineChartService


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("App-Log", "_______________________Init")
        setContentView(/* layoutResID = */ activity_main)
        textView = findViewById<TextView>(R.id.textView)
        mainButton = MainButton(findViewById<Button>(button), this, this)
        chart = LineChartService(findViewById<LineChart>(R.id.chart), this)
        recorder = AudioRecorderService(this, this)
        //register button cb
        mainButton.setOnClickListener(cb = {
            //clear chart
            chart.clearGraph()
            //start chart update thread
            chart.startRunnable()
            //todo: runnable
            Thread {
                recorder.startRecording(
                    setTimeout,
                    ::updateUiOnRecord,
                    ::analyzeResults
                )
            }.start()
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.stopRecording()
    }

    fun updateUiOnRecord(
        timeData: AudioRecorderService.TimeData,
        newElement: AudioRecorderService.ElementData
    ) {
        runOnUiThread {          //UI UPDATE
            mainButton.updateButtonViewProgressDefault((timeData.totalTime.toDouble() / setTimeout.toDouble() * 100).toInt())
            //add to GRAPH entry list
            chart.addEntryAndUpdate(
                Entry(
                    timeData.totalTime,
                    newElement.peak
                )
            )
        }
    }

    fun analyzeResults(recordedTrack: ArrayList<DataElement>): Float {
        Log.v("App-Log", "Analyze Results")
        val secondHit: DataElement? = recordedTrack.maxBy { it.peak }
        val secondHitIndex = recordedTrack.indexOf(secondHit) ?: 0
        val splitIndex = if (secondHitIndex > 1) {
            secondHitIndex - 1
        } else {
            0
        }
        var firstHitTemp: DataElement? = recordedTrack.slice(0..<splitIndex).maxBy { it.peak }
        var timeDiffMs = Calculation.getTimeDiffMs(secondHit!!, firstHitTemp!!)
        val peakDiff = (secondHit.peak - firstHitTemp.peak).absoluteValue
        //IF too fast = we took parts of break peak ->
        //IF too slow = hit peak > break peak ->
        //-->Find the closest valid peak that isn't too fast
        if (timeDiffMs >= DEFAULT_MAX_BREAKSPEED_MS || timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS || peakDiff >= Constants.DEFAULT_AMPL_CHANGE_DB_THRESHOLD) {
            var firstHitTempIndex: Int
            val sortedGraph = recordedTrack.sortedBy { it.peak }.reversed()
            var i = 1
            var peakLow = false
            do {
                firstHitTemp = sortedGraph?.get(i)
                firstHitTempIndex = recordedTrack.indexOf(firstHitTemp) ?: 0
                peakLow =
                    if (timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS) false else Calculation.checkLowPeak(
                        firstHitTempIndex,
                        secondHitIndex,
                        recordedTrack,
                        Constants.DEFAULT_AMPL_LOW_PEAK_THRESHOLD
                    )
                timeDiffMs = Calculation.getTimeDiffMs(secondHit, firstHitTemp!!)
                i++
            } while ((timeDiffMs >= DEFAULT_MAX_BREAKSPEED_MS || timeDiffMs <= Constants.DEFAULT_MIN_BREAKSPEED_MS || !peakLow) && i < recordedTrack?.size!!)
        }
        val firstHit = firstHitTemp
        val timeDiffS = (timeDiffMs.toDouble() / 1000).absoluteValue.toFloat()
        getResults(timeDiffS)
        return timeDiffS
    }

    private fun getResults(timeDiffS: Float) {
        if (timeDiffS <= 0) {
            return
        }
        val S = Calculation.getDistance(ball, table)
        val velocity = (S / timeDiffS) / 100 * 2.24
        val velocityRounded = velocity.toBigDecimal().setScale(1, RoundingMode.UP)
        //TODO:
        result = "" + velocityRounded + "mph"
        Log.v("App-Log", "velocityRounded $velocityRounded $velocity ")
        //\
        runOnUiThread {
            textView.text = result
        }
    }


}
