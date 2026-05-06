package com.wintozo.spidi

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.ListView

class GameActivity : AppCompatActivity() {

    private var state: GameState.GameState = GameState.GameState()
    private var currentTab: String = "game"
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoClickRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        state = SettingsManager.loadState(this)
        recalculateStats()
        setupWallpaper()
        setupTabs()
        showTab("game")
        updateHeader()
        startAutoClick()
        startSaveTimer()
        setupMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoClick()
        stopMusic()
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.release()
    }

    override fun onBackPressed() {
        // Prevent going back from game
        super.onBackPressed()
        finish()
    }

    private fun recalculateStats() {
        val recalculated = GameState.recalculateStats(state)
        state = recalculated
    }

    private fun setupWallpaper() {
        val wallpaperId = resources.getIdentifier(state.selectedWallpaper, "drawable", packageName)
        if (wallpaperId != 0) {
            findViewById<FrameLayout>(R.id.content_container)?.setBackgroundResource(wallpaperId)
        }
    }

    private fun setupTabs() {
        findViewById<LinearLayout>(R.id.tab_game)?.setOnClickListener { switchTab("game") }
        findViewById<LinearLayout>(R.id.tab_upgrades)?.setOnClickListener { switchTab("upgrades") }
        findViewById<LinearLayout>(R.id.tab_gifts)?.setOnClickListener { switchTab("gifts") }
        findViewById<LinearLayout>(R.id.tab_settings)?.setOnClickListener { switchTab("settings") }
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        updateTabIndicators(tab)
        showTab(tab)
    }

    private fun updateTabIndicators(activeTab: String) {
        val tabs = listOf("game" to R.id.tab_game_indicator, "upgrades" to R.id.tab_upgrades_indicator,
            "gifts" to R.id.tab_gifts_indicator, "settings" to R.id.tab_settings_indicator)

        tabs.forEach { (tab, indicatorId) ->
            val indicator = findViewById<View>(indicatorId)
            indicator?.visibility = if (tab == activeTab) View.VISIBLE else View.GONE
        }
    }

    private fun showTab(tab: String) {
        val container = findViewById<FrameLayout>(R.id.content_container)
        container?.removeAllViews()

        when (tab) {
            "game" -> {
                val view = layoutInflater.inflate(R.layout.tab_game, container, false)
                setupGameTab(view)
                container?.addView(view)
            }
            "upgrades" -> {
                val view = layoutInflater.inflate(R.layout.tab_upgrades, container, false)
                setupUpgradesTab(view)
                container?.addView(view)
            }
            "gifts" -> {
                val view = layoutInflater.inflate(R.layout.tab_gifts, container, false)
                setupGiftsTab(view)
                container?.addView(view)
            }
            "settings" -> {
                val view = layoutInflater.inflate(R.layout.tab_settings, container, false)
                setupSettingsTab(view)
                container?.addView(view)
            }
        }
    }

    private fun setupGameTab(view: View) {
        val coinsDisplay = view.findViewById<TextView>(R.id.game_coins)
        val clickPowerDisplay = view.findViewById<TextView>(R.id.stat_click_power)
        val autoClickDisplay = view.findViewById<TextView>(R.id.stat_auto_click)
        val totalClicksDisplay = view.findViewById<TextView>(R.id.stat_total_clicks)
        val autoClickContainer = view.findViewById<View>(R.id.stat_autoclick_container)
        val clickButton = view.findViewById<ImageButton>(R.id.click_button)

        coinsDisplay?.text = GameState.formatNumber(state.coins)
        clickPowerDisplay?.text = GameState.formatNumber(state.clickPower)
        totalClicksDisplay?.text = GameState.formatNumber(state.totalClicks)

        if (state.autoClickPerSecond > 0) {
            autoClickContainer?.visibility = View.VISIBLE
            autoClickDisplay?.text = GameState.formatNumber(state.autoClickPerSecond)
        } else {
            autoClickContainer?.visibility = View.GONE
        }

        clickButton?.setOnClickListener {
            handleClick(it)
        }

        // Update coins periodically
        handler.postDelayed(object : Runnable {
            override fun run() {
                coinsDisplay?.text = GameState.formatNumber(state.coins)
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun handleClick(view: View) {
        state = state.copy(coins = state.coins + state.clickPower, totalCoins = state.totalCoins + state.clickPower,
            totalClicks = state.totalClicks + 1)

        // Check for 100k medal
        if (!state.medal100k && state.totalClicks >= 100000) {
            state = state.copy(medal100k = true)
            showToast("🏅 Медаль 100K получена!", "#22c55e")
        }

        updateHeader()
        recalculateStats()
    }

    private fun setupUpgradesTab(view: View) {
        val coinsDisplay = view.findViewById<TextView>(R.id.upgrades_coins)
        val listView = view.findViewById<ListView>(R.id.upgrades_list)

        coinsDisplay?.text = GameState.formatNumber(state.coins)

        val upgrades = state.upgrades
        val adapter = UpgradeAdapter(upgrades, state.coins) { upgrade ->
            buyUpgrade(upgrade)
        }

        listView?.adapter = adapter
    }

    private fun buyUpgrade(upgrade: GameState.Upgrade) {
        if (state.coins >= upgrade.cost) {
            val now = System.currentTimeMillis()
            state = state.copy(
                coins = state.coins - upgrade.cost,
                upgrades = state.upgrades.map { u ->
                    if (u.id == upgrade.id) {
                        u.copy(active = true, expiresAt = now + u.duration)
                    } else u
                }
            )

            if (upgrade.isAutoClicker) {
                state = state.copy(baseAutoClick = state.baseAutoClick + upgrade.multiplier.toLong())
            } else {
                state = state.copy(baseClickPower = state.baseClickPower * upgrade.multiplier.toLong())
            }

            recalculateStats()
            updateHeader()
            showToast("✨ ${upgrade.name} активирован!", "#22c55e")
        } else {
            showToast("Недостаточно монет!", "#ef4444")
        }
    }

    private fun setupGiftsTab(view: View) {
        val messageView = view.findViewById<TextView>(R.id.gifts_message)
        val listView = view.findViewById<ListView>(R.id.gifts_list)
        val medalIcon = view.findViewById<ImageView>(R.id.medal_100k)

        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val canClaim = state.lastDailyCheck != today

        messageView?.text = if (canClaim) "Можно получить подарок!" else "⏰ Возвращайся завтра!"

        if (state.medal100k) {
            medalIcon?.visibility = View.VISIBLE
        }

        val gifts = state.dailyGifts
        val adapter = GiftAdapter(gifts, state.currentDay, canClaim, today) { gift ->
            claimGift(gift, today)
        }

        listView?.adapter = adapter
    }

    private fun claimGift(gift: GameState.DailyGift, today: String) {
        if (gift.specialReward == "golden_spidi") {
            state = state.copy(
                baseClickPower = state.baseClickPower + 50,
                coins = state.coins + (gift.coins ?: 0),
                totalCoins = state.totalCoins + (gift.coins ?: 0),
                goldenSpidi = true,
                dailyGifts = state.dailyGifts.map { g -> if (g.day == gift.day) g.copy(claimed = true) else g },
                lastDailyCheck = today,
                currentDay = Math.min(state.currentDay + 1, 5)
            )
            showToast("🌟 Золотой Спиди! +50 к силе!", "#22c55e")
        } else {
            state = state.copy(
                coins = state.coins + (gift.coins ?: 0),
                totalCoins = state.totalCoins + (gift.coins ?: 0),
                dailyGifts = state.dailyGifts.map { g -> if (g.day == gift.day) g.copy(claimed = true) else g },
                lastDailyCheck = today,
                currentDay = Math.min(state.currentDay + 1, 5)
            )
            showToast("+${gift.coins} монет!", "#22c55e")
        }

        recalculateStats()
        updateHeader()
    }

    private fun setupSettingsTab(view: View) {
        val wallpaperGrid = view.findViewById<GridView>(R.id.wallpaper_grid)
        val musicList = view.findViewById<LinearLayout>(R.id.music_list)
        val volumeSlider = view.findViewById<SeekBar>(R.id.volume_slider)
        val volumeText = view.findViewById<TextView>(R.id.volume_text)
        val switchMusic = view.findViewById<Switch>(R.id.switch_music)
        val btnUploadWallpaper = view.findViewById<Button>(R.id.btn_upload_wallpaper)
        val btnUploadMusic = view.findViewById<Button>(R.id.btn_upload_music)

        // Wallpaper grid
        val wallpaperAdapter = WallpaperGridAdapter(this, GameState.wallpapers)
        wallpaperAdapter.selectedIndex = GameState.wallpapers.indexOf(state.selectedWallpaper)
        wallpaperGrid?.adapter = wallpaperAdapter

        wallpaperGrid?.setOnItemClickListener { _, _, position, _ ->
            state = state.copy(selectedWallpaper = GameState.wallpapers[position])
            wallpaperAdapter.selectedIndex = position
            wallpaperAdapter.notifyDataSetChanged()
            setupWallpaper()
        }

        // Music list
        musicList?.removeAllViews()
        val musicTrackViews = mutableListOf<LinearLayout>()
        GameState.musicTracks.forEachIndexed { index, track ->
            val trackView = layoutInflater.inflate(R.layout.item_music_track, musicList, false) as LinearLayout
            val nameText = trackView.findViewById<TextView>(R.id.music_track_name)
            nameText?.text = track.name

            if (state.selectedMusic == track.id) {
                trackView.isSelected = true
            }

            trackView.setOnClickListener {
                state = state.copy(selectedMusic = track.id)
                musicTrackViews.forEach { it.isSelected = false }
                trackView.isSelected = true
                setupMusic()
            }

            musicTrackViews.add(trackView)
            musicList?.addView(trackView)
        }

        // Volume
        volumeSlider?.progress = (state.musicVolume * 100).toInt()
        volumeText?.text = "${(state.musicVolume * 100).toInt()}%"

        volumeSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                state = state.copy(musicVolume = progress / 100f)
                volumeText?.text = "${progress}%"
                mediaPlayer?.setVolume(progress / 100f, progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Music toggle
        switchMusic?.isChecked = state.musicEnabled
        switchMusic?.setOnCheckedChangeListener { _, isChecked ->
            state = state.copy(musicEnabled = isChecked)
            if (isChecked) startMusic() else stopMusic()
        }

        // Reset progress
        view.findViewById<Button>(R.id.btn_reset_progress)?.setOnClickListener {
            SettingsManager.resetState(this)
            finish()
            startActivity(Intent(this, OobeActivity::class.java))
        }

        // Recreate wizard
        view.findViewById<Button>(R.id.btn_recreate_wizard)?.setOnClickListener {
            state = state.copy(completedOnboarding = false)
            SettingsManager.saveState(this, state)
            finish()
            startActivity(Intent(this, OobeActivity::class.java))
        }
    }

    private fun setupMusic() {
        if (state.musicEnabled) {
            startMusic()
        }
    }

    private fun startMusic() {
        try {
            stopMusic()
            val track = GameState.musicTracks.find { it.id == state.selectedMusic } ?: return
            mediaPlayer = MediaPlayer.create(this, track.resId)
            mediaPlayer?.setVolume(state.musicVolume, state.musicVolume)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun updateHeader() {
        val coinsDisplay = findViewById<TextView>(R.id.coins_display)
        val medalIcon = findViewById<ImageView>(R.id.medal_icon)

        coinsDisplay?.text = GameState.formatNumber(state.coins)
        medalIcon?.visibility = if (state.medal100k) View.VISIBLE else View.GONE
    }

    private fun startAutoClick() {
        if (state.autoClickPerSecond > 0) {
            autoClickRunnable = Runnable {
                state = state.copy(
                    coins = state.coins + state.autoClickPerSecond * state.clickPower,
                    totalCoins = state.totalCoins + state.autoClickPerSecond * state.clickPower,
                    totalClicks = state.totalClicks + state.autoClickPerSecond
                )
                updateHeader()
                recalculateStats()
                handler.postDelayed(autoClickRunnable!!, 1000)
            }
            handler.postDelayed(autoClickRunnable!!, 1000)
        }
    }

    private fun stopAutoClick() {
        autoClickRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startSaveTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                SettingsManager.saveState(this@GameActivity, state)
                handler.postDelayed(this, 5000)
            }
        }, 5000)
    }

    private fun showToast(msg: String, color: String) {
        val toast = findViewById<TextView>(R.id.toast)
        toast?.apply {
            text = msg
            setBackgroundResource(R.drawable.button_secondary_bg)
            visibility = View.VISIBLE
            handler.postDelayed({ visibility = View.GONE }, 3000)
        }
    }
}
