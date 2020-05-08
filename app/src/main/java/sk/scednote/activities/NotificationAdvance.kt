package sk.scednote.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.notification_advance.*
import sk.scednote.NoteReminder
import sk.scednote.R
import sk.scednote.model.Database


/**
 * V aktivite sa nastavuje dany casovy predstih v akom aplikacia upozorni uzivatela, ze sa termin blizi
 * ku koncu
 */
class NotificationAdvance : AppCompatActivity() {
    private lateinit var data: Database
    private var millis: Int
        get() = (((days.value * 24) + hours.value) * 60 + minutes.value) * 60000
        set(value ) {
            var time = value.coerceIn(0, (((days.maxValue * 24) + 59) * 60 + 59) * 60000) / 60000
            minutes.value = time % 60
            time /= 60
            hours.value = time % 24
            time /= 24
            days.value = time
        }

    override fun onSupportNavigateUp() = onBackPressed().let { true }
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        data = Database()
        setContentView(R.layout.notification_advance)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        days.maxValue = 7
        days.minValue = 0
        hours.maxValue = 23
        hours.minValue = 0
        minutes.maxValue = 59
        minutes.minValue = 0
        millis = NoteReminder.reminderAdvance

        set.setOnClickListener {
            NoteReminder.reminderAdvance = millis
            NoteReminder.enableReminders(data.getDeadlinedNotes())
            finish()
        }
        unset.setOnClickListener {
            NoteReminder.disableReminders(data.getDeadlinedNotes())
            finish()
        }
        cancel.setOnClickListener { finish() }
        unset.visibility = if (NoteReminder.enabled) View.VISIBLE else View.GONE
    }
    override fun onDestroy() {
        data.close()
        super.onDestroy()
    }
}
