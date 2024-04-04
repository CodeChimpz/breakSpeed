package com.example.myapplication.ui

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.myapplication.MainActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

class LineChartService constructor(
    val chart: LineChart,
    private val activity: MainActivity,
    val updateDelayMs: Long = 10,
) {
    private var entries: CopyOnWriteArrayList<Entry> = CopyOnWriteArrayList()
    private lateinit var handlerGraph: Handler
    private lateinit var runnableGraph: Runnable // reference to the runnable object

    init {
        // Set formatting
        chart.description.isEnabled = false;
        chart.setDrawGridBackground(false);
        chart.xAxis.setDrawGridLines(false);
        chart.axisLeft.setDrawGridLines(false);
        chart.axisRight.setDrawGridLines(false);
        chart.axisRight.setDrawLimitLinesBehindData(false);
        chart.axisLeft.setDrawLabels(false);
        chart.axisRight.setDrawLabels(false);
        chart.xAxis.setDrawLabels(false);
        chart.xAxis.setDrawLimitLinesBehindData(false);
        chart.legend.isEnabled = false;
    }

    fun startRunnable() {
        handlerGraph = Handler(Looper.getMainLooper())
        Log.v("App-Log-GRAPHING", "GRAPHING init")
        runnableGraph = Runnable {
            if (entries.size > 0) {
                activity.runOnUiThread {
                    updateGraph()
                }
            }
            handlerGraph.postDelayed(runnableGraph, updateDelayMs)
        }
        handlerGraph.post(runnableGraph)
    }

    fun stopRunnable() {
        handlerGraph.removeCallbacks(runnableGraph)
    }

    fun addEntryAndUpdate(entry: Entry) {
        entries.add(entry)
//        val dataSet = LineDataSet(entries, "Plot")
//        updateGraph(dataSet)
    }

    fun updateGraph(dataSet: LineDataSet = LineDataSet(entries, "Plot")) {
        dataSet.setDrawValues(false)
        dataSet.setDrawCircles(false)
        val lineData = LineData(dataSet)
        //Display updated DataSet
        chart.setData(lineData)
        chart.invalidate();
    }

    fun clearGraph() {
        entries.clear()
        updateGraph()
    }

}