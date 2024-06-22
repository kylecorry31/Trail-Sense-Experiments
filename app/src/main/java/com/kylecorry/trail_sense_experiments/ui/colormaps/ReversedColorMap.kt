package com.kylecorry.trail_sense_experiments.ui.colormaps

class ReversedColorMap(private val map: ColorMap) : ColorMap {
    override fun getColor(percent: Float): Int {
        return map.getColor(1 - percent)
    }
}