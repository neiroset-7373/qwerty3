package com.wintozo.spidi

object GameState {
    data class Upgrade(
        val id: String,
        val name: String,
        val description: String,
        val cost: Long,
        val multiplier: Float,
        val isAutoClicker: Boolean,
        val icon: String,
        val duration: Long,
        var active: Boolean = false,
        var expiresAt: Long? = null
    )

    data class DailyGift(
        val day: Int,
        val reward: String,
        val coins: Int? = null,
        val specialReward: String? = null,
        var claimed: Boolean = false
    )

    data class MusicTrack(val id: String, val name: String, @androidx.annotation.RawRes val resId: Int)
    
    data class GameState(
        val coins: Long = 0,
        val totalCoins: Long = 0,
        val totalClicks: Long = 0,
        val baseClickPower: Long = 1,
        val baseAutoClick: Long = 0,
        val clickPower: Long = 1,
        val autoClickPerSecond: Long = 0,
        val deviceType: String = "pc",
        val selectedWallpaper: String = "",
        val selectedMusic: String = "track1",
        val musicEnabled: Boolean = true,
        val musicVolume: Float = 0.3f,
        val completedOnboarding: Boolean = false,
        val medal100k: Boolean = false,
        val goldenSpidi: Boolean = false,
        val lastDailyCheck: String = "",
        val currentDay: Int = 1,
        val upgrades: List<Upgrade> = emptyList(),
        val dailyGifts: List<DailyGift> = emptyList()
    )

    const val ONE_DAY_MS = 24 * 60 * 60 * 1000L

    val defaultUpgrades = listOf(
        Upgrade("mult_x2", "Множитель x2", "Удваивает силу клика на 24 часа", 250, 2f, false, "✕2", ONE_DAY_MS),
        Upgrade("mult_x3", "Множитель x3", "Утраивает силу клика на 24 часа", 450, 3f, false, "✕3", ONE_DAY_MS),
        Upgrade("mult_x4", "Множитель x4", "Учетверяет силу клика на 24 часа", 760, 4f, false, "✕4", ONE_DAY_MS),
        Upgrade("mult_x5", "Множитель x5", "Увеличивает силу клика в 5 раз на 24 часа", 1000, 5f, false, "✕5", ONE_DAY_MS),
        Upgrade("mult_x10", "Множитель x10", "Увеличивает силу клика в 10 раз на 24 часа", 10500, 10f, false, "✕10", ONE_DAY_MS),
        Upgrade("mult_x100", "Множитель x100", "Увеличивает силу клика в 100 раз на 24 часа", 11500, 100f, false, "✕100", ONE_DAY_MS),
        Upgrade("mult_x1000", "Множитель x1000", "Увеличивает силу клика в 1000 раз на 24 часа", 13000, 1000f, false, "✕1K", ONE_DAY_MS),
        Upgrade("autoclicker", "Автокликер", "Автоматически кликает 1 раз в секунду на 24 часа", 500, 1f, true, "🤖", ONE_DAY_MS)
    )

    val defaultGifts = listOf(
        DailyGift(1, "100 монет", coins = 100),
        DailyGift(2, "1 000 монет", coins = 1000),
        DailyGift(3, "5 000 монет", coins = 5000),
        DailyGift(4, "10 000 монет", coins = 10000),
        DailyGift(5, "Золотой Спиди (+50 сила клика навсегда)", specialReward = "golden_spidi", coins = 0)
    )

    val wallpapers = listOf(
        "wallpaper_1", "wallpaper_2", "wallpaper_3", "wallpaper_4",
        "wallpaper_5", "wallpaper_6", "wallpaper_7", "wallpaper_8"
    )

    val musicTracks = listOf(
        MusicTrack("track1", "Неофициальная мелодия", R.raw.unofficial),
        MusicTrack("track2", "Spidi Original", R.raw.spidi_official)
    )

    fun recalculateStats(state: GameState): GameState {
        val now = System.currentTimeMillis()
        var clickMultiplier = 1f
        var autoClick = 0L

        val updatedUpgrades = state.upgrades.map { up ->
            val expiresAt = up.expiresAt
            if (up.active && expiresAt != null && now < expiresAt) {
                if (up.isAutoClicker) {
                    autoClick += up.multiplier.toLong()
                } else {
                    clickMultiplier *= up.multiplier
                }
                up
            } else if (up.active && expiresAt != null && now >= expiresAt) {
                up.copy(active = false, expiresAt = null)
            } else {
                up
            }
        }

        val newClickPower = (state.baseClickPower * clickMultiplier).toLong()
        val newAutoClick = state.baseAutoClick + autoClick

        return state.copy(
            upgrades = updatedUpgrades,
            clickPower = newClickPower,
            autoClickPerSecond = newAutoClick
        )
    }

    fun formatNumber(n: Long): String {
        return when {
            n >= 1_000_000_000 -> "${(n / 1_000_000_000.0).format(2)}B"
            n >= 1_000_000 -> "${(n / 1_000_000.0).format(2)}M"
            n >= 1_000 -> "${(n / 1_000.0).format(1)}K"
            else -> n.toString()
        }
    }

    fun formatTimeRemaining(expiresAt: Long): String {
        val now = System.currentTimeMillis()
        val diff = expiresAt - now
        if (diff <= 0) return "Истёк"
        
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
        
        return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this).replace(',', '.')
