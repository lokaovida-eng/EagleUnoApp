package com.eagleuno.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class NormalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 0f, 0.0, 0L)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            speedKmH: Float = 0f,
            distanceKm: Double = 0.0,
            movingTimeMillis: Long = 0L
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_normal_layout)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)

                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                val hours = (movingTimeMillis / 3600000L)
                val minutes = (movingTimeMillis % 3600000L) / 60000L
                val seconds = (movingTimeMillis % 60000L) / 1000L
                val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)

                try { views.setTextViewText(R.id.widget_speed_text, "${speedKmH.toInt()}") } catch (_: Exception) {}
                try { views.setTextViewText(R.id.widget_distance_text, "%.2f km".format(distanceKm).replace('.', ',')) } catch (_: Exception) {}
                try { views.setTextViewText(R.id.widget_time_text, formattedTime) } catch (_: Exception) {}

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateAllWidgets(
            context: Context,
            speedKmH: Float,
            distanceKm: Double = 0.0,
            movingTimeMillis: Long = 0L
        ) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, NormalWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId, speedKmH, distanceKm, movingTimeMillis)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
