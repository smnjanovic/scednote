package sk.scednote

import android.Manifest
import android.app.ActionBar
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import kotlinx.android.synthetic.main.screenshot.*
import sk.scednote.model.Design
import sk.scednote.model.TimetableEditor
import sk.scednote.model.data.Ahsl

class Screenshot : AppCompatActivity() {
    companion object {
        val IMG_REQUEST = 1000
        val IMG_PERMISSION = 1001
    }
    //posuvnik a jeho ciselny indikator
    data class RangeNum(var range: SeekBar, val number: TextView)
    //tlacidlo, pozadie aktivneho a neaktivneho tlacidla, titulok vyberu, id zdroja farby popredia a pozadia
    data class Target(val btn: View, val btn_active: Int, val btn_inactive: Int, val label: Int, val target: String)

    private lateinit var rangeNum : Array<RangeNum>
    private lateinit var targets: Array<Target>
    private lateinit var tbleditor: TimetableEditor
    private var currTarget: Int = 0

    //pozicia tabulky
    private var tY = 0F
    private var tDy = 0F
    private var tlme = 0 //table last mouse event

    //pozicia obrazku a sposob zobrazenia
    private val imX = 0F
    private val imY = 0F
    private val imDx = 0F
    private val imDy = 0F
    private val imW = 0F
    private val imH = 0F
    private val ilme = 0 //image last mouse event


