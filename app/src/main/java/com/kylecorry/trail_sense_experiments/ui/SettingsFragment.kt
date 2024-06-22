package com.kylecorry.trail_sense_experiments.ui

import android.os.Bundle
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.AndromedaPreferenceFragment
import com.kylecorry.trail_sense_experiments.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : AndromedaPreferenceFragment() {

    private val navigationMap = mapOf<Int, Int>(
        // Pref key to action id
    )

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        for (nav in navigationMap) {
            navigateOnClick(preference(nav.key), nav.value)
        }

        setIconColor(preferenceScreen, Resources.androidTextColorSecondary(requireContext()))
    }

}