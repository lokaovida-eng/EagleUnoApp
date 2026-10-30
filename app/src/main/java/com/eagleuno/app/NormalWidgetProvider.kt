package com.eagleuno.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class NormalWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, 0f, 0f)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            speedKmH: Float,
            distanceKm: Float
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_normal_layout)
            views.setTextViewText(R.id.tv_widget_speed, "${speedKmH.toInt()}")
            views.setTextViewText(R.id.tv_widget_unit, "KM/H")
            views.setTextViewText(R.id.tv_widget_distance, String.format("%.1f km", distanceKm))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context, speedKmH: Float, distanceKm: Float) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, NormalWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (widgetId in allWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId, speedKmH, distanceKm)
            }
        }
    }
}
