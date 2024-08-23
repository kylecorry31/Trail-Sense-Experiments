package com.kylecorry.trail_sense_experiments.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html.ImageGetter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.files.AssetFileSystem
import com.kylecorry.andromeda.fragments.BoundFragment
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.list.ListView
import com.kylecorry.andromeda.markdown.MarkdownService
import com.kylecorry.trail_sense_experiments.databinding.FragmentSurvivalGuideBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SurvivalGuideFragment : BoundFragment<FragmentSurvivalGuideBinding>() {

    private var listView: ListView<CharSequence>? = null
    private var paragraphs by state(emptyList<CharSequence>())
    private var html by state("")
    private val filesystem by lazy { AssetFileSystem(requireContext()) }
    private val markdown by lazy { MarkdownService(requireContext()) }

    @SuppressLint("ClickableViewAccessibility", "MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageGetter = ImageGetter { source ->
            val realSource = source.replace("images/", "survival_guide/")
            val stream = runBlocking { filesystem.stream(realSource) }
            stream.use { s ->
                Drawable.createFromStream(s, realSource)
                    ?.also {
                        val viewWidth =
                            binding.guideList.width.toFloat() - Resources.dp(requireContext(), 32f)
                        val scaleFactor = viewWidth / it.intrinsicWidth
                        it.setBounds(
                            0,
                            0,
                            (it.intrinsicWidth * scaleFactor).toInt(),
                            (it.intrinsicHeight * scaleFactor).toInt()
                        )
                    }
            }
        }

        listView =
            ListView(binding.guideList, android.R.layout.simple_list_item_1) { view, item ->
                // TODO: Add support for list items https://stackoverflow.com/questions/3150400/html-list-tag-not-working-in-android-textview-what-can-i-do
                view.findViewById<TextView>(android.R.id.text1).text = HtmlCompat.fromHtml(
                    item.toString(),
                    HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM,
                    imageGetter,
                    null
                )
            }
    }

    override fun onResume() {
        super.onResume()
        inBackground {
            if (paragraphs.isNotEmpty()) {
                return@inBackground
            }
            html = filesystem.read("survival_guide/guide.html")
            paragraphs = html.split("\n").filter { it.isNotBlank() }
            println(paragraphs.size)
        }
    }

    override fun onUpdate() {
        super.onUpdate()
        effect("paragraphs", paragraphs, lifecycleHookTrigger.onResume()) {
            listView?.setData(paragraphs)
        }

    }

    override fun generateBinding(
        layoutInflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSurvivalGuideBinding {
        return FragmentSurvivalGuideBinding.inflate(layoutInflater, container, false)
    }
}