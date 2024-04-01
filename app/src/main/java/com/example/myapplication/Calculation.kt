package com.example.myapplication

import android.media.AudioRecord
import com.example.myapplication.util.Ball
import com.example.myapplication.util.DataElement
import com.example.myapplication.util.Table
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

class Calculation {
    companion object {
        fun getTimeDiffMs(secondHit: DataElement, firstHit: DataElement): Long {
            return secondHit.timeLong.minus(firstHit.timeLong).absoluteValue
        }

        fun checkLowPeak(
            first: Int,
            second: Int,
            recordedTrack: ArrayList<DataElement>,
            AMPL_LOW_PEAK_THRESHOLD: Int
        ): Boolean {
            val max = max(first, second)
            val min = min(first, second)
            val minPeak = recordedTrack.slice(min..max).minBy { it.peak }
            val firstPeak = recordedTrack[first]
            return firstPeak.peak - minPeak.peak > AMPL_LOW_PEAK_THRESHOLD
        }

        fun getDistance(
            ball: Ball, table: Table
        ): Double {
            return table.distanceCm - ball.radius
        }
    }
}