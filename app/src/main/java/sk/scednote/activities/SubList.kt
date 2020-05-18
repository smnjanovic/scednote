package sk.scednote.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.subjects.*
import sk.scednote.R
import sk.scednote.adapters.SubjectAdapter
import sk.scednote.fragments.Confirm

/**
 * Načíta zoznam predmetov
 */
class SubList : ShakeCompatActivity() {
    companion object {
        private const val SUBLIST = "SUBLIST"
        const val DELETE_DIALOG = "DELETE_DIALOG"
    }

    private lateinit var subAdapt: SubjectAdapter
    private var removalId = (-1).toLong()

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.subjects)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        savedInstanceState?.let { removalId = it.getLong(DELETE_DIALOG, -1) }

        subAdapt = SubjectAdapter(SubjectAdapter.ADAPTER_TYPE_ITEM, intent.extras)
        //nasledujuca aktivita musi vratit vysledok, a tuto metodu mozno zavolat len v aktivite
        subAdapt.onEdit {
            with(it.tag as SubjectAdapter.ItemHolder) { customizeSubject(subAdapt.getItemId(adapterPosition)) }
        }
        subAdapt.onDelete { button ->
            try {
                (button.tag as SubjectAdapter.ItemHolder).also {holder->
                    val pos = holder.adapterPosition
                    removalId = subAdapt.getItemId(pos)
                    if (!holder.isObsolete()) {
                        Confirm.newInstance(getString(R.string.subject_not_obsolete)).apply {
                            setOnConfirm { _, _ -> subAdapt.deleteRecord(pos) }
                            show(supportFragmentManager,
                                DELETE_DIALOG
                            )
                        }
                    }
                    else subAdapt.deleteRecord(pos)
                }
            }
            catch (ex: IndexOutOfBoundsException) {
                throw (ClassCastException("${ex.message}! ViewHolder is no longer available!"))
            }
            catch (ex: ClassCastException) {
                throw (ClassCastException("${ex.message}! Tag must be a reference to the viewHolder!"))
            }
        }

        subList.apply {
            layoutManager = LinearLayoutManager(this@SubList)
            adapter = subAdapt
            savedInstanceState?.let { layoutManager?.onRestoreInstanceState(it.getParcelable(SUBLIST)) }
        }

        (supportFragmentManager.findFragmentByTag(DELETE_DIALOG) as Confirm?)?.let {
            val pos = subAdapt.getPositionById(removalId)
            if  (pos in 0 until subAdapt.itemCount)
                it.setOnConfirm { _, _ -> subAdapt.deleteRecord(pos) }
            else
                it.dismiss()
        }
        addSubject.setOnClickListener { customizeSubject() }
    }

    /**
     * Uprava zoznamu, ak bol predmet pridany, upraveny alebo vymazany
     * @param requestCode kod ziadosti
     * @param resultCode kod vysledku
     * @param data ýsledné dáta
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EditSubject.SUB_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.let {
                val result = it.getIntExtra(EditSubject.ACTION, EditSubject.CANCELED)
                val id = it.getLongExtra(EditSubject.SUB_ID, -1)
                when (result) {
                    EditSubject.UPDATED -> subAdapt.updateRecord(id)
                    EditSubject.REPLACED -> subAdapt.reload()
                    EditSubject.MERGED -> subAdapt.reload()
                    EditSubject.ADDED -> subAdapt.insertRecord(id)
                    else -> {}
                }
            }
        }
    }

    /**
     * vytvorenie zalohy
     * @param outState balik zaloh
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        subAdapt.backupData(outState)
        outState.putLong(DELETE_DIALOG, removalId)
    }

    private fun customizeSubject(id: Long? = null) {
        startActivityForResult(Intent(this@SubList, EditSubject::class.java).apply {
            id?.let{ putExtra(EditSubject.SUB_ID, it) }
        }, EditSubject.SUB_REQUEST_CODE)
    }
}
