package com.simpleplayer.android

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.Locale

class MainActivity : ComponentActivity() {
    companion object {
        private const val SPEED_150 = 1.5f
        private const val SPEED_175 = 1.75f
        private const val BG = 0xFF0D0D11.toInt()
        private const val PANEL = 0xFF19191F.toInt()
        private const val ACCENT = 0xFFC6A7FF.toInt()
        private const val WHITE = 0xFFF5F5F7.toInt()
        private const val MUTED = 0xFFB8B6C0.toInt()
    }

    private lateinit var root: FrameLayout
    private lateinit var stage: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var plusButton: TextView
    private lateinit var controls: LinearLayout
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    private lateinit var speedButton: Button
    private lateinit var subtitleButton: Button
    private lateinit var subtitleText: TextView

    private var player: ExoPlayer? = null
    private var speed = SPEED_150
    private var hasVideo = false
    private var seeking = false
    private var fullscreen = false
    private var captionsOn = true
    private var cues: List<SubtitleCue> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    private val videoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openVideo(it) }
    }
    private val subtitlePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openSubtitle(it) }
    }

    private val updater = object : Runnable {
        override fun run() {
            updateTimeline()
            updateSubtitle()
            handler.postDelayed(this, 120L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        buildInterface()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (fullscreen) exitFullscreen() else finish()
            }
        })
        handler.post(updater)
    }

    private fun buildInterface() {
        root = FrameLayout(this).apply { setBackgroundColor(BG) }
        stage = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(stage, FrameLayout.LayoutParams(-1, -1))

        playerView = PlayerView(this).apply {
            useController = false
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setShutterBackgroundColor(Color.BLACK)
            setBackgroundColor(Color.BLACK)
        }
        stage.addView(playerView, FrameLayout.LayoutParams(-1, -1))
        playerView.setOnClickListener { if (hasVideo) togglePlayback() }

        subtitleText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
            setShadowLayer(7f, 1f, 2f, Color.BLACK)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            maxLines = 4
            visibility = View.GONE
            isClickable = false
        }
        stage.addView(
            subtitleText,
            FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
                leftMargin = dp(14)
                rightMargin = dp(14)
            }
        )

        plusButton = TextView(this).apply {
            text = "+"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(0xFF21172E.toInt())
            background = circle(ACCENT)
            elevation = dp(8).toFloat()
            contentDescription = "Open video"
            setOnClickListener { videoPicker.launch(arrayOf("video/*")) }
        }
        stage.addView(
            plusButton,
            FrameLayout.LayoutParams(dp(76), dp(76), Gravity.CENTER)
        )

        controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(PANEL, dp(13).toFloat())
            elevation = dp(12).toFloat()
            visibility = View.GONE
        }

        val timeline = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        currentTime = makeLabel("00:00")
        totalTime = makeLabel("00:00").apply { gravity = Gravity.CENTER_VERTICAL or Gravity.END }
        seekBar = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(ACCENT)
            thumbTintList = android.content.res.ColorStateList.valueOf(ACCENT)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0xFF494650.toInt())
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val p = player ?: return
                        if (p.duration > 0) currentTime.text = timeText(p.duration * value / 1000L)
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) { seeking = true }
                override fun onStopTrackingTouch(bar: SeekBar?) {
                    val p = player
                    if (p != null && p.duration > 0) p.seekTo(p.duration * seekBar.progress / 1000L)
                    seeking = false
                }
            })
        }
        timeline.addView(currentTime, LinearLayout.LayoutParams(dp(42), dp(34)))
        timeline.addView(seekBar, LinearLayout.LayoutParams(0, dp(34), 1f).apply {
            leftMargin = dp(7); rightMargin = dp(7)
        })
        timeline.addView(totalTime, LinearLayout.LayoutParams(dp(42), dp(34)))
        controls.addView(timeline)

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }
        subtitleButton = makeButton("CC+").apply {
            setOnClickListener {
                if (cues.isEmpty()) subtitlePicker.launch(arrayOf("text/*", "application/octet-stream"))
                else {
                    captionsOn = !captionsOn
                    updateSubtitleButton()
                    updateSubtitle()
                }
            }
            setOnLongClickListener {
                subtitlePicker.launch(arrayOf("text/*", "application/octet-stream"))
                true
            }
        }
        val fullscreenButton = makeButton("⛶").apply {
            textSize = 20f
            setOnClickListener { toggleFullscreen() }
        }
        speedButton = makeButton("1.5×").apply {
            minWidth = dp(76)
            setPadding(dp(14), 0, dp(14), 0)
            setOnClickListener { toggleSpeed() }
            contentDescription = "Switch playback speed between 1.5 and 1.75 times"
        }
        actions.addView(subtitleButton, LinearLayout.LayoutParams(dp(58), dp(40)).apply { rightMargin = dp(7) })
        actions.addView(fullscreenButton, LinearLayout.LayoutParams(dp(48), dp(40)).apply { rightMargin = dp(7) })
        actions.addView(speedButton, LinearLayout.LayoutParams(dp(76), dp(40)))
        controls.addView(actions, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(4) })

        root.addView(
            controls,
            FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                leftMargin = dp(9); rightMargin = dp(9); bottomMargin = dp(9)
            }
        )
        setContentView(root)
    }

    private fun openVideo(uri: Uri) {
        val exo = player ?: ExoPlayer.Builder(this).build().also {
            player = it
            playerView.player = it
        }
        cues = emptyList()
        subtitleText.visibility = View.GONE
        captionsOn = true
        updateSubtitleButton()
        speed = SPEED_150
        speedButton.text = "1.5×"

        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.volume = 1f
        exo.setPlaybackParameters(PlaybackParameters(SPEED_150))
        exo.playWhenReady = true
        exo.prepare()

        hasVideo = true
        plusButton.visibility = View.GONE
        controls.visibility = View.VISIBLE
    }

    private fun openSubtitle(uri: Uri) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("Could not read subtitle")
            val parsed = SubtitleParser.parse(text)
            if (parsed.isEmpty()) {
                Toast.makeText(this, "No SRT/VTT subtitle cues found", Toast.LENGTH_LONG).show()
                return
            }
            cues = parsed
            captionsOn = true
            updateSubtitleButton()
            updateSubtitle()
            Toast.makeText(this, "Subtitle loaded", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open subtitle: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleSpeed() {
        val p = player ?: return
        speed = if (speed == SPEED_150) SPEED_175 else SPEED_150
        p.setPlaybackParameters(PlaybackParameters(speed))
        speedButton.text = if (speed == SPEED_150) "1.5×" else "1.75×"
    }

    private fun togglePlayback() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    private fun updateTimeline() {
        val p = player ?: return
        if (!hasVideo) return
        val duration = p.duration
        val position = p.currentPosition.coerceAtLeast(0L)
        currentTime.text = timeText(position)
        if (duration > 0) {
            totalTime.text = timeText(duration)
            if (!seeking) seekBar.progress = ((position.toDouble() / duration) * 1000).toInt().coerceIn(0, 1000)
        }
    }

    private fun updateSubtitle() {
        val p = player ?: return
        if (!hasVideo || !captionsOn || cues.isEmpty()) {
            subtitleText.visibility = View.GONE
            return
        }
        val cue = cues.firstOrNull { p.currentPosition >= it.startMs && p.currentPosition < it.endMs }
        if (cue == null) subtitleText.visibility = View.GONE
        else {
            subtitleText.text = cue.text
            subtitleText.visibility = View.VISIBLE
        }
    }

    private fun updateSubtitleButton() {
        subtitleButton.text = when {
            cues.isEmpty() -> "CC+"
            captionsOn -> "CC"
            else -> "CC OFF"
        }
    }

    private fun toggleFullscreen() {
        if (fullscreen) exitFullscreen() else enterFullscreen()
    }

    private fun enterFullscreen() {
        fullscreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitFullscreen() {
        fullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        WindowCompat.setDecorFitsSystemWindows(window, true)
    }

    private fun timeText(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun makeLabel(value: String) = TextView(this).apply {
        text = value
        textSize = 11f
        setTextColor(MUTED)
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
    }

    private fun makeButton(value: String) = Button(this).apply {
        text = value
        textSize = 12f
        isAllCaps = false
        setTextColor(WHITE)
        background = rounded(0xFF25242D.toInt(), dp(9).toFloat())
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        setStroke(dp(1), 0xFF3B3944.toInt())
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(updater)
        playerView.player = null
        player?.release()
        player = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }
}
