package com.kylecorry.trail_sense_experiments.ui.colormaps

import androidx.annotation.ColorInt

interface ColorMap {
    /**
     * Get the color for a given percent [0, 1]
     */
    @ColorInt
    fun getColor(percent: Float): Int
}