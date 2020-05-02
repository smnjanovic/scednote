package sk.scednote.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import sk.scednote.R
import sk.scednote.widgets.updateAppWidget

/**
 * The configuration screen for the [NotesWidget] AppWidget.
 */
class NotesWidgetConfiguration : Activity() {
    companion object {
        private const val CATEGORY = "CATEGORY"

    }

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID


    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

    }
}