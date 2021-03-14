package com.grand.duke.elliot.wavrecorder.main

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.grand.duke.elliot.wavrecorder.base.BaseActivity
import com.grand.duke.elliot.wavrecorder.R
import com.grand.duke.elliot.wavrecorder.audio_recorder.*
import com.grand.duke.elliot.wavrecorder.audio_recorder.view.WaveformView
import com.grand.duke.elliot.wavrecorder.databinding.ActivityMainBinding
import com.grand.duke.elliot.wavrecorder.permission.Permission
import com.grand.duke.elliot.wavrecorder.util.*
import com.grand.duke.elliot.wavrecorder.util.WavHeader.writeWavHeader
import com.grand.duke.elliot.wavrecorder.wav_file_list.WavFileListFragment
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : BaseActivity(), WaveformView.OnTouchListener {

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)
    private val uiController = UiController()

    private val sampleRateInHz = SAMPLE_RATE_IN_HZ
    private val bufferSize = minBufferSize()
    private val buffer = ByteArray(bufferSize)

    enum class State {
        Initialized,
        PausePlaying,
        PauseRecording,
        Playing,
        Recording,
        StopPlaying,
        StopRecording
    }

    private fun isPlaying() = viewModel.state == State.Playing
    private fun isRecording() = viewModel.state == State.Recording

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(viewModelStore, MainViewModelFactory(application))[MainViewModel::class.java]
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setOnOptionsMenu(
            binding.toolbar,
            R.menu.menu_main_activity,
            arrayOf(
                R.id.wav_file_list to {
                    startWavFileListFragment()
                }
            )
        )

        binding.imagePlayAndPause.setOnClickListener {
            onPlay()
        }

        binding.imageRecordAndPause.setOnClickListener {
            onRecord()
        }

        binding.imageStop.setOnClickListener {
            if (isPlaying())
                stopPlaying()

            if (isRecording())
                stopRecording()

            viewModel.audioPlayer.release()
            viewModel.audioRecorder.stop()
            WavHeader.updateWavHeader(File(viewModel.audioFilePath))

            val wavFileName = binding.textInputEditTextAudioFileName.text.toString()
            val wavFileNames = FileUtil.getWavFileNames(this)

            when {
                wavFileName.isBlank() -> showToast("파일이름을 입력하세요.")
                wavFileNames?.contains(wavFileName) == true -> showToast("같은 이름의 파일이 존재합니다.")
                else -> {
                    if(binding.waveformView.chunkCount().isNotZero())
                        showSaveConfirmationDialog()
                }
            }
        }

        binding.waveformView.setOnTouchListener(this)
        initSeekBar()
    }

    override fun onStart() {
        super.onStart()
        Permission.requestPermissions(this,
            listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            showToast("failed") // TODO change to res.
            finish()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isRecording())
            pauseRecording()

        if (isPlaying())
            pausePlaying()
    }

    private fun update(state: State, overwriteRecording: Boolean = false) {
        viewModel.state = state

        when (state) {
            State.Initialized -> binding.waveformView.update(WaveformView.State.Initialized)
            State.PausePlaying -> binding.waveformView.update(WaveformView.State.PausePlaying)
            State.PauseRecording -> binding.waveformView.update(WaveformView.State.PauseRecording)
            State.Playing -> binding.waveformView.update(WaveformView.State.Playing)
            State.Recording -> {
                if (overwriteRecording)
                    binding.waveformView.update(WaveformView.State.OverwriteRecording)
                else
                    binding.waveformView.update(WaveformView.State.Recording)
            }
            State.StopPlaying -> binding.waveformView.update(WaveformView.State.StopPlaying)
            State.StopRecording -> binding.waveformView.update(WaveformView.State.StopRecording)
        }

        uiController.update(viewModel.state)
    }

    /** Playing. */
    private fun onPlay() {
        when(viewModel.state) {
            State.Initialized -> Timber.e("Invalid state.")
            State.PausePlaying -> resumePlaying()
            State.PauseRecording -> startPlaying()
            State.Playing -> pausePlaying()
            State.Recording -> Timber.e("Invalid state.")
            State.StopPlaying -> startPlaying()
            State.StopRecording -> startPlaying()
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
                    audioPlayer.init(sampleRateInHz, bufferSize, 1F)
                    file.seek(binding.waveformView.pivot() * bufferSize.toLong())

                    var read: Int
                    while (file.read(buffer).also { read = it } > 0) {
                        audioPlayer.play(buffer, read)
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

    /** Recording. */
    private fun onRecord() {
        when(viewModel.state) {
            State.Initialized -> startRecording()
            State.PausePlaying -> resumeRecording()
            State.PauseRecording -> resumeRecording()
            State.Playing -> Timber.e("Invalid state.")
            State.Recording -> pauseRecording()
            State.StopPlaying -> resumeRecording()
            State.StopRecording -> startRecording()
        }
    }

    private fun pauseRecording() {
        update(State.PauseRecording)
        viewModel.closeAudioFile()
        viewModel.audioRecorder.stop()
        binding.seekBar.update()
    }

    private fun resumeRecording() {
        update(State.Recording, true)  // Overwrite recording.

        // binding.waveformView.updateState(WaveformView.State.OVERWRITING) UI work in update => ui controller. TODO.

        if (viewModel.openAudioFile().not())
            return // TODO; show some message.

        viewModel.audioFile?.seek(binding.waveformView.pivot() * bufferSize + 44L)  // Wav header.

        viewModel.audioRecorder.start(sampleRateInHz, bufferSize, object: AudioRecorder.AudioDataCallback {
            override fun onAudioData(audioData: ByteArray, sizeInBytes: Int) {
                try {
                    viewModel.audioFile?.write(audioData, 0, sizeInBytes)
                    coroutineScope.launch {
                        byte2short(audioData).maxOrNull()?.let {
                            binding.waveformView.add(it.toInt())
                            // UPDATE UI TODO..
                            updateTimer()
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e)
                    viewModel.closeAudioFile()
                }
            }

            override fun onError() {
                Timber.e("Audio recording failed.")
                viewModel.closeAudioFile()
            }

        })
    }

    private fun startRecording() {
        update(State.Recording)

        if (viewModel.openAudioFile().not()) {
            showToast("오디오 파일을 열지 못했습니다.")
            return
        }

        viewModel.audioFile?.let { writeWavHeader(it, MONO, sampleRateInHz, BIT_DEPTH) }
        viewModel.audioRecorder.start(sampleRateInHz, bufferSize, object: AudioRecorder.AudioDataCallback {
            override fun onAudioData(audioData: ByteArray, sizeInBytes: Int) {
                try {
                    viewModel.audioFile?.write(audioData, 0, sizeInBytes)
                    coroutineScope.launch {
                        byte2short(audioData).maxOrNull()?.let {
                            binding.waveformView.add(it.toInt())
                            updateTimer()
                        }
                    }
                } catch(e: IOException) {
                    Timber.e(e)
                    viewModel.closeAudioFile()
                }
            }

            override fun onError() {
                Timber.e("Audio recording failed.")
                viewModel.closeAudioFile()
            }
        })
    }

    private fun showSaveConfirmationDialog() {
        showMaterialAlertDialog(
            title = getString(R.string.save_confirmation_dialog_title),
            message = getString(R.string.save_confirmation_dialog_message),
            neutralButtonText = getString(R.string.cancel),
            neutralButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
            },
            negativeButtonText = getString(R.string.save_confirmation_dialog_negative_button_text),
            negativeButtonClickListener = { dialogInterface, _ ->
                dialogInterface?.dismiss()
                if (FileUtil.delete(File(viewModel.audioFilePath)).not())
                    Timber.e("Failed to delete file.")
                binding.waveformView.clear()
                viewModel.audioPlayer.release()
                viewModel.audioRecorder.stop()
                update(State.Initialized)
            },
            positiveButtonText = getString(R.string.save_confirmation_dialog_positive_button_text),
            positiveButtonClickListener = { dialogInterface, _ ->
                binding.waveformView.clear()
                viewModel.audioPlayer.release()
                viewModel.audioRecorder.stop()
                dialogInterface?.dismiss()
                renameAudioFile()
                update(State.Initialized)
            }
        )
    }

    private fun stopRecording() {
        update(State.StopRecording)
        viewModel.closeAudioFile()
        viewModel.audioRecorder.stop()
    }

    private fun renameAudioFile(): Boolean {
        val name = binding.textInputEditTextAudioFileName.text.toString()

        if (name.isBlank())
            return false

        val src = File(viewModel.audioFilePath)
        val destFilePath = application.getExternalFilesDir(null).toString() + "/${name}.wav"
        return FileUtil.renameTo(src, File(destFilePath))
    }

    private fun startWavFileListFragment() {
        supportFragmentManager.beginTransaction()
            .addToBackStack(null)
            .setCustomAnimations(
                R.anim.anim_slide_in_left,
                R.anim.anim_slide_out_left,
                R.anim.anim_slide_in_right,
                R.anim.anim_slide_out_left
            ).replace(
                R.id.constraintLayout_containerView,
                WavFileListFragment(),
                null
            ).commit()
    }

    private inner class UiController {
        fun update(state: State) {
            when(state) {
                State.Initialized -> {
                    binding.waveformView.isEnabled = false
                    binding.imagePlayAndPause.isEnabled = false
                    binding.seekBar.visibility = View.GONE
                }
                State.PausePlaying -> {
                    binding.imagePlayAndPause.scaleDown(0.5F, 100L) {
                        binding.imagePlayAndPause.setBackgroundResource(R.drawable.ic_round_play_arrow_24)
                        binding.imagePlayAndPause.setImageResource(android.R.color.transparent)
                        binding.imagePlayAndPause.scaleUp(1F, 100L)
                    }

                    if (binding.seekBar.isNotVisible())
                        binding.seekBar.visibility = View.VISIBLE

                    binding.imageRecordAndPause.isEnabled = true
                }
                State.PauseRecording -> {
                    binding.imageRecordAndPause.scaleDown(0.5F, 100L) {
                        binding.imageRecordAndPause.setBackgroundResource(R.drawable.ic_round_fiber_manual_record_24)
                        binding.imageRecordAndPause.setImageResource(android.R.color.transparent)
                        binding.imageRecordAndPause.scaleUp(1F, 100L)
                    }

                    if (binding.seekBar.isNotVisible())
                        binding.seekBar.visibility = View.VISIBLE

                    binding.waveformView.isEnabled = true
                    binding.imagePlayAndPause.isEnabled = true
                }
                State.Playing -> {
                    binding.imagePlayAndPause.scaleDown(0.5F, 100L) {
                        binding.imagePlayAndPause.setBackgroundResource(R.drawable.ic_round_pause_24)
                        binding.imagePlayAndPause.setImageResource(android.R.color.transparent)
                        binding.imagePlayAndPause.scaleUp(1F, 100L)
                    }

                    if (binding.seekBar.isNotVisible())
                        binding.seekBar.visibility = View.VISIBLE

                    binding.imageRecordAndPause.isEnabled = false
                }
                State.Recording -> {
                    binding.imageRecordAndPause.scaleDown(0.5F, 100L) {
                        binding.imageRecordAndPause.setBackgroundResource(R.drawable.ic_round_pause_24)
                        binding.imageRecordAndPause.setImageResource(android.R.color.transparent)
                        binding.imageRecordAndPause.scaleUp(1F, 100L)
                    }

                    if (binding.seekBar.isVisible)
                        binding.seekBar.visibility = View.GONE

                    binding.waveformView.isEnabled = false
                    binding.imagePlayAndPause.isEnabled = false
                }
                State.StopPlaying -> {
                    binding.imagePlayAndPause.scaleDown(0.5F, 100L) {
                        binding.imagePlayAndPause.setBackgroundResource(R.drawable.ic_round_play_arrow_24)
                        binding.imagePlayAndPause.setImageResource(android.R.color.transparent)
                        binding.imagePlayAndPause.scaleUp(1F, 100L)
                    }

                    if (binding.seekBar.isNotVisible())
                        binding.seekBar.visibility = View.VISIBLE

                    binding.imageRecordAndPause.isEnabled = true
                }
                State.StopRecording -> {
                    binding.imageRecordAndPause.scaleDown(0.5F, 100L) {
                        binding.imageRecordAndPause.setBackgroundResource(R.drawable.ic_round_fiber_manual_record_24)
                        binding.imageRecordAndPause.setImageResource(android.R.color.transparent)
                        binding.imageRecordAndPause.scaleUp(1F, 100L)
                    }

                    if (binding.seekBar.isNotVisible())
                        binding.seekBar.visibility = View.VISIBLE

                    binding.waveformView.isEnabled = true
                    binding.imagePlayAndPause.isEnabled = true
                }
            }
        }
    }

    private fun updateTimer() {
        binding.textTimer.text = waveformView.elapsedTime().toDateFormat(TIMER_PATTERN)
    }

    private fun dragWhilePlaying() {
        viewModel.state = State.PausePlaying
        binding.waveformView.update(WaveformView.State.DragWhilePlaying)
        viewModel.closeAudioFile()
        viewModel.audioPlayer.release()
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