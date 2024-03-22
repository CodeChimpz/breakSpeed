package com.example.myapplication.buttons

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R

class MainButton constructor(
    private val buttonSaved: Button,
    private val context: Context,
    private val activity: MainActivity
) {

    var activeRecording: Boolean = false

    //    Activates button and lets cb execute
    fun setOnClickListener(cb: () -> Unit) {
        buttonSaved.setOnClickListener {
            clickStateCheck({
                activateView()
                cb()
//                while (activeRecording) {
//                    val progressInt = progress()
//                    updateButtonViewProgressDefault(progressInt)
//                }
            }, {
            })
        }
    }

    //    onClick will run if is not activeRecording, onStop if is activeRecording
    private fun clickStateCheck(onClick: () -> Unit, onStop: () -> Unit) {
        if (activeRecording) {
//            activeRecording = false
//            onStop()
            return
        } else {
            activeRecording = true
            onClick()
        }
    }

    fun deactivateView() {
        Log.v("App-Log", "Change state PENDING")
        activeRecording = false
        activity.runOnUiThread {
            buttonSaved.background = ContextCompat.getDrawable(context, R.drawable.btn_circular)
            buttonSaved.text = activity.getString(R.string.button_text_idle)
        }
    }

    private fun activateView() {
        Log.v("App-Log", "Change state ACTIVATED")
        activity.runOnUiThread {
            buttonSaved.background =
                ContextCompat.getDrawable(context, R.drawable.btn_img_circular_pending)
            buttonSaved.text = activity.getString(R.string.button_text_pending)
        }
    }

    fun updateButtonViewProgressDefault(percentage: Int) {
//        Log.v("App-Log", "$percentage")
        val progressBar: ProgressBar = activity.findViewById(R.id.progress_bar)
        if (percentage !in 0..< 100) {
            progressBar.progress = 0
            return
        }
        if (!activeRecording) {
            return
        }
        activity.runOnUiThread {
            progressBar.progress = percentage
        }
    }
}