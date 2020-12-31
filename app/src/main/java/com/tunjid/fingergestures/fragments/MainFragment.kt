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

package com.tunjid.fingergestures.fragments


import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.tunjid.androidx.core.delegates.fragmentArgs
import com.tunjid.androidx.recyclerview.addScrollListener
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.fingergestures.models.uiState
import com.tunjid.fingergestures.models.updatePartial
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.baseclasses.divider
import com.tunjid.fingergestures.databinding.FragmentHomeBinding
import com.tunjid.fingergestures.di.activityViewModelFactory
import com.tunjid.fingergestures.mapDistinct
import com.tunjid.fingergestures.models.AppState
import com.tunjid.fingergestures.ui.main.MainViewModel
import com.tunjid.fingergestures.ui.main.Tab
import com.tunjid.fingergestures.ui.main.mainAdapter
import kotlin.math.abs

private var MainFragment.tab by fragmentArgs<Tab>()
private fun MainFragment.tabItems(state: AppState?) = state?.items?.filter { it.tab == tab }

class MainFragment : Fragment(R.layout.fragment_home) {

    private val viewModel by activityViewModelFactory<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listAdapter = mainAdapter(tabItems(viewModel.state.value))

        FragmentHomeBinding.bind(view).optionsList.apply {
            layoutManager = verticalLayoutManager()
            adapter = listAdapter
            itemAnimator = null

            addItemDecoration(divider())
            addScrollListener { _, dy -> if (abs(dy) > 3) ::uiState.updatePartial { copy(toolbarShows = dy < 0) } }
        }

        viewModel.state
            .mapDistinct(::tabItems)
            .observe(viewLifecycleOwner, listAdapter::submitList)
    }

    override fun onResume() {
        super.onResume()
        ::uiState.updatePartial { copy(toolbarShows = true) }
    }

    companion object {
        fun newInstance(tab: Tab): MainFragment = MainFragment().apply {
            this.tab = tab
        }
    }
}
