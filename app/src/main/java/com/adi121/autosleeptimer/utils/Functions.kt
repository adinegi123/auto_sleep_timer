package com.adi121.autosleeptimer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

fun Context.showAlertDialog(
    title: String? = null,
    message: String? = null,
    positiveButtonText: String = "OK",
    negativeButtonText: String = "Cancel",
    positiveAction: (() -> Unit)? = null,
    negativeAction: (() -> Unit)? = null
) {
    val builder = AlertDialog.Builder(this)

    title?.let { builder.setTitle(it) }
    message?.let { builder.setMessage(it) }

    builder.setPositiveButton(positiveButtonText) { dialog, _ ->
        positiveAction?.invoke()
        dialog.dismiss()
    }

    builder.setNegativeButton(negativeButtonText) { dialog, _ ->
        //negativeAction?.invoke()
        dialog.dismiss()
    }

    val dialog = builder.create()
    dialog.show()
}

fun Context.openAppSystemSettings() {
    startActivity(Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
    })
}