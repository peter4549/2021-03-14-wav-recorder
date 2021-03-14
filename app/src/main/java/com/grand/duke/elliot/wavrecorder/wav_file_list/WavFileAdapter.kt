package com.grand.duke.elliot.wavrecorder.wav_file_list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.grand.duke.elliot.wavrecorder.R
import com.grand.duke.elliot.wavrecorder.audio_recorder.TIMER_PATTERN
import com.grand.duke.elliot.wavrecorder.databinding.ItemWavFileBinding
import com.grand.duke.elliot.wavrecorder.util.toDateFormat

class WavFileAdapter(private val wavFileList: ArrayList<WavFile>):
    RecyclerView.Adapter<WavFileAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ViewDataBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(wavFile: WavFile, position: Int) {
            if (binding is ItemWavFileBinding) {
                binding.textName.text = wavFile.name
                binding.textDate.text = wavFile.date.toDateFormat(binding.root.context.getString(R.string.pattern_year_month_date))

                val second = (wavFile.duration.toLong() / 1000) % 60
                val minute = (wavFile.duration.toLong() / (1000 * 60)) % 60
                val hour = (wavFile.duration.toLong() / (1000 * 60 * 60)) % 24
                val time = String.format("%02d:%02d:%02d", hour, minute, second)

                binding.textTime.text = time

                binding.root.setOnClickListener {
                    onItemClickListener?.onClick(wavFile)
                }

                binding.root.setOnLongClickListener {
                    onItemClickListener?.onLongClick(wavFile, adapterPosition)
                    true
                }
            }
        }
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(onItemClickListener: OnItemClickListener) {
        this.onItemClickListener = onItemClickListener
    }

    interface OnItemClickListener {
        fun onClick(wavFile: WavFile)
        fun onLongClick(wavFile: WavFile, position: Int)
    }

    private fun from(parent: ViewGroup): ViewHolder {
        val binding = ItemWavFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(wavFileList[position], position)
    }

    override fun getItemCount(): Int = wavFileList.count()
}