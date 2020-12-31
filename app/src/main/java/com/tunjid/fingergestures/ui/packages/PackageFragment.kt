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

package com.tunjid.fingergestures.ui.packages


import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.tunjid.androidx.core.delegates.fragmentArgs
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.ui.divider
import com.tunjid.fingergestures.ui.recursiveBottomSheetNavigator
import com.tunjid.fingergestures.databinding.FragmentPackagesBinding
import com.tunjid.fingergestures.di.activityViewModelFactory
import com.tunjid.fingergestures.di.viewModelFactory
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.models.Input
import com.tunjid.fingergestures.models.Unique
import com.tunjid.fingergestures.ui.main.MainViewModel
import com.tunjid.fingergestures.viewholders.PackageViewHolder

class PackageFragment : Fragment(R.layout.fragment_packages) {

    private val viewModel by viewModelFactory<PackageViewModel>()
    private val appViewModel by activityViewModelFactory<MainViewModel>()
    private var preference by fragmentArgs<RotationGestureConsumer.Preference>()
    private val bottomSheetNavigator by recursiveBottomSheetNavigator()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentPackagesBinding.bind(view)
        val listAdapter = listAdapterOf(
            initialItems = viewModel.state.value?.installedApps ?: listOf(),
            viewHolderCreator = { viewGroup, _ ->
                PackageViewHolder(
                    itemView = viewGroup.inflate(R.layout.viewholder_package_vertical),
                    listener = ::onPackageClicked
                )
            },
            viewHolderBinder = { holder, item, _ -> holder.bind(item) }
        )

        binding.optionsList.apply {
            adapter = listAdapter
            layoutManager = verticalLayoutManager()
            addItemDecoration(view.context.divider())
        }

        viewModel.state.apply {
            mapDistinct(PackageState::title)
                .observe(viewLifecycleOwner, binding.titleBar::setTitle)

            mapDistinct(PackageState::installedApps)
                .observe(viewLifecycleOwner) {
                    if (it.isEmpty()) listAdapter.submitList(it)
                    else listAdapter.submitList(it) {
                        binding.progressBar.isVisible = false
                        TransitionManager.beginDelayedTransition(view as ViewGroup, AutoTransition().addTarget(binding.progressBar))
                    }
                }

            mapDistinct(PackageState::needsPremium)
                .filter(Unique<Boolean>::item)
                .map(Unique<Boolean>::item)
                .filterUnhandledEvents()
                .observe(viewLifecycleOwner) { appViewModel.accept(Input.UiInteraction.GoPremium(R.string.auto_rotate_description)) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.accept(PackageInput.Fetch(preference))
    }

    private fun onPackageClicked(app: ApplicationInfo) {
        viewModel.accept(PackageInput.Add(preference = preference, app = app))
        bottomSheetNavigator.pop()
    }

    companion object {

        fun newInstance(preferenceName: RotationGestureConsumer.Preference) =
            PackageFragment().apply { this.preference = preferenceName }
    }
}

