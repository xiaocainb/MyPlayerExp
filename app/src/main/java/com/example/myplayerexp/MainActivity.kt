package com.example.myplayerexp

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myplayerexp.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var playerViewModel: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updatePlayerProgress()

        playerViewModel = ViewModelProvider.AndroidViewModelFactory(application)
            .create(PlayerViewModel::class.java).apply {

                progressBarVisibility.observe(this@MainActivity, Observer {
                    binding.progressBar.visibility = it
                })

                videoResolution.observe(this@MainActivity, Observer {
                    binding.include.seekBar.max = mediaPlayer.duration
//                  作为消息队列防止旋转时黑屏但声音还在
                    binding.playerFrame.post {
                        resizePlayer(it.first, it.second)
                    }
                })

                controllerFrameVisibility.observe(this@MainActivity, Observer {
                    binding.include.controllerFrame.visibility = it
                })

                bufferPercent.observe(this@MainActivity, Observer {
                    binding.include.seekBar.secondaryProgress =
                        binding.include.seekBar.max * it / 100
                })

                playerStatus.observe(this@MainActivity, Observer {
                    binding.include.controlButton.isClickable = true
                    when (it) {
                        PlayerStatus.Paused -> binding.include.controlButton.setImageResource(R.drawable.ic_play)
                        PlayerStatus.Completed -> binding.include.controlButton.setImageResource(R.drawable.ic_replay)
                        PlayerStatus.NotReady -> binding.include.controlButton.isClickable = false
                        else -> binding.include.controlButton.setImageResource(R.drawable.ic_pause)
                    }
                })
            }
//      添加生命周期
        lifecycle.addObserver(playerViewModel.mediaPlayer)
//      处理点击隐藏播放界面
        binding.playerFrame.setOnClickListener {
            playerViewModel.toggleControllerVisibility()
        }
//      处理点击播放按钮
        binding.include.controlButton.setOnClickListener {
            playerViewModel.tooglePlayerStatus()
        }
//      用户拖动处理
        binding.include.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    playerViewModel.playerSeekToProgress(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
//                TODO("Not yet implemented")
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
//                TODO("Not yet implemented")
            }
        })

        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
//              将holder给到mediaPlayer
                playerViewModel.mediaPlayer.setDisplay(holder)
//              屏幕常亮
                playerViewModel.mediaPlayer.setScreenOnWhilePlaying(true)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI()
            playerViewModel.emmitVideoResolution()
        }
    }

    private fun resizePlayer(width: Int, height: Int) {
        if (width == 0 || height == 0) return
        binding.surfaceView.layoutParams = FrameLayout.LayoutParams(
            binding.playerFrame.height * width / height,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun updatePlayerProgress() {
        lifecycleScope.launch {
            while (true) {
                delay(500)
//              实时更新进度条
                binding.include.seekBar.progress = playerViewModel.mediaPlayer.currentPosition
            }
        }
    }
}