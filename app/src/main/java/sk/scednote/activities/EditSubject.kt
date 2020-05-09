package sk.scednote.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.sub_edit.*
import sk.scednote.R
import sk.scednote.events.TxtValid
import sk.scednote.fragments.Confirm
import sk.scednote.model.Database
import sk.scednote.model.Subject
import java.util.*

class EditSubject : AppCompatActivity() {
    companion object {
        const val SUB_REQUEST_CODE = 1
        const val SUB_ID = "sub_id"

        //vysledok aktivity - dialog
        const val      POPUP_MERGE = "MERGE"
        const val    POPUP_REPLACE = "REPLACE"
        const val           ACTION = "ACTION"
        const val         CANCELED = 1000
        const val          UPDATED = 1001
        const val         REPLACED = 1002
        const val           MERGED = 1003
        const val            ADDED = 1004

        //bundle - pre rotaciu displeja
        const val BUNDLE_ABB       = "abb"
        const val BUNDLE_FULL      = "full"
        const val BUNDLE_ABB_ERR   = "abb_err"
        const val BUNDLE_FULL_ERR  = "full_err"
    }

    private lateinit var data: Database
    private var id = (-1).toLong()

    /**
     * Navrat
     * @return [Boolean] true
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Navrat
     */
    override fun onBackPressed() {
        submit(CANCELED)
        finish()
    }

    //uspesna zmena alebo vytvorenie predmetu
    private fun submit(action: Int) {
        setResult(if (action != CANCELED) Activity.RESULT_OK else Activity.RESULT_CANCELED, Intent().apply {
            putExtra(SUB_ID, id)
            putExtra(ACTION, action)
        })
        finish()
    }

    private fun parseSubject(): Subject {
        return Subject(
            id,
            txt_abb.text.toString().trim().toUpperCase(Locale.ROOT),
            txt_full.text.toString()
        )
    }
    private fun mergeSubjects(inputSubject: Subject, targetSubject: Subject) {
        data.mergeSubjects(inputSubject, targetSubject)
        id = targetSubject.id
        submit(MERGED)
    }
    private fun replaceSubject(inputSubject: Subject, targetSubject: Subject) {
        data.updateSubject(targetSubject.id, targetSubject.abb, inputSubject.full)
        id = targetSubject.id
        submit(REPLACED)
    }

    /**
     * Priprava aktivity
     * @param saved zaloha
     */
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContentView(R.layout.sub_edit)
        //vratit spat
        supportActionBar?.let {
            it.setTitle(if (id < 0) R.string.sub_new_tit else R.string.sub_edit_tit)
            it.setDisplayHomeAsUpEnabled(true)
        }

        data = Database()
        //aktivita bola spustena pri pokuse o vytvorenie hodiny. Na to vsak musi existovat predmet
        id = intent.getLongExtra(SUB_ID, -1)

        //vyplnenie dat
        if (id > -1)
            data.getSubject(id)?.apply {
                txt_abb.setText(abb)
                txt_full.setText(full)
            }
        else
            sub_confirm.isEnabled = false

        //udalosti
        val fn = fun(): Boolean {
            val abb = txt_abb.error == null && txt_abb.text?.isNotEmpty() ?: false
            val full = txt_full.error == null && txt_full.text?.isNotEmpty() ?: false
            return abb && full
        }
        txt_full.addTextChangedListener(TxtValid(txt_full, "[A-Za-zÀ-ž][0-9A-Za-zÀ-ž ]*", 3..40, sub_confirm, fn))
        txt_abb.addTextChangedListener(TxtValid(txt_abb, "[A-Za-zÀ-ž][0-9A-Za-zÀ-ž]*", 1..5, sub_confirm, fn))
        sub_abort.setOnClickListener { submit(CANCELED) }

        sub_confirm.setOnClickListener {
            val resultSub = parseSubject()
            val idSub = if (id > 0) data.getSubject(id) else null
            val abbSub = data.getSubject(resultSub.abb)

            //novy predmet
            if (idSub == null && abbSub == null) {
                id = data.insertSubject(resultSub.abb, resultSub.full)
                submit(ADDED)
            }
            //2 predmety s rovnakou skratkou
            else if (idSub != null && abbSub != null && abbSub.id != id) {
                val msg = resources.getString(R.string.same_abb)
                val merge = resources.getString(R.string.merge)
                Confirm.newInstance(msg, merge).apply {
                    setOnConfirm { _, _ -> mergeSubjects(resultSub, abbSub) }
                    show(supportFragmentManager, POPUP_MERGE)
                }
            }
            //novy predmet s uz existujucou skratkou
            else if (idSub == null && abbSub != null) {
                val msg = resources.getString(R.string.used_abb)
                val update = resources.getString(R.string.update)
                Confirm.newInstance(msg, update).apply {
                    setOnConfirm { _, _ -> replaceSubject(resultSub, abbSub) }
                    show(supportFragmentManager, POPUP_REPLACE)
                }
            }
            //aktualizacia
            else {
                data.updateSubject(resultSub.id, resultSub.abb, resultSub.full)
                submit(UPDATED)
            }
        }

        (supportFragmentManager.findFragmentByTag(POPUP_REPLACE) as Confirm?)?.let { popup ->
            popup.setOnConfirm { _, _ ->
                data.getSubject(txt_abb.text.toString().trim())?.let {
                    replaceSubject(parseSubject(), it)
                } ?: popup.dismiss()
            }
        }

        (supportFragmentManager.findFragmentByTag(POPUP_MERGE) as Confirm?)?.let { popup ->
            popup.setOnConfirm { _, _ ->
                data.getSubject(txt_abb.text.toString().trim())?.let {
                    mergeSubjects(parseSubject(), it)
                } ?: popup.dismiss()
            }
        }
    }

    /**
     * zálohovanie stavu aktivity pred systémom vynútením ukončením
     * @param outState záloha
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_ABB, txt_abb.text.toString())
        outState.putString(BUNDLE_FULL, txt_full.text.toString())
        outState.putString(BUNDLE_ABB_ERR, txt_abb.error?.toString() ?: "")
        outState.putString(BUNDLE_FULL_ERR, txt_full.error?.toString() ?: "")
    }

    /**
     * obnova predosleho stavu systemom okoncenej aktivity
     * @param inState zdroj zalohy
     */
    override fun onRestoreInstanceState(inState: Bundle) {
        super.onRestoreInstanceState(inState)
        txt_abb.setText(inState.getString(BUNDLE_ABB))
        txt_full.setText(inState.getString(BUNDLE_FULL))
        val abbErr = inState.getString(BUNDLE_ABB_ERR) ?: ""
        val fullErr = inState.getString(BUNDLE_FULL_ERR) ?: ""
        txt_abb.error = if (abbErr.isNotEmpty()) inState.getString(BUNDLE_ABB_ERR) else null
        txt_full.error = if (fullErr.isNotEmpty()) inState.getString(BUNDLE_FULL_ERR) else null
    }

    /**
     * Zavretie databaz
     */
    override fun onDestroy() {
        data.close()
        super.onDestroy()
    }
}
