package sk.scednote

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.util.Log
import android.widget.RemoteViews
import sk.scednote.scedule.TimetableImage
import sk.scednote.widgets.ScedWidget


/**
 * https://twigstechtips.blogspot.com/2012/12/android-quickly-access-resources-from.html
 * Zbavenie sa nutnosti vsade posielat Context
 */

class ScedNoteApp: Application() {
    companion object {
        private var context: Context? = null
        val ctx: Context get() = context!!
        val res: Resources get() = ctx.resources

        /**
         * Zdroj:
         * https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver
         */
        fun updateTableWidgets() {
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, ScedWidget::class.java))
            Log.d("moriak", "try this")
            Intent(ctx, ScedWidget::class.java).let { intent ->
                intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                ctx.sendBroadcast(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}