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

package com.tunjid.fingergestures.viewholders

import android.content.pm.ApplicationInfo
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.tunjid.androidbootstrap.recyclerview.ListManager
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.AppAdapter
import com.tunjid.fingergestures.adapters.PackageAdapter
import com.tunjid.fingergestures.fragments.PackageFragment
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS

class RotationViewHolder(itemView: View,
                         @param:RotationGestureConsumer.PersistedSet override val sizeCacheKey: String,
                         items: List<ApplicationInfo>,
                         listener: AppAdapter.AppAdapterListener
) : DiffViewHolder<ApplicationInfo>(itemView, items, listener) {

    override val listSupplier: () -> List<ApplicationInfo>
        get() = { RotationGestureConsumer.getInstance().getList(sizeCacheKey) }

    private val packageClickListener = object : PackageAdapter.PackageClickListener {
        override fun onPackageClicked(packageName: String) {
            val gestureConsumer = RotationGestureConsumer.getInstance()
            val builder = AlertDialog.Builder(itemView.context)

            when {
                !App.canWriteToSettings() -> builder.setMessage(R.string.permission_required)
                !gestureConsumer.canAutoRotate() -> builder.setMessage(R.string.auto_rotate_prompt)
                !gestureConsumer.isRemovable(packageName) -> builder.setMessage(R.string.auto_rotate_cannot_remove)
                else -> builder.setTitle(gestureConsumer.getRemoveText(sizeCacheKey))
                        .setPositiveButton(R.string.yes) { _, _ ->
                            gestureConsumer.removeFromSet(packageName, sizeCacheKey)
                            bind()
                        }
                        .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            }

            builder.show()
        }
    }

    init {
        itemView.findViewById<View>(R.id.add).setOnClickListener {
            when {
                !App.canWriteToSettings() -> AlertDialog.Builder(itemView.context).setMessage(R.string.permission_required).show()
                !RotationGestureConsumer.getInstance().canAutoRotate() -> AlertDialog.Builder(itemView.context).setMessage(R.string.auto_rotate_prompt).show()
                else -> adapterListener.showBottomSheetFragment(PackageFragment.newInstance(sizeCacheKey))
            }
        }

        val title = itemView.findViewById<TextView>(R.id.title)
        val isRotationList = ROTATION_APPS == sizeCacheKey

        title.setText(if (isRotationList) R.string.auto_rotate_apps else R.string.auto_rotate_apps_excluded)
        title.setOnClickListener {
            AlertDialog.Builder(itemView.context)
                    .setMessage(
                            if (isRotationList) R.string.auto_rotate_description
                            else R.string.auto_rotate_ignored_description
                    )
                    .show()
        }
    }

    override fun bind() {
        super.bind()

        diff()
        if (!App.canWriteToSettings()) adapterListener.requestPermission(MainActivity.SETTINGS_CODE)
    }

    override fun diffHash(item: ApplicationInfo): String = item.packageName

    override fun createListManager(itemView: View): ListManager<*, Void> {
        return ListManagerBuilder<PackageViewHolder, Void>()
                .withAdapter(PackageAdapter(true, items, packageClickListener))
                .withRecyclerView(itemView.findViewById(R.id.item_list))
                .withGridLayoutManager(3)
                .build()
    }
}
