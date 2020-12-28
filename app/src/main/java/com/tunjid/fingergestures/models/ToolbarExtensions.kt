package com.tunjid.androidx.uidrivers

import android.content.res.ColorStateList
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.tunjid.androidx.core.content.colorAt
import com.tunjid.androidx.core.graphics.drawable.withTint
import com.tunjid.androidx.core.text.color
import com.tunjid.fingergestures.R

internal fun Toolbar.updatePartial(toolbarState: ToolbarState) {
    val (@MenuRes menu: Int, title: CharSequence, invalidatedAlone: Boolean) = toolbarState
    if (invalidatedAlone) return refreshMenu()

    val currentTitle = this.title?.toString() ?: ""
    if (currentTitle.isNotBlank()) TransitionManager.beginDelayedTransition(this, AutoTransition().apply {
        // We only want to animate the title, but it's lazy initialized.
        // If it's there, use it, else fuzzy match to it's initialization
        val titleTextView = children.filterIsInstance<TextView>()
            .filter { it.text?.toString() == currentTitle }
            .firstOrNull()
        if (titleTextView != null) addTarget(titleTextView) else addTarget(TextView::class.java)
    })

    this.title = if (title.isEmpty()) " " else title
    refreshMenu(menu)
    updateIcons()
}

private fun Toolbar.refreshMenu(menu: Int? = null) {
    if (menu != null) {
        this.menu.clear()
        if (menu != 0) inflateMenu(menu)
    }
    uiState.toolbarMenuRefresher.invoke(this.menu)
}

private fun Toolbar.updateIcons() {
    TransitionManager.beginDelayedTransition(this, AutoTransition().setDuration(100).addTarget(ActionMenuView::class.java))
    val tint = titleTint

    menu.forEach {
        it.icon = it.icon?.withTint(tint)
        it.title = it.title.color(tint)
        it.actionView?.backgroundTintList = ColorStateList.valueOf(tint)
    }

    overflowIcon = overflowIcon?.withTint(tint)
}

private val Toolbar.titleTint: Int
    get() = (title as? Spanned)?.run {
        getSpans(0, title.length, ForegroundColorSpan::class.java)
            .firstOrNull()
            ?.foregroundColor
    } ?: context.colorAt(R.color.toggle_text)
