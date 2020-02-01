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
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.theartofdev.edmodo.cropper.CropImage
import com.tunjid.androidx.core.components.args
import com.tunjid.androidx.navigation.Navigator
import com.tunjid.androidx.recyclerview.addScrollListener
import com.tunjid.androidx.recyclerview.notifyDataSetChanged
import com.tunjid.androidx.recyclerview.notifyItemChanged
import com.tunjid.androidx.recyclerview.verticalLayoutManager
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.BackgroundManager.Companion.DAY_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.BackgroundManager.Companion.NIGHT_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapterListener
import com.tunjid.fingergestures.adapters.appAdapter
import com.tunjid.fingergestures.adapters.padded
import com.tunjid.fingergestures.baseclasses.MainActivityFragment
import com.tunjid.fingergestures.viewmodels.AppViewModel
import kotlin.math.abs

class AppFragment : MainActivityFragment(R.layout.fragment_home),
        Navigator.TagProvider,
        AppAdapterListener {

    var items by args<IntArray>()
        private set

    private var recyclerView: RecyclerView? = null

    private val viewModel by activityViewModels<AppViewModel>()

    override val stableTag: String get() = items.contentToString()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<RecyclerView>(R.id.options_list).apply {
            layoutManager = verticalLayoutManager()
            adapter = appAdapter(items, viewModel.liveState, this@AppFragment).padded()
            addItemDecoration(divider())
            addScrollListener { _, dy -> if (abs(dy) > 3) toggleToolbar(dy < 0) }
        }
    }

    override fun onResume() {
        super.onResume()
        notifyDataSetChanged()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toggleToolbar(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activity ?: return

        when {
            resultCode != Activity.RESULT_OK -> showSnackbar(R.string.cancel_wallpaper)
            requestCode == DAY_WALLPAPER_PICK_CODE || requestCode == NIGHT_WALLPAPER_PICK_CODE -> cropImage(data!!.data, requestCode)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
    }

    override fun pickWallpaper(@BackgroundManager.WallpaperSelection selection: Int) {
        if (!App.hasStoragePermission)
            showSnackbar(R.string.enable_storage_settings)
        else
            startActivityForResult(Intent.createChooser(Intent()
                    .setType(IMAGE_SELECTION)
                    .setAction(Intent.ACTION_GET_CONTENT), ""), selection)
    }

    override fun notifyItemChanged(@AppViewModel.AdapterIndex index: Int) {
        val listIndex = items.indexOf(index)
        if (listIndex != -1) recyclerView?.notifyItemChanged(listIndex)
    }

    fun notifyDataSetChanged() {
        recyclerView?.notifyDataSetChanged()
    }

    fun cropImage(source: Uri?, @BackgroundManager.WallpaperSelection selection: Int) {
        val backgroundManager = BackgroundManager.instance
        val aspectRatio = backgroundManager.screenAspectRatio ?: return

        val activity = activity ?: return

        val file = backgroundManager.getWallpaperFile(selection, activity)
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

        private const val IMAGE_SELECTION = "image/*"

        fun newInstance(items: IntArray): AppFragment = AppFragment().apply {
            this.items = items
        }
    }
}
