package com.kylecorry.trail_sense_experiments.ui.colormaps

import android.graphics.Color

class GrayscaleColorMap : ColorMap {
    override fun getColor(percent: Float): Int {
        val value = (percent * 255).toInt()
        return Color.rgb(value, value, value)
    }
}