package sk.scednote

import sk.scednote.model.data.Ahsl
import sk.scednote.model.Database
import sk.scednote.model.Design
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.screenshot.*
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException


class Screenshot : AppCompatActivity() {
    //posuvnik a jeho ciselny indikator
    data class RangeNum(var range: SeekBar, val number: TextView)

    //mnozina objektov, ktore spolu suvisia v ramci vybraneho terca nastavenia farieb
    data class Target(
        val button: View,
        val activeBgID: Int,
        val inactiveBgID: Int,
        val headingID: Int,
        val resBg: Int,
        val resFg: Int
    )

    private var targets : Array<Target> = emptyArray()
    private var currTarget: Int = -1
    private var rangeNum : Array<RangeNum> = emptyArray()

    private val data = Database(this)

    //Zdroj: https://devofandroid.blogspot.com/2018/03/add-back-button-to-action-bar-android.html
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        //parent!!.intent.putExtra("bg_b", farba)
        //parent!!.intent.putExtra("bg_h", farba)
        //parent!!.intent.putExtra("bg_p", farba)
        //parent!!.intent.putExtra("bg_c", farba)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screenshot)

        //vratit spat
        val actionBar = supportActionBar
        actionBar?.setTitle(R.string.scr_tit)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        //inicializacie
        this.targets = arrayOf(
            Target(
                bgB,
                R.drawable.bg_b,
                R.drawable.bg_b_x,
                R.string.bgB,
                R.color.des_background,
                R.color.des_background
            ),
            Target(
                bgH,
                R.drawable.bg_h,
                R.drawable.bg_h_x,
                R.string.bgH,
                R.color.des_heading_bg,
                R.color.des_heading_fg
            ),
            Target(
                bgP,
                R.drawable.bg_p,
                R.drawable.bg_p_x,
                R.string.bgP,
                R.color.des_presentations_bg,
                R.color.des_presentations_fg
            ),
            Target(
                bgC,
                R.drawable.bg_c,
                R.drawable.bg_c_x,
                R.string.bgC,
                R.color.des_courses_bg,
                R.color.des_courses_fg
            )
        )

        this.rangeNum = arrayOf(
            RangeNum(rangeH, valH),
            RangeNum(rangeS, valS),
            RangeNum(rangeL, valL),
            RangeNum(rangeA, valA)
        )

        //udalosti
        for(i in this.targets.indices)
            this.targets[i].button.setOnClickListener { changeColorTarget(i) }

