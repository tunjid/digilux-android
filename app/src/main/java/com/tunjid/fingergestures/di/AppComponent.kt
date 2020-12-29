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

package com.tunjid.fingergestures.di

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.tunjid.fingergestures.models.Broadcast
import dagger.Component
import io.reactivex.Flowable
import javax.inject.Singleton

@Singleton
@Component(modules = [
    AppModule::class,
    AppViewModelModule::class,
])
interface AppComponent {
    @AppContext
    fun appContext(): Context

    fun broadcasts(): Flowable<Broadcast>

    fun dependencies(): AppDependencies

    fun viewModelFactory(): ViewModelProvider.Factory
}