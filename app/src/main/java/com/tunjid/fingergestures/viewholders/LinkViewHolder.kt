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

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderSimpleTextBinding

private var BindingViewHolder<ViewholderSimpleTextBinding>.item by viewHolderDelegate<Item.Link>()

fun ViewGroup.link() = viewHolderFrom(ViewholderSimpleTextBinding::inflate).apply {
    itemView.setOnClickListener { view -> view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.linkItem.link))) }
}

fun BindingViewHolder<ViewholderSimpleTextBinding>.bind(item: Item.Link) = binding.run {
    this@bind.item = item

    title.setText(item.linkItem.titleRes)
    title.setCompoundDrawablesRelativeWithIntrinsicBounds(item.linkItem.icon, 0, 0, 0)
}

val ReviewLinkItem = LinkItem(
    titleRes = R.string.review_app,
    icon = R.drawable.ic_rate_review_white_24dp,
    link = "market://details?id=com.tunjid.fingergestures"
)
val SupportLinkItem = LinkItem(
    titleRes = R.string.help_support,
    icon = R.drawable.ic_help_24dp,
    link = "https://github.com/tunjid/digilux-android/issues"
)

class LinkItem internal constructor(
    @field:StringRes
    internal val titleRes: Int,
    @field:DrawableRes
    internal val icon: Int,
    internal val link: String
)