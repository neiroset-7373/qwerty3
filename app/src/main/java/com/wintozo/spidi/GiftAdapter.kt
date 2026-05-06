package com.wintozo.spidi

import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class GiftAdapter(
    private val gifts: List<GameState.DailyGift>,
    private val currentDay: Int,
    private val canClaim: Boolean,
    private val today: String,
    private val onClaim: (GameState.DailyGift) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = gifts.size
    override fun getItem(position: Int): Any = gifts[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = (convertView as? LinearLayout) ?: LinearLayout(parent?.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val gift = gifts[position]
        val isClaimed = gift.claimed
        val isCurrentDay = gift.day == currentDay && canClaim

        view.removeAllViews()

        val dayBg = LinearLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                marginEnd = 12
            }
            setBackgroundColor(if (isClaimed) 0xFF22c55e.toInt() else if (isCurrentDay) 0xFF3b82f6.toInt() else 0xFF999999.toInt())
            gravity = android.view.Gravity.CENTER
        }

        val dayText = TextView(view.context).apply {
            text = if (isClaimed) "✓" else gift.day.toString()
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.WHITE)
        }
        dayBg.addView(dayText)
        view.addView(dayBg)

        val infoLayout = LinearLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val dayLabelRow = LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val dayLabel = TextView(view.context).apply {
            text = "День ${gift.day}"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#1a1a1a"))
        }
        dayLabelRow.addView(dayLabel)

        if (isCurrentDay) {
            val nowLabel = TextView(view.context).apply {
                text = " Сейчас!"
                textSize = 10f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(android.graphics.Color.parseColor("#3b82f6"))
                setBackgroundResource(R.drawable.button_secondary_bg)
                setPadding(8, 4, 8, 4)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 8
                }
            }
            dayLabelRow.addView(nowLabel)
        }
        infoLayout.addView(dayLabelRow)

        val rewardRow = LinearLayout(view.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 0)
        }

        val rewardIcon = ImageView(view.context).apply {
            setImageResource(if (gift.specialReward == "golden_spidi") R.drawable.golden_spidi else R.drawable.coin)
            layoutParams = LinearLayout.LayoutParams(20, 20)
        }
        rewardRow.addView(rewardIcon)

        val rewardText = TextView(view.context).apply {
            text = gift.reward
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#4b5563"))
            setPadding(8, 0, 0, 0)
        }
        rewardRow.addView(rewardText)
        infoLayout.addView(rewardRow)

        view.addView(infoLayout)

        val claimButton = LinearLayout(view.context).apply {
            layoutParams = LinearLayout.LayoutParams(70, 40)
            gravity = android.view.Gravity.CENTER
        }

        if (isClaimed) {
            claimButton.setBackgroundColor(0xFFdcfce7.toInt())
            val checkText = TextView(view.context).apply {
                text = "✓"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#22c55e"))
            }
            claimButton.addView(checkText)
        } else if (isCurrentDay) {
            claimButton.setBackgroundResource(R.drawable.button_primary_bg)
            val giftIcon = ImageView(view.context).apply {
                setImageResource(R.drawable.gifts)
                layoutParams = LinearLayout.LayoutParams(14, 14)
            }
            claimButton.addView(giftIcon)

            val claimText = TextView(view.context).apply {
                text = "Взять"
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(android.graphics.Color.WHITE)
                setPadding(4, 0, 0, 0)
            }
            claimButton.addView(claimText)

            claimButton.setOnClickListener {
                onClaim(gift)
            }
        } else {
            claimButton.setBackgroundColor(0xFFF5F5F5.toInt())
            val lockText = TextView(view.context).apply {
                text = "🔒"
                textSize = 14f
            }
            claimButton.addView(lockText)
        }

        view.addView(claimButton)

        return view
    }
}
