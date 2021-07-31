package com.example.myplayerexp

import android.graphics.Point
import android.media.MediaPlayer
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlayerStatus {
    Playing, Paused, Completed, NotReady
}

class PlayerViewModel : ViewModel() {
    val mediaPlayer = MyMediaPlayer()
    private var controllerShowTime = 0L

    private val _progressBarVisibility = MutableLiveData(View.VISIBLE)
    val progressBarVisibility: LiveData<Int> = _progressBarVisibility
    private val _videoResolution = MutableLiveData(Pair(0, 0))
    val videoResolution: LiveData<Pair<Int, Int>> = _videoResolution
    private val _controllerFrameVisibility = MutableLiveData(View.INVISIBLE)
    val controllerFrameVisibility: LiveData<Int> = _controllerFrameVisibility
    private val _bufferPercent = MutableLiveData(0)
    val bufferPercent: LiveData<Int> = _bufferPercent
    private val _playerStatus = MutableLiveData(PlayerStatus.NotReady)
    val playerStatus: LiveData<PlayerStatus> = _playerStatus


    init {
        loadVideo()
    }

    private fun loadVideo() {
        mediaPlayer.apply {
//            reset()加载不同视频要reset
            _progressBarVisibility.value = View.VISIBLE
            _playerStatus.value = PlayerStatus.NotReady
            setDataSource("https://assets.shimonote.com/homepage/videos/one-minute-introduction-to-shimo.mp4")

            setOnPreparedListener {
                _progressBarVisibility.value = View.INVISIBLE
                it.start()
                _playerStatus.value = PlayerStatus.Playing
            }
            setOnVideoSizeChangedListener { _, width, height ->
                _videoResolution.value = Pair(width, height)
            }
            setOnBufferingUpdateListener { _, percent ->
                _bufferPercent.value = percent
            }
            setOnSeekCompleteListener {
                mediaPlayer.start()
                _progressBarVisibility.value = View.INVISIBLE
            }
            setOnCompletionListener {
                _playerStatus.value = PlayerStatus.Completed
            }
            prepareAsync()
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer.release()
    }

    fun emmitVideoResolution() {
//       直接触发，然后调用resize。因为横屏全屏视频被拉伸，要重新调整宽高比。
        _videoResolution.value = _videoResolution.value
    }

    fun toggleControllerVisibility() {
        if (_controllerFrameVisibility.value == View.INVISIBLE) {
            _controllerFrameVisibility.value = View.VISIBLE
//          呼叫后要计时，防止点击次数过多发生碰撞
            controllerShowTime = System.currentTimeMillis()
//          延时后自动消失
            viewModelScope.launch {
                delay(3000)
                if (System.currentTimeMillis() - controllerShowTime > 3000) {
                    _controllerFrameVisibility.value = View.INVISIBLE
                }
            }
        } else {
            _controllerFrameVisibility.value = View.INVISIBLE
        }
    }

    fun playerSeekToProgress(progress: Int) {
//      处理拖动时，先让进度条出现
        _progressBarVisibility.value = View.VISIBLE
        mediaPlayer.seekTo(progress)
    }

    fun tooglePlayerStatus() {
        when (_playerStatus.value) {
            PlayerStatus.Playing -> {
                mediaPlayer.pause()
                _playerStatus.value = PlayerStatus.Paused
            }
            PlayerStatus.Paused -> {
                mediaPlayer.start()
                _playerStatus.value = PlayerStatus.Playing
            }
            PlayerStatus.Completed -> {
                mediaPlayer.start()
                _playerStatus.value = PlayerStatus.Playing
            }
            else -> return
        }
    }
}