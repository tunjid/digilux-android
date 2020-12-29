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

import android.app.PendingIntent
import android.content.pm.ApplicationInfo
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.palette.graphics.Palette
import com.tunjid.androidx.recyclerview.diff.Differentiable
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.typed
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.databinding.*
import com.tunjid.fingergestures.gestureconsumers.*
import com.tunjid.fingergestures.models.Action
import com.tunjid.fingergestures.models.Brightness
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.viewholders.*
import com.tunjid.fingergestures.viewmodels.main.Inputs
import com.tunjid.fingergestures.viewmodels.main.Tab
import java.io.File

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
            Item.AdFree::class.hashCode() -> parent.adFree()
            Item.DiscreteBrightness::class.hashCode() -> parent.discreteBrightness()
            Item.ScreenDimmer::class.hashCode() -> parent.screenDimmer()
            Item.ColorAdjuster::class.hashCode() -> parent.colorAdjuster()
            Item.WallpaperTrigger::class.hashCode() -> parent.wallpaperTrigger()
            Item.WallpaperPick::class.hashCode() -> parent.wallpaperPick()
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
            is Item.AdFree -> holder.typed<ViewholderSimpleTextBinding>().bind(item)
            is Item.DiscreteBrightness -> holder.typed<ViewholderHorizontalListBinding>().bind(item)
            is Item.ScreenDimmer -> holder.typed<ViewholderScreenDimmerBinding>().bind(item)
            is Item.ColorAdjuster -> holder.typed<ViewholderSliderColorBinding>().bind(item)
            is Item.WallpaperTrigger -> holder.typed<ViewholderWallpaperTriggerBinding>().bind(item)
            is Item.WallpaperPick -> holder.typed<ViewholderWallpaperPickBinding>().bind(item)
            is Item.Padding -> Unit
        }
    },
    viewTypeFunction = { it::class.hashCode() },
    itemIdFunction = { it.diffId.hashCode().toLong() }
)

sealed class Item(
    override val diffId: String
) : Differentiable {

    abstract val tab: Tab
    abstract val sortKey: Int

    data class Padding(
        override val tab: Tab,
        override val sortKey: Int,
        override val diffId: String
    ) : Item(diffId)

    data class Toggle(
        override val tab: Tab,
        override val sortKey: Int,
        @StringRes val titleRes: Int,
        val consumer: (Boolean) -> Unit,
        val isChecked: Boolean,
    ) : Item(titleRes.toString())

    data class Slider(
        override val tab: Tab,
        override val sortKey: Int,
        @StringRes val titleRes: Int,
        @StringRes val infoRes: Int,
        val consumer: (Int) -> Unit,
        val value: Int,
        val isEnabled: Boolean,
        val function: (Int) -> String
    ) : Item(titleRes.toString())

    data class Mapper(
        override val tab: Tab,
        override val sortKey: Int,
        @param:GestureMapper.GestureDirection
        @field:GestureMapper.GestureDirection
        val direction: String,
        @param:GestureMapper.GestureDirection
        @field:GestureMapper.GestureDirection
        val doubleDirection: String,
        val gesturePair: GestureMapper.GesturePair,
        val canUseDoubleSwipes: Boolean,
        val input: Inputs
    ) : Item(direction)

    data class Rotation(
        override val tab: Tab,
        override val sortKey: Int,
        val preference: RotationGestureConsumer.Preference?,
        @StringRes val titleRes: Int,
        @StringRes val infoRes: Int,
        val canAutoRotate: Boolean,
        val removeText: String?,
        val editor: SetPreferenceEditor<ApplicationInfo>?,
        val unRemovablePackages: List<String>,
        val items: List<Package>,
        val input: Inputs
    ) : Item(titleRes.toString())

    data class Link(
        override val tab: Tab,
        override val sortKey: Int,
        val linkItem: LinkItem,
        val input: Inputs
    ) : Item(linkItem.link)

    data class AudioStream(
        override val tab: Tab,
        override val sortKey: Int,
        val stream: AudioGestureConsumer.Stream,
        val hasDoNotDisturbAccess: Boolean,
        val titleFunction: (Int) -> String,
        val consumer: (Int) -> Unit,
        val input: Inputs
    ) : Item("AudioStreamType")

    data class AdFree(
        override val tab: Tab,
        override val sortKey: Int,
        val hasAds: Boolean,
        val notAdFree: Boolean,
        val notPremium: Boolean,
        val input: Inputs
    ) : Item("AdFree")

    data class WallpaperPick(
        override val tab: Tab,
        override val sortKey: Int,
        val dayFile: File?,
        val nightFile: File?,
        val editWallPaperPendingIntent: PendingIntent,
        val screenDimensionRatio: String,
        val input: Inputs
    ) : Item("WallpaperView")

    data class WallpaperTrigger(
        override val tab: Tab,
        override val sortKey: Int,
        val dayStatus: WallpaperStatus,
        val nightStatus: WallpaperStatus,
        val selectTime: (WallpaperSelection, Int, Int) -> Unit,
        val cancelAutoWallpaper: () -> Unit,
        val input: Inputs
    ) : Item("WallpaperTrigger")

    data class PopUp(
        override val tab: Tab,
        override val sortKey: Int,
        val accessibilityButtonEnabled: Boolean,
        val editor: SetPreferenceEditor<GestureAction>,
        val items: List<Action>,
        val input: Inputs
    ) : Item("PopUp")

    data class DiscreteBrightness(
        override val tab: Tab,
        override val sortKey: Int,
        val brightnesses: List<Brightness>,
        val editor: SetPreferenceEditor<Int>,
        val input: Inputs
    ) : Item("DiscreteBrightness")

    data class ColorAdjuster(
        override val tab: Tab,
        override val sortKey: Int,
        @ColorInt
        val backgroundColor: Int,
        @ColorInt
        val sliderColor: Int,
        val palette: Palette?,
        val canPickColorFromWallpaper: Boolean,
        val backgroundColorSetter: (Int) -> Unit,
        val sliderColorSetter: (Int) -> Unit,
        val input: Inputs
    ) : Item("ColorAdjuster")

    data class ScreenDimmer(
        override val tab: Tab,
        override val sortKey: Int,
        val dimmerState: BrightnessGestureConsumer.DimmerState,
        val consumer: (Boolean) -> Unit,
        val input: Inputs
    ) : Item("ScreenDimmer")
}
