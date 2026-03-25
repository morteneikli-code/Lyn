package no.lyn.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.PendingIntentCompat

class LynWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id -> refresh(context, appWidgetManager, id) }
    }

    companion object {
        private const val PREFS = "lyn_widget"
        private const val KEY_DIST = "last_dist_km"

        /** Called from MainActivity after every completed measurement. */
        fun onMeasurementSaved(context: Context, distanceKm: Double) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putFloat(KEY_DIST, distanceKm.toFloat()).apply()

            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LynWidget::class.java))
            ids.forEach { id -> refresh(context, manager, id) }
        }

        private fun refresh(context: Context, manager: AppWidgetManager, id: Int) {
            val dist = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getFloat(KEY_DIST, -1f)

            val views = RemoteViews(context.packageName, R.layout.widget_lyn)

            if (dist >= 0f) {
                views.setTextViewText(R.id.widget_result, "%.1f km".format(dist))
                views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_last))
            } else {
                views.setTextViewText(R.id.widget_result, context.getString(R.string.widget_measure))
                views.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_tap))
            }

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntentCompat.getActivity(context, 0, intent, 0, false)
            views.setOnClickPendingIntent(R.id.widget_root, pi)

            manager.updateAppWidget(id, views)
        }
    }
}
