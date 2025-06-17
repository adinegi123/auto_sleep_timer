package com.adi121.autosleeptimer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.adi121.autosleeptimer.MainActivity
import com.adi121.autosleeptimer.R
import com.adi121.autosleeptimer.utils.QuotesData
import com.adi121.autosleeptimer.utils.SharedPrefs
import kotlin.random.Random


class OverlayService: Service() {

    private lateinit var windowsManager:WindowManager
    private lateinit var overLayView:View

    private var countDownTimer:CountDownTimer?=null

    private var audioManager: AudioManager? = null
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object{
        const val ACTION_STOP_SERVICE_DIRECT = "ACTION_STOP_SERVICE_DIRECT"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if(intent?.action== ACTION_STOP_SERVICE_DIRECT){
            stopSelf()
            countDownTimer?.cancel()
            countDownTimer=null

            val receiverIntent = Intent("TIMER_FINISH")
            receiverIntent.putExtra("isFinish", true)
            LocalBroadcastManager.getInstance(this@OverlayService).sendBroadcast(receiverIntent)
        }else {

            val hour = intent?.getStringExtra("hour")
            val minute = intent?.getStringExtra("minute")

            startForegroundService()

            startTimer(hour.toString(), minute.toString()) {
                // timer ends
                showOverLay()
                requestAudioFocus()
                stopSelf()


            }
        }


        return super.onStartCommand(intent, flags, startId)

    }


    private fun startForegroundService() {
        val channelId = "overlay_service_channel"
        val channelName = "Overlay Timer Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this,OverlayService::class.java)
            .apply {
                action= ACTION_STOP_SERVICE_DIRECT
            }

        val stopServicePendingIntent = PendingIntent.getService(this,5,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Timer Running")
            .setContentText("Your countdown is active.")
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.baseline_stop,"Cancel",stopServicePendingIntent)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .build()

        startForeground(1, notification) // Start service in foreground
    }


    private fun showOverLay(){
        windowsManager=getSystemService(WINDOW_SERVICE) as WindowManager
        overLayView=LayoutInflater.from(this).inflate(R.layout.overlay_layout,null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )

        params.gravity = Gravity.CENTER

        overLayView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            windowsManager.removeView(overLayView)
            abandonAudioFocus()

        }

        if(SharedPrefs.getInstance(this)?.iSQuoteEnabled==true){
            val textQuote = overLayView.findViewById<TextView>(R.id.tvQuote)
            val textAuthor = overLayView.findViewById<TextView>(R.id.tvAuthor)

            val quote = QuotesData.quotes[Random.nextInt(0,QuotesData.quotes.size-1)]

            textQuote.text=quote.text
            textAuthor.text= quote.author ?: "Unknown"

        }

        windowsManager.addView(overLayView, params)
    }


    private fun requestAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // For Android 8.0 (API 26+) and newer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioManager?.requestAudioFocus(focusRequest)
        } else {
            // For older devices
            audioManager?.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }



    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager?.abandonAudioFocusRequest(
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).build()
            )
        } else {
            audioManager?.abandonAudioFocus(audioFocusListener)
        }
    }



    private fun startTimer(hour: String, minute: String, onFinish: () -> Unit) {

        // cancel previously running timer
        countDownTimer?.cancel()

        // Convert hours and minutes to integers
        val hh = hour.toInt()
        val mm = minute.toInt()

        // Convert total time to milliseconds
        val totalMillis = ((hh * 60 + mm) * 60 * 1000).toLong()

        // Start countdown timer
        countDownTimer= object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                //val remainingMinutes = millisUntilFinished / (60 * 1000)
                //val remainingSeconds = (millisUntilFinished / 1000) % 60
                //Log.d("TAG---", "Time left: $remainingMinutes min $remainingSeconds sec")

                val intent = Intent("REMAINING_TIME")
                intent.putExtra("timeInMillis", millisUntilFinished)
                LocalBroadcastManager.getInstance(this@OverlayService).sendBroadcast(intent)
            }

            override fun onFinish() {
                Log.d("TAG---", "Timer Completed")
                val receiverIntent = Intent("TIMER_FINISH")
                receiverIntent.putExtra("isFinish", true)
                LocalBroadcastManager.getInstance(this@OverlayService).sendBroadcast(receiverIntent)
                onFinish() // Execute action when the timer ends
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer=null
    }



}