        //nastavovanie farieb
        for (i in this.rangeNum.indices) {
            val numBox = this.rangeNum[i].number
            numBox.text = this.rangeNum[i].range.progress.toString()
            this.rangeNum[i].range.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                //zmena H,S,L,A
                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    numBox.text = value.toString()
                    recolor()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                //ulozenie zmeny do databazy
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    data.setColor(getTargetEntry(),
                        Ahsl(
                            rangeA.progress,
                            rangeH.progress,
                            rangeS.progress,
                            rangeL.progress
                        )
                    )
                }
            })
        }

        scrDesign.setOnClickListener {
            switchView(scrDesignBox)
            changeColorTarget(if (currTarget in targets.indices) currTarget else 0)
        }

        //nacitanie obrazku - povolenie pristupu k suborom
        photo.setOnClickListener {

        }

        //Zobrazit / skryt nastavenie pomeru stran
        fitBtn.setOnClickListener {
            switchView(scrImgFitBox)
        }

        bgIcon.setOnClickListener {
            bg_fg.isChecked = false
            it.alpha = 1.0F
            fgIcon.alpha = 0.25F
            updateColor()
        }

        fgIcon.setOnClickListener {
            bg_fg.isChecked = true
            it.alpha = 1.0F
            bgIcon.alpha = 0.25F
            updateColor()
        }

        bg_fg.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            bgIcon.alpha = if (checked) 0.25F else 1.0F
            fgIcon.alpha = if (checked) 1.0F else 0.25F
            updateColor()
        }

    }

    override fun onDestroy() {
        data.close()
        super.onDestroy()
    }

    /**
     * Zmeni sa cielova skupina GUI objektov nastavovani farieb
     */
    private fun changeColorTarget(target : Int = -1) {
        if (scrDesignBox.visibility == View.VISIBLE) {
            //zmena vzhladu tlacidiel po zmene
            for (i in targets.indices) {
                var opt = targets[i]
                opt.button.setBackgroundResource(if (target != i) opt.inactiveBgID else opt.activeBgID)
            }

            if(!targets.indices.contains(target))
                Heading.text = ""
            else {
                Heading.setText(targets[target].headingID)

                //doplnok - ak nastavujem farbu pozadia, nesmie byt priehladna
                if (targets[target].button == bgB) {
                    rangeA.progress = 100
                    valA.text = "100"
                    rangeA.visibility = View.GONE
                    valA.visibility = View.GONE
                    A.visibility = View.GONE
                }
                else {
                    rangeA.visibility = View.VISIBLE
                    valA.visibility = View.VISIBLE
                    A.visibility = View.VISIBLE
                }
                this.currTarget = target
            }
            updateColor()
        }
    }

    /**
     *  ziska nazov zdroja povodnej farby resp. hodnotu primarneho kluca v tabulke v sql
     *  Vinimky sa nekontroluju. Vzdy ak tuto funkciu volam, musim uz vediet, ci nastavujem
     *  pozadie alebo farbu vyplne / textu konkretnej skupiny buniek v tabulke (hlavicka, prednasky, cvicenia)
     */
    private fun getTargetEntry(): String {
        if (this.currTarget !in this.targets.indices)
            throw(IndexOutOfBoundsException("currTarget ${this.currTarget}: No target was chosen or it doesn't exist!"))
        val target = if (bg_fg.isChecked) this.targets[this.currTarget].resFg else this.targets[this.currTarget].resBg
        if (resources.getResourceTypeName(target) != "color")
            throw (IllegalArgumentException("Resource must be a color!"))
        return resources.getResourceEntryName(target)
    }

    private fun switchView(target: View?) {
        for (p in arrayOf<View>(scrDesignBox, scrImgFitBox)) {
            p.visibility = if (p != target || target.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    //na zaklade vyberu v GUI vyhladam farbu z databazy
    private fun updateColor() {
        if (currTarget in targets.indices) {
            val target = getTargetEntry()
            var ahsl = data.getColor(target)
            //ak zaznam v tabulke nie je, vytvorit ho
            if (ahsl == null) {
                val res = resources.getString(
                    if (bg_fg.isChecked) this.targets[this.currTarget].resFg
                    else this.targets[this.currTarget].resBg
                );
                recolor(res)
                ahsl = Design.hex2hsl(res)
                data.setColor(target, ahsl)
            }
            else {
                rangeA.progress = ahsl.a
                rangeH.progress = ahsl.h
                rangeS.progress = ahsl.s
                rangeL.progress = ahsl.l
                recolor(ahsl)
            }
            //aktualizacia farieb podla vybranej skupiny moze byt rozvrh
        }
    }

    /**
     * Viditelne zmeni farbu danych poloziek v layoute
     */
    private fun recolor(value: Any? = null) {
        var hex: String
        if (value != null) {
            if (value is String && value.length > 0 && value[0] == '#' && !value.matches("[^0-9a-fA-F]".toRegex())) {
                hex =
                    if(value.length in 4..9 && value != 6 && value != 8) value
                    else if (value.length < 4) "${value}0000".substring(0..4)
                    else if (value.length < 9) "${value}0"
                    else value.substring(0..9)
            }
            else if (value is Ahsl)
                hex = Design.hsl2hex(value)
            else
                return recolor()
        }
        else
            hex = Design.hsl2hex(
                Ahsl(
                    rangeA.progress,
                    rangeH.progress,
                    rangeS.progress,
                    rangeL.progress
                )
            )
        testrat.setBackgroundColor(Color.parseColor(hex))

    }
}
