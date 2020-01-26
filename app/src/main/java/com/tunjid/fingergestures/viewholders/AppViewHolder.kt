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

package com.tunjid.fingergestures.viewholders

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapterListener
import com.tunjid.fingergestures.billing.PurchasesManager
import io.reactivex.disposables.CompositeDisposable

open class AppViewHolder(
        itemView: View,
        internal val listener: AppAdapterListener = AppAdapterListener.noOpInstance
) : RecyclerView.ViewHolder(itemView) {

    protected val disposables = CompositeDisposable()

    open fun bind() {}

    fun clear() = disposables.clear()

    internal fun goPremium(@StringRes description: Int) {
        val context = itemView.context
        AlertDialog.Builder(context)
                .setTitle(R.string.go_premium_title)
                .setMessage(context.getString(R.string.go_premium_body, context.getString(description)))
                .setPositiveButton(R.string.continue_text) { _, _ -> listener?.purchase(PurchasesManager.PREMIUM_SKU) }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
    }
}
