package com.mobtech.mtplayer

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.mobtech.mtplayer.preferences.ContextUtils
import java.util.*


abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let { ctx ->
            // Be sure that prefs are initialized
            AppPreferences.initPrefs(newBase).locale?.run {
                val locale = Locale.forLanguageTag(this)
                val localeUpdatedContext = ContextUtils.updateLocale(ctx, locale)
                super.attachBaseContext(localeUpdatedContext)
                return
            }
            super.attachBaseContext(ContextUtils.updateLocale(ctx, Locale.getDefault()))
        }
    }
}
