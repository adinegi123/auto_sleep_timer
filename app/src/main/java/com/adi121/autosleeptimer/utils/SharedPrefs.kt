package com.adi121.autosleeptimer.utils
import android.content.Context
import android.content.SharedPreferences


class SharedPrefs private constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("SharedPref", Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor

    init {
        editor = sharedPreferences.edit()
    }
    companion object {
        private var sharedPrefs: SharedPrefs? = null
        fun getInstance(context: Context): SharedPrefs? {
            if (sharedPrefs == null) {
                sharedPrefs = SharedPrefs(context)
            }
            return sharedPrefs
        }
    }

    var iSQuoteEnabled:Boolean
        get()=sharedPreferences.getBoolean("quoteEnabled",true)
        set(value) {
            editor.putBoolean("quoteEnabled",value).apply()
        }


    var lastAdShownTime:Long
    get()=sharedPreferences.getLong("lastAdShownTime",0L)
    set(value) {
        editor.putLong("lastAdShownTime",value).apply()
    }

    var lastUserMinute:Int
        get() = sharedPreferences.getInt("lastUserMinute",0)
        set(value) {
            editor.putInt("lastUserMinute",value).apply()
        }

    var lastUserHour:Int
        get() = sharedPreferences.getInt("lastUserHour",0)
        set(value) {
            editor.putInt("lastUserHour",value).apply()
        }


    var isFirstTime:Boolean
        get()=sharedPreferences.getBoolean("isFirstTime",true)
        set(value) {
            editor.putBoolean("isFirstTime",value).apply()
        }


}