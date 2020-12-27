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
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tunjid.androidx.recyclerview.gridLayoutManager
import com.tunjid.androidx.recyclerview.listAdapterOf
import com.tunjid.androidx.recyclerview.viewbinding.BindingViewHolder
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderDelegate
import com.tunjid.androidx.recyclerview.viewbinding.viewHolderFrom
import com.tunjid.androidx.view.util.inflate
import com.tunjid.fingergestures.App
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.Item
import com.tunjid.fingergestures.databinding.ViewholderHorizontalListBinding
import com.tunjid.fingergestures.fragments.PackageFragment
import com.tunjid.fingergestures.models.Input
import com.tunjid.fingergestures.models.Package

private var BindingViewHolder<ViewholderHorizontalListBinding>.item by viewHolderDelegate<Item.Rotation>()
private var BindingViewHolder<ViewholderHorizontalListBinding>.listAdapter: ListAdapter<Package, PackageViewHolder> by viewHolderDelegate()

fun ViewGroup.rotation() = viewHolderFrom(ViewholderHorizontalListBinding::inflate).apply {
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
    binding.title.setOnClickListener { MaterialAlertDialogBuilder(it.context).setMessage(item.infoRes).show() }
    binding.add.setOnClickListener {
        when {
            !App.canWriteToSettings -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.permission_required).show()
            !item.canAutoRotate -> MaterialAlertDialogBuilder(itemView.context).setMessage(R.string.auto_rotate_prompt).show()
            item.preference != null -> item.preference
                ?.let(PackageFragment.Companion::newInstance)
                ?.let(Input.UiInteraction::ShowSheet)
                ?.let(item.input::accept)
        }
    }
    binding.itemList.apply {
        adapter = listAdapter
        layoutManager = gridLayoutManager(3)
    }
}

fun BindingViewHolder<ViewholderHorizontalListBinding>.bind(item: Item.Rotation) = binding.run {
    this@bind.item = item

    title.setText(item.titleRes)
    add.isVisible = item.preference != null
    listAdapter.submitList(item.items)

    if (!App.canWriteToSettings) item.input.accept(Input.Permission.Request.Settings)
}

private fun BindingViewHolder<ViewholderHorizontalListBinding>.onPackageClicked(app: ApplicationInfo) {
    val builder = MaterialAlertDialogBuilder(itemView.context)
    val editor = item.editor

    when {
        !App.canWriteToSettings -> builder.setMessage(R.string.permission_required)
        !item.canAutoRotate -> builder.setMessage(R.string.auto_rotate_prompt)
        item.unRemovablePackages.contains(app.packageName) -> builder.setMessage(R.string.auto_rotate_cannot_remove)
        editor != null -> builder.setTitle(item.removeText)
            .setPositiveButton(R.string.yes) { _, _ -> editor - app }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
        else -> builder.setTitle(R.string.app_rotation_exclude_title)
            .setPositiveButton(R.string.yes) { _, _ -> editor + app }
            .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
    }
    builder.show()
}
