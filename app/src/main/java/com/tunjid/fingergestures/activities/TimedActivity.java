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

package com.tunjid.fingergestures.activities;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Window;

import com.tunjid.fingergestures.BackgroundManager;
import com.tunjid.fingergestures.R;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class TimedActivity extends AppCompatActivity {

    protected BackgroundManager backgroundManager;

    private LocalTime endTime = LocalTime.now();
    private final AtomicReference<Disposable> reference = new AtomicReference<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brightness);

        backgroundManager = BackgroundManager.getInstance();

        Window window = getWindow();
        window.setLayout(MATCH_PARENT, MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        updateEndTime();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void finish() {
        Disposable disposable = reference.get();
        if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        super.finish();
    }

    protected void updateEndTime() {
        endTime = LocalTime.now().plus(backgroundManager.getSliderDurationMillis(), ChronoUnit.MILLIS);
    }

    protected void waitToFinish() {
        if (reference.get() != null) return;
        reference.set(countdownDisposable());
    }

    @NonNull
    private Disposable countdownDisposable() {
        return Flowable.interval(backgroundManager.getSliderDurationMillis(), MILLISECONDS).subscribe(i -> {
            if (LocalTime.now().isBefore(endTime)) return;
            reference.get().dispose();
            finish();
        }, throwable -> {});
    }
}
