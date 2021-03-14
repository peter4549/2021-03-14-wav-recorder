package com.grand.duke.elliot.wavrecorder.wav_file_list

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.grand.duke.elliot.wavrecorder.R
import com.grand.duke.elliot.wavrecorder.audio_player.AudioPlaybackActivity
import com.grand.duke.elliot.wavrecorder.databinding.FragmentWavFileListBinding
import com.grand.duke.elliot.wavrecorder.util.FileUtil
import java.io.File

class WavFileListFragment: Fragment(), WavFileAdapter.OnItemClickListener {

    private lateinit var viewModel: WavFileListViewModel
    private lateinit var binding: FragmentWavFileListBinding
    private lateinit var wavFileAdapter: WavFileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(viewModelStore, WavFileListViewModelFactory(requireActivity().application))[WavFileListViewModel::class.java]
        binding = FragmentWavFileListBinding.inflate(inflater, container, false)

        wavFileAdapter = WavFileAdapter(viewModel.wavFileList).apply {
            setOnItemClickListener(this@WavFileListFragment)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = wavFileAdapter
        }

        return binding.root
    }

    override fun onClick(wavFile: WavFile) {
        startAudioPlaybackActivity(wavFile)
    }

    override fun onLongClick(wavFile: WavFile, position: Int) {
        showMaterialAlertDialog(
                title = getString(R.string.delete_confirmation_dialog_title),
                message = getString(R.string.delete_confirmation_dialog_message),
                neutralButtonText = null,
                neutralButtonClickListener = null,
                negativeButtonText = getString(R.string.cancel),
                negativeButtonClickListener = { dialogFragment, _ ->
                    dialogFragment?.dismiss()
                },
                positiveButtonText = getString(R.string.delete),
                positiveButtonClickListener = { dialogFragment, _ ->
                    if(FileUtil.delete(File(wavFile.path))) {
                        viewModel.wavFileList.remove(wavFile)
                        wavFileAdapter.notifyItemRemoved(position)
                    }

                    dialogFragment?.dismiss()
                }
        )

    }

    private fun startAudioPlaybackActivity(wavFile: WavFile) {
        val intent = Intent(requireActivity(), AudioPlaybackActivity::class.java)
        intent.putExtra(EXTRA_NAME_WAV_FILE, wavFile)
        startActivity(intent)
    }

    private fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(requireContext(), text, duration).show()
    }

    private fun showMaterialAlertDialog(
            title: String?,
            message: String?,
            neutralButtonText: String?,
            neutralButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
            negativeButtonText: String?,
            negativeButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
            positiveButtonText: String?,
            positiveButtonClickListener: ((DialogInterface?, Int) -> Unit)?
    ) {
        val materialAlertDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(neutralButtonText, neutralButtonClickListener)
                .setNegativeButton(negativeButtonText, negativeButtonClickListener)
                .setPositiveButton(positiveButtonText, positiveButtonClickListener)
                .setCancelable(false)
                .show()

        val textMessage = materialAlertDialog.findViewById<TextView>(android.R.id.message)
        val button1 = materialAlertDialog.findViewById<Button>(android.R.id.button1)
        val button2 = materialAlertDialog.findViewById<Button>(android.R.id.button2)
        val button3 = materialAlertDialog.findViewById<Button>(android.R.id.button3)

        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            textMessage?.setTextAppearance(R.style.NanumSquareFontFamilyStyle)
            button1?.setTextAppearance(R.style.NanumSquareFontFamilyStyle)
            button2?.setTextAppearance(R.style.NanumSquareFontFamilyStyle)
            button3?.setTextAppearance(R.style.NanumSquareFontFamilyStyle)
        }
        else {
            textMessage?.setTextAppearance(requireContext(), R.style.NanumSquareFontFamilyStyle)
            button1?.setTextAppearance(requireContext(), R.style.NanumSquareFontFamilyStyle)
            button2?.setTextAppearance(requireContext(), R.style.NanumSquareFontFamilyStyle)
            button3?.setTextAppearance(requireContext(), R.style.NanumSquareFontFamilyStyle)
        }
    }

    companion object {
        const val EXTRA_NAME_WAV_FILE = "com.grand.duke.elliot.wavrecorder.wav_file_list" +
                ".extra_name_wav_file"
    }
}