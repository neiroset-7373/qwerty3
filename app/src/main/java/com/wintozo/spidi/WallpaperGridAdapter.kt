package com.wintozo.spidi

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class WallpaperGridAdapter(
    private val context: Context,
    private val wallpapers: List<String>
) : BaseAdapter() {

    var selectedIndex = 0

    override fun getCount(): Int = wallpapers.size
    override fun getItem(position: Int): Any = wallpapers[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = if (convertView == null) {
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        } else {
            convertView as ImageView
        }

        val wallpaperId = context.resources.getIdentifier(
            wallpapers[position], "drawable", context.packageName
        )

        imageView.setImageResource(wallpaperId)
        imageView.isSelected = position == selectedIndex

        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return imageView
    }
}
