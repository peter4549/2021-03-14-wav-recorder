package com.grand.duke.elliot.wavrecorder.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.core.view.isVisible
import timber.log.Timber

fun View.scaleDown (
    scale: Float,
    duration: Long,
    animationEndCallback: ((view: View) -> Unit)? = null
) {
    if (scale >= 1F) {
        Timber.e("scale must be less than 1F.")
        return
    }

    alpha = 1F
    visibility = View.VISIBLE

    this.animate()
        .scaleX(scale)
        .scaleY(scale)
        .alpha(0F)
        .setDuration(duration)
        .setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator?) {  }

            override fun onAnimationEnd(animator: Animator?) {
                visibility = View.GONE
                animationEndCallback?.invoke(this@scaleDown)
            }

            override fun onAnimationCancel(animator: Animator?) {  }
            override fun onAnimationRepeat(animator: Animator?) {  }
        })
        .start()
}

fun View.scaleUp (
    scale: Float,
    duration: Long,
    animationEndCallback: ((view: View) -> Unit)? = null
) {
    if (scale <= 0F) {
        Timber.e("scale must be greater than 0F.")
        return
    }

    alpha = 0F
    visibility = View.VISIBLE

    this.animate()
        .scaleX(scale)
        .scaleY(scale)
        .alpha(1F)
        .setDuration(duration)
        .setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator?) {  }

            override fun onAnimationEnd(animator: Animator?) {
                animationEndCallback?.invoke(this@scaleUp)
            }

            override fun onAnimationCancel(animator: Animator?) {  }
            override fun onAnimationRepeat(animator: Animator?) {  }
        })
        .start()
}

fun View.fadeIn(duration: Number, onAnimationEnd: (view: View) -> Unit) {
    this.apply {
        alpha = 0F
        visibility = View.VISIBLE

        animate()
            .alpha(1F)
            .setDuration(duration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    onAnimationEnd.invoke(this@fadeIn)
                }
            })
    }
}

fun View.fadeOut(duration: Number, onAnimationEnd: (view: View) -> Unit) {
    this.apply {
        alpha = 1F
        visibility = View.VISIBLE

        animate()
            .alpha(0F)
            .setDuration(duration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    this@fadeOut.visibility = View.GONE
                    onAnimationEnd.invoke(this@fadeOut)
                }
            })
    }
}

fun View.isNotVisible() = !isVisible

fun View.expand() {
    val matchParentMeasureSpec: Int = View.MeasureSpec.makeMeasureSpec(
            (this.parent as View).width,
            View.MeasureSpec.EXACTLY
    )

    val wrapContentMeasureSpec: Int =
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    this.measure(matchParentMeasureSpec, wrapContentMeasureSpec)
    val targetHeight: Int = this.measuredHeight
    val originHeight = this.height

    // Older versions of Android (prior to API 21) cancel the animation of 0 height views.
    this.layoutParams.height = originHeight
    this.visibility = View.VISIBLE

    val animation: Animation = object : Animation() {
        override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation?
        ) {
            if (interpolatedTime == 1F)
                this@expand.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            else
                this@expand.layoutParams.height =
                        if ((targetHeight * interpolatedTime).toInt() < originHeight)
                            originHeight
                        else
                            (targetHeight * interpolatedTime).toInt()
            this@expand.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    // 1dp/ms expansion rate
    //animation.duration = (targetHeight / this.context.resources.displayMetrics.density).toLong()
    animation.duration = 300L
    this.fadeIn(animation.duration) {}
    this.startAnimation(animation)
}

fun View.collapse(targetHeight: Int) {
    val initialHeight: Int = this.measuredHeight
    val animation: Animation = object : Animation() {
        override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation?
        ) {
            if (interpolatedTime == 1F) {
                this@collapse.layoutParams.height = targetHeight
            } else {
                this@collapse.layoutParams.height =
                        if ((initialHeight - (initialHeight * interpolatedTime).toInt()) > targetHeight)
                            initialHeight - (initialHeight * interpolatedTime).toInt()
                        else
                            targetHeight
            }

            this@collapse.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }
    }

    // 1dp/ms collapse rate
    // animation.duration = (initialHeight / this.context.resources.displayMetrics.density).toLong()

    animation.duration = 300L
    this.fadeOut(animation.duration) {}
    this.startAnimation(animation)
}