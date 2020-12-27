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


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.theartofdev.edmodo.cropper.CropImage
import com.tunjid.androidx.core.content.colorAt
import com.tunjid.androidx.core.delegates.fragmentArgs
import com.tunjid.androidx.recyclerview.addScrollListener
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.androidx.uidrivers.uiState
import com.tunjid.androidx.uidrivers.updatePartial
import com.tunjid.fingergestures.*
import com.tunjid.fingergestures.adapters.appAdapter
import com.tunjid.fingergestures.baseclasses.divider
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.databinding.FragmentHomeBinding
import com.tunjid.fingergestures.models.InsetFlags
import com.tunjid.fingergestures.viewmodels.AppViewModel
import com.tunjid.fingergestures.viewmodels.Input
import com.tunjid.fingergestures.viewmodels.Tab
import kotlin.math.abs

class AppFragment : Fragment(R.layout.fragment_home) {

    private var tab by fragmentArgs<Tab>()

    private val viewModel by activityViewModels<AppViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiState = uiState.copy(
            toolbarShows = true,
            toolbarOverlaps = true,
            toolbarMenuRes = R.menu.activity_main,
            toolbarMenuRefresher = ::refreshToolbarMenu,
            toolbarTitle = getString(R.string.app_name),
            showsBottomNav = true,
            insetFlags = InsetFlags.NO_BOTTOM,
            fabClickListener = { viewModel.accept(Input.Permission.Action.Clicked()) },
            navBarColor = requireContext().colorAt(
                if (BackgroundManager.instance.usesColoredNav()) R.color.colorPrimary
                else R.color.black
            ),
        )

        FragmentHomeBinding.bind(view).optionsList.apply {
            val items = viewModel.liveState
                .map { state -> state.items.filter { it.tab == tab } }
                .distinctUntilChanged()
            val listAdapter = appAdapter(items.value)

            layoutManager = verticalLayoutManager()
            adapter = listAdapter
            itemAnimator = null

            items.observe(viewLifecycleOwner, listAdapter::submitList)

            addItemDecoration(divider())
            addScrollListener { _, dy -> if (abs(dy) > 3) ::uiState.updatePartial { copy(toolbarShows = dy < 0) } }
        }
    }

    override fun onResume() {
        super.onResume()
        ::uiState.updatePartial { copy(toolbarShows = true) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (resultCode){
            Activity.RESULT_OK -> ::uiState.updatePartial { copy(snackbarText = getString(R.string.cancel_wallpaper)) }
            else -> WallpaperSelection.values().firstOrNull { it.code == requestCode }
                ?.let { cropImage(data!!.data, it) }
        }
    }

    private fun refreshToolbarMenu(menu: Menu) {
        val item = menu.findItem(R.id.action_start_trial)
        val isTrialVisible = !PurchasesManager.instance.isPremiumNotTrial

        if (item != null) item.isVisible = isTrialVisible
        if (isTrialVisible && item != null) item.actionView = TrialView(requireContext(), item)
    }

    fun cropImage(source: Uri?, selection: WallpaperSelection) {
        val backgroundManager = BackgroundManager.instance
        val aspectRatio = backgroundManager.screenAspectRatio ?: return

        val activity = activity ?: return
        val file = backgroundManager.getWallpaperFile(selection) ?: return
        val destination = Uri.fromFile(file)

        CropImage.activity(source)
            .setOutputUri(destination)
            .setFixAspectRatio(true)
            .setAspectRatio(aspectRatio[0], aspectRatio[1])
            .setMinCropWindowSize(100, 100)
            .setOutputCompressFormat(Bitmap.CompressFormat.PNG)
            .start(activity, this)
    }

    companion object {

        fun newInstance(tab: Tab): AppFragment = AppFragment().apply {
            this.tab = tab
        }
    }
}
