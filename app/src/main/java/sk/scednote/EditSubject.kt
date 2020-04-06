package sk.scednote

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.sub_edit.*
import sk.scednote.lists.TxtValid
import sk.scednote.model.Database
import sk.scednote.model.Popups
import sk.scednote.model.data.BtnFn
import sk.scednote.model.data.Subject

class EditSubject : AppCompatActivity() {

    private lateinit var data: Database
    private var subBeforeLes = false //aktivita bola spustena automaticky, pretoze bez predmetu nie je hodina
    private var id = (-1).toLong()

    override fun onSupportNavigateUp(): Boolean {
        success()
        finish()
        return true
    }

    //uspesna zmena alebo vytvorenie predmetu
    private fun success() {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra("sub_id", id) })
        finish()
    }

    private fun update(subject: Subject) {
        id = data.updateSubject(subject.id!!, txt_abb.text.toString().trim(), txt_full.text.toString().trim())
        Log.d("moriak", "Atempted update: $id")
        this.success()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sub_edit)

        data = Database(this)
        //aktivita bola spustena pri pokuse o vytvorenie hodiny. Na to vsak musi existovat predmet
        subBeforeLes = intent.getBooleanExtra("subject-before-lesson", false)
        id = intent.getLongExtra("sub_id", -1)

        //vratit spat
        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.sub_edit_tit)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        if (id > -1) {
            data.getSubject(id)?.apply {
                txt_abb.setText(abb)
                txt_full.setText(full)
            }
        }
        else
            sub_confirm.isEnabled = false

        txt_abb.addTextChangedListener(TxtValid(this, "[A-Za-zÀ-ž][0-9A-Za-zÀ-ž]+", 1..5, txt_abb, sub_confirm))
        txt_full.addTextChangedListener(TxtValid(this, "[0-9A-Za-zÀ-ž ]+", 3..40, txt_full, sub_confirm))

        sub_abort.setOnClickListener { success() }

        //inputy kontroluju spravnost vstupu
        sub_confirm.setOnClickListener {
            //hladam priznaky existencie tohoto predmetu
            var subject: Subject? = null
            if (id > -1) subject = data.getSubject(id)
            val abb = txt_abb.text.toString().trim()
            if(id < 0 && abb.isNotEmpty()) subject = data.getSubject(abb)

            //predmet neexistuje treba ho vytvorit
            if (subject == null) {
                id = data.insertSubject(txt_abb.text.toString(), txt_full.text.toString())
                success()
            }
            else {
                //predmet s touto skratkou uz existuje. Upozornujem
                if (id < 0) {
                    Popups.alert(
                        this, resources.getString(R.string.same_abb),
                        BtnFn(resources.getString(R.string.update), fun(){update(subject)}),
                        BtnFn(resources.getString(R.string.back))
                    )
                }
                else update(subject)
            }
        }
    }

    override fun onDestroy() {
        data.close()
        super.onDestroy()
    }
}
