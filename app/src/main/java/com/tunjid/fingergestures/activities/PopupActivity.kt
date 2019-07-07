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

package com.tunjid.fingergestures.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.card.MaterialCardView
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.PopUpGestureConsumer
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.ActionAdapter
import com.tunjid.fingergestures.gestureconsumers.GestureConsumer
import com.tunjid.fingergestures.gestureconsumers.GestureMapper
import com.tunjid.fingergestures.viewholders.ActionViewHolder
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class PopupActivity : AppCompatActivity() {

    private lateinit var disposables: CompositeDisposable

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        finish()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popup)

        disposables = CompositeDisposable()

        val window = window
        window.setLayout(MATCH_PARENT, MATCH_PARENT)

        val items = ArrayList<Int>()
        val spanSizer = AtomicInteger(0)
        val backgroundManager = BackgroundManager.getInstance()

        val listManager = ListManagerBuilder<ActionViewHolder, Void>()
                .withRecyclerView(findViewById(R.id.item_list))
                .withGridLayoutManager(6)
                .withAdapter(ActionAdapter(true, true, items, ActionAdapter.ActionClickListener { this.onActionClicked(it) }))
                .onLayoutManager { manager ->
                    (manager as GridLayoutManager).spanSizeLookup = object : SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return spanSizer.get()
                        }
                    }
                }
                .build()

        val text = findViewById<TextView>(R.id.text)
        text.setTextColor(backgroundManager.sliderColor)
        this.findViewById<MaterialCardView>(R.id.card).setCardBackgroundColor(backgroundManager.backgroundColor)

        disposables.add(App.diff(items) { PopUpGestureConsumer.getInstance().list }.subscribe({ result ->
            val size = items.size
            listManager.onDiff(result)
            spanSizer.set(if (size == 1) 6 else if (size == 2) 3 else 2)
            text.visibility = if (size == 0) VISIBLE else GONE
        }, { it.printStackTrace() }))

        findViewById<View>(R.id.constraint_layout).setOnTouchListener { _, _ ->
            finish()
            true
        }
    }

    override fun onResume() {
        super.onResume()

        if (PopUpGestureConsumer.getInstance().shouldAnimatePopup())
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun finish() {
        super.finish()
        val shouldAnimate = PopUpGestureConsumer.getInstance().shouldAnimatePopup()
        if (shouldAnimate)
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
        else
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        disposables.clear()
    }

    private fun onActionClicked(@GestureConsumer.GestureAction action: Int) {
        finish()
        GestureMapper.getInstance().performAction(action)
    }
}

