/*
 * Copyright (c) 2017, 2018, 2019 Adetunji Dahunsi.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tunjid.fingergestures.models

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getDrawable
import com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator.GlyphState
import com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator.newState
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import java.util.*

class UiState {

    val fabVisible: Boolean
    val glyphState: GlyphState

    constructor(context: Context) {
        this.fabVisible = false
        this.glyphState = newState(context.getText(R.string.enable_accessibility), getDrawable(context, R.drawable.ic_human_24dp))
    }

    private constructor(fabVisible: Boolean, glyphState: GlyphState) {
        this.fabVisible = fabVisible
        this.glyphState = glyphState
    }

    fun visibility(fabVisible: Boolean): UiState {
        return UiState(fabVisible, this.glyphState)
    }

    fun glyph(@StringRes text: Int, @DrawableRes icon: Int): UiState {
        val glyphState = App.transformApp({ app -> newState(app.getText(text), getDrawable(app, icon)) }, this.glyphState)
        return UiState(fabVisible, glyphState)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val uiState = other as UiState?
        return fabVisible == uiState!!.fabVisible && glyphState == uiState.glyphState
    }

    override fun hashCode(): Int {
        return Objects.hash(fabVisible, glyphState)
    }
}
