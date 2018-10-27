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
    private static final String SUPPORT_LINK = "https://plus.google.com/communities/101040733188459773494";

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
