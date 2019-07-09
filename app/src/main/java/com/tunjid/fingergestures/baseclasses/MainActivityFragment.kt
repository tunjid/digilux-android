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


import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.fragments.AppFragment
import io.reactivex.disposables.CompositeDisposable

abstract class MainActivityFragment : BaseFragment() {

    protected var disposables = CompositeDisposable()

    protected val currentAppFragment: AppFragment?
        get() {
            val activity = activity as MainActivity? ?: return null

            return activity.currentFragment
        }

    override fun onDestroyView() {
        disposables.clear()
        super.onDestroyView()
    }

    protected fun toggleToolbar(visible: Boolean) {
        val activity = activity as MainActivity?
        activity?.toggleToolbar(visible)
    }

    fun showSnackbar(@StringRes resource: Int) {
        val activity = activity as MainActivity?
        activity?.showSnackbar(resource)
    }

    fun purchase(@PurchasesManager.SKU sku: String) {
        val activity = activity as MainActivity?
        activity?.purchase(sku)
    }

    fun requestPermission(@MainActivity.PermissionRequest permission: Int) {
        val activity = activity as MainActivity?
        activity?.requestPermission(permission)
    }

    fun showBottomSheetFragment(fragment: MainActivityFragment) {
        val fragmentManager = fragmentManager ?: return

        fragmentManager.beginTransaction().replace(R.id.bottom_sheet, fragment).commit()
        toggleBottomSheet(true)
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
