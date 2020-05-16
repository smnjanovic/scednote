package sk.scednote

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import sk.scednote.activities.NoteList
import sk.scednote.model.Note

/**
 * K úloham pripnutých k predmetom sa vzťahuju notifikácie na bežiace na pozadí
 */
class NoteReminder : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "CHANNEL_ID"
        private const val CHANNEL_NOTE = "CHANNEL_NOTE"
        private const val CHANNEL_NOTE_DESC = "Note reminder channel!"
        private const val CHANNEL_NOTE_GROUP = "CHANNEL_NOTE_GROUP"
        private const val TITLE = "TITLE"
        private const val CONTENT = "CONTENT"
        private const val NOTE_ID = "NOTE_ID"
        private const val NOTE_WHEN = "WHEN"
        private const val TIME = "TIME"
        private const val NOTE_REMINDER_ADVANCE = "NOTE_REMINDER_ADVANCE"
        private const val NOTIFICATIONS_ENABLED = "NOTIFICATIONS_ENABLED"
        /**
         * predstih v upozorneniach
         */
        var reminderAdvance: Int
            get() = ScedNoteApp.ctx.getSharedPreferences(NOTE_REMINDER_ADVANCE, Context.MODE_PRIVATE).getInt(NOTE_REMINDER_ADVANCE, 3600000)
            set(millis) {
                ScedNoteApp.ctx.getSharedPreferences(NOTE_REMINDER_ADVANCE, Context.MODE_PRIVATE).edit().apply {
                    putInt(NOTE_REMINDER_ADVANCE, millis)
                    apply()
                }
            }
        var enabled: Boolean
            get() = ScedNoteApp.ctx.getSharedPreferences(NOTIFICATIONS_ENABLED,
                Context.MODE_PRIVATE).getBoolean(NOTIFICATIONS_ENABLED, true)
            set(value) {
                ScedNoteApp.ctx.getSharedPreferences(NOTIFICATIONS_ENABLED, Context.MODE_PRIVATE).edit().apply {
                    putBoolean(NOTIFICATIONS_ENABLED, value)
                    apply()
                }
            }

        /**
         * Prevod ID Poznamky typu long na ID notifikacie typu Int. Aky pretecie kapacita, pocita sa
         * znova od 0.
         *
         * @param id [Long] ID of the Note
         * @return [Int] ID of the notification
         */
        fun noteIdToInt (id: Long) = (id % Int.MAX_VALUE).toInt()

        private fun getAlarmPIntent(note: Note) = ScedNoteApp.ctx.let { ctx ->
            Intent(ctx, NoteReminder::class.java).let {intent ->
                note.deadline?.timeInMillis?.let {millis ->
                    intent.action = CHANNEL_ID
                    intent.putExtra(NOTE_ID, note.id)
                    intent.putExtra(TIME, millis)
                    intent.putExtra(TITLE, note.sub.abb)
                    intent.putExtra(CONTENT, note.info)
                    intent.putExtra(NOTE_WHEN, note.deadline!!.timeInMillis)
                    PendingIntent.getBroadcast(ctx, noteIdToInt(note.id), intent, PendingIntent.FLAG_UPDATE_CURRENT)
                }
            }
        }

        /**
         * Vytvorí sa kanál, v ktorom budu zoskupene notifikácie na úlohy
         */
        fun createNoteReminderChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = ScedNoteApp.ctx.getSystemService(NotificationManager::class.java)!!
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, CHANNEL_NOTE, NotificationManager.IMPORTANCE_HIGH).apply {
                        description = CHANNEL_NOTE_DESC
                    }
                )
            }
        }

        /**
         * Nastavit notifikaciu
         * @param note [Note] poznamka ku ktorej nastavujem pripomienku
         */
        fun setReminder(note: Note) {
            if (enabled && note.deadline != null) {
                getAlarmPIntent(note)?.let { pintent ->
                    val alarm = ScedNoteApp.ctx.getSystemService(ALARM_SERVICE) as AlarmManager
                    alarm.setExact(RTC_WAKEUP, note.deadline!!.timeInMillis - reminderAdvance, pintent)
                }
            }
        }

        /**
         * Zrusit notifikaciu
         * @param note [Note] Ooznamka, ktorej pripomienku rusim
         */
        fun cancelReminder(note: Note) {
            getAlarmPIntent(note)?.let { (ScedNoteApp.ctx.getSystemService(ALARM_SERVICE) as AlarmManager).cancel(it) }
        }

        /**
         * Upravit vsetky notifikacie
         *
         * @param notes [ArrayList][Note] Zoznam poznamok
         */
        fun enableReminders(notes: ArrayList<Note>) {
            enabled = true
            for (note in notes)
                setReminder(note)
        }

        /**
         * vypnut vsetky notifikacie
         */
        fun disableReminders(notes: ArrayList<Note>) {
            for (note in notes)
                cancelReminder(note)
            enabled = false
        }
    }

    /**
     * Metoda na zaklade vstupu rozhodne aku (s akym obsahom) notifikaciu zobrazi
     *
     * @param context
     * @param intent
     */
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == CHANNEL_ID) {
            val noteID = noteIdToInt(intent.getLongExtra(NOTE_ID, 0))
            val openIntent = Intent(context, NoteList::class.java).let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                PendingIntent.getActivity(context, noteID, it, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.scednote)
                .setContentTitle(intent.getStringExtra(TITLE))
                .setContentText(intent.getStringExtra(CONTENT))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(openIntent)
                .setAutoCancel(true)
                .setGroup(CHANNEL_NOTE_GROUP)
                .setGroupSummary(true)
                .setWhen(intent.getLongExtra(NOTE_WHEN, System.currentTimeMillis()))
                .build()
            NotificationManagerCompat.from(context).notify(noteID, notification)
        }
    }
}