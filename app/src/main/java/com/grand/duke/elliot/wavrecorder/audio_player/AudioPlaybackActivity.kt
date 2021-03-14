package com.grand.duke.elliot.wavrecorder.audio_player

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.grand.duke.elliot.wavrecorder.R
import com.grand.duke.elliot.wavrecorder.audio_recorder.TIMER_PATTERN
import com.grand.duke.elliot.wavrecorder.audio_recorder.view.WaveformView
import com.grand.duke.elliot.wavrecorder.base.BaseActivity
import com.grand.duke.elliot.wavrecorder.databinding.ActivityAudioPlaybackBinding
import com.grand.duke.elliot.wavrecorder.main.MainActivity
import com.grand.duke.elliot.wavrecorder.shared_preferences.SharedPreferencesManager
import com.grand.duke.elliot.wavrecorder.util.*
import com.grand.duke.elliot.wavrecorder.wav_file_list.WavFile
import com.grand.duke.elliot.wavrecorder.wav_file_list.WavFileListFragment.Companion.EXTRA_NAME_WAV_FILE
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.IOException

class AudioPlaybackActivity: BaseActivity(), WaveformView.OnTouchListener {

    private lateinit var viewModel: AudioPlaybackViewModel
    private lateinit var binding: ActivityAudioPlaybackBinding
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val uiController = UiController()

    private var playingSpeed = 1F

    enum class State {
        Initialized,
        PausePlaying,
        Playing,
        StopPlaying
    }

