package sk.scednote.model

import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import sk.scednote.R
import sk.scednote.ScedNoteApp
import java.util.*

enum class Command {
    OPTIMIZE, HOME, SUBJECTS, SCHEDULE, NOTES, DESIGN, SUB_NOTE, DAY, UNSPECIFIED;

    companion object {
        private val res = ScedNoteApp.res

        /**
         * Identifikuje príkaz
         */
        fun identifyCommand(str: String): Command {
            val edit = fun(value: String) = value.trim().toLowerCase(Locale.ROOT)
            for (v in values())
                if (edit(v.command) == edit(str))
                    return v
            ScedNoteApp.database.getSubjectByFullName(str)?.let { return SUB_NOTE }
            for (d in Day.values())
                if (d.title.trim().toLowerCase(Locale.ROOT) == str.trim().toLowerCase(Locale.ROOT))
                    return DAY
            return UNSPECIFIED
        }

        operator fun get(n: Int): Command? {
            for (v in values())
                if (v.ordinal == n)
                    return v
            return null
        }
    }

    private val command by lazy {
        res.getString(when(this) {
            OPTIMIZE -> R.string.voice_inst_optimize
            HOME -> R.string.voice_inst_home
            SUBJECTS -> R.string.voice_inst_subjects
            SCHEDULE -> R.string.voice_inst_schedule
            NOTES -> R.string.voice_inst_notes
            DESIGN -> R.string.voice_inst_design
            SUB_NOTE -> R.string.voice_inst_sub
            DAY -> R.string.voice_inst_day_of_week
            UNSPECIFIED -> R.string.unspecified_command
        })
    }

    private val cmdDescription by lazy {
        res.getString(when(this) {
            OPTIMIZE -> R.string.voice_inst_optimize_info
            HOME -> R.string.voice_inst_home_info
            SUBJECTS -> R.string.voice_inst_subjects_info
            SCHEDULE -> R.string.voice_inst_scedule_info
            NOTES -> R.string.voice_inst_notes_info
            DESIGN -> R.string.voice_inst_design_info
            SUB_NOTE -> R.string.voice_inst_sub_info
            DAY -> R.string.voice_inst_day_of_week_info
            UNSPECIFIED -> R.string.unspecified_command
        })
    }
    val spanned by lazy {
        buildSpannedString {
            bold { append("$command\n") }
            append(cmdDescription)
        }
    }

    val next by lazy {
        values().let { vals -> vals[(ordinal + 1) % vals.size] }
    }
    val prev by lazy {
        values().let { vals ->
            vals[(ordinal - 1).let { if (it >= 0) it else it + vals.size }]
        }
    }
}
