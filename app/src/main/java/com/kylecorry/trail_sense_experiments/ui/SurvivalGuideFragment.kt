package com.kylecorry.trail_sense_experiments.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.luna.coroutines.onIO
import com.kylecorry.luna.coroutines.onMain
import com.kylecorry.trail_sense_experiments.databinding.FragmentSurvivalGuideBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SurvivalGuideFragment : BoundFragment<FragmentSurvivalGuideBinding>() {

    private val assets by lazy { AssetFileSystem(requireContext()) }
    private val markdown by lazy { MarkdownService(requireContext()) }
    private var chapter by state(1)

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMarkdown()
    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideBinding {
        return FragmentSurvivalGuideBinding.inflate(layoutInflater, container, false)
    }

    private fun setupMarkdown() {
        binding.guideScroll.isVisible = true

        binding.toolbar.leftButton.setOnClickListener {
            val newChapter = chapter + 1
            chapter = if (newChapter > 8) 1 else newChapter
        }
    }

    private fun loadMarkdownChapter(chapter: Int) {
        inBackground {
            val chapter1 = onIO {
                assets.read("survival_guide/chapter-$chapter.md")
            }
            onMain {
                markdown.setMarkdown(binding.guideText, chapter1)
            }
        }
    }

    override fun onUpdate() {
        super.onUpdate()

        effect("chapter", chapter, lifecycleHookTrigger.onResume()) {
            binding.guideScroll.scrollTo(0, 0)
            loadMarkdownChapter(chapter)
        }
    }
}