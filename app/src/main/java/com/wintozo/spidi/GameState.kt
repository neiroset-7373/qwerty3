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
        val duration: Long = 24 * 60 * 60 * 1000,
        var expiresAt: Long? = null,
        var active: Boolean = false
    )

    data class DailyGift(
        val day: Int,
        val reward: String,
        val coins: Int = 0,
        val specialReward: String? = null,
        var claimed: Boolean = false
    )

    data class MusicTrack(val id: String, val name: String, @androidx.annotation.RawRes val resId: Int)

    data class CustomMusic(val name: String, val url: String)

    data class GameState(
        var coins: Long = 0,
        var totalCoins: Long = 0,
        var totalClicks: Long = 0,
        var clickPower: Long = 1,
        var baseClickPower: Long = 1,
        var autoClickPerSecond: Long = 0,
        var baseAutoClick: Long = 0,
        var upgrades: List<Upgrade> = defaultUpgrades.toList(),
        var dailyGifts: List<DailyGift> = defaultGifts.toList(),
        var lastDailyCheck: String = "",
        var currentDay: Int = 1,
        var medal100k: Boolean = false,
        var goldenSpidi: Boolean = false,
        var selectedWallpaper: String = wallpapers[0],
        var selectedIconPack: String = "new",
        var selectedMusic: String = "track1",
        var musicEnabled: Boolean = true,
        var musicVolume: Float = 0.3f,
        var completedOnboarding: Boolean = false,
        var deviceType: String = "pc",
        var customWallpapers: MutableList<String> = mutableListOf(),
        var customMusic: MutableList<CustomMusic> = mutableListOf(),
        var lastSaved: String = ""
    )

    val wallpapers = listOf(
        "wallpeper_1", "wallpaper_2", "wallpeper_3", "wallpaper_4",
        "wallpaper_5", "wallpaper_6", "wallpaper_7", "wallpaper_8"
    )

    val iconUrls = mapOf(
        "game" to R.drawable.game,
        "upgrades" to R.drawable.upgrades,
        "gifts" to R.drawable.gifts,
        "settings" to R.drawable.settings,
        "clickPower" to R.drawable.sila_clicka,
        "upgradesSection" to R.drawable.upgrades,
        "autoClicker" to R.drawable.autoclicker,
        "settingsSection" to R.drawable.settings,
        "medal100k" to R.drawable.medal,
        "goldenSpidi" to R.drawable.golden_spidi,
        "coin" to R.drawable.coin,
        "clickBtn" to R.drawable.click_default,
        "phone" to R.drawable.phone,
        "tablet" to R.drawable.tablet,
        "pc" to R.drawable.computer
    )

    val logoUrl = R.drawable.logo

    val musicTracks = listOf(
        MusicTrack("track1", "Неофициальная мелодия", R.raw.unofficial),
        MusicTrack("track2", "Spidi Original", R.raw.spidi_official)
    )

    private val oneDayMs = 24 * 60 * 60 * 1000L

    val defaultUpgrades = listOf(
        Upgrade("mult_x2", "Множитель x2", "Удваивает силу клика на 24 часа", 250, 2f, false, "✕2", oneDayMs),
        Upgrade("mult_x3", "Множитель x3", "Утраивает силу клика на 24 часа", 450, 3f, false, "✕3", oneDayMs),
        Upgrade("mult_x4", "Множитель x4", "Учетверяет силу клика на 24 часа", 760, 4f, false, "✕4", oneDayMs),
        Upgrade("mult_x5", "Множитель x5", "Увеличивает силу клика в 5 раз на 24 часа", 1000, 5f, false, "✕5", oneDayMs),
        Upgrade("mult_x10", "Множитель x10", "Увеличивает силу клика в 10 раз на 24 часа", 10500, 10f, false, "✕10", oneDayMs),
        Upgrade("mult_x100", "Множитель x100", "Увеличивает силу клика в 100 раз на 24 часа", 11500, 100f, false, "✕100", oneDayMs),
        Upgrade("mult_x1000", "Множитель x1000", "Увеличивает силу клика в 1000 раз на 24 часа", 13000, 1000f, false, "✕1K", oneDayMs),
        Upgrade("autoclicker", "Автокликер", "Автоматически кликает 1 раз в секунду на 24 часа", 500, 1f, true, "🤖", oneDayMs)
    )

    val defaultGifts = listOf(
        DailyGift(1, "100 монет", 100),
        DailyGift(2, "1 000 монет", 1000),
        DailyGift(3, "5 000 монет", 5000),
        DailyGift(4, "10 000 монет", 10000),
        DailyGift(5, "Золотой Спиди (+50 сила клика навсегда)", specialReward = "golden_spidi")
    )

    fun formatNumber(n: Long): String {
        return when {
            n >= 1_000_000_000 -> String.format("%.2fB", n / 1_000_000_000.0)
            n >= 1_000_000 -> String.format("%.2fM", n / 1_000_000.0)
            n >= 1_000 -> String.format("%.1fK", n / 1_000.0)
            else -> n.toString()
        }
    }

    fun getTodayStr(): String {
        return java.text.SimpleDateFormat("EEE MMM dd yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    }

    fun formatTimeRemaining(expiresAt: Long): String {
        val now = System.currentTimeMillis()
        val diff = expiresAt - now
        if (diff <= 0) return "Истёк"
        val hours = diff / (1000 * 60 * 60)
        val minutes = (diff % (1000 * 60 * 60)) / (1000 * 60)
        return if (hours > 0) "${hours}ч ${minutes}м" else "${minutes}м"
    }

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
}

