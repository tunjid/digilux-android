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

import android.content.Context;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import io.reactivex.disposables.CompositeDisposable;

import android.view.View;

import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder;
import com.tunjid.fingergestures.R;
import com.tunjid.fingergestures.adapters.AppAdapter;
import com.tunjid.fingergestures.billing.PurchasesManager;

public class AppViewHolder extends InteractiveViewHolder<AppAdapter.AppAdapterListener> {

    protected final CompositeDisposable disposables = new CompositeDisposable();

    public AppViewHolder(View itemView) {
        super(itemView);
    }

    AppViewHolder(View itemView, AppAdapter.AppAdapterListener listener) {
        super(itemView, listener);
    }

    public void bind() {}

    public void clear() { disposables.clear();}

    void goPremium(@StringRes int description) {
        Context context = itemView.getContext();
        new AlertDialog.Builder(context)
                .setTitle(R.string.go_premium_title)
                .setMessage(context.getString(R.string.go_premium_body, context.getString(description)))
                .setPositiveButton(R.string.continue_text, (dialog, which) -> adapterListener.purchase(PurchasesManager.PREMIUM_SKU))
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
