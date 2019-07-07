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


import android.content.Context
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProviders
import com.tunjid.androidbootstrap.recyclerview.ListManagerBuilder
import com.tunjid.fingergestures.R
import com.tunjid.fingergestures.adapters.PackageAdapter
import com.tunjid.fingergestures.baseclasses.MainActivityFragment
import com.tunjid.fingergestures.billing.PurchasesManager
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer
import com.tunjid.fingergestures.gestureconsumers.RotationGestureConsumer.ROTATION_APPS
import com.tunjid.fingergestures.viewholders.PackageViewHolder
import com.tunjid.fingergestures.viewmodels.AppViewModel
import com.tunjid.fingergestures.viewmodels.AppViewModel.EXCLUDED_ROTATION_LOCK
import com.tunjid.fingergestures.viewmodels.AppViewModel.ROTATION_LOCK

class PackageFragment : MainActivityFragment() {

    private lateinit var viewModel: AppViewModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        viewModel = ViewModelProviders.of(requireActivity()).get(AppViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_packages, container, false) as ViewGroup

        val toolbar = root.findViewById<Toolbar>(R.id.title_bar)
        val progressBar = root.findViewById<ProgressBar>(R.id.progress_bar)
        val listManager = ListManagerBuilder<PackageViewHolder, Void>()
                .withAdapter(PackageAdapter(false, viewModel.state.installedApps, object : PackageAdapter.PackageClickListener {
                    override fun onPackageClicked(packageName: String) = this@PackageFragment.onPackageClicked(packageName)
                }))
                .withRecyclerView(root.findViewById(R.id.options_list))
                .withLinearLayoutManager()
                .addDecoration(divider())
                .build()

        val persistedSet = arguments!!.getString(ARG_PERSISTED_SET)
        toolbar.title = RotationGestureConsumer.getInstance().getAddText(persistedSet)

        disposables.add(viewModel.updatedApps().subscribe({ result ->
            TransitionManager.beginDelayedTransition(root, AutoTransition())
            progressBar.visibility = View.GONE
            listManager.onDiff(result)
        }, Throwable::printStackTrace))

        return root
    }

    private fun onPackageClicked(packageName: String) {
        val args = arguments ?: return showSnackbar(R.string.generic_error)

        @RotationGestureConsumer.PersistedSet
        val persistedSet = args.getString(ARG_PERSISTED_SET)
                ?: return showSnackbar(R.string.generic_error)

        val added = RotationGestureConsumer.getInstance().addToSet(packageName, persistedSet)

        if (!added) {
            val context = requireContext()
            AlertDialog.Builder(context)
                    .setTitle(R.string.go_premium_title)
                    .setMessage(context.getString(R.string.go_premium_body, context.getString(R.string.auto_rotate_description)))
                    .setPositiveButton(R.string.continue_text) { _, _ -> purchase(PurchasesManager.PREMIUM_SKU) }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .show()
            return
        }

        toggleBottomSheet(false)
        currentAppFragment?.notifyItemChanged(if (ROTATION_APPS == persistedSet) ROTATION_LOCK else EXCLUDED_ROTATION_LOCK)
    }

    companion object {

        private const val ARG_PERSISTED_SET = "PERSISTED_SET"

        fun newInstance(@RotationGestureConsumer.PersistedSet preferenceName: String) =
                PackageFragment().apply { arguments = Bundle().apply { putString(ARG_PERSISTED_SET, preferenceName) } }
    }
}

