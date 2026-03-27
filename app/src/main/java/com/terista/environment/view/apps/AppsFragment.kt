package com.terista.environment.view.apps

import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import cbfg.rvadapter.RVAdapter
import com.afollestad.materialdialogs.MaterialDialog
import top.niunaijun.blackbox.BlackBoxCore
import com.terista.environment.R
import com.terista.environment.bean.AppInfo
import com.terista.environment.databinding.FragmentAppsBinding
import com.terista.environment.util.*
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
        fun newInstance(userID: Int): AppsFragment {
            val fragment = AppsFragment()
            fragment.arguments = bundleOf("userID" to userID)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, InjectionUtil.getAppsFactory())[AppsViewModel::class.java]
        userID = requireArguments().getInt("userID", 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        viewBinding.stateView.showEmpty()

        mAdapter = RVAdapter<AppInfo>(requireContext(), AppsAdapter())
            .bind(viewBinding.recyclerView)

        viewBinding.recyclerView.adapter = mAdapter

        viewBinding.recyclerView.layoutAnimation =
    android.view.animation.AnimationUtils.loadLayoutAnimation(
        requireContext(),
        android.R.anim.slide_in_left
    )
viewBinding.recyclerView.scheduleLayoutAnimation()

        val layoutManager = GridLayoutManager(requireContext(), 4)
        viewBinding.recyclerView.layoutManager = layoutManager

        // 🔥 PREMIUM SPACING
        viewBinding.recyclerView.setPadding(12, 12, 12, 12)
        viewBinding.recyclerView.clipToPadding = false

        viewBinding.recyclerView.setHasFixedSize(true)

        // 🔥 FADE ANIMATION
        viewBinding.recyclerView.alpha = 0f
        viewBinding.recyclerView.animate().alpha(1f).setDuration(300).start()

        ItemTouchHelper(AppsTouchCallBack { from, to ->
            onItemMove(from, to)
            viewModel.updateSortLiveData.postValue(true)
        }).attachToRecyclerView(viewBinding.recyclerView)

        mAdapter.setItemClickListener { _, data, _ ->
            showLoading()
            viewModel.launchApk(data.packageName, userID)
        }

        setOnLongClick()

        return viewBinding.root
    }

    override fun onStart() {
        super.onStart()
        viewModel.getInstalledAppsWithRetry(userID)
    }

    private fun onItemMove(from: Int, to: Int) {
        val items = mAdapter.getItems()
        Collections.swap(items, from, to)
        mAdapter.notifyItemMoved(from, to)
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
                        R.id.app_shortcut -> ShortcutUtil.createShortcut(requireContext(), userID, data)
                    }
                    true
                }
                it.show()
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
