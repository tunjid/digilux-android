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
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapterListener
class LinkViewHolder(
        itemView: View,
        linkItem: LinkItem,
        listener: AppAdapterListener
) : AppViewHolder(itemView, listener) {

    init {
        val title = itemView.findViewById<TextView>(R.id.title)

        title.setText(linkItem.titleRes)
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(linkItem.icon, 0, 0, 0)

        itemView.setOnClickListener { view -> view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(linkItem.link))) }
    }


    class LinkItem internal constructor(@field:StringRes internal var titleRes: Int, @field:DrawableRes internal var icon: Int, internal var link: String)

    companion object {

        private const val APP_URI = "market://details?id=com.tunjid.fingergestures"
        private const val SUPPORT_LINK = "https://github.com/tunjid/digilux-android/issues"

        val REVIEW_LINK_ITEM = LinkItem(R.string.review_app, R.drawable.ic_rate_review_white_24dp, APP_URI)
        val SUPPORT_LINK_ITEM = LinkItem(R.string.help_support, R.drawable.ic_help_24dp, SUPPORT_LINK)
    }
}
