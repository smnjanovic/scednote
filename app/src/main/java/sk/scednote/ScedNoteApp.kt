package sk.scednote

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import sk.scednote.widgets.NotesWidget
import sk.scednote.widgets.ScedWidget


/**
 * https://twigstechtips.blogspot.com/2012/12/android-quickly-access-resources-from.html
 * Zbavenie sa nutnosti posielat Context ako hlavny parameter
 * Využitie metód na aktualizáciu widgetov
 */

class ScedNoteApp: Application() {
    companion object {
        private var context: Context? = null
        val ctx: Context get() = context!!
        val res: Resources get() = ctx.resources

        /**
         * Aktualizuje widgety s tabulkou rozvrhu
         *
         * Zdroj:
         * https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver
         */
        fun updateTableWidgets() {
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(ComponentName(ctx, ScedWidget::class.java))
            Intent(ctx, ScedWidget::class.java).let { intent ->
                intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                ctx.sendBroadcast(intent)
            }
        }

        /**
         * Aktualizuje widgety so zoznamom uloh vybranych podla kategorie
         *
         * Zdroj:
         * https://stackoverflow.com/questions/3455123/programmatically-update-widget-from-activity-service-receiver
         */
        fun updateNoteWidgets() {
            val manager = AppWidgetManager.getInstance(ctx)
            val ids = manager.getAppWidgetIds(ComponentName(ctx, NotesWidget::class.java))
            Intent(ctx, NotesWidget::class.java).let { intent ->
                intent.action = "android.appwidget.action.APPWIDGET_UPDATE"
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                ctx.sendBroadcast(intent)
            }
        }
    }

    /**
     * Vytvori vsade dostupny context
     */
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}