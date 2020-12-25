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


import android.view.Menu
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidx.view.util.InsetFlags
import com.tunjid.fingergestures.GlobalUiController
import com.tunjid.fingergestures.InsetProvider
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.TrialView
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.activityGlobalUiController
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.models.UiState

abstract class MainActivityFragment(
        @LayoutRes layoutRes: Int
) : Fragment(layoutRes),
        GlobalUiController {

    override val insetFlags: InsetFlags = InsetFlags.NO_BOTTOM

    override var uiState: UiState by activityGlobalUiController()

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.action_start_trial)
        val isTrialVisible = !PurchasesManager.instance.isPremiumNotTrial

        if (item != null) item.isVisible = isTrialVisible
        if (isTrialVisible && item != null) item.actionView = TrialView(requireContext(), item)

        return super.onPrepareOptionsMenu(menu)
    }

    fun purchase(@PurchasesManager.SKU sku: String) {
        val activity = activity as MainActivity?
        activity?.purchase(sku)
    }

    protected fun toggleBottomSheet(show: Boolean) {
        val activity = activity as MainActivity?
        activity?.toggleBottomSheet(show)
    }

    protected fun divider(): RecyclerView.ItemDecoration {
        val context = requireContext()

        val itemDecoration = DividerItemDecoration(context, VERTICAL)
        val decoration = ContextCompat.getDrawable(context, android.R.drawable.divider_horizontal_dark)

        if (decoration != null) itemDecoration.setDrawable(decoration)

        return itemDecoration
    }
}
