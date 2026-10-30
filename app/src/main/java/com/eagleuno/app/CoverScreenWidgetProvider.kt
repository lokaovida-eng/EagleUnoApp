package com.eagleuno.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class CoverScreenWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, 0f)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            speedKmH: Float
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_cover_layout)
            views.setTextViewText(R.id.tv_cover_speed, "${speedKmH.toInt()}")
            views.setTextViewText(R.id.tv_cover_unit, "KM/H")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllCoverWidgets(context: Context, speedKmH: Float) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CoverScreenWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (widgetId in allWidgetIds) {
                updateWidget(context, appWidgetManager, widgetId, speedKmH)
            }
        }
    }
}
