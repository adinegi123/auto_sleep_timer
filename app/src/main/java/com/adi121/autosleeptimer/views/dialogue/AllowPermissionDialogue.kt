package com.adi121.autosleeptimer.views.dialogue

import android.graphics.Point
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.adi121.autosleeptimer.R
import com.adi121.autosleeptimer.databinding.DialogPermissionsBinding


class AllowPermissionDialogue(
    private val shouldShowNotificationPerm:Boolean,
    private val onClick:()->Unit
): DialogFragment() {


    private lateinit var dialogBinding: DialogPermissionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialogBinding=DialogPermissionsBinding.inflate(inflater)
        return dialogBinding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setLayout(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )

        try {
            // Set the dialog to not focusable (makes navigation ignore us adding the window)
            dialog!!.window!!.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            )

            // Show the dialog!
            dialog!!.setOnShowListener { dialogInterface ->
                // Set the dialog to immersive
                dialog!!.window!!
                    .decorView.systemUiVisibility =
                    dialog!!.ownerActivity!!.window.decorView.systemUiVisibility

                // Clear the not focusable flag from the window
                dialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        dialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_rounded_background)
        dialog?.setCancelable(true)

        if(!shouldShowNotificationPerm){
            dialogBinding.ivNotification.isVisible=false
            dialogBinding.tvNotificationPer.isVisible=false
            dialogBinding.tvNotificationDetail.isVisible=false
        }


        dialogBinding.tvAllow.setOnClickListener {
            dismiss()
            onClick()
        }


    }

    override fun onStart() {
        super.onStart()

        val window: Window? = dialog!!.window
        val size = Point()

        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)

        val width: Int = size.x

        window.setLayout((width * 0.85).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }


}