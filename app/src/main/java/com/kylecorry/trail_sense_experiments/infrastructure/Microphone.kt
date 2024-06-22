package com.kylecorry.trail_sense_experiments.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MicrophoneInfo
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import android.util.Range
import com.kylecorry.andromeda.permissions.Permissions

class Microphone(
    private val context: Context,
    private val sampleRate: Int,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val bufferSize: Int = AudioRecord.getMinBufferSize(
        sampleRate,
        channelConfig,
        audioFormat
    )

) {
    private var recorder: AudioRecord? = null

    fun read(byteArray: ByteArray, offset: Int, size: Int): Int {
        return recorder?.read(byteArray, offset, size) ?: 0
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!Permissions.canRecordAudio(context)) {
            return
        }

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
        recorder?.startRecording()

        recorder?.audioSessionId?.let {
            if (AcousticEchoCanceler.isAvailable()) {
                val echo = AcousticEchoCanceler.create(it)
                echo.enabled = true
            }
            if (NoiseSuppressor.isAvailable()) {
                val noise = NoiseSuppressor.create(it)
                noise.enabled = true
            }
            if (AutomaticGainControl.isAvailable()) {
                val gain = AutomaticGainControl.create(it)
                gain.enabled = true
            }
        }

    }

    fun getDecibelRange(): Range<Float>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }
        // TODO: Handle multiple microphones
        val activeDevice = recorder?.activeMicrophones?.firstOrNull() ?: return null
        val min = activeDevice.minSpl
        val max = activeDevice.maxSpl

        if (min == MicrophoneInfo.SPL_UNKNOWN || max == MicrophoneInfo.SPL_UNKNOWN) {
            return null
        }

        return Range(min, max)
    }

    fun getSensitivity(): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null
        }
        // TODO: Handle multiple microphones
        val activeDevice = recorder?.activeMicrophones?.firstOrNull() ?: return null
        val sensitivity = activeDevice.sensitivity
        if (sensitivity == MicrophoneInfo.SENSITIVITY_UNKNOWN) {
            return null
        }
        return sensitivity
    }


    fun stop() {
        recorder?.stop()
        recorder = null
    }
}