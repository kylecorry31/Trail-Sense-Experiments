package com.kylecorry.trail_sense_experiments.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.ExpansionLayout
import com.kylecorry.andromeda.core.ui.Views
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
            val text = onIO {
                assets.read("survival_guide/chapter-$chapter.md")
            }
            onMain {
                binding.guideScroll.removeAllViews()
                binding.guideScroll.addView(getGuideView(requireContext(), text))
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

    private fun getGuideView(context: Context, text: String): View {
        val markdown = MarkdownService(context)
        val sections = TextUtils.groupSections(TextUtils.getSections(text), null)
        val children = sections.mapNotNull { section ->
            val first = section.firstOrNull() ?: return@mapNotNull null
            if (first.level != null && first.title != null) {
                // Create an expandable section
                val expandable = expandable(
                    context, first.title
                ) {
                    markdown.setMarkdown(it,
                        first.content + "\n" + section.drop(1)
                            .joinToString("\n") { it.toMarkdown() })
                }
                expandable
            } else {
                // Only text nodes
                val t = Views.text(context, null).also {
                    (it as TextView).movementMethod = LinkMovementMethodCompat.getInstance()
                }
                markdown.setMarkdown(t as TextView, section.joinToString("\n") { it.toMarkdown() })
                t
            }
        }

        return Views.linear(children, padding = Resources.dp(context, 16f).toInt())
    }

    private fun expandable(
        context: Context,
        title: String,
        setContent: (TextView) -> Unit
    ): ExpansionLayout {
        val expandable = ExpansionLayout(context, null)

        val titleView = Views.text(context, title) as TextView
//        titleView.setCompoundDrawables(right = R.drawable.ic_drop_down)
//        CustomUiUtils.setImageColor(titleView, Resources.androidTextColorSecondary(context))
        titleView.compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        val padding = Resources.dp(context, 16f).toInt()
        val margin = Resources.dp(context, 8f).toInt()
        titleView.setPadding(padding)
        titleView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).also {
            it.setMargins(0, margin, 0, margin)
            it.gravity = Gravity.CENTER_VERTICAL
        }
//        titleView.setBackgroundResource(R.drawable.rounded_rectangle)
        titleView.backgroundTintList = ColorStateList.valueOf(
            Resources.getAndroidColorAttr(
                context,
                android.R.attr.colorBackgroundFloating
            )
        )

        expandable.addView(titleView)

        expandable.addView(
            Views.text(context, null).also {
                (it as TextView).movementMethod = LinkMovementMethodCompat.getInstance()
                it.setPadding(margin)
                setContent(it)
            }
        )

//        expandable.setOnExpandStateChangedListener { isExpanded ->
//            titleView.setCompoundDrawables(right = if (isExpanded) R.drawable.ic_drop_down_expanded else R.drawable.ic_drop_down)
//            CustomUiUtils.setImageColor(titleView, Resources.androidTextColorSecondary(context))
//        }

        return expandable
    }
}