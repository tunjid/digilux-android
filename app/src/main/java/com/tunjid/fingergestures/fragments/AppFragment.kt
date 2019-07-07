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


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.theartofdev.edmodo.cropper.CropImage
import com.tunjid.androidbootstrap.core.abstractclasses.BaseFragment
import com.tunjid.androidbootstrap.recyclerview.ListManager
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.BackgroundManager
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.baseclasses.MainActivityFragment
import com.tunjid.fingergestures.viewholders.AppViewHolder
import com.tunjid.fingergestures.viewmodels.AppViewModel

import java.util.Arrays
import java.util.stream.IntStream
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders

import com.tunjid.fingergestures.BackgroundManager.DAY_WALLPAPER_PICK_CODE
import com.tunjid.fingergestures.BackgroundManager.NIGHT_WALLPAPER_PICK_CODE
import kotlin.math.abs

class AppFragment : MainActivityFragment(), AppAdapter.AppAdapterListener {

    lateinit var items: IntArray
        private set

    private lateinit var listManager: ListManager<AppViewHolder, Void>

    override fun getStableTag(): String = Arrays.toString(arguments!!.getIntArray(ARGS_ITEM))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        items = arguments!!.getIntArray(ARGS_ITEM)!!
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false) as ViewGroup

        listManager = ListManagerBuilder<AppViewHolder, Void>()
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withAdapter(AppAdapter(items, ViewModelProviders.of(requireActivity()).get(AppViewModel::class.java).state, this))
                .withLinearLayoutManager()
                .addDecoration(divider())
                .addScrollListener { _, dy -> if (abs(dy) > 3) toggleToolbar(dy < 0) }
                .build()

        return root
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
        listManager.clear()
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
        val index = IntStream.range(0, items.size).filter { i -> items[i] == index }.findFirst().orElse(-1)
        if (index != -1) listManager.notifyItemChanged(index)
    }

    fun notifyDataSetChanged() {
        listManager.notifyDataSetChanged()
    }

    @SuppressLint("CommitTransaction")
    override fun provideFragmentTransaction(fragmentTo: BaseFragment?): FragmentTransaction? {
        return fragmentManager!!.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
    }

    fun cropImage(source: Uri?, @BackgroundManager.WallpaperSelection selection: Int) {
        val backgroundManager = BackgroundManager.getInstance()
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

        private const val ARGS_ITEM = "items"
        private const val IMAGE_SELECTION = "image/*"

        fun newInstance(items: IntArray): AppFragment = AppFragment().apply {
            arguments = Bundle().apply { putIntArray(ARGS_ITEM, items) }
        }
    }
}
