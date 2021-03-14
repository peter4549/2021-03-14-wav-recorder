package com.grand.duke.elliot.wavrecorder.permission

import android.Manifest
import android.content.Context
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import timber.log.Timber

object Permission {

    fun requestPermissions(
        context: Context,
        permissions: List<String>,
        onDeniedPermissionResponses: (List<PermissionDeniedResponse>) -> Unit
    ) {
        Dexter.withContext(context)
            .withPermissions(permissions).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted())
                        Timber.d("All permissions are granted.")
                    else {
                        onDeniedPermissionResponses(report.deniedPermissionResponses)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            }).check()
    }
}