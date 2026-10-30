package com.eagleuno.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class CoverScreenWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 0f)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            speedKmH: Float = 0f
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_cover_layout)

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
                views.setImageViewResource(R.id.widget_bg_image, R.drawable.theme_red)

                val speedFormatted = "${speedKmH.toInt()} km/h"
                try { views.setTextViewText(R.id.widget_speed_text, speedFormatted) } catch (_: Exception) {}

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateAllCoverWidgets(context: Context, speedKmH: Float) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, CoverScreenWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId, speedKmH)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
