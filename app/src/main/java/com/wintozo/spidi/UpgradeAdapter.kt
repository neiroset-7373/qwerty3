package com.wintozo.spidi

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class UpgradeAdapter(
    private val upgrades: List<GameState.Upgrade>,
    private val coins: Long,
    private val onBuy: (GameState.Upgrade) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = upgrades.size
    override fun getItem(position: Int): Any = upgrades[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = (convertView as? LinearLayout) ?: LinearLayout(parent?.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val upgrade = upgrades[position]
        val now = System.currentTimeMillis()
        val expiresAt = upgrade.expiresAt
        val isActive = upgrade.active && expiresAt != null && now < expiresAt
        val canAfford = coins >= upgrade.cost

        view.removeAllViews()

        val iconBg = LinearLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                marginEnd = 12
            }
            setBackgroundColor(if (isActive) 0xFF22c55e.toInt() else 0xFFe8f4fd.toInt())
            gravity = android.view.Gravity.CENTER
        }

        val iconText = TextView(view.context).apply {
            text = upgrade.icon
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
        }
        iconBg.addView(iconText)
        view.addView(iconBg)

        val infoLayout = LinearLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val nameText = TextView(view.context).apply {
            text = upgrade.name
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#1a1a1a"))
        }
        infoLayout.addView(nameText)

        val descText = TextView(view.context).apply {
            text = upgrade.description
            textSize = 11f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            setPadding(0, 4, 0, 0)
        }
        infoLayout.addView(descText)

        view.addView(infoLayout)

        val buyButton = LinearLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(70, 42)
            setBackgroundResource(R.drawable.button_primary_bg)
            gravity = android.view.Gravity.CENTER
            isClickable = !isActive
            isFocusable = !isActive
        }

        if (isActive) {
            val checkText = TextView(view.context).apply {
                text = "✓"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#22c55e"))
            }
            buyButton.addView(checkText)
            buyButton.setBackgroundColor(0xFFdcfce7.toInt())
        } else {
            val coinRow = LinearLayout(view.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
            }

            val coinIcon = ImageView(view.context).apply {
                setImageResource(R.drawable.coin)
                layoutParams = LinearLayout.LayoutParams(12, 12)
            }
            coinRow.addView(coinIcon)

            val costText = TextView(view.context).apply {
                text = upgrade.cost.toString()
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(android.graphics.Color.WHITE)
                setPadding(4, 0, 0, 0)
            }
            coinRow.addView(costText)
            buyButton.addView(coinRow)

            val buyText = TextView(view.context).apply {
                text = "Купить"
                textSize = 9f
                setTextColor(android.graphics.Color.WHITE)
                alpha = 0.8f
            }
            buyButton.addView(buyText)

            if (!canAfford) {
                buyButton.setBackgroundColor(0xFFF5F5F5.toInt())
            }

            buyButton.setOnClickListener {
                if (canAfford) onBuy(upgrade)
            }
        }

        view.addView(buyButton)

        return view
    }
}
