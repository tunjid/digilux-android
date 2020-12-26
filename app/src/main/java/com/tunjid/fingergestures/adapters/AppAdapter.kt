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
import androidx.fragment.app.Fragment
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.typed
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.databinding.*
import com.tunjid.fingergestures.gestureconsumers.AudioGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.viewholders.*
import com.tunjid.fingergestures.viewmodels.AppViewModel
import com.tunjid.fingergestures.viewmodels.Inputs
import com.tunjid.fingergestures.viewmodels.Tab

fun appAdapter(items: List<Item>?) = listAdapterOf(
    initialItems = items ?: listOf(),
    viewHolderCreator = { parent, viewType ->
        when (viewType) {
            Item.Toggle::class.hashCode() -> parent.toggle()
            Item.Slider::class.hashCode() -> parent.sliderAdjuster()
            Item.Mapper::class.hashCode() -> parent.mapper()
            Item.Rotation::class.hashCode() -> parent.rotation()
            Item.PopUp::class.hashCode() -> parent.popUp()
            Item.Link::class.hashCode() -> parent.link()
            Item.AudioStream::class.hashCode() -> parent.audioStream()
            Item.Padding::class.hashCode() -> parent.viewHolderFrom(ViewholderPaddingBinding::inflate)
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
            is Item.Link -> holder.typed<ViewholderSimpleTextBinding>().bind(item)
            is Item.AudioStream -> holder.typed<ViewholderAudioStreamTypeBinding>().bind(item)
            is Item.Padding -> Unit
            is Item.AdFree -> Unit
            is Item.WallpaperView -> Unit
            is Item.WallpaperTrigger -> Unit
            is Item.DiscreteBrightness -> Unit
            is Item.ColorAdjuster -> Unit
            is Item.ScreenDimmer -> Unit
        }
    },
    viewTypeFunction = { it::class.hashCode() },
    itemIdFunction = { it.diffId.hashCode().toLong() }
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
        @param:GestureMapper.GestureDirection
        @field:GestureMapper.GestureDirection
        val doubleDirection: String,
        val gesturePair: GestureMapper.GesturePair,
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
        val linkItem: LinkItem,
        val input: Inputs
    ) : Item(linkItem.link)

    data class AudioStream(
        override val tab: Tab,
        override val index: Int,
        val stream: AudioGestureConsumer.Stream,
        val hasDoNotDisturbAccess: Boolean,
        val titleFunction: (Int) -> String,
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


interface AppAdapterListener {
    fun purchase(@PurchasesManager.SKU sku: String)

    fun pickWallpaper(@BackgroundManager.WallpaperSelection selection: Int)

    fun requestPermission(@MainActivity.PermissionRequest permission: Int)

    fun showSnackbar(@StringRes message: Int)

    fun notifyItemChanged(@AppViewModel.AdapterIndex index: Int)

    fun showBottomSheetFragment(fragment: Fragment)

    companion object {
        val noOpInstance
            get() = object : AppAdapterListener {
                override fun purchase(@PurchasesManager.SKU sku: String) = Unit

                override fun pickWallpaper(@BackgroundManager.WallpaperSelection selection: Int) = Unit

                override fun requestPermission(@MainActivity.PermissionRequest permission: Int) = Unit

                override fun showSnackbar(@StringRes message: Int) = Unit

                override fun notifyItemChanged(@AppViewModel.AdapterIndex index: Int) = Unit

                override fun showBottomSheetFragment(fragment: Fragment) = Unit
            }
    }
}
