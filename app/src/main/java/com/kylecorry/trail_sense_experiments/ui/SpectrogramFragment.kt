package com.kylecorry.trail_sense_experiments.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.views.chart.data.LineChartLayer
import com.kylecorry.luna.timer.CoroutineTimer
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.analysis.FrequencyAnalysis
import com.kylecorry.trail_sense_experiments.R
import com.kylecorry.trail_sense_experiments.databinding.FragmentSpectrogramBinding
import com.kylecorry.trail_sense_experiments.infrastructure.Microphone
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.log10

@AndroidEntryPoint
class SpectrogramFragment : BoundFragment<FragmentSpectrogramBinding>() {

    private val sampleRate = 44100
    private val frequencyCutOff = 12000f
    private val fftSize = 4096
    private var historyLength = 50

    // State
    private var history by state(emptyList<Vector2>())
    private var spectrogram: List<List<Float>> by state(emptyList())
    private var mostResonant by state<Float?>(null)
    private var mostResonantDb by state<Float?>(null)

    private val line by lazy {
        LineChartLayer(emptyList(), Resources.androidTextColorPrimary(requireContext()))
    }

    private val audioBuffer = ByteArray(fftSize)
    private val microphone by lazy { Microphone(requireContext(), sampleRate) }

    private val updateTimer = CoroutineTimer {
        // TODO: Read as fast as possible and consume in a separate coroutine (skipping if busy or spinning up another coroutine?)
        val bytes = microphone.read(audioBuffer, 0, audioBuffer.size)
        if (bytes == audioBuffer.size) {
            val readings = pcm16BufferToFloat(audioBuffer).toList()
            updateFFT(readings)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chart.plot(line)
    }

    override fun onResume() {
        super.onResume()
        microphone.start()
        updateTimer.interval(1)
    }

    override fun onPause() {
        super.onPause()
        microphone.stop()
        updateTimer.stop()
    }

    override fun onUpdate() {
        super.onUpdate()

        binding.chart.configureYAxis(minimum = -125f, maximum = 0f)
        line.data = history

        binding.spectogram.fftSize = audioBuffer.size
        binding.spectogram.spectogram = spectrogram

        binding.toolbar.title.text = memo("mostResonant", mostResonant) {
            val f = mostResonant ?: return@memo ""
            getString(R.string.hertz_amount, DecimalFormatter.format(f, 0))
        }

        binding.toolbar.subtitle.text = memo("mostResonantDb", mostResonantDb) {
            val mag = mostResonantDb ?: return@memo ""
            getString(R.string.decibels_amount, DecimalFormatter.format(mag, 0))
        }
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSpectrogramBinding {
        return FragmentSpectrogramBinding.inflate(layoutInflater, container, false)
    }

    private fun fftDecibels(fftValue: Float, fftSize: Int): Float {
        val offset = -20f * log10(fftSize.toFloat())
        return 20 * log10(fftValue.coerceAtLeast(1e-10f)) + offset
    }

    /**
     * Converts a PCM 16-bit buffer to a float array
     *
     * @param buffer the PCM 16-bit buffer. Must have an even number of bytes.
     * @param out the output array, must be exactly half the size of the input buffer
     * @return the float array. Range [-1, 1]
     */
    private fun pcm16BufferToFloat(
        buffer: ByteArray,
        out: FloatArray = FloatArray(buffer.size / 2)
    ): FloatArray {
        if (out.size != buffer.size / 2) {
            throw IllegalArgumentException("Output array must be half the size of the input array")
        }

        if (buffer.size % 2 != 0) {
            throw IllegalArgumentException("Input buffer must have an even number of bytes")
        }

        for (i in 0 until buffer.size / 2) {
            out[i] =
                (buffer[i * 2].toInt() and 0xFF or (buffer[i * 2 + 1].toInt() shl 8)).toFloat() / Short.MAX_VALUE
        }
        return out
    }

    private fun updateFFT(readings: List<Float>) {
        val twiddle = memo("twiddle", readings.size) {
            FrequencyAnalysis.getTwiddleFactorsFFT(readings.size)
        }
        val hanning = FrequencyAnalysis.hanningWindow(readings)

        val fft =
            FrequencyAnalysis.fft(hanning, twiddle)

        val maxIndex =
            FrequencyAnalysis.getIndexFFT(frequencyCutOff, fft.size, sampleRate.toFloat())

        val frequencies = fft
            .subList(0, maxIndex)
            .map { it.magnitude }

        // Update the history
        history = frequencies
            .mapIndexed { index, value ->
                Vector2(
                    FrequencyAnalysis.getFrequencyFFT(
                        index,
                        fft.size,
                        sampleRate.toFloat()
                    ), fftDecibels(value, fft.size)
                )
            }

        // Update the most resonant frequency
        mostResonant = FrequencyAnalysis.getMostResonantFrequencyFFT(fft, sampleRate.toFloat())
        mostResonantDb = mostResonant?.let {
            fftDecibels(
                FrequencyAnalysis.getMagnitudeOfFrequencyFFT(
                    fft,
                    it,
                    sampleRate.toFloat()
                ), fft.size
            )
        }

        // Update the spectrogram
        spectrogram = (spectrogram + listOf(frequencies)).takeLast(historyLength)
    }
}