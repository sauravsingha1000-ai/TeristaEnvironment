package com.terista.environment.view.apps

import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cbfg.rvadapter.RVAdapter
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.R
import com.terista.environment.bean.AppInfo
import com.terista.environment.databinding.FragmentAppsBinding
import com.terista.environment.util.InjectionUtil
import com.terista.environment.util.ShortcutUtil
import com.terista.environment.util.inflate
import com.terista.environment.util.MemoryManager
import com.terista.environment.util.toast
import com.terista.environment.view.base.LoadingActivity
import java.util.*
import kotlin.math.abs

class AppsFragment : Fragment() {

    var userID: Int = 0
    private lateinit var viewModel: AppsViewModel
    private lateinit var mAdapter: RVAdapter<AppInfo>
    private val viewBinding: FragmentAppsBinding by inflate()

    private var popupMenu: PopupMenu? = null

    companion object {
        private const val TAG = "AppsFragment"

        fun newInstance(userID: Int): AppsFragment {
            val fragment = AppsFragment()
            fragment.arguments = bundleOf("userID" to userID)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(this, InjectionUtil.getAppsFactory())
                .get(AppsViewModel::class.java)
        userID = requireArguments().getInt("userID", 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewBinding.stateView.showEmpty()

        mAdapter =
            RVAdapter<AppInfo>(requireContext(), AppsAdapter())
                .bind(viewBinding.recyclerView)

        viewBinding.recyclerView.adapter = mAdapter

        // 🔥 UPDATED GRID (spacing + performance)
        val layoutManager = GridLayoutManager(requireContext(), 4)
        viewBinding.recyclerView.layoutManager = layoutManager

        // 🔥 NEW: spacing + smooth feel
        viewBinding.recyclerView.setPadding(12, 12, 12, 12)
        viewBinding.recyclerView.clipToPadding = false

        viewBinding.recyclerView.setHasFixedSize(true)
        viewBinding.recyclerView.setItemViewCacheSize(20)

        val touchCallBack = AppsTouchCallBack { from, to ->
            onItemMove(from, to)
            viewModel.updateSortLiveData.postValue(true)
        }

        ItemTouchHelper(touchCallBack)
            .attachToRecyclerView(viewBinding.recyclerView)

        mAdapter.setItemClickListener { _, data, _ ->
            showLoading()
            viewModel.launchApk(data.packageName, userID)
        }

        interceptTouch()
        setOnLongClick()

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun onStart() {
        super.onStart()

        BlackBoxCore.get().addServiceAvailableCallback {
            viewModel.getInstalledAppsWithRetry(userID)
        }

        viewModel.getInstalledAppsWithRetry(userID)
    }

    private fun interceptTouch() {
        val point = Point()

        viewBinding.recyclerView.setOnTouchListener { _, e ->

            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    point.set(0, 0)
                }

                MotionEvent.ACTION_UP -> {
                    popupMenu?.show()
                    popupMenu = null
                }
            }
            false
        }
    }

    private fun onItemMove(fromPosition: Int, toPosition: Int) {
        val items = mAdapter.getItems()

        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(items, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(items, i, i - 1)
            }
        }

        mAdapter.notifyItemMoved(fromPosition, toPosition)
    }

    private fun setOnLongClick() {
        mAdapter.setItemLongClickListener { view, data, _ ->
            popupMenu = PopupMenu(requireContext(), view).also {
                it.inflate(R.menu.app_menu)
                it.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.app_remove -> unInstallApk(data)
                        R.id.app_clear -> clearApk(data)
                        R.id.app_stop -> stopApk(data)
                        R.id.app_shortcut ->
                            ShortcutUtil.createShortcut(requireContext(), userID, data)
                    }
                    true
                }
                it.show()
            }
        }
    }

    private fun initData() {
        viewBinding.stateView.showLoading()

        viewModel.getInstalledApps(userID)

        viewModel.appsLiveData.observe(viewLifecycleOwner) {
            mAdapter.setItems(it)

            if (it.isEmpty()) {
                viewBinding.stateView.showEmpty()
            } else {
                viewBinding.stateView.showContent()
            }
        }
    }

    private fun unInstallApk(info: AppInfo) {
        MaterialDialog(requireContext()).show {
            title(R.string.uninstall_app)
            message(text = getString(R.string.uninstall_app_hint, info.name))
            positiveButton(R.string.done) {
                showLoading()
                viewModel.unInstall(info.packageName, userID)
            }
            negativeButton(R.string.cancel)
        }
    }

    private fun stopApk(info: AppInfo) {
        BlackBoxCore.get().stopPackage(info.packageName, userID)
        toast(getString(R.string.is_stop, info.name))
    }

    private fun clearApk(info: AppInfo) {
        showLoading()
        viewModel.clearApkData(info.packageName, userID)
    }

    fun installApk(source: String) {
        showLoading()
        viewModel.install(source, userID)
    }

    private fun showLoading() {
        (requireActivity() as? LoadingActivity)?.showLoading()
    }
}
