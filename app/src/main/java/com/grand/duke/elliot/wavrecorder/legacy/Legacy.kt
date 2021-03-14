package com.grand.duke.elliot.wavrecorder.legacy

/*
private fun setupSeekBarThumb(seekBar: SeekBar) {
    seekBar.viewTreeObserver
        .addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (seekBar.height > 0) {
                    val seekBarThumb = ContextCompat.getDrawable(seekBar.context, R.drawable.seek_bar_thumb)
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        (seekBarThumb as BitmapDrawable).bitmap,
                        seekBar.measuredHeight,
                        seekBar.measuredHeight,
                        true
                    )
                    val bitmapDrawable = BitmapDrawable(resources, scaledBitmap)
                    bitmapDrawable.setBounds(
                        0,
                        0,
                        bitmapDrawable.intrinsicWidth,
                        bitmapDrawable.intrinsicHeight
                    )
                    seekBar.thumb = bitmapDrawable
                    seekBar.viewTreeObserver.removeOnPreDrawListener(this)
                }

                return true
            }
        })
}
 */