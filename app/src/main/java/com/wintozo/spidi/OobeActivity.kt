package com.wintozo.spidi

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class OobeActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private var currentStep = 1
    private val totalSteps = 5
    private var selectedDevice = "pc"

    private lateinit var progressBar: ProgressBar
    private lateinit var stepCounter: TextView
    private lateinit var stepTitle: TextView
    private lateinit var stepDescription: TextView
    private lateinit var stepContent: FrameLayout
    private lateinit var backBtn: Button
    private lateinit var nextBtn: Button

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Activity.FEATURE_NO_TITLE)
        supportActionBar?.hide()

        settingsManager = SettingsManager(this)

        setContentView(R.layout.activity_oobe)

        initViews()
        loadStep(1)
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progress_bar)
        stepCounter = findViewById(R.id.step_counter)
        stepTitle = findViewById(R.id.step_title)
        stepDescription = findViewById(R.id.step_description)
        stepContent = findViewById(R.id.step_content)
        backBtn = findViewById(R.id.back_btn)
        nextBtn = findViewById(R.id.next_btn)

        backBtn.setOnClickListener { prevStep() }
        nextBtn.setOnClickListener { nextStep() }
    }

    private fun loadStep(step: Int) {
        currentStep = step
        val progress = (step.toFloat() / totalSteps * 100).toInt()
        progressBar.progress = progress
        stepCounter.text = "Шаг $step из $totalSteps"

        stepContent.removeAllViews()

        when (step) {
            1 -> loadDeviceStep()
            2 -> loadWallpaperStep()
            3 -> loadMusicStep()
            4 -> loadVolumeStep()
            5 -> loadFinalStep()
        }

        updateButtons()
        animateStep()
    }

    private fun loadDeviceStep() {
        stepTitle.text = "Выберите устройство"
        stepDescription.text = "С чего вы будете играть?"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val devices = listOf(
            Triple(R.drawable.phone, "Телефон", "Играйте на ходу", "phone"),
            Triple(R.drawable.tablet, "Планшет", "Больше экрана", "tablet"),
            Triple(R.drawable.computer, "Компьютер", "Полный опыт", "pc")
        )

        devices.forEachIndexed { index, (iconRes, name, desc, type) ->
            val card = createDeviceCard(iconRes, name, desc, type, index == 0)
            layout.addView(card)
        }

        stepContent.addView(layout)
    }

    private fun createDeviceCard(iconRes: Int, name: String, desc: String, type: String, isFirst: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, if (!isFirst) 12 else 0)
            }
            background = ContextCompat.getDrawable(this@OobeActivity, R.drawable.device_card_bg)
            setPadding(20, 20, 20, 20)
            isClickable = true
            isFocusable = true
        }

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                setMargins(0, 0, 16, 0)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val descText = TextView(this).apply {
            text = desc
            textSize = 14f
            setTextColor(Color.parseColor("#9ca3af"))
        }

        textLayout.addView(nameText)
        textLayout.addView(descText)

        val check = ImageView(this).apply {
            setImageResource(R.drawable.check_circle)
            visibility = if (selectedDevice == type) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(24, 24)
        }

        card.addView(icon)
        card.addView(textLayout)
        card.addView(check)

        val selectedType = type
        card.setOnClickListener {
            selectedDevice = selectedType
            check.visibility = View.VISIBLE
            (0 until card.parent.childCount).forEach { i ->
                val sibling = card.parent.getChildAt(i)
                if (sibling != card && sibling is LinearLayout) {
                    (0 until sibling.childCount).forEach { j ->
                        (sibling.getChildAt(j) as? ImageView)?.let { img ->
                            if (img != check) img.visibility = View.GONE
                        }
                    }
                }
            }
            settingsManager.deviceType = selectedDevice
        }

        return card
    }

    private fun loadWallpaperStep() {
        stepTitle.text = "Выберите обои"
        stepDescription.text = "Персонализируйте свой опыт"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val wallpapers = listOf(
            R.drawable.wallpeper_1, R.drawable.wallpaper_2, R.drawable.wallpeper_3,
            R.drawable.wallpaper_4, R.drawable.wallpaper_5, R.drawable.wallpaper_6,
            R.drawable.wallpaper_7, R.drawable.wallpaper_8
        )

        var selectedWallpaper = settingsManager.selectedWallpaper.toIntOrNull() ?: R.drawable.wallpeper_1

        wallpapers.forEach { wpRes ->
            val container = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    setMargins(0, 0, 8, 0)
                }
            }

            val img = ImageView(this).apply {
                setImageResource(wpRes)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = ContextCompat.getDrawable(this@OobeActivity, R.drawable.wallpaper_select_bg)
            }

            val check = ImageView(this).apply {
                setImageResource(R.drawable.check_circle)
                layoutParams = FrameLayout.LayoutParams(24, 24).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(4, 4, 4, 4)
                }
                visibility = if (selectedWallpaper == wpRes) View.VISIBLE else View.GONE
            }

            container.addView(img)
            container.addView(check)

            val wpResLocal = wpRes
            container.setOnClickListener {
                selectedWallpaper = wpResLocal
                check.visibility = View.VISIBLE
                settingsManager.selectedWallpaper = wpResLocal.toString()
                (0 until layout.childCount).forEach { i ->
                    val child = layout.getChildAt(i)
                    if (child is FrameLayout && child != container) {
                        (child.getChildAt(1) as? ImageView)?.visibility = View.GONE
                    }
                }
            }

            layout.addView(container)
        }

        stepContent.addView(layout)
    }

    private fun loadMusicStep() {
        stepTitle.text = "Выберите музыку"
        stepDescription.text = "Фоновая музыка для игры"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        var selectedMusic = settingsManager.selectedMusic

        GameState.musicTracks.forEach { track ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 8)
                }
                background = ContextCompat.getDrawable(this@OobeActivity, R.drawable.button_secondary_bg)
                setPadding(16, 16, 16, 16)
                isClickable = true
                isFocusable = true
            }

            val icon = ImageView(this).apply {
                setImageResource(R.drawable.medal)
                layoutParams = LinearLayout.LayoutParams(40, 40)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }

            val name = TextView(this).apply {
                text = track.name
                textSize = 16f
                setTextColor(Color.parseColor("#1a1a1a"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val check = ImageView(this).apply {
                setImageResource(R.drawable.check_circle)
                visibility = if (selectedMusic == track.id) View.VISIBLE else View.GONE
                layoutParams = LinearLayout.LayoutParams(24, 24)
            }

            card.addView(icon)
            card.addView(name)
            card.addView(check)

            val trackId = track.id
            card.setOnClickListener {
                selectedMusic = trackId
                check.visibility = View.VISIBLE
                settingsManager.selectedMusic = selectedMusic
                settingsManager.musicEnabled = true
                (0 until layout.childCount).forEach { i ->
                    val child = layout.getChildAt(i)
                    if (child is LinearLayout && child != card) {
                        (child.getChildAt(2) as? ImageView)?.visibility = View.GONE
                    }
                }
            }

            layout.addView(card)
        }

        stepContent.addView(layout)
    }

    private fun loadVolumeStep() {
        stepTitle.text = "Громкость музыки"
        stepDescription.text = "Настройте уровень громкости"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 32, 0, 0)
        }

        val slider = SeekBar(this).apply {
            max = 100
            progress = (settingsManager.musicVolume * 100).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val progressText = TextView(this).apply {
            text = "${slider.progress}%"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1a1a1a"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 16 }
        }

        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                progressText.text = "$progress%"
                settingsManager.musicVolume = progress / 100f
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        layout.addView(slider)
        layout.addView(progressText)
        stepContent.addView(layout)
    }

    private fun loadFinalStep() {
        stepTitle.text = "Готово!"
        stepDescription.text = "Приятной игры!"
        backBtn.visibility = View.GONE
        nextBtn.text = "Начать игру"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                setMargins(0, 0, 0, 24)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = ContextCompat.getDrawable(this@OobeActivity, R.drawable.circle_bg)
        }

        val startBtn = Button(this).apply {
            text = "Начать игру"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48
            )
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#3b82f6"))
            setTextColor(Color.WHITE)
        }

        startBtn.setOnClickListener {
            settingsManager.completedOnboarding = true
            val intent = Intent(this@OobeActivity, GameActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        layout.addView(logo)
        layout.addView(startBtn)
        stepContent.addView(layout)
    }

    private fun prevStep() {
        if (currentStep > 1) {
            animateTransition()
            loadStep(currentStep - 1)
        }
    }

    private fun nextStep() {
        if (currentStep < totalSteps) {
            animateTransition()
            loadStep(currentStep + 1)
        }
    }

    private fun updateButtons() {
        backBtn.visibility = if (currentStep == 1) View.GONE else View.VISIBLE
        nextBtn.text = if (currentStep == totalSteps) "Начать игру" else "Далее"
    }

    private fun animateStep() {
        stepContent.alpha = 0f
        stepContent.visibility = View.VISIBLE
        
        val fadeIn = ObjectAnimator.ofFloat(stepContent, "alpha", 0f, 1f)
        fadeIn.duration = 300
        fadeIn.interpolator = AccelerateDecelerateInterpolator()
        fadeIn.start()
    }

    private fun animateTransition() {
        stepContent.alpha = 1f
        val fadeOut = ObjectAnimator.ofFloat(stepContent, "alpha", 1f, 0f)
        fadeOut.duration = 150
        fadeOut.interpolator = AccelerateInterpolator()
        fadeOut.start()
        fadeOut.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                stepContent.visibility = View.GONE
            }
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

    override fun onBackPressed() {
        if (currentStep > 1) {
            prevStep()
        } else {
            super.onBackPressed()
        }
    }
}

    private fun updateProgress() {
        (0 until totalSteps).forEach { i ->
            val progressView = findViewById<View>(resources.getIdentifier("progress_step_${i + 1}", "id", packageName))
            val labelView = findViewById<TextView>(resources.getIdentifier("label_step_${i + 1}", "id", packageName))
            
            if (progressView != null) {
                progressView.setBackgroundColor(if (i < currentStep) 0xFF3b82f6.toInt() else 0xFFE0E0E0.toInt())
            }
            if (labelView != null) {
                labelView.setTextColor(if (i + 1 == currentStep) 0xFF1a1a1a.toInt() else 0xFF666666.toInt())
                labelView.setTypeface(null, if (i + 1 == currentStep) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
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
        val phoneText = view.findViewById<TextView>(R.id.phone_label)
        val tabletText = view.findViewById<TextView>(R.id.tablet_label)

        phoneOption.setOnClickListener {
            selectedDeviceType = "phone"
            updateDeviceSelection(phoneOption, tabletOption, phoneIndicator, tabletIndicator, phoneText, tabletText)
        }

        tabletOption.setOnClickListener {
            selectedDeviceType = "tablet"
            updateDeviceSelection(tabletOption, phoneOption, tabletIndicator, phoneIndicator, tabletText, phoneText)
        }

        // Default to phone
        selectedDeviceType = "phone"
        updateDeviceSelection(phoneOption, tabletOption, phoneIndicator, tabletIndicator, phoneText, tabletText)
    }

    private fun updateDeviceSelection(selected: LinearLayout, other: LinearLayout, 
                                      selectedIndicator: View, otherIndicator: View,
                                      selectedText: TextView?, otherText: TextView?) {
        selected.isSelected = true
        other.isSelected = false
        selectedIndicator.visibility = View.VISIBLE
        otherIndicator.visibility = View.GONE
        
        selectedText?.setTextColor(0xFF1a1a1a.toInt())
        otherText?.setTextColor(0xFF666666.toInt())
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
