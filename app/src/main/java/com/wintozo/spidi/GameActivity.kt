package com.wintozo.spidi

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class GameActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var state: GameState.GameState
    private var clickPower = 1L
    private var autoClickPerSecond = 0L
    private var baseClickPower = 1L
    private var baseAutoClick = 0L
    private var deviceType = "pc"
    private var wallpaper: Int = R.drawable.wallpeper_1
    private var selectedMusic = "track1"
    private var musicEnabled = true
    private var musicVolume = 0.3f

    private lateinit var wallpaperBg: ImageView
    private lateinit var coinsDisplay: TextView
    private lateinit var clickPowerDisplay: TextView
    private lateinit var autoClickDisplay: TextView
    private lateinit var totalClicksDisplay: TextView
    private lateinit var clickBtn: ImageView
    private lateinit var clickBtnContainer: FrameLayout
    private lateinit var coinsContainer: FrameLayout
    private lateinit var floatingCoinsContainer: FrameLayout
    private lateinit var totalClicksTextView: TextView
    private lateinit var header: LinearLayout
    private lateinit var bottomNav: LinearLayout
    private lateinit var tabGame: LinearLayout
    private lateinit var tabUpgrades: LinearLayout
    private lateinit var tabGifts: LinearLayout
    private lateinit var tabSettings: LinearLayout
    private lateinit var contentContainer: FrameLayout
    private lateinit var toastView: TextView
    private lateinit var medalIcon: ImageView

    private lateinit var upgradesList: ListView
    private lateinit var giftsList: ListView

    private var totalClicks = 0L
    private var coins = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val autoClickHandler = Handler(Looper.getMainLooper())
    private var autoClickRunnable: Runnable? = null

    private var soundPool: SoundPool? = null
    private var clickSoundId = 0

    private var musicPlayer: android.media.MediaPlayer? = null
    private var isMusicPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()

        settingsManager = SettingsManager(this)
        loadState()

        setContentView(R.layout.activity_game)

        initViews()
        setupListeners()
        applySettings()
        startAutoClicker()
        updateUI()
    }

    private fun initViews() {
        wallpaperBg = findViewById(R.id.wallpaper_bg)
        coinsDisplay = findViewById(R.id.coins_display)
        clickPowerDisplay = findViewById(R.id.click_power_display)
        autoClickDisplay = findViewById(R.id.auto_click_display)
        totalClicksDisplay = findViewById(R.id.total_clicks_display)
        clickBtn = findViewById(R.id.click_btn)
        clickBtnContainer = findViewById(R.id.click_btn_container)
        coinsContainer = findViewById(R.id.coins_container)
        floatingCoinsContainer = findViewById(R.id.floating_coins_container)
        totalClicksTextView = findViewById(R.id.total_clicks_display)
        header = findViewById(R.id.header)
        bottomNav = findViewById(R.id.bottom_nav)
        tabGame = findViewById(R.id.tab_game)
        tabUpgrades = findViewById(R.id.tab_upgrades)
        tabGifts = findViewById(R.id.tab_gifts)
        tabSettings = findViewById(R.id.tab_settings)
        contentContainer = findViewById(R.id.content_container)
        toastView = findViewById(R.id.toast_view)
        medalIcon = findViewById(R.id.medal_icon)

        loadClickSound()
    }

    private fun loadClickSound() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SOUND)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build()
        } else {
            soundPool = SoundPool(3, AudioManager.STREAM_MUSIC, 0)
        }
        clickSoundId = soundPool?.load(this, R.raw.click, 1) ?: 0
    }

    private fun playClickSound() {
        if (!musicEnabled) return
        soundPool?.play(clickSoundId, 0.5f, 0.5f, 0, 0, 1f)
    }

    private fun setupListeners() {
        clickBtn.setOnClickListener {
            handleMainClick()
        }

        tabGame.setOnClickListener { showTab("game") }
        tabUpgrades.setOnClickListener { showTab("upgrades") }
        tabGifts.setOnClickListener { showTab("gifts") }
        tabSettings.setOnClickListener { showTab("settings") }
    }

    private fun handleMainClick() {
        coins += clickPower
        totalClicks++
        playClickSound()
        animateClick()
        spawnFloatingCoin()
        updateUI()
        saveState()
    }

    private fun animateClick() {
        clickBtn.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(50)
            .withEndAction {
                clickBtn.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(50)
                    .start()
            }
            .start()
    }

    private fun spawnFloatingCoin() {
        val coin = ImageView(this).apply {
            setImageResource(R.drawable.coin)
            layoutParams = FrameLayout.LayoutParams(32, 32)
        }

        val randomX = (0..100).random()
        val randomY = (0..100).random()

        coin.x = randomX.toFloat()
        coin.y = randomY.toFloat()

        floatingCoinsContainer.addView(coin)

        val animator = ObjectAnimator.ofFloat(coin, "translationY", 0f, -150f)
        animator.duration = 800
        animator.interpolator = DecelerateInterpolator()

        val alphaAnimator = ObjectAnimator.ofFloat(coin, "alpha", 1f, 0f)
        alphaAnimator.duration = 800

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animator, alphaAnimator)
        animatorSet.start()

        animatorSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                floatingCoinsContainer.removeView(coin)
            }
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    private fun showTab(tabName: String) {
        contentContainer.removeAllViews()

        val inflate = LayoutInflater.from(this)
        when (tabName) {
            "game" -> {
                inflate.inflate(R.layout.tab_game, contentContainer, false)
                    .let { contentContainer.addView(it) }
                updateTabIndicator(tabGame)
            }
            "upgrades" -> {
                inflate.inflate(R.layout.tab_upgrades, contentContainer, false)
                    .let { contentContainer.addView(it) }
                setupUpgradesTab()
                updateTabIndicator(tabUpgrades)
            }
            "gifts" -> {
                inflate.inflate(R.layout.tab_gifts, contentContainer, false)
                    .let { contentContainer.addView(it) }
                setupGiftsTab()
                updateTabIndicator(tabGifts)
            }
            "settings" -> {
                inflate.inflate(R.layout.tab_settings, contentContainer, false)
                    .let { contentContainer.addView(it) }
                setupSettingsTab()
                updateTabIndicator(tabSettings)
            }
        }
    }

    private fun updateTabIndicator(selectedTab: LinearLayout) {
        listOf(tabGame, tabUpgrades, tabGifts, tabSettings).forEach { tab ->
            val indicator = when (tab) {
                tabGame -> tabGame.findViewById<View>(R.id.tab_game_indicator)
                tabUpgrades -> tabUpgrades.findViewById<View>(R.id.tab_upgrades_indicator)
                tabGifts -> tabGifts.findViewById<View>(R.id.tab_gifts_indicator)
                tabSettings -> tabSettings.findViewById<View>(R.id.tab_settings_indicator)
                else -> null
            }
            indicator?.visibility = if (tab == selectedTab) View.VISIBLE else View.GONE
            
            val textView = tab.getChildAt(1) as? TextView
            textView?.apply {
                setTextColor(if (tab == selectedTab) Color.parseColor("#1a1a1a") else Color.parseColor("#9ca3af"))
            }
        }
    }

    private fun setupUpgradesTab() {
        val contentView = findViewById<View>(R.id.upgrades_list) as? ListView ?: return
        upgradesList = contentView
        
        val upgrades = state.upgrades.map { up ->
            GameState.Upgrade(
                id = up.id,
                name = up.name,
                description = up.description,
                cost = up.cost,
                multiplier = up.multiplier,
                isAutoClicker = up.isAutoClicker,
                icon = up.icon,
                duration = up.duration,
                active = up.active,
                expiresAt = up.expiresAt
            )
        }.toMutableList()

        val adapter = object : BaseAdapter() {
            override fun getCount() = upgrades.size
            override fun getItem(position: Int) = upgrades[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                var view = convertView
                val holder: UpgradeViewHolder

                if (view == null) {
                    view = LayoutInflater.from(this@GameActivity)
                        .inflate(R.layout.item_upgrade, parent, false)
                    holder = UpgradeViewHolder(view)
                    view.tag = holder
                } else {
                    holder = view.tag as UpgradeViewHolder
                }

                val upgrade = upgrades[position]
                holder.bind(upgrade)
                return view!!
            }
        }

        upgradesList.adapter = adapter
    }

    inner class UpgradeViewHolder(view: View) {
        private val icon: TextView = view.findViewById(R.id.upgrade_icon)
        private val name: TextView = view.findViewById(R.id.upgrade_name)
        private val desc: TextView = view.findViewById(R.id.upgrade_desc)
        private val time: TextView = view.findViewById(R.id.upgrade_time)
        private val cost: TextView = view.findViewById(R.id.upgrade_cost)
        private val buyBtn: Button = view.findViewById(R.id.buy_btn)

        fun bind(upgrade: GameState.Upgrade) {
            icon.text = upgrade.icon
            name.text = upgrade.name
            desc.text = upgrade.description
            cost.text = "${GameState.formatNumber(upgrade.cost)} 💰"

            if (upgrade.active) {
                time.visibility = View.VISIBLE
                time.text = "Осталось: ${GameState.formatTimeRemaining(upgrade.expiresAt!!)}"
                buyBtn.text = "Активно"
                buyBtn.isEnabled = false
                buyBtn.setBackgroundColor(Color.parseColor("#9ca3af"))
            } else {
                time.visibility = View.GONE
                buyBtn.text = "Купить"
                buyBtn.isEnabled = coins >= upgrade.cost
                buyBtn.setBackgroundColor(Color.parseColor("#3b82f6"))
            }

            buyBtn.setOnClickListener {
                if (coins >= upgrade.cost && !upgrade.active) {
                    coins -= upgrade.cost
                    upgrade.active = true
                    upgrade.expiresAt = System.currentTimeMillis() + upgrade.duration
                    GameState.recalculateStats(state)
                    updateUI()
                    saveState()
                    showToast("Куплено: ${upgrade.name}")
                    setupUpgradesTab()
                }
            }
        }
    }

    private fun setupGiftsTab() {
        val contentView = findViewById<View>(R.id.gifts_list) as? ListView ?: return
        giftsList = contentView

        val gifts = state.dailyGifts.toMutableList()

        val adapter = object : BaseAdapter() {
            override fun getCount() = gifts.size
            override fun getItem(position: Int) = gifts[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                var view = convertView
                val holder: GiftViewHolder

                if (view == null) {
                    view = LayoutInflater.from(this@GameActivity)
                        .inflate(R.layout.item_gift, parent, false)
                    holder = GiftViewHolder(view)
                    view.tag = holder
                } else {
                    holder = view.tag as GiftViewHolder
                }

                val gift = gifts[position]
                holder.bind(gift)
                return view!!
            }
        }

        giftsList.adapter = adapter
    }

    inner class GiftViewHolder(view: View) {
        private val day: TextView = view.findViewById(R.id.gift_day)
        private val reward: TextView = view.findViewById(R.id.gift_reward)
        private val special: TextView = view.findViewById(R.id.gift_special)
        private val claimBtn: Button = view.findViewById(R.id.claim_btn)

        fun bind(gift: GameState.DailyGift) {
            day.text = gift.day.toString()
            reward.text = gift.reward
            
            if (gift.specialReward != null) {
                special.visibility = View.VISIBLE
                special.text = gift.specialReward
            } else {
                special.visibility = View.GONE
            }

            if (gift.claimed) {
                claimBtn.text = "Забрано"
                claimBtn.isEnabled = false
                claimBtn.setBackgroundColor(Color.parseColor("#9ca3af"))
            } else {
                claimBtn.text = "Забрать"
                claimBtn.isEnabled = true
                claimBtn.setBackgroundColor(Color.parseColor("#3b82f6"))
            }

            claimBtn.setOnClickListener {
                if (!gift.claimed) {
                    gift.claimed = true
                    gift.coins?.let { coins += it }
                    gift.specialReward?.let {
                        if (it == "golden_spidi") {
                            state.goldenSpidi = true
                            baseClickPower += 50
                        }
                    }
                    updateUI()
                    saveState()
                    showToast("Получено: ${gift.reward}")
                    setupGiftsTab()
                }
            }
        }
    }

    private fun setupSettingsTab() {
        val contentView = contentContainer.findViewById<View>(R.id.music_tracks_container)
        val wallpapersContainer = contentContainer.findViewById<LinearLayout>(R.id.wallpapers_grid)
        val deviceContainer = contentContainer.findViewById<LinearLayout>(R.id.device_type_container)
        val volumeSlider = contentContainer.findViewById<SeekBar>(R.id.music_volume_slider)
        val resetBtn = contentContainer.findViewById<Button>(R.id.reset_progress_btn)

        // Music tracks
        GameState.musicTracks.forEach { track ->
            val musicItem = LayoutInflater.from(this)
                .inflate(R.layout.item_music_track, contentView, false)
            val name = musicItem.findViewById<TextView>(R.id.music_name)
            val check = musicItem.findViewById<ImageView>(R.id.music_check)
            
            name.text = track.name
            check.visibility = if (selectedMusic == track.id) View.VISIBLE else View.GONE

            musicItem.setOnClickListener {
                selectedMusic = track.id
                settingsManager.selectedMusic = selectedMusic
                stopMusic()
                if (musicEnabled) startMusic()
                setupSettingsTab()
            }

            contentView.addView(musicItem)
        }

        // Wallpapers
        wallpapersContainer?.let { container ->
            GameState.wallpapers.forEachIndexed { index, wallpaperRes ->
                val wpItem = LayoutInflater.from(this)
                    .inflate(R.layout.item_wallpaper, container, false)
                val img = wpItem.findViewById<ImageView>(R.id.wallpaper_img)
                val check = wpItem.findViewById<ImageView>(R.id.wallpaper_check)
                
                img.setImageResource(wallpaperRes)
                check.visibility = if (this.wallpaper == wallpaperRes) View.VISIBLE else View.GONE

                wpItem.setOnClickListener {
                    wallpaper = wallpaperRes
                    settingsManager.selectedWallpaper = wallpaper.toString()
                    wallpaperBg.setImageResource(wallpaper)
                    setupSettingsTab()
                }

                container.addView(wpItem)
            }
        }

        // Device type
        val devices = listOf(
            Pair(R.drawable.phone, "Телефон", "phone"),
            Pair(R.drawable.tablet, "Планшет", "tablet"),
            Pair(R.drawable.computer, "Компьютер", "pc")
        )

        devices.forEach { (iconRes, name, type) ->
            val deviceItem = LayoutInflater.from(this)
                .inflate(R.layout.item_device, deviceContainer, false)
            val icon = deviceItem.findViewById<ImageView>(R.id.device_icon)
            val deviceName = deviceItem.findViewById<TextView>(R.id.device_name)
            val check = deviceItem.findViewById<ImageView>(R.id.device_check)

            icon.setImageResource(iconRes)
            deviceName.text = name
            check.visibility = if (deviceType == type) View.VISIBLE else View.GONE

            deviceItem.setOnClickListener {
                deviceType = type
                settingsManager.deviceType = deviceType
                setupSettingsTab()
            }

            deviceContainer.addView(deviceItem)
        }

        // Volume
        volumeSlider?.progress = (musicVolume * 100).toInt()
        volumeSlider?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicVolume = progress / 100f
                settingsManager.musicVolume = musicVolume
                updateMusicVolume()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Reset
        resetBtn?.setOnClickListener {
            coins = 0
            totalClicks = 0
            totalClicks = 0
            baseClickPower = 1
            baseAutoClick = 0
            state.upgrades = GameState.defaultUpgrades.map { it.copy(active = false, expiresAt = null) }
            state.dailyGifts = GameState.defaultGifts.map { it.copy(claimed = false) }
            updateUI()
            saveState()
            showToast("Прогресс сброшен")
        }
    }

    private fun applySettings() {
        deviceType = settingsManager.deviceType
        wallpaper = settingsManager.selectedWallpaper.toIntOrNull() ?: R.drawable.wallpeper_1
        selectedMusic = settingsManager.selectedMusic
        musicEnabled = settingsManager.musicEnabled
        musicVolume = settingsManager.musicVolume

        wallpaperBg.setImageResource(wallpaper)
        updateUI()

        if (musicEnabled) {
            startMusic()
        }

        showTab("game")
    }

    private fun startMusic() {
        stopMusic()
        val trackRes = if (selectedMusic == "track1") R.raw.unofficial else R.raw.spidi_official
        musicPlayer = android.media.MediaPlayer.create(this, trackRes)
        musicPlayer?.isLooping = true
        updateMusicVolume()
        musicPlayer?.start()
        isMusicPlaying = true
    }

    private fun stopMusic() {
        musicPlayer?.stop()
        musicPlayer?.release()
        musicPlayer = null
        isMusicPlaying = false
    }

    private fun updateMusicVolume() {
        musicPlayer?.setVolume(musicVolume, musicVolume)
    }

    private fun updateUI() {
        val formattedCoins = GameState.formatNumber(coins)
        coinsDisplay.text = formattedCoins
        clickPowerDisplay.text = GameState.formatNumber(clickPower)
        autoClickDisplay.text = "$autoClickPerSecond/сек"
        totalClicksDisplay.text = GameState.formatNumber(totalClicks)

        if (state.goldenSpidi) {
            medalIcon.visibility = View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        toastView.text = message
        toastView.visibility = View.VISIBLE
        toastView.alpha = 1f
        toastView.animate()
            .alpha(0f)
            .setDuration(2000)
            .setStartDelay(1500)
            .withEndAction { toastView.visibility = View.GONE }
            .start()
    }

    private fun startAutoClicker() {
        autoClickRunnable = object : Runnable {
            override fun run() {
                if (autoClickPerSecond > 0) {
                    coins += autoClickPerSecond
                    totalClicks += autoClickPerSecond
                    updateUI()
                    saveState()
                }
                autoClickHandler.postDelayed(this, 1000)
            }
        }
        autoClickRunnable?.let { autoClickHandler.post(it) }
    }

    private fun loadState() {
        val saved = settingsManager.loadState()
        coins = saved.coins
        totalClicks = saved.totalClicks
        baseClickPower = saved.baseClickPower
        baseAutoClick = saved.baseAutoClick
        state = saved
        GameState.recalculateStats(state)
        clickPower = state.clickPower
        autoClickPerSecond = state.autoClickPerSecond
    }

    private fun saveState() {
        state = state.copy(
            coins = coins,
            totalCoins = coins,
            totalClicks = totalClicks,
            baseClickPower = baseClickPower,
            baseAutoClick = baseAutoClick,
            clickPower = clickPower,
            autoClickPerSecond = autoClickPerSecond,
            deviceType = deviceType,
            selectedWallpaper = wallpaper.toString(),
            selectedMusic = selectedMusic,
            musicEnabled = musicEnabled,
            musicVolume = musicVolume
        )
        settingsManager.saveState(state)
    }

    override fun onPause() {
        super.onPause()
        saveState()
        if (!musicEnabled) stopMusic()
    }

    override fun onResume() {
        super.onResume()
        loadState()
        if (musicEnabled && !isMusicPlaying) startMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoClickHandler.removeCallbacksAndMessages(null)
        stopMusic()
        soundPool?.release()
        soundPool = null
    }
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
