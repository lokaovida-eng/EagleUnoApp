package com.eagleuno.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class NormalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, "Bloco Neon", "Nenhuma nota recente")
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String = "Bloco Neon",
            content: String = "Nenhuma nota recente"
        ) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_normal_layout)

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                views.setTextViewText(R.id.widget_speed_text, if (title.isBlank()) "Nota" else title)
                views.setTextViewText(R.id.widget_distance_text, if (content.isBlank()) "Sem conteúdo" else content)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        fun updateAllWidgets(
            context: Context,
            title: String = "Bloco Neon",
            content: String = "Nenhuma nota recente"
        ) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, NormalWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

                for (widgetId in allWidgetIds) {
                    updateAppWidget(context, appWidgetManager, widgetId, title, content)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}
