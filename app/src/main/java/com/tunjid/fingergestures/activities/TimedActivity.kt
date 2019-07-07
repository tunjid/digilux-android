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

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.R
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference

abstract class TimedActivity : AppCompatActivity() {

    protected lateinit var backgroundManager: BackgroundManager

    private var endTime = LocalTime.now()
    private val reference = AtomicReference<Disposable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brightness)

        backgroundManager = BackgroundManager.getInstance()

        val window = window
        window.setLayout(MATCH_PARENT, MATCH_PARENT)
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        updateEndTime()
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun finish() {
        val disposable = reference.get()
        if (disposable != null && !disposable.isDisposed) disposable.dispose()
        super.finish()
    }

    protected fun updateEndTime() {
        endTime = LocalTime.now().plus(backgroundManager.sliderDurationMillis.toLong(), ChronoUnit.MILLIS)
    }

    protected fun waitToFinish() {
        if (reference.get() != null) return
        reference.set(countdownDisposable())
    }

    private fun countdownDisposable(): Disposable {
        return Flowable.interval(backgroundManager.sliderDurationMillis.toLong(), MILLISECONDS).subscribe({ i ->
            if (LocalTime.now().isBefore(endTime)) return@subscribe
            reference.get().dispose()
            finish()
        }, Throwable::printStackTrace)
    }
}
