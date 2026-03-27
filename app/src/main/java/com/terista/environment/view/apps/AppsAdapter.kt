package com.terista.environment.view.apps

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import cbfg.rvadapter.RVHolder
import cbfg.rvadapter.RVHolderFactory
import com.terista.environment.R
import com.terista.environment.bean.AppInfo
import com.terista.environment.databinding.ItemAppBinding
import android.util.Log
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.ViewTreeObserver

class AppsAdapter : RVHolderFactory() {

    companion object {
        private const val TAG = "AppsAdapter"
        private const val MAX_ICON_SIZE = 96
        private val DEFAULT_ICON_COLOR = Color.parseColor("#CCCCCC")
    }

    override fun createViewHolder(parent: ViewGroup?, viewType: Int, item: Any): RVHolder<out Any> {
        return try {
            AppsVH(inflate(R.layout.item_app, parent))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}")
            FallbackAppsVH(inflate(R.layout.item_app, parent))
        }
    }

    class AppsVH(itemView: View) : RVHolder<AppInfo>(itemView) {
        val binding = ItemAppBinding.bind(itemView)

        init {
            binding.icon.scaleType = ImageView.ScaleType.CENTER_CROP

            // 🔥 ULTRA PREMIUM CLICK ANIMATION
            itemView.setOnClickListener {
                it.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(80)
                    .withEndAction {
                        it.animate().scaleX(1f).scaleY(1f).duration = 80
                    }
            }

            itemView.viewTreeObserver.addOnPreDrawListener(object :
                ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    itemView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }

        override fun setContent(item: AppInfo, isSelected: Boolean, payload: Any?) {
            try {
                setIconSafely(item.icon)
                binding.name.text = item.name ?: "Unknown App"

                binding.cornerLabel.visibility =
                    if (item.isXpModule) View.VISIBLE else View.INVISIBLE

            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                setSafeDefaults()
            }
        }

        private fun setIconSafely(icon: Drawable?) {
            try {
                if (icon != null) {
                    binding.icon.setImageDrawable(optimizeIcon(icon))
                } else {
                    binding.icon.setImageDrawable(ColorDrawable(DEFAULT_ICON_COLOR))
                }
            } catch (e: Exception) {
                binding.icon.setImageDrawable(ColorDrawable(DEFAULT_ICON_COLOR))
            }
        }

        private fun optimizeIcon(icon: Drawable): Drawable {
            return try {
                if (icon is BitmapDrawable) {
                    val bitmap = icon.bitmap
                    val scaled = Bitmap.createScaledBitmap(bitmap, MAX_ICON_SIZE, MAX_ICON_SIZE, true)
                    BitmapDrawable(itemView.resources, scaled)
                } else icon
            } catch (e: Exception) {
                icon
            }
        }

        private fun setSafeDefaults() {
            binding.icon.setImageDrawable(ColorDrawable(DEFAULT_ICON_COLOR))
            binding.name.text = "Unknown App"
            binding.cornerLabel.visibility = View.INVISIBLE
        }
    }

    class FallbackAppsVH(itemView: View) : RVHolder<AppInfo>(itemView) {
        val binding = ItemAppBinding.bind(itemView)

        override fun setContent(item: AppInfo, isSelected: Boolean, payload: Any?) {
            binding.icon.setImageDrawable(ColorDrawable(DEFAULT_ICON_COLOR))
            binding.name.text = item.name ?: "Unknown App"
            binding.cornerLabel.visibility = View.INVISIBLE
        }
    }
}
