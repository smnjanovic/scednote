package sk.scednote.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import sk.scednote.R
import sk.scednote.activities.Main
import sk.scednote.scedule.TimetableImage

/**
 * Jediná úloha tohoto widgetu je zobraziť tabuľku rozvrhu, ktorá po kliknutí otvorí aplikáciu
 * Zdroj: https://www.youtube.com/playlist?list=PLrnPJCHvNZuDCoET8jL2VK4YVRNhVEy0K
 */
class ScedWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, Main::class.java)
        val pintent = PendingIntent.getActivity(context, 0, intent, 0)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            val remote = RemoteViews(context.packageName, R.layout.timetable_widget)
            remote.setImageViewBitmap(R.id.table_image, TimetableImage().drawTable())
            remote.setOnClickPendingIntent(R.id.table_image, pintent) // zistit co dalej
            appWidgetManager.updateAppWidget(appWidgetId, remote)
        }
    }
}

