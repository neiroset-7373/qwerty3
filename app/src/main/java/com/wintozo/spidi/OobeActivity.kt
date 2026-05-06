package com.wintozo.spidi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class OobeActivity : AppCompatActivity() {

    private var currentStep = 1
    private val totalSteps = 5
    private var selectedDeviceType = "phone"
    private var selectedWallpaper = "wallpaper_1"
    private var selectedMusic = "track1"
    private var musicEnabled = true
    private var musicVolume = 0.3f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oobe)

        updateProgress()
        showStep(1)
        setupNavigation()
    }

    private fun updateProgress() {
        (0 until totalSteps).forEach { i ->
            val progressView = findViewById<View>(resources.getIdentifier("progress_step_${i + 1}", "id", packageName))
            val labelView = findViewById<TextView>(resources.getIdentifier("label_step_${i + 1}", "id", packageName))
            
            if (progressView != null) {
                progressView.setBackgroundColor(if (i < currentStep) 0xFFFF9800.toInt() else 0xFFE0E0E0.toInt())
            }
            if (labelView != null) {
                labelView.setTextColor(if (i + 1 == currentStep) 0xFF1a1a1a.toInt() else 0xFF666666.toInt())
                labelView.typeface = if (i + 1 == currentStep) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            }
        }
        
        findViewById<TextView>(R.id.step_counter)?.text = "Шаг $currentStep из $totalSteps"
    }

    private fun showStep(step: Int) {
        val container = findViewById<FrameLayout>(R.id.step_container)
        container.removeAllViews()

        when (step) {
            1 -> {
                val view = layoutInflater.inflate(R.layout.step_device, container, false)
                setupDeviceStep(view)
            }
            2 -> {
                val view = layoutInflater.inflate(R.layout.step_wallpaper, container, false)
                setupWallpaperStep(view)
            }
            3 -> {
                val view = layoutInflater.inflate(R.layout.step_icons, container, false)
                setupIconsStep(view)
            }
            4 -> {
                val view = layoutInflater.inflate(R.layout.step_music, container, false)
                setupMusicStep(view)
            }
            5 -> {
                val view = layoutInflater.inflate(R.layout.step_final, container, false)
                setupFinalStep(view)
            }
        }
    }

    private fun setupDeviceStep(view: View) {
        val phoneOption = view.findViewById<LinearLayout>(R.id.option_phone)
        val tabletOption = view.findViewById<LinearLayout>(R.id.option_tablet)
        val phoneIndicator = view.findViewById<View>(R.id.indicator_phone)
        val tabletIndicator = view.findViewById<View>(R.id.indicator_tablet)
        val phoneText = phoneOption.findViewById<TextView>(phoneOption.id + 1) // Approximate

        phoneOption.setOnClickListener {
            selectedDeviceType = "phone"
            updateDeviceSelection(phoneOption, tabletOption, phoneIndicator, tabletIndicator)
        }

        tabletOption.setOnClickListener {
            selectedDeviceType = "tablet"
            updateDeviceSelection(tabletOption, phoneOption, tabletIndicator, phoneIndicator)
        }

        // Default to phone
        selectedDeviceType = "phone"
        updateDeviceSelection(phoneOption, tabletOption, phoneIndicator, tabletIndicator)
    }

    private fun updateDeviceSelection(selected: LinearLayout, other: LinearLayout, 
                                      selectedIndicator: View, otherIndicator: View) {
        selected.isSelected = true
        other.isSelected = false
        selectedIndicator.visibility = View.VISIBLE
        otherIndicator.visibility = View.GONE
        
        val selectedText = selected.findViewById<TextView>(selected.id + 1)
        selectedText?.setTextColor(0xFF1a1a1a.toInt())
    }

    private fun setupWallpaperStep(view: View) {
        val grid = view.findViewById<GridView>(R.id.wallpaper_grid)
        val wallpaperAdapter = WallpaperGridAdapter(this, GameState.wallpapers)
        grid.adapter = wallpaperAdapter

        grid.setOnItemClickListener { _, _, position, _ ->
            selectedWallpaper = GameState.wallpapers[position]
            wallpaperAdapter.selectedIndex = position
            wallpaperAdapter.notifyDataSetChanged()
        }
    }

    private fun setupIconsStep(view: View) {
        // Only new pack available
        view.findViewById<LinearLayout>(R.id.option_new_pack)?.isSelected = true
    }

    private fun setupMusicStep(view: View) {
        val track1 = view.findViewById<LinearLayout>(R.id.track_1)
        val track2 = view.findViewById<LinearLayout>(R.id.track_2)
        val switchMusic = view.findViewById<Switch>(R.id.switch_music)

        track1.setOnClickListener {
            selectMusicTrack(track1, track2, "track1")
        }

        track2.setOnClickListener {
            selectMusicTrack(track2, track1, "track2")
        }

        switchMusic?.setOnCheckedChangeListener { _, isChecked ->
            musicEnabled = isChecked
        }

        // Default to track 1
        selectMusicTrack(track1, track2, "track1")
    }

    private fun selectMusicTrack(selected: LinearLayout, other: LinearLayout, trackId: String) {
        selected.isSelected = true
        other.isSelected = false
        selectedMusic = trackId
    }

    private fun setupFinalStep(view: View) {
        findViewById<View>(R.id.btn_start)?.setOnClickListener {
            completeOOBE()
        }
    }

    private fun setupNavigation() {
        val btnBack = findViewById<Button>(R.id.btn_back)
        val btnNext = findViewById<Button>(R.id.btn_next)
        val navContainer = findViewById<View>(R.id.navigation_container)
        val startButton = findViewById<Button>(R.id.btn_start)

        btnBack?.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateProgress()
                showStep(currentStep)
                updateNavigationVisibility(navContainer, startButton)
            }
        }

        btnNext?.setOnClickListener {
            if (currentStep < totalSteps) {
                currentStep++
                updateProgress()
                showStep(currentStep)
                updateNavigationVisibility(navContainer, startButton)
            }
        }

        updateNavigationVisibility(navContainer, startButton)
    }

    private fun updateNavigationVisibility(navContainer: View?, startButton: View?) {
        if (currentStep < totalSteps) {
            navContainer?.visibility = View.VISIBLE
            startButton?.visibility = View.GONE
            findViewById<Button>(R.id.btn_back)?.isEnabled = currentStep > 1
        } else {
            navContainer?.visibility = View.GONE
            startButton?.visibility = View.VISIBLE
        }
    }

    private fun completeOOBE() {
        val state = GameState.GameState(
            deviceType = selectedDeviceType,
            selectedWallpaper = selectedWallpaper,
            selectedMusic = selectedMusic,
            musicEnabled = musicEnabled,
            musicVolume = musicVolume,
            completedOnboarding = true,
            upgrades = GameState.defaultUpgrades,
            dailyGifts = GameState.defaultGifts
        )

        SettingsManager.saveState(this, state)

        val intent = Intent(this, GameActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
