package com.kylecorry.trail_sense_experiments.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.trail_sense_experiments.databinding.FragmentSurvivalGuideBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SurvivalGuideFragment : BoundFragment<FragmentSurvivalGuideBinding>() {

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webviewAssetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(requireContext()))
            .build()

        binding.webView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                return webviewAssetLoader.shouldInterceptRequest(request.url)
            }
        }

        // Disable settings
        binding.webView.settings.allowFileAccess = false
        binding.webView.settings.allowContentAccess = false
        binding.webView.settings.allowFileAccessFromFileURLs = false
        binding.webView.settings.allowUniversalAccessFromFileURLs = false

        // It doesn't actually make a network request, this just makes it use the webview asset loader
        binding.webView.loadUrl("https://appassets.androidplatform.net/assets/survival_guide/guide.html")
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideBinding {
        return FragmentSurvivalGuideBinding.inflate(layoutInflater, container, false)
    }
}