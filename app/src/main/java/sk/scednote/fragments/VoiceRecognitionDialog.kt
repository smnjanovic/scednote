package sk.scednote.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.voice.view.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.activities.*
import sk.scednote.events.Voice
import sk.scednote.model.Command
import sk.scednote.model.Day
import java.util.*

class VoiceRecognitionDialog: AppCompatDialogFragment() {
    companion object {
        private const val BACKUP_HINT = "BACKUP_HINT"
        private const val BACKUP_HINT_VISIBILITY = "BACKUP_HINT_VISIBILITY"
        private const val BACKUP_MESSAGE = "BACKUP_MESSAGE"
    }

    private lateinit var customView: View

    //sucasne zobrazeny hlasovy pokyn, pokiak sa nachadzam v rezime zobrazenia napovedy
    private var command = Command.OPTIMIZE

    // true ak je viditelna napoveda hlasovych pokynov
    private var hint: Boolean = false
        set(hintVisible) {
            customView.prevCmd.visibility = if (hintVisible) View.VISIBLE else View.GONE
            customView.nextCmd.visibility = if (hintVisible) View.VISIBLE else View.GONE
            customView.info.visibility = if (!hintVisible) View.VISIBLE else View.GONE
            customView.hideInfo.visibility = if (hintVisible) View.VISIBLE else View.GONE
            customView.message.text = if (!hintVisible) resources.getString(R.string.press_to_command) else command.spanned
            field = hintVisible
        }

    private var recording = false

    private val voice = Voice()
    private lateinit var speech: SpeechRecognizer
    private lateinit var speechIntent: Intent

    /**
     * Dialóg s rozhraním pre zadanie hlasového pokynu. Je dostupná nápoveda pokynov.
     * @param savedInstanceState zapamatany stav fragmentu pred otocenim
     */
    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {activity ->
            //GUI
            customView = activity.layoutInflater.inflate(R.layout.voice, null)
            voiceSetup()

            //udalosti
            val toggleInfo = View.OnClickListener {clicked ->
                when (clicked) {
                    customView.info -> hint = true
                    customView.hideInfo -> hint = false
                }
            }
            val navigate = View.OnClickListener {clicked ->
                command = when (clicked) {
                    customView.prevCmd -> command.prev.let { if (it == Command.UNSPECIFIED) it.prev else it }
                    customView.nextCmd -> command.next.let { if (it == Command.UNSPECIFIED) it.next else it }
                    else -> command
                }
                customView.message.text = command.spanned
            }

            AlertDialog.Builder(activity).apply {
                setNegativeButton(activity.getString(R.string.cancel), fun(_,_) {})
                setView(customView)
                savedInstanceState?.let {
                    command = Command[it.getInt(BACKUP_HINT)]!!
                    hint = it.getBoolean(BACKUP_HINT_VISIBILITY)
                    it.getString(BACKUP_MESSAGE)?.let { msg -> customView.message.text = msg }
                }
                setTitle(activity.getString(R.string.voice_inst_heading))
                customView.apply {
                    prevCmd.setOnClickListener(navigate)
                    nextCmd.setOnClickListener(navigate)
                    info.setOnClickListener(toggleInfo)
                    hideInfo.setOnClickListener(toggleInfo)

                    //vypnut / zapnut nahravanie (Google: ma chybu, ze zastavit ide len na prvý krát 16.5.2020)
                    mic.setOnClickListener {
                        if (!recording) {
                            hint = false
                            recording = true
                            mic.setImageDrawable(resources.getDrawable(android.R.drawable.ic_notification_overlay, null))
                            customView.message.text = resources.getString(R.string.give_an_order)
                            speech.startListening(speechIntent)
                        }
                        else {
                            recording = false
                            message.text = resources.getString(R.string.wait_please)
                            mic.setImageDrawable(resources.getDrawable(android.R.drawable.ic_btn_speak_now, null))
                            speech.stopListening()
                        }
                    }
                }
            }.create()
        } ?: throw( NullPointerException("Activity must exist!"))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BACKUP_HINT, command.ordinal)
        outState.putBoolean(BACKUP_HINT_VISIBILITY, hint)
        outState.putString(BACKUP_MESSAGE, customView.message.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun notifyResult(str: String) {
        customView.message.text = str
        customView.mic.setImageDrawable(resources.getDrawable(android.R.drawable.ic_btn_speak_now, null))
        speech.stopListening()
        recording = false
    }

    private fun voiceSetup () {
        activity?.apply {
            speech = SpeechRecognizer.createSpeechRecognizer(activity)
            speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            speech.setRecognitionListener(voice)

            fun leave (obj: Class<out AppCompatActivity>, bundle: Bundle = Bundle()) {
                startActivity (Intent(this, obj).apply {
                    putExtras(bundle)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
                parent?.let { finish() }
                dismiss()
            }

            voice.setUpCommand {cmd, result ->
                val unspecified = resources.getString(R.string.unspecified_command) + ": $result"
                notifyResult(result)
                when(cmd) {
                    Command.OPTIMIZE -> {
                        ScedNoteApp.database.removeObsoleteData()
                        notifyResult(resources.getString(R.string.obsolete_subjects_gone))
                    }
                    Command.HOME -> leave(Main::class.java)
                    Command.SUBJECTS -> leave(SubList::class.java)
                    Command.SCHEDULE -> leave(Scedule::class.java)
                    Command.NOTES -> leave(NoteList::class.java)
                    Command.DESIGN -> leave(Screenshot::class.java)
                    Command.SUB_NOTE -> {
                        ScedNoteApp.database.getSubjectByFullName(result)?.id?.let { id ->
                            val bdl = Bundle().apply { putLong(NoteList.CATEGORY, id) }
                            leave(NoteList::class.java, bdl)
                        } ?: notifyResult(unspecified)
                    }
                    Command.DAY -> {
                        var d = -1
                        for (day in Day.titles.indices)
                            if (Day[day].title.trim().toLowerCase(Locale.ROOT) == result.trim().toLowerCase(Locale.ROOT))
                                d = day
                        if (d > -1) leave(Scedule::class.java, Bundle().apply { putInt(Scedule.DAYS, d) })
                        else notifyResult(unspecified)
                    }
                    Command.UNSPECIFIED -> { notifyResult(unspecified) }
                }
            }

            voice.handleError {
                speech.cancel()
                notifyResult(it)
            }
        }
    }
}