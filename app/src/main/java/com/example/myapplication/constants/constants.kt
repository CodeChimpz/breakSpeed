package com.example.myapplication.constants

object Constants {
    //AUDIO_RECORDER
    const val AUDIO_SAMPLE_RATE = 44100
    //divisor of min buff size provided by AudioRecorder instance
    const val AUDIO_MIN_BUFFER_DIV = 4
    //Default Calculations related values
    const val DEFAULT_DISTANCE_CM = 143.0
    const val DEFAULT_WIDTH_CM = 143.0
    const val DEFAULT_LONG_CM = 143.0
    const val DEFAULT_BALL_RADIUS_CM = 5.7

    const val DEFAULT_AMPL_LOW_PEAK_THRESHOLD = 500
    const val DEFAULT_AMPL_CHANGE_DB_THRESHOLD = 500
    const val DEFAULT_MAX_BREAKSPEED_MS = 800
    const val DEFAULT_MIN_BREAKSPEED_MS = 80

    //Depreciated
    const val DEFAULT_RECORDING_TIMEOUT: Long = 10000
}