    //Zdroj: https://devofandroid.blogspot.com/2018/03/add-back-button-to-action-bar-android.html
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screenshot)

        //inicializacie
        tbleditor = TimetableEditor(this, timetable, scrBox)
        targets = arrayOf (
            Target(bgB, R.drawable.bg_b_active, R.drawable.bg_b_inactive, R.string.bgB, Design.BACKGROUND),
            Target(bgH, R.drawable.bg_h_active, R.drawable.bg_h_inactive, R.string.bgH, Design.TABLE_HEAD),
            Target(bgP, R.drawable.bg_p_active, R.drawable.bg_p_inactive, R.string.bgP, Design.PRESENTATIONS),
            Target(bgC, R.drawable.bg_c_active, R.drawable.bg_c_inactive, R.string.bgC, Design.COURSES)
        )
        rangeNum = arrayOf(RangeNum(rangeH, valH), RangeNum(rangeS, valS), RangeNum(rangeL, valL), RangeNum(rangeA, valA))

        //horny panel
        supportActionBar?.apply {
            setTitle(R.string.scr_tit)
            setDisplayHomeAsUpEnabled(true)
        }
        setUpEvents()
        tbleditor.fillTable() //vykreslenie tabulky
    }

    override fun onResume() {
        //prisposobenie velkosti
        super.onResume()
        val size = Point()
        this.windowManager.defaultDisplay.getRealSize(size)
        scrBox.updateLayoutParams {
            width = size.x.coerceAtMost(size.y)
            height = size.x.coerceAtLeast(size.y)
        }

        Handler().postDelayed({
            tbleditor.scaleByWidth()
            scaleFrame()
            Log.d("moriak", "${imgFit.checkedRadioButtonId}");
        }, 100)
    }

    override fun onRequestPermissionsResult(request: Int, permits: Array<out String>, grantResults: IntArray) {
        when (request) {
            IMG_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    attemptToPickImage()
                else
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Ak fotku nie je mozne pridat, stara zostava
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == IMG_REQUEST) {
            bgImage.setImageURI(data?.data ?: return)
            bgImage.updateLayoutParams {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            //TO DO(scale image by choice given )
        }
    }

    override fun onDestroy() {
        tbleditor.close()
        super.onDestroy()
    }

    /**
     * Zmeni sa cielova skupina GUI objektov nastavovani farieb
     */
    private fun changeColorTarget(target : Int = 0) {
        for (i in targets.indices)
            with(targets[i]) { btn.setBackgroundResource(if (target != i) btn_inactive else btn_active) }
        //nadpis, prip. zvysok
        if(target in targets.indices) {
            Heading.setText(targets[target].label)
            //vinimocna situacia ked nastavujem farbu platna
            if (targets[target].btn == bgB) {
                rangeA.visibility = View.GONE
                valA.visibility = View.GONE
                A.visibility = View.GONE
                rangeA.progress = 100
                valA.text = ("100")
            }
            else {
                rangeA.visibility = View.VISIBLE
                valA.visibility = View.VISIBLE
                A.visibility = View.VISIBLE
            }
            currTarget = target
            with(tbleditor.getHsl(targets[currTarget].target)!!) {
                rangeA.progress = a
                rangeH.progress = h
                rangeS.progress = s
                rangeL.progress = l
            }
        }
    }

    /**
     *  ziska nazov zdroja povodnej farby resp. hodnotu primarneho kluca v tabulke v sql
     *  Vinimky sa nekontroluju. Vzdy ak tuto funkciu volam, musim uz vediet, ci nastavujem
     *  pozadie alebo farbu vyplne / textu konkretnej skupiny buniek v tabulke (hlavicka, prednasky, cvicenia)
     */
    private fun getTarget(): String {
        if (currTarget !in targets.indices)
            throw(IndexOutOfBoundsException("currTarget ${currTarget}: No target has been chosen or it doesn't exist!"))
        return targets[currTarget].target
    }

    private fun switchView(target: View) {
        val switchable = arrayOf<View>(scrDesignBox, scrImgFitBox)
        for (p in switchable)
            p.visibility = if (p != target || target.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        Handler().postDelayed({ scaleFrame() }, 0)
    }

    private fun pickupColor():Ahsl {
        return Ahsl(rangeA.progress, rangeH.progress, rangeS.progress, rangeL.progress)
    }

    private fun scaleFrame() {
        val scaleX = (container.measuredWidth + scrBox.marginStart + scrBox.marginEnd) / scrBox.measuredWidth.toFloat()
        val scaleY = (container.measuredHeight + scrBox.marginTop + scrBox.marginBottom) / scrBox.measuredHeight.toFloat()
        val scale = scaleX.coerceAtMost(scaleY)
        scrBox.scaleX = scale
        scrBox.scaleY = scale
    }

    private fun setUpEvents() {
        for(i in targets.indices)
            targets[i].btn.setOnClickListener { changeColorTarget(i) }

        //zmena farieb uzivatelom - SeekBar
        for (i in rangeNum.indices) {
            val runner = rangeNum[i].range
            val watcher = rangeNum[i].number
            watcher.text = runner.progress.toString()
            runner.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                    watcher.text = value.toString()
                    tbleditor.recolor(pickupColor(), getTarget())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) { }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    tbleditor.storecolor(pickupColor(), getTarget())
                }
            })
        }

        //prepinanie vypinanie editorov
        scrDesign.setOnClickListener {
            switchView(scrDesignBox)
            changeColorTarget(if (currTarget in targets.indices) currTarget else 0)
        }

        //Zobrazit / skryt nastavenie pomeru stran
        fitBtn.setOnClickListener {switchView(scrImgFitBox)}

        //Nastavit ako tapetu
        screenshot.setOnClickListener {
            setAsWallpaper()
        }

        /**
         * pokus o pridanie obrazku na pozadie. Vypytat si povolenie ak ho este nemam
         * https://www.youtube.com/watch?v=O6dWwoULFI8
         */
        photo.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), IMG_PERMISSION)
                else attemptToPickImage()
            else attemptToPickImage()
        }

        //posuvanie tabulky rozvrhu - http://www.singhajit.com/android-draggable-view/
        timetable.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(view: View, motion: MotionEvent): Boolean {
                when (motion.action) {
                    MotionEvent.ACTION_DOWN -> {
                        tDy = view.y - motion.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val top = timetable.marginTop.toFloat()
                        val range = top..scrBox.measuredHeight - top - timetable.height
                        view.y = (motion.rawY + tDy  / scrBox.scaleY).coerceIn(range)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (tlme != MotionEvent.ACTION_DOWN) tY = view.y
                        else view.performClick()
                    }
                    else -> return false
                }
                tlme = motion.action
                return true
            }
        })

        bgImage.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                //TO DO("Not yet implemented")
                return true
            }
        })

        imgFit.setOnCheckedChangeListener(object : RadioGroup.OnCheckedChangeListener {
            override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {

            }
        })
    }

    private fun attemptToPickImage() {
        startActivityForResult(Intent(Intent.ACTION_PICK).also { it.type = "image/*" }, IMG_REQUEST)
    }

    private fun setAsWallpaper() {
        val wrap = ConstraintLayout.LayoutParams.WRAP_CONTENT
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        scrBox.layoutParams = RelativeLayout.LayoutParams(wrap,wrap)
        scrBox.measure(unspecified, unspecified)
        scrBox.layout(0, 0, scrBox.measuredWidth, scrBox.measuredHeight)
        val bitmap = Bitmap.createBitmap(scrBox.measuredWidth, scrBox.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        scrBox.layout(scrBox.left,scrBox.top,scrBox.right,scrBox.bottom)
        scrBox.draw(canvas);
        WallpaperManager.getInstance(this).setBitmap(bitmap)
    }
}
