package com.kylecorry.trail_sense_experiments.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import com.kylecorry.andromeda.canvas.CanvasView
import com.kylecorry.trail_sense_experiments.ui.colormaps.InfernoColorMap
import com.kylecorry.trail_sense_experiments.ui.colormaps.PrecalculatedColorMap
import kotlin.math.log10

class SpectogramView(context: Context, attrs: AttributeSet?) : CanvasView(context, attrs) {

    var spectogram: List<List<Float>> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    var fftSize: Int = 0
        set(value) {
            if (value != field) {
                field = value
                offset = -20f * log10(value.toFloat())
                invalidate()
            }
        }

    private var offset = 0f

    private val colorMap = PrecalculatedColorMap(InfernoColorMap())
    private var bitmap: Bitmap? = null

    override fun draw() {
        val width = width.toFloat()
        val height = height.toFloat()

        if (spectogram.isEmpty()) {
            return
        }

        clear()

        val max = 0f
        val min = -125f

        val rangeReciprocal = 1 / (max - min)

        if (bitmap == null || bitmap!!.width != spectogram.size || bitmap!!.height != spectogram[0].size) {
            bitmap?.recycle()
            bitmap =
                Bitmap.createBitmap(spectogram.size, spectogram[0].size, Bitmap.Config.ARGB_8888)
        }

        bitmap?.apply {
            for (i in spectogram.indices) {
                for (j in spectogram[i].indices) {
                    val value = 20 * log10(spectogram[i][j].coerceAtLeast(1e-10f)) + offset
                    val normalized = ((value - min) * rangeReciprocal).coerceIn(0f, 1f)
                    val color = colorMap.getColor(normalized)
                    setPixel(i, this.height - j - 1, color)
                }
            }
        }

        image(bitmap!!, 0f, 0f, width, height)
    }

    override fun setup() {
    }
}