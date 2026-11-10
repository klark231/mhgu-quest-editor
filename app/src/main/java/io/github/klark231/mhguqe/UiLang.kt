package io.github.klark231.mhguqe

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * App language. Android 13+ keeps it per app (and shows it in system settings too),
 * older versions get it from prefs and MainActivity wraps its context with it.
 */
object UiLang {
    /** "" = follow the phone */
    fun get(ctx: Context): String =
        if (Build.VERSION.SDK_INT >= 33) ctx.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
        else Prefs(ctx).uiLang

    fun set(ctx: Context, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            // the system recreates the activity by itself
            ctx.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tag)
        } else {
            Prefs(ctx).uiLang = tag
            ctx.activity()?.recreate()
        }
    }

    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = Prefs(base).uiLang
        if (tag.isEmpty()) return base
        val loc = Locale.forLanguageTag(tag)
        Locale.setDefault(loc)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocales(LocaleList(loc))
        return base.createConfigurationContext(cfg)
    }

    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }
}