    private fun isPlaying() = viewModel.state == State.Playing

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, AudioPlaybackViewModelFactory())[AudioPlaybackViewModel::class.java]
        binding = DataBindingUtil.setContentView(this, R.layout.activity_audio_playback)

        binding.waveformView.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.waveformView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val wavFile = intent.getParcelableExtra<WavFile>(EXTRA_NAME_WAV_FILE)
                wavFile?.let { viewModel.setWavFile(it) }
                        ?: run {
                            // todo.error message.
                            finish()
                        }
                update(State.Initialized)
                binding.seekBar.update()
            }
        })

        binding.linearLayoutSectionRepeat.setOnClickListener {
            binding.waveformView.selectSection()
        }

        binding.imagePlayAndPause.setOnClickListener {
            onPlay()
        }

        sharedPreferencesManager = SharedPreferencesManager.getInstance(application)
        playingSpeed = sharedPreferencesManager.getPlayingSpeed()
        initPlayingSpeedSpinner()
        initSeekBar()
    }

    /** Playing. */
    private fun onPlay() {
        when(viewModel.state) {
            State.Initialized -> startPlaying()
            State.PausePlaying -> resumePlaying()
            State.Playing -> pausePlaying()
            State.StopPlaying -> startPlaying()
        }
    }

    private fun pausePlaying() {
        update(State.PausePlaying)
        viewModel.closeAudioFile()
        viewModel.audioPlayer.release()
    }

    private fun resumePlaying() {
        startPlaying()
    }

    private fun startPlaying() {
        update(State.Playing)

        if (viewModel.openAudioFile().not())
            return

        Observable.just(viewModel.audioFile).subscribeOn(Schedulers.io()).subscribe { randomAccessFile ->
            randomAccessFile?.let { file ->
                try {
                    val audioPlayer = viewModel.audioPlayer
                    audioPlayer.init(viewModel.sampleRateInHz, viewModel.bufferSize, playingSpeed)
                    file.seek(binding.waveformView.pivot() * viewModel.bufferSize.toLong())

                    var read: Int
                    while (file.read(viewModel.buffer).also { read = it } > 0) {
                        audioPlayer.play(viewModel.buffer, read)
                        coroutineScope.launch {
                            binding.waveformView.shift(1)
                            updateTimer()
                            binding.seekBar.updateProgress()
                        }
                    }

                    coroutineScope.launch {
                        stopPlaying()
                    }
                } catch (e: IOException) {
                    coroutineScope.launch {
                        if (isPlaying()) {
                            // showToast(getString(R.string.audio_player_playback_failure_message))
                            stopPlaying()
                        }
                    }

                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopPlaying() {
        update(State.StopPlaying)
        viewModel.closeAudioFile()
        viewModel.audioPlayer.release()
    }

    private fun update(state: State) {
        viewModel.state = state
        uiController.update(viewModel.state)
    }

    private fun updateTimer() {
        binding.textTimer.text = waveformView.elapsedTime().toDateFormat(TIMER_PATTERN)
    }

    private fun initPlayingSpeedSpinner() {
        val playingSpeeds = arrayOf(
                0.25F, 0.5F, 0.75F, 1.0F,
                1.25F, 1.5F, 1.75F, 2.0F
        )

        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, playingSpeeds)
        binding.appCompatSpinner.adapter = arrayAdapter
        binding.appCompatSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // todo state 처리.
                playingSpeed = playingSpeeds[position]
                viewModel.audioPlayer.init(viewModel.sampleRateInHz, viewModel.bufferSize, playingSpeed)
                sharedPreferencesManager.putPlayingSpeed(playingSpeed)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }

        binding.appCompatSpinner.setSelection(3)  // 1.0F
    }

    private inner class UiController {
        fun update(state: State) {
            when(state) {
                State.Initialized -> {
                    binding.waveformView.addInitAmplitudes(viewModel.getAmplitudes())
                }
                State.PausePlaying -> {
                    binding.imagePlayAndPause.scaleDown(0.5F, 100L) {
                        binding.imagePlayAndPause.setBackgroundResource(R.drawable.ic_round_play_arrow_24)
                        binding.imagePlayAndPause.setImageResource(android.R.color.transparent)
                        binding.imagePlayAndPause.scaleUp(1F, 100L)
                    }
                }
                State.Playing -> {
                    binding.imagePlayAndPause.scaleDown(0.5F, 100L) {
                        binding.imagePlayAndPause.setBackgroundResource(R.drawable.ic_round_pause_24)
                        binding.imagePlayAndPause.setImageResource(android.R.color.transparent)
                        binding.imagePlayAndPause.scaleUp(1F, 100L)
                    }
                }
                State.StopPlaying -> {
                    binding.imagePlayAndPause.scaleDown(0.5F, 100L) {
                        binding.imagePlayAndPause.setBackgroundResource(R.drawable.ic_round_play_arrow_24)
                        binding.imagePlayAndPause.setImageResource(android.R.color.transparent)
                        binding.imagePlayAndPause.scaleUp(1F, 100L)
                    }
                }
            }
        }
    }

    /** WaveformView.OnTouchListener */
    override fun onTouchActionDown() {
        dragWhilePlaying()
    }

    override fun onTouchActionMove() {
        updateTimer()
        binding.seekBar.updateProgress()
    }

    override fun onTouchActionUp() {
        if (binding.waveformView.end().not())
            resumePlaying()
        else
            stopPlaying()
    }

    private fun dragWhilePlaying() {
        viewModel.state = State.PausePlaying
        binding.waveformView.update(WaveformView.State.DragWhilePlaying)
        viewModel.closeAudioFile()
        viewModel.audioPlayer.release()
    }

    /** SeekBar. */
    private fun initSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    seekBar.updatePivot()
                    if (binding.waveformView.isPlaying())
                        dragWhilePlaying()
                }

                updateTimer()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (binding.waveformView.isDragWhilePlaying()) {
                    if (!binding.waveformView.end())
                        resumePlaying()
                    else
                        stopPlaying()
                }
            }
        })
    }

    private fun SeekBar.updatePivot() {
        if (this.max.isNotZero()) {
            binding.waveformView.setPivot(progress)
            binding.waveformView.invalidate()
        }
    }

    private fun SeekBar.update() {
        max = binding.waveformView.chunkCount().dec()
        if (binding.waveformView.chunkCount().isNotZero())
            progress = binding.waveformView.pivot()
    }

    private fun SeekBar.updateProgress() {
        if (binding.waveformView.chunkCount().isNotZero())
            progress = binding.waveformView.pivot()
    }
}