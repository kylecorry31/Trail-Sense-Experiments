package com.kylecorry.trail_sense_experiments.ui

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.tryOrNothing
import com.kylecorry.andromeda.fragments.AndromedaActivity
import com.kylecorry.andromeda.fragments.ColorTheme
import com.kylecorry.trail_sense_experiments.R
import com.kylecorry.trail_sense_experiments.app.NavigationUtils.setupWithNavController
import com.kylecorry.trail_sense_experiments.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AndromedaActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding!!

    private val permissions = mutableListOf<String>(
        Manifest.permission.RECORD_AUDIO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        ExceptionHandler.initialize(this)
        setColorTheme(ColorTheme.System, true)
        enableEdgeToEdge(
            navigationBarStyle = if (isDarkTheme()) {
                SystemBarStyle.dark(Resources.androidBackgroundColorSecondary(this))
            } else {
                SystemBarStyle.light(
                    Resources.androidBackgroundColorSecondary(this),
                    Color.BLACK
                )
            }
        )

        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setupWithNavController(findNavController(), false)

        bindLayoutInsets()

        requestPermissions(permissions) {
            findNavController().navigate(R.id.fragmentSpectrogram)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        setIntent(intent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.bottomNavigation.selectedItemId = savedInstanceState.getInt(
            "page",
            R.id.fragmentSpectrogram
        )
        if (savedInstanceState.containsKey("navigation")) {
            tryOrNothing {
                val bundle = savedInstanceState.getBundle("navigation_arguments")
                findNavController().navigate(savedInstanceState.getInt("navigation"), bundle)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", binding.bottomNavigation.selectedItemId)
        findNavController().currentBackStackEntry?.arguments?.let {
            outState.putBundle("navigation_arguments", it)
        }
        findNavController().currentDestination?.id?.let {
            outState.putInt("navigation", it)
        }
    }

    private fun bindLayoutInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun findNavController(): NavController {
        return (supportFragmentManager.findFragmentById(R.id.fragment_holder) as NavHostFragment).navController
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
