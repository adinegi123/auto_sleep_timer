package com.adi121.autosleeptimer

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.adi121.autosleeptimer.databinding.ActivityMainBinding

import com.adi121.autosleeptimer.service.OverlayService
import com.adi121.autosleeptimer.utils.Constants.UPDATE_REQUEST_CODE
import com.adi121.autosleeptimer.utils.SharedPrefs
import com.adi121.autosleeptimer.utils.openAppSystemSettings
import com.adi121.autosleeptimer.utils.showAlertDialog
import com.adi121.autosleeptimer.views.SettingsActivity
import com.adi121.autosleeptimer.views.dialogue.AllowPermissionDialogue
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // shared-preference
    private val sharedPrefs by lazy {
        SharedPrefs.getInstance(this)
    }

    private val appUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private val appUpdatedListener: InstallStateUpdatedListener by lazy {
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                when {
                    installState.installStatus() == InstallStatus.DOWNLOADED -> popupSnackbarForCompleteUpdate()
                    installState.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(
                        this
                    )
                    else -> Log.d("Instalstate", installState.installStatus().toString())
                }
            }
        }
    }

    private var selectedHour = "00"
    private var selectedMinute = "00"

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("TAG---", "In activity result")
            if (Settings.canDrawOverlays(this@MainActivity)) {

            }
        }
    }

    // broadcast - receiver for fetching time

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val data = intent?.getLongExtra("timeInMillis", 0L)
            Log.d("TAG---", "onReceive: $data")
            // Update UI with data
            setTimeFromMillis(data ?: 0L)
        }
    }

    // broadcast -receiver for when timer ends

    private val receiverTimerEndRange = object:BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            val data = p1?.getBooleanExtra("isFinish",false)
            if(data==true){
                serviceNotRunning()
            }
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        enableEdgeToEdge()


        // settings launcher
        binding.ivSettings.setOnClickListener {
            startActivity(Intent(this,SettingsActivity::class.java))
        }

        // set number picker data

        // hour
        binding.numberPickerHour.minValue=0
        binding.numberPickerHour.maxValue=23

        // minute
        binding.numberPickerMin.minValue=0
        binding.numberPickerMin.maxValue=59

        // set previously selected values on launch

        binding.numberPickerHour.value=sharedPrefs?.lastUserHour?:0
        binding.numberPickerMin.value=sharedPrefs?.lastUserMinute?:0

        selectedHour = if((sharedPrefs?.lastUserHour ?: 0) < 10) "0${sharedPrefs?.lastUserHour ?: 0}" else (sharedPrefs?.lastUserHour ?: 0).toString()
        selectedMinute = if((sharedPrefs?.lastUserMinute ?: 0) < 10) "0${sharedPrefs?.lastUserMinute ?: 0}" else (sharedPrefs?.lastUserMinute ?: 0).toString()


        binding.numberPickerHour.setOnValueChangedListener(object :NumberPicker.OnValueChangeListener{
            override fun onValueChange(p0: NumberPicker?, p1: Int, p2: Int) {
                selectedHour = if(p2<10) "0$p2" else p2.toString()
            }

        })


        binding.numberPickerMin.setOnValueChangedListener(object :NumberPicker.OnValueChangeListener{
            override fun onValueChange(p0: NumberPicker?, p1: Int, p2: Int) {
                selectedMinute = if(p2<10) "0$p2" else p2.toString()
            }

        })


        // check if first time
        if(sharedPrefs?.isFirstTime==true){
            sharedPrefs?.isFirstTime=false
            showPermissionDialogue(
                Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU
            )
        }else{
            if(!Settings.canDrawOverlays(this@MainActivity)){
                showPermissionDialogue(
                    Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU
                )
            }

        }


        // register receiver

        // receiver
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiverTimerEndRange, IntentFilter("TIMER_FINISH"))


        // check if service already running
        if (isMyServiceRunning(OverlayService::class.java)) {
            // service running
            serviceRunningUiChanges()

            // receiver
            LocalBroadcastManager.getInstance(this)
                .registerReceiver(receiver, IntentFilter("REMAINING_TIME"))

        } else {
            // service not running
            serviceNotRunning()
        }


        // cancel service

        binding.tvCancel.setOnClickListener {
            // stop receiver
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)

            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
            serviceNotRunning()
        }


        binding.tvStart.setOnClickListener {

            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        101
                    )

                    if(!Settings.canDrawOverlays(this)){
                        showPermissionDialogue(false)
                    }
                }else{
                    if(Settings.canDrawOverlays(this)){
                        buttonClick()
                    }else{
                        showPermissionDialogue(false)

                    }
                }
            }else{
                if(Settings.canDrawOverlays(this)){
                    buttonClick()
                }else{
                    showPermissionDialogue(false)

                }
            }
        }
    }


    private fun showPermissionDialogue(isAndroid13:Boolean){
        AllowPermissionDialogue(
            isAndroid13
        ){
            // ask permissions here
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            )
            startForResult.launch(intent)

            notificationPermission()


        }.show(supportFragmentManager,"taggg")
    }


    private fun buttonClick(){
        if (selectedHour == "00" && selectedMinute == "00") {
            Toast.makeText(this, "Please select valid time", Toast.LENGTH_SHORT).show()
        } else {

            // start service here
            if (Settings.canDrawOverlays(this@MainActivity)) {

                // update shared-preference value

                sharedPrefs?.lastUserHour= selectedHour.toInt()
                sharedPrefs?.lastUserMinute= selectedMinute.toInt()

                val intent = Intent(this, OverlayService::class.java)
                intent.putExtra("hour", selectedHour)
                intent.putExtra("minute", selectedMinute)
                startService(intent)

                // receiver
                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(receiver, IntentFilter("REMAINING_TIME"))

                serviceRunningUiChanges()

            } else {
                showPermissionDialogue(false)
            }

        }
    }


    private fun serviceRunningUiChanges() {
        binding.tvCancel.isVisible = true
        binding.tvStart.isVisible = false

        binding.layoutTimer.isVisible = false
        binding.layoutTimerText.isVisible = true
        binding.tvLabel.text = "Remaining Time"

    }

    private fun serviceNotRunning() {
        binding.layoutTimer.isVisible = true
        binding.layoutTimerText.isVisible = false
        binding.tvLabel.text = "After how much time?"
        binding.tvCancel.isVisible = false
        binding.tvStart.isVisible = true

    }

    fun setTimeFromMillis(milliseconds: Long) {
        val totalSeconds = milliseconds / 1000
        val hours = (totalSeconds / 3600).toInt()
        val minutes = ((totalSeconds % 3600) / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()

        // Format hours and minutes to always have 2 digits (e.g., "01", "10")
        val formattedHours = String.format("%02d", hours)
        val formattedMinutes = String.format("%02d", minutes)
        val formattedSeconds = String.format("%02d", seconds)

        // Set values to TextViews
        binding.tvHour.text = formattedHours
        binding.tvMinute.text = formattedMinutes
        binding.tvSeconds.text=formattedSeconds
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }


    private fun checkForAppUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                try {
                    val installType = when {
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    }
                    if (installType == AppUpdateType.FLEXIBLE) appUpdateManager.registerListener(
                        appUpdatedListener
                    )

                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        installType!!,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
            findViewById(R.id.main),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Restart") { appUpdateManager.completeUpdate() }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.black))
        snackbar.show()
    }



    private fun notificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        if(requestCode==101){
            val permissionGranted = ContextCompat.checkSelfPermission(
                this,Manifest.permission.POST_NOTIFICATIONS
            )==PackageManager.PERMISSION_GRANTED
            if(permissionGranted){
                // perform action here
            }else{
                onPermissionDenied(requestCode, permissions, grantResults)
            }
        }
    }

    private fun onPermissionDenied(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val deniedPermissionsWithDontAskAgain = mutableListOf<String>()
        for (i in permissions.indices) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED &&
                !shouldShowRequestPermissionRationale(permissions[i])
            ) {
                deniedPermissionsWithDontAskAgain.add(permissions[i])
            }
        }
        if (deniedPermissionsWithDontAskAgain.isNotEmpty()) {

            showAlertDialog(
                title = "Permissions Required",
                message = "This app requires notification permissions to work!",
                positiveButtonText = "Ok",
                negativeButtonText = "Cancel",
                positiveAction = {
                    openAppSystemSettings()
                },
                negativeAction = {}

            )

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverTimerEndRange)
    }


}