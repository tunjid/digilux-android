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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.observe
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.gridLayoutManager
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.activities.MainActivity
import com.tunjid.fingergestures.adapters.AppAdapterListener
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderHorizontalListBinding
import com.tunjid.fingergestures.fragments.PackageFragment
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.lifecycleOwner
import com.tunjid.fingergestures.models.Package
import com.tunjid.fingergestures.viewmodels.Input

private var BindingViewHolder<ViewholderHorizontalListBinding>.item by viewHolderDelegate<Item.Rotation>()
private var BindingViewHolder<ViewholderHorizontalListBinding>.listAdapter: ListAdapter<Package, PackageViewHolder> by viewHolderDelegate()

fun ViewGroup.rotation() = viewHolderFrom(ViewholderHorizontalListBinding::inflate).apply {
    binding.root.setOnClickListener { MaterialAlertDialogBuilder(it.context).setMessage(item.infoRes).show() }
    binding.add.setOnClickListener {
        when {
            !App.canWriteToSettings() -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.permission_required).show()
            !RotationGestureConsumer.instance.canAutoRotate() -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.auto_rotate_prompt).show()
            item.persistedSet != null -> item.persistedSet
                ?.let(PackageFragment.Companion::newInstance)
                ?.let(Input::ShowSheet)
                ?.let(item.input::accept)
        }
    }
    listAdapter = listAdapterOf(
        initialItems = listOf(),
        viewHolderCreator = { viewGroup, _ ->
            PackageViewHolder(
                itemView = viewGroup.inflate(R.layout.viewholder_package_horizontal),
                listener = ::onPackageClicked
            )
        },
        viewHolderBinder = { holder, item, _ -> holder.bind(item) }
    )
    binding.itemList.apply {
        adapter = listAdapter
        layoutManager = gridLayoutManager(3)
    }
}

fun BindingViewHolder<ViewholderHorizontalListBinding>.bind(item: Item.Rotation) = binding.run {
    this@bind.item = item

    title.setText(item.titleRes)
    add.isVisible = item.persistedSet != null
    listAdapter.submitList(item.items)
}

class RotationViewHolder(itemView: View,
    @param:RotationGestureConsumer.PersistedSet
    val persistedSet: String?,
    @StringRes titleRes: Int,
    @StringRes infoRes: Int,
    items: LiveData<List<Package>>,
    listener: AppAdapterListener
) : AppViewHolder(itemView, listener) {

    init {
        itemView.findViewById<TextView>(R.id.title).apply {
            setText(titleRes)
            setOnClickListener {
                MaterialAlertDialogBuilder(it.context).setMessage(infoRes).show()
            }
        }

        itemView.findViewById<View>(R.id.add).apply {
            isVisible = persistedSet != null
            setOnClickListener {
                when {
                    !App.canWriteToSettings() -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.permission_required).show()
                    !RotationGestureConsumer.instance.canAutoRotate() -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.auto_rotate_prompt).show()
                    persistedSet != null -> listener.showBottomSheetFragment(PackageFragment.newInstance(persistedSet))
                }
            }
        }

        itemView.findViewById<RecyclerView>(R.id.item_list).run {
            layoutManager = gridLayoutManager(3)
            adapter = listAdapterOf(
                initialItems = items.value ?: listOf(),
                viewHolderCreator = { viewGroup, _ ->
                    PackageViewHolder(
                        itemView = viewGroup.inflate(R.layout.viewholder_package_horizontal),
                        listener = this@RotationViewHolder::onPackageClicked
                    )
                },
                viewHolderBinder = { holder, item, _ -> holder.bind(item) }
            ).also { items.observe(lifecycleOwner, it::submitList) }
        }
    }

    private fun onPackageClicked(packageName: String) {
        val gestureConsumer = RotationGestureConsumer.instance
        val builder = MaterialAlertDialogBuilder(itemView.context)

        when {
            !App.canWriteToSettings() -> builder.setMessage(R.string.permission_required)
            !gestureConsumer.canAutoRotate() -> builder.setMessage(R.string.auto_rotate_prompt)
            !gestureConsumer.isRemovable(packageName) -> builder.setMessage(R.string.auto_rotate_cannot_remove)
            persistedSet != null -> builder.setTitle(gestureConsumer.getRemoveText(persistedSet))
                .setPositiveButton(R.string.yes) { _, _ ->
                    gestureConsumer.removeFromSet(packageName, persistedSet)
                }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            else -> builder.setTitle(R.string.app_rotation_exclude_title)
                .setPositiveButton(R.string.yes) { _, _ ->
                    gestureConsumer.addToSet(packageName, RotationGestureConsumer.EXCLUDED_APPS)
                }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
        }
        builder.show()
    }

    override fun bind() {
        super.bind()
        if (!App.canWriteToSettings()) listener.requestPermission(MainActivity.SETTINGS_CODE)
    }
}

private fun BindingViewHolder<ViewholderHorizontalListBinding>.onPackageClicked(packageName: String) {
    val gestureConsumer = RotationGestureConsumer.instance
    val builder = MaterialAlertDialogBuilder(itemView.context)
    val persistedSet = item.persistedSet

    when {
        !App.canWriteToSettings() -> builder.setMessage(R.string.permission_required)
        !gestureConsumer.canAutoRotate() -> builder.setMessage(R.string.auto_rotate_prompt)
        !gestureConsumer.isRemovable(packageName) -> builder.setMessage(R.string.auto_rotate_cannot_remove)
        persistedSet != null -> builder.setTitle(gestureConsumer.getRemoveText(persistedSet))
            .setPositiveButton(R.string.yes) { _, _ ->
                gestureConsumer.removeFromSet(packageName, persistedSet)
            }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
        else -> builder.setTitle(R.string.app_rotation_exclude_title)
            .setPositiveButton(R.string.yes) { _, _ ->
                gestureConsumer.addToSet(packageName, RotationGestureConsumer.EXCLUDED_APPS)
            }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
    }
    builder.show()
}