package com.wintozo.spidi

import android.content.Context
import android.content.SharedPreferences
import com.wintozo.spidi.GameState.Upgrade
import com.wintozo.spidi.GameState.DailyGift

object SettingsManager {
    private const val PREFS_NAME = "spidi_clicker_v31_prefs"
    private const val KEY_COINS = "coins"
    private const val KEY_TOTAL_COINS = "totalCoins"
    private const val KEY_TOTAL_CLICKS = "totalClicks"
    private const val KEY_BASE_CLICK_POWER = "baseClickPower"
    private const val KEY_AUTO_CLICK = "baseAutoClick"
    private const val KEY_DEVICE_TYPE = "deviceType"
    private const val KEY_WALLPAPER = "selectedWallpaper"
    private const val KEY_MUSIC = "selectedMusic"
    private const val KEY_MUSIC_ENABLED = "musicEnabled"
    private const val KEY_MUSIC_VOLUME = "musicVolume"
    private const val KEY_ONBOARDING = "completedOnboarding"
    private const val KEY_MEDAL_100K = "medal100k"
    private const val KEY_GOLDEN_SPIDI = "goldenSpidi"
    private const val KEY_LAST_DAILY_CHECK = "lastDailyCheck"
    private const val KEY_CURRENT_DAY = "currentDay"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveState(context: Context, state: GameState) {
        val prefs = getPrefs(context)
        with(prefs.edit()) {
            putLong(KEY_COINS, state.coins)
            putLong(KEY_TOTAL_COINS, state.totalCoins)
            putLong(KEY_TOTAL_CLICKS, state.totalClicks)
            putLong(KEY_BASE_CLICK_POWER, state.baseClickPower)
            putLong(KEY_AUTO_CLICK, state.baseAutoClick)
            putLong("clickPower", state.clickPower)
            putLong("autoClickPerSecond", state.autoClickPerSecond)
            putString(KEY_DEVICE_TYPE, state.deviceType)
            putString(KEY_WALLPAPER, state.selectedWallpaper)
            putString(KEY_MUSIC, state.selectedMusic)
            putBoolean(KEY_MUSIC_ENABLED, state.musicEnabled)
            putFloat(KEY_MUSIC_VOLUME, state.musicVolume)
            putBoolean(KEY_ONBOARDING, state.completedOnboarding)
            putBoolean(KEY_MEDAL_100K, state.medal100k)
            putBoolean(KEY_GOLDEN_SPIDI, state.goldenSpidi)
            putString(KEY_LAST_DAILY_CHECK, state.lastDailyCheck)
            putInt(KEY_CURRENT_DAY, state.currentDay)
            
            // Save upgrades
            putInt("upgrades_count", state.upgrades.size)
            state.upgrades.forEachIndexed { index, upgrade ->
                putString("upgrade_${index}_id", upgrade.id)
                putString("upgrade_${index}_name", upgrade.name)
                putString("upgrade_${index}_desc", upgrade.description)
                putLong("upgrade_${index}_cost", upgrade.cost)
                putFloat("upgrade_${index}_mult", upgrade.multiplier)
                putBoolean("upgrade_${index}_isAuto", upgrade.isAutoClicker)
                putString("upgrade_${index}_icon", upgrade.icon)
                putLong("upgrade_${index}_duration", upgrade.duration)
                putBoolean("upgrade_${index}_active", upgrade.active)
                putLong("upgrade_${index}_expires", upgrade.expiresAt ?: 0)
            }
            
            // Save daily gifts
            putInt("gifts_count", state.dailyGifts.size)
            state.dailyGifts.forEachIndexed { index, gift ->
                putInt("gift_${index}_day", gift.day)
                putString("gift_${index}_reward", gift.reward)
                putInt("gift_${index}_coins", gift.coins ?: 0)
                putString("gift_${index}_special", gift.specialReward ?: "")
                putBoolean("gift_${index}_claimed", gift.claimed)
            }
            
            apply()
        }
    }

    fun loadState(context: Context): GameState {
        val prefs = getPrefs(context)
        
        val upgrades = mutableListOf<Upgrade>()
        val upgradesCount = prefs.getInt("upgrades_count", GameState.defaultUpgrades.size)
        for (i in 0 until upgradesCount) {
            val id = prefs.getString("upgrade_${i}_id", GameState.defaultUpgrades[i].id) ?: GameState.defaultUpgrades[i].id
            upgrades.add(Upgrade(
                id = id,
                name = prefs.getString("upgrade_${i}_name", GameState.defaultUpgrades[i].name)!!,
                description = prefs.getString("upgrade_${i}_desc", GameState.defaultUpgrades[i].description)!!,
                cost = prefs.getLong("upgrade_${i}_cost", GameState.defaultUpgrades[i].cost),
                multiplier = prefs.getFloat("upgrade_${i}_mult", GameState.defaultUpgrades[i].multiplier),
                isAutoClicker = prefs.getBoolean("upgrade_${i}_isAuto", GameState.defaultUpgrades[i].isAutoClicker),
                icon = prefs.getString("upgrade_${i}_icon", GameState.defaultUpgrades[i].icon)!!,
                duration = prefs.getLong("upgrade_${i}_duration", GameState.defaultUpgrades[i].duration),
                active = prefs.getBoolean("upgrade_${i}_active", GameState.defaultUpgrades[i].active),
                expiresAt = prefs.getLong("upgrade_${i}_expires", 0).takeIf { it > 0 }
            ))
        }
        if (upgrades.isEmpty()) upgrades.addAll(GameState.defaultUpgrades)

        val dailyGifts = mutableListOf<DailyGift>()
        val giftsCount = prefs.getInt("gifts_count", GameState.defaultGifts.size)
        for (i in 0 until giftsCount) {
            dailyGifts.add(DailyGift(
                day = prefs.getInt("gift_${i}_day", GameState.defaultGifts[i].day),
                reward = prefs.getString("gift_${i}_reward", GameState.defaultGifts[i].reward)!!,
                coins = prefs.getInt("gift_${i}_coins", GameState.defaultGifts[i].coins ?: 0),
                specialReward = prefs.getString("gift_${i}_special", GameState.defaultGifts[i].specialReward)?.takeIf { it.isNotEmpty() },
                claimed = prefs.getBoolean("gift_${i}_claimed", GameState.defaultGifts[i].claimed)
            ))
        }
        if (dailyGifts.isEmpty()) dailyGifts.addAll(GameState.defaultGifts)

        return GameState(
            coins = prefs.getLong(KEY_COINS, 0),
            totalCoins = prefs.getLong(KEY_TOTAL_COINS, 0),
            totalClicks = prefs.getLong(KEY_TOTAL_CLICKS, 0),
            baseClickPower = prefs.getLong(KEY_BASE_CLICK_POWER, 1),
            baseAutoClick = prefs.getLong(KEY_AUTO_CLICK, 0),
            clickPower = prefs.getLong("clickPower", 1),
            autoClickPerSecond = prefs.getLong("autoClickPerSecond", 0),
            deviceType = prefs.getString(KEY_DEVICE_TYPE, "pc")!!,
            selectedWallpaper = prefs.getString(KEY_WALLPAPER, GameState.wallpapers[0])!!,
            selectedMusic = prefs.getString(KEY_MUSIC, GameState.musicTracks[0].id)!!,
            musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true),
            musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.3f),
            completedOnboarding = prefs.getBoolean(KEY_ONBOARDING, false),
            medal100k = prefs.getBoolean(KEY_MEDAL_100K, false),
            goldenSpidi = prefs.getBoolean(KEY_GOLDEN_SPIDI, false),
            lastDailyCheck = prefs.getString(KEY_LAST_DAILY_CHECK, "")!!,
            currentDay = prefs.getInt(KEY_CURRENT_DAY, 1),
            upgrades = upgrades,
            dailyGifts = dailyGifts
        )
    }

    fun resetState(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
    }
}
