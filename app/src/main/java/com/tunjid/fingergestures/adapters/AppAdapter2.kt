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

package com.tunjid.fingergestures.adapters

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.typed
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.databinding.ViewholderHorizontalListBinding
import com.tunjid.fingergestures.databinding.ViewholderMapperBinding
import com.tunjid.fingergestures.databinding.ViewholderPaddingBinding
import com.tunjid.fingergestures.databinding.ViewholderSliderDeltaBinding
import com.tunjid.fingergestures.databinding.ViewholderToggleBinding
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.viewholders.LinkViewHolder
import com.tunjid.fingergestures.viewholders.bind
import com.tunjid.fingergestures.viewholders.mapper
import com.tunjid.fingergestures.viewholders.popUp
import com.tunjid.fingergestures.viewholders.rotation
import com.tunjid.fingergestures.viewholders.sliderAdjuster
import com.tunjid.fingergestures.viewholders.toggle
import com.tunjid.fingergestures.viewmodels.Inputs
import com.tunjid.fingergestures.viewmodels.Tab

private fun appAdapter2(items: List<Item>?) = listAdapterOf(
    initialItems = items ?: listOf(),
    viewHolderCreator = { parent, viewType ->
        when (viewType) {
            Item.Toggle::class.hashCode() -> parent.toggle()
            Item.Slider::class.hashCode() -> parent.sliderAdjuster()
            Item.Mapper::class.hashCode() -> parent.mapper()
            Item.Rotation::class.hashCode() -> parent.rotation()
            Item.PopUp::class.hashCode() -> parent.popUp()
            else -> parent.viewHolderFrom(ViewholderPaddingBinding::inflate)
        }
    },
    viewHolderBinder = { holder, item, _ ->
        when (item) {
            is Item.Toggle -> holder.typed<ViewholderToggleBinding>().bind(item)
            is Item.Slider -> holder.typed<ViewholderSliderDeltaBinding>().bind(item)
            is Item.Mapper -> holder.typed<ViewholderMapperBinding>().bind(item)
            is Item.Rotation -> holder.typed<ViewholderHorizontalListBinding>().bind(item)
            is Item.PopUp -> holder.typed<ViewholderHorizontalListBinding>().bind(item)
            is Item.Padding -> TODO()
            is Item.Link -> TODO()
            is Item.AudioStreamType -> TODO()
            is Item.AdFree -> TODO()
            is Item.WallpaperView -> TODO()
            is Item.WallpaperTrigger -> TODO()
            is Item.DiscreteBrightness -> TODO()
            is Item.ColorAdjuster -> TODO()
            is Item.ScreenDimmer -> TODO()
        }
    }
)

sealed class Item(
    override val diffId: String
) : Differentiable {

    abstract val tab: Tab
    abstract val index: Int

    data class Padding(
        override val tab: Tab,
        override val index: Int,
        override val diffId: String
    ) : Item(diffId)

    data class Toggle(
        override val tab: Tab,
        override val index: Int,
        @StringRes val titleRes: Int,
        val consumer: (Boolean) -> Unit,
        val isChecked: Boolean,
    ) : Item(titleRes.toString())

    data class Slider(
        override val tab: Tab,
        override val index: Int,
        @StringRes val titleRes: Int,
        @StringRes val infoRes: Int,
        val consumer: (Int) -> Unit,
        val value: Int,
        val isEnabled: Boolean,
        val function: (Int) -> String
    ) : Item(titleRes.toString())

    data class Mapper(
        override val tab: Tab,
        override val index: Int,
        @param:GestureMapper.GestureDirection
        @field:GestureMapper.GestureDirection
        val direction: String,
        val input: Inputs
    ) : Item(direction)

    data class Rotation(
        override val tab: Tab,
        override val index: Int,
        @param:RotationGestureConsumer.PersistedSet
        val persistedSet: String?,
        @StringRes val titleRes: Int,
        @StringRes val infoRes: Int,
        val items: List<Package>,
        val input: Inputs
    ) : Item(titleRes.toString())

    data class Link(
        override val tab: Tab,
        override val index: Int,
        val linkItem: LinkViewHolder.LinkItem,
        val input: Inputs
    ) : Item(linkItem.link)

    data class AudioStreamType(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("AudioStreamType")

    data class AdFree(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("AdFree")

    data class WallpaperView(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("WallpaperView")

    data class WallpaperTrigger(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("WallpaperTrigger")

    data class PopUp(
        override val tab: Tab,
        override val index: Int,
        val items: List<Action>,
        val input: Inputs
    ) : Item("PopUp")

    data class DiscreteBrightness(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("DiscreteBrightness")

    data class ColorAdjuster(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("ColorAdjuster")

    data class ScreenDimmer(
        override val tab: Tab,
        override val index: Int,
        val input: Inputs
    ) : Item("ScreenDimmer")
}


