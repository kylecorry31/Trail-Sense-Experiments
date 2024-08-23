package com.kylecorry.trail_sense_experiments.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.files.LocalFileSystem
import com.kylecorry.andromeda.files.ZipUtils
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense_experiments.databinding.FragmentSurvivalGuideBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SurvivalGuideFragment : BoundFragment<FragmentSurvivalGuideBinding>() {

    private val assets by lazy { AssetFileSystem(requireContext()) }
    private val localFiles by lazy { LocalFileSystem(requireContext()) }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webviewAssetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    requireContext(),
                    requireContext().filesDir
                )
            )
            .build()

        binding.webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return webviewAssetLoader.shouldInterceptRequest(request.url)
            }
        }

        // TODO: Add a way to find text in the webview
        // TODO: Add a way to quickly navigate to a section in the webview

        // Disable settings
        binding.webView.settings.allowFileAccess = false
        binding.webView.settings.allowContentAccess = false
        binding.webView.settings.allowFileAccessFromFileURLs = false
        binding.webView.settings.allowUniversalAccessFromFileURLs = false

        // Set default background color of the webview
        binding.webView.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onResume() {
        super.onResume()

        inBackground {
            if (localFiles.getDirectory("survival-guide", false).exists()) {
                onMain {
                    openGuide()
                }
                return@inBackground
            }
            val zip = assets.stream("survival-guide.zip")
            ZipUtils.unzip(zip, localFiles.getDirectory("survival-guide"), 300)
            onMain {
                openGuide()
            }
        }

    }

    private fun openGuide() {
        // It doesn't actually make a network request, this just makes it use the webview asset loader
        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/survival-guide/guide.html")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideBinding {
        return FragmentSurvivalGuideBinding.inflate(layoutInflater, container, false)
    }
}