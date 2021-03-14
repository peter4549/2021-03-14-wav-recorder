package com.grand.duke.elliot.wavrecorder.base

import android.content.DialogInterface
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.grand.duke.elliot.wavrecorder.R

open class BaseActivity: AppCompatActivity() {

    private var menuRes: Int? = null
    private var onHomePressed: (() -> Unit)? = null
    private var optionsItemIdToOnSelected = mutableMapOf<Int, () -> Unit>()
    private var showHomeAsUpEnabled = false

    protected fun showToast(text: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, text, duration).show()
    }

    protected fun setDisplayHomeAsUpEnabled(toolbar: Toolbar, onHomePressed: () -> Unit) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        this.onHomePressed = onHomePressed
        showHomeAsUpEnabled = true
    }

    protected fun setOnOptionsMenu(
        toolbar: Toolbar,
        menuRes: Int,
        optionsItemIdToOnSelected: Array<Pair<Int, () -> Unit>>
    ) {
        setSupportActionBar(toolbar)
        this.menuRes = menuRes
        optionsItemIdToOnSelected.forEach {
            if (this.optionsItemIdToOnSelected.keys.notContains(it.first))
                this.optionsItemIdToOnSelected[it.first] = it.second
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.clear()
        menuRes?.let { menuInflater.inflate(it, menu) }
        return menuRes != null || showHomeAsUpEnabled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onHomePressed?.invoke()
                true
            }
            else -> {
                optionsItemIdToOnSelected[item.itemId]?.invoke()
                true
            }
        }
    }

    protected fun showMaterialAlertDialog(
        title: String?,
        message: String?,
        neutralButtonText: String?,
        neutralButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        negativeButtonText: String?,
        negativeButtonClickListener: ((DialogInterface?, Int) -> Unit)?,
        positiveButtonText: String?,
        positiveButtonClickListener: ((DialogInterface?, Int) -> Unit)?
    ) {
        val materialAlertDialog = MaterialAlertDialogBuilder(this)
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
            textMessage?.setTextAppearance(this, R.style.NanumSquareFontFamilyStyle)
            button1?.setTextAppearance(this, R.style.NanumSquareFontFamilyStyle)
            button2?.setTextAppearance(this, R.style.NanumSquareFontFamilyStyle)
            button3?.setTextAppearance(this, R.style.NanumSquareFontFamilyStyle)
        }
    }

    private fun MutableSet<Int>.notContains(element: Int) = !contains(element)
}