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

package com.tunjid.fingergestures.baseclasses


import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.uidrivers.liveUiState
import com.tunjid.androidx.view.util.marginLayoutParams
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.databinding.ActivityMainBinding
import com.tunjid.fingergestures.mapDistinct
import kotlin.properties.ReadOnlyProperty

fun Fragment.divider(): RecyclerView.ItemDecoration {
    val context = requireContext()

    val itemDecoration = DividerItemDecoration(context, VERTICAL)
    val decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark)

    if (decoration != null) itemDecoration.setDrawable(decoration)

    return itemDecoration
}

interface BottomSheetController {
    val bottomSheetNavigator: BottomSheetNavigator?
}

/**
 * Crawls up the Fragment hierarchy to find the [BottomSheetController].
 * If the calling Fragment is directly in the BottomSheetController, this is found in one step,
 * if it's in a multi stack navigator, it takes 2 steps
 */

fun Fragment.recursiveBottomSheetNavigator() = ReadOnlyProperty<Fragment, BottomSheetNavigator> { _, _ ->
    generateSequence(parentFragment, Fragment::getParentFragment)
        .plus(activity)
        .filterIsInstance<BottomSheetController>()
        .firstOrNull()
        ?.bottomSheetNavigator
        ?: throw IllegalArgumentException("This Fragment is not hosted  by a BottomSheetController")
}

class BottomSheetNavigator(
    private val host: FragmentActivity,
    private val binding: ActivityMainBinding,
    private val onSheetSlide: (Float) -> Unit
) : Navigator {

    private val bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)

    init {
        binding.bottomSheet.setOnApplyWindowInsetsListener { _, insets ->  insets.consumeSystemWindowInsets()}
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) =
                onSheetSlide((slideOffset + 1) / 2) // callback range is [-1, 1])

            override fun onStateChanged(bottomSheet: View, newState: Int) = Unit
        })

        binding.bottomSheet.doOnLayout { sheet ->
            sheet.liveUiState
                .mapDistinct { it.systemUI.static.statusBarSize }
                .observe(host) { sheet.marginLayoutParams.topMargin = it }
        }
    }

    override val containerId: Int = R.id.bottom_sheet

    override val current: Fragment?
        get() = host.supportFragmentManager.findFragmentById(containerId)

    override val previous: Fragment? = null

    override fun clear(upToTag: String?, includeMatch: Boolean) {
        pop()
    }

    override fun find(tag: String): Fragment? = current.takeIf { it?.tag == tag }

    override fun pop(): Boolean {
        val willPop = bottomSheetBehavior.state != BottomSheetBehavior.STATE_HIDDEN
        if (willPop) bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        return willPop
    }

    override fun push(fragment: Fragment, tag: String): Boolean {
        val same = tag == current?.tag

        binding.bottomSheet.post {
            if (!same) host.supportFragmentManager
                .beginTransaction()
                .replace(binding.bottomSheet.id, fragment, tag)
                .commit()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }

        return !same
    }
}
