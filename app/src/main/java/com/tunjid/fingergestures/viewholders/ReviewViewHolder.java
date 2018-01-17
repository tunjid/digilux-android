package com.tunjid.fingergestures.viewholders;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;

public class ReviewViewHolder extends AppViewHolder {

    private static final String APP_URI = "market://details?id=com.tunjid.fingergestures";

    public ReviewViewHolder(View itemView, AppAdapter.HomeAdapterListener listener) {
        super(itemView, listener);
        TextView title = itemView.findViewById(R.id.title);

        title.setText(R.string.review_app);
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_rate_review_white_24dp, 0, 0, 0);

        itemView.setOnClickListener(this::reviewApp);
    }

    private void reviewApp(View view) {
        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(APP_URI)));
    }
}
