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

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tunjid.fingergestures.viewmodels.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

inline fun <reified VM : ViewModel> Fragment.viewModelFactory() =
    viewModels<VM> { dagger.viewModelFactory }

inline fun <reified VM : ViewModel> Fragment.activityViewModelFactory() =
    activityViewModels<VM> { dagger.viewModelFactory }

inline fun <reified VM : ViewModel> FragmentActivity.viewModelFactory() =
    viewModels<VM> { dagger.viewModelFactory }

val Dagger.viewModelFactory get() = appComponent.viewModelFactory()

@Singleton
class AppViewModelFactory @Inject constructor(creators: ViewModelCreators) : ViewModelFactory(creators)

@Module
abstract class AppViewModelModule {

    @Binds
    abstract fun bindAppViewModelFactory(factory: AppViewModelFactory): ViewModelProvider.Factory

    @Binds @IntoMap @ViewModelKey(AppViewModel::class)
    abstract fun bindAppViewModel(splashViewModel: AppViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(ActionViewModel::class)
    abstract fun bindActionViewModel(viewModel: ActionViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(PopUpViewModel::class)
    abstract fun bindPopUpViewModel(viewModel: PopUpViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(PackageViewModel::class)
    abstract fun bindPersonPackageViewModel(viewModel: PackageViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(BrightnessViewModel::class)
    abstract fun bindBrightnessViewModel(viewModel: BrightnessViewModel): ViewModel
}