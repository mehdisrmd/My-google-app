package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.ShieldPreferences

class StreakWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = ShieldPreferences(context)
        val days = prefs.getStreakDays()
        val badge = getMilestoneBadge(days)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)
            views.setTextViewText(R.id.widget_streak_days, days.toString())
            views.setTextViewText(R.id.widget_streak_badge, badge)

            // Main widget click opens Dashboard/Streak
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, mainPendingIntent)

            // Panic SOS button click opens MainActivity directly in Panic Mode
            val panicIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_PANIC_MODE, true)
            }
            val panicPendingIntent = PendingIntent.getActivity(
                context,
                1,
                panicIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_panic_button, panicPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    companion object {
        fun getMilestoneBadge(days: Int): String {
            return when {
                days >= 90 -> "Diamond Master"
                days >= 60 -> "Platinum Veteran"
                days >= 30 -> "Gold Warrior"
                days >= 14 -> "Silver Guardian"
                days >= 7 -> "Bronze Shield"
                days >= 3 -> "Spark"
                else -> "Novice"
            }
        }

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, StreakWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, StreakWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
