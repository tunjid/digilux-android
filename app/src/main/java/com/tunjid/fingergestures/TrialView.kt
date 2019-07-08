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

package com.tunjid.fingergestures


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.Window.FEATURE_OPTIONS_PANEL
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.tunjid.fingergestures.billing.PurchasesManager
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.disposables.Disposable

@SuppressLint("ViewConstructor")
class TrialView(context: Context, menuItem: MenuItem) : FrameLayout(context) {

    private val textView: TextView
    private val imageView: ImageView
    private var disposable: Disposable? = null

    init {
        val root = LayoutInflater.from(context).inflate(R.layout.trial_view, this, true)

        textView = root.findViewById(R.id.text)
        imageView = root.findViewById(R.id.icon)

        val clickListener = { _: View ->
            val activity = getActivity(getContext())
            activity?.onMenuItemSelected(FEATURE_OPTIONS_PANEL, menuItem)
            Unit
        }

        setOnClickListener(clickListener)
        imageView.setOnClickListener(clickListener)

        val flowable = PurchasesManager.instance.trialFlowable

        if (flowable == null) changeState(false)
        else disposable = flowable.map(Long::toString)
                .doOnSubscribe { changeState(true) }
                .doOnComplete {
                    val activity = getActivity(getContext()) ?: return@doOnComplete
                    activity.runOnUiThread {
                        changeState(false)
                        activity.recreate()
                    }
                }
                .observeOn(mainThread())
                .subscribe(textView::setText, Throwable::printStackTrace)

        CheatSheet.setup(this, menuItem.title)
    }

    override fun onDetachedFromWindow() {
        if (disposable != null) disposable!!.dispose()
        super.onDetachedFromWindow()
    }

    private fun changeState(isOnTrial: Boolean) {
        imageView.visibility = if (isOnTrial) View.GONE else View.VISIBLE
        textView.visibility = if (isOnTrial) View.VISIBLE else View.GONE
    }

    private fun getActivity(context: Context): Activity? {
        var nested = context
        while (nested !is Activity && nested is ContextWrapper) nested = nested.baseContext
        return if (nested is Activity) nested else null
    }
}
