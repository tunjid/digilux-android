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

package com.tunjid.fingergestures.viewholders;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;

public class LinkViewHolder extends AppViewHolder {

    private static final String APP_URI = "market://details?id=com.tunjid.fingergestures";
    private static final String SUPPORT_LINK = "https://github.com/tunjid/digilux-android/issues";

    public static final LinkItem REVIEW_LINK_ITEM = new LinkItem(R.string.review_app, R.drawable.ic_rate_review_white_24dp, APP_URI);
    public static final LinkItem SUPPORT_LINK_ITEM = new LinkItem(R.string.help_support, R.drawable.ic_help_24dp, SUPPORT_LINK);

    public LinkViewHolder(View itemView, LinkItem linkItem, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
        TextView title = itemView.findViewById(R.id.title);

        title.setText(linkItem.titleRes);
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(linkItem.icon, 0, 0, 0);

        itemView.setOnClickListener(view -> view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linkItem.link))));
    }


    public static class LinkItem {
        @StringRes int titleRes;
        @DrawableRes int icon;
        String link;

        LinkItem(int titleRes, int icon, String link) {
            this.titleRes = titleRes;
            this.icon = icon;
            this.link = link;
        }
    }
}
