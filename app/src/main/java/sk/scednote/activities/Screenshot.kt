package sk.scednote.activities

import android.Manifest
import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.*
import kotlinx.android.synthetic.main.screenshot.*
import sk.scednote.R
import sk.scednote.ScedNoteApp
import sk.scednote.events.Movement
import sk.scednote.model.Ahsl
import sk.scednote.model.Design
import sk.scednote.scedule.TimetableBuilder
import java.util.*
import kotlin.properties.Delegates

/**
 * Aktivita nastavuje dizajn tabulky rozvrhu a dizajn potenciálneho pozadia
 */
class Screenshot : ShakeCompatActivity() {
    companion object {
        private const val CURR_TARGET = "CURR_TARGET"
        private const val TOOLS_COLLAPSED = "COLOR_TOOLS"
        private const val IMG_REQUEST = 1000
        private const val IMG_PERMISSION = 1001

        //adresy ulozenych preferencii
        private const val COORDS = "COORDS"
        private const val TABLE_Y = "TABLE_Y"
        private const val IMAGE_FIT = "IMAGE_FIT"
        private const val IMAGE_X = "IMAGE_X"
        private const val IMAGE_Y = "IMAGE_Y"
        private const val IMAGE_URI = "IMAGE_URI"
    }

    //nastavovace farieb
    private data class Target(val title: String, val button: Button)
    //trieda vybera cielovu skupinu zafarbenia
    private inner class Targets {
        private val inactive = Color.parseColor("#44FFFFFF")
        private val targets = TreeMap<String, Target>()
        private var curr: String? = null
        val button get() = targets[curr ?: ""]?.button
        val target get() = curr

        /**
         * Vyber kategorie (cielovej skupiny pohladov zafarbenia)
         *
         * @param tar ciel udavany textovym retazcom, ktory je primarnym klucom v databaze a identifikatorom v resouces/colors
         */
        fun chooseTarget(tar: String?) {
            //odznacit stare
            targets[target ?: ""]?.button?.foreground?.setTint(inactive)

            //zvolit nove
            curr = targets[tar ?: ""]?.let {
                if (it.button.tag is String && it.button.tag == tar) tar else null
            }

            button?.let {
                val isBg = it == bgB
                rangeA.visibility = if (isBg) View.GONE else View.VISIBLE
                valA.visibility = if (isBg) View.GONE else View.VISIBLE
                A.visibility = if (isBg) View.GONE else View.VISIBLE
                if (isBg) {
                    rangeA.progress = 100
                    valA.text = ("100")
                }

                it.foreground?.setTintList(null)
                Heading.text = targets[it.tag as String]?.title

                tbleditor.getHsl(it.tag as String)?.apply {
                    rangeA.progress = a
                    rangeH.progress = h
                    rangeS.progress = s
                    rangeL.progress = l
                }
            }
        }

        /**
         * Vyber kategorie (cielovej skupiny pohladov zafarbenia)
         *
         * @param tar tlacidlo, ktore reprezentuje danu cielovu skupinu zafarbenia
         */
        fun chooseTarget(tar: Button) = chooseTarget(if (tar.tag is String) tar.tag as String else null)

        /**
         * Vlozenie novej skupiny cieloveho zafarbenia
         * @param btn Tlačidlo, ktoré prepne cieľovú skupinu Pohľadov (View)
         * @param title ako sa bude skupina volať
         * @param colorGroup Identifikator skupiny farieb (skupiny: Prednáška, Cvičenie, Hlavička tabuľky, Pozadie, Voľno)
         */
        fun put(btn: Button, title: Int, colorGroup: String) {
            targets[colorGroup] = Target(
                ScedNoteApp.res.getString(title),
                btn.apply { tag = colorGroup })
        }
    }

    //posluchac na zmenu farieb aplikovany 4 SeekBar-om
    private inner class OnRecolorListener: SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
            if (seekBar.tag is TextView) (seekBar.tag as TextView).text = seekBar.progress.toString()
            targets.target?.let { tbleditor.recolor(readColor(), it) }
        }
        override fun onStartTrackingTouch(seekBar: SeekBar) { }
        override fun onStopTrackingTouch(seekBar: SeekBar) {
            targets.target?.let { tbleditor.storecolor(readColor(), it) }
        }
    }

    private val targets = Targets()

    private lateinit var editor: Design.ImageEditor
    private lateinit var tbleditor: TimetableBuilder

    //rozlisenie zariadenia
    private var frmW by Delegates.notNull<Int>()
    private var frmH by Delegates.notNull<Int>()

    private val pickGroupListener = View.OnClickListener {
        if (it is Button) targets.chooseTarget(it)
    }
    private val onRecolorListener = OnRecolorListener()
    private val tableMovement = Movement().setUp {
        val mT = (scrBox.height / 10F).coerceAtMost(120F)
        val mL = (scrBox.width - timetable.width) / 2F
        it.setBoundsY(mT .. scrBox.height - timetable.height - mT)
        it.setBoundsX(mL .. mL)
        it.mentionScale(scrBox.scaleX)
    }
    private val imageMovement = Movement().setUp {
        it.setBoundsX(if (editor.horizontalMovement) editor.bounds else 0F .. 0F)
        it.setBoundsY(if (editor.verticalMovement) editor.bounds else 0F .. 0F)
        it.mentionScale(scrBox.scaleX)
    }

    /**
     * @get() zisti aky styl zobrazenia uzivatel zvolil (nezvoleny ak obrazok nie je k dispozicii alebo je fakt nezvoleny)
     * @set(value) nastavi velkost a polohu obrazka
     */
    private var fit: Design.ImgFit
        get() = when {
            imgCover.isChecked -> Design.ImgFit.COVER
            imgContain.isChecked -> Design.ImgFit.CONTAIN
            imgFill.isChecked -> Design.ImgFit.FILL
            imgFit.checkedRadioButtonId < 0 || bgImage.drawable == null -> Design.ImgFit.UNDEFINED
            else -> Design.ImgFit.UNDEFINED
        }
        set(value) {
            val imagePresent = fit == Design.ImgFit.UNDEFINED || value == Design.ImgFit.UNDEFINED
            scrImgFitBox.visibility = if (imagePresent) View.GONE else View.VISIBLE
            imgRemove.visibility = if (imagePresent) View.GONE else View.VISIBLE
            if (imagePresent) bgImage.setImageURI(null) else editor.setFit(value)
        }

    private var toolsCollapsed
        get() = scrDesignBox.visibility == View.GONE
        set(value) {
            scrDesignBox.visibility = if (value) View.GONE else View.VISIBLE
            scrDesign.visibility = if (value) View.VISIBLE else View.GONE
        }


    /**
     * Vytvorenie tlacidla pre screenshot a vlozenie do menu
     * @param menu Menu
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.screenshot_menu, menu)
        return true
    }

    /**
     * Krok späť
     * Zdroj: https://devofandroid.blogspot.com/2018/03/add-back-button-to-action-bar-android.html
     * @return [Boolean] true
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * reakcie na tuknutie na polozku menu
     * @param item položka menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            //nastavenie obrazku na pozadie
            R.id.shotBtn -> setAsWallpaper()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }


    /**
     * @param savedInstanceState zaloha na obnovenie
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screenshot)

        // inicializacie a nastavenia
        val size = Point()
        this.windowManager.defaultDisplay.getRealSize(size)
        editor = Design.ImageEditor(size.x.coerceAtMost(size.y), size.x.coerceAtLeast(size.y), bgImage)
        frmW = size.x.coerceAtMost(size.y)
        frmH = size.x.coerceAtLeast(size.y)
        scrBox.updateLayoutParams {
            width = frmW
            height = frmH
        }

        tbleditor = TimetableBuilder(timetable, scrBox)
        tbleditor.fillTable()
        targets.put(bgB, R.string.bgB, Design.bg_b)
        targets.put(bgH, R.string.bgH, Design.bg_h)
        targets.put(bgP, R.string.bgP, Design.bg_p)
        targets.put(bgC, R.string.bgC, Design.bg_c)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUpEvents()
        //vybrat z pamate posledne nastavenia
        accessPrefs {
            val uri = it.getString(IMAGE_URI, "")?.toUri()
            val imFit = it.getInt(IMAGE_FIT, Design.ImgFit.UNDEFINED.position)
            //uri je null alebo odkaz na obrazok
            bgImage.setImageURI(uri)

            //ak obrazok nie je nacitany, musel byt odstraneny, premenovany, mozno presunuty
            bgImage.drawable?.let { when (Design.ImgFit[imFit]) {
                Design.ImgFit.UNDEFINED -> imgFit.clearCheck()
                Design.ImgFit.FILL -> imgFill.isChecked = true
                Design.ImgFit.CONTAIN -> imgContain.isChecked = true
                Design.ImgFit.COVER -> imgCover.isChecked = true
            }}
        }
    }

    /**
     * @param saved zaloha
     */
    override fun onRestoreInstanceState(saved: Bundle) {
        //ak bol predtym rozbaleny box nastaveni farieb bude otvoreny aj po otoceni displeja
        targets.chooseTarget(saved.getString(CURR_TARGET))
        toolsCollapsed = saved.getBoolean(TOOLS_COLLAPSED, true)
        targets.target?.let { viewColorTools() }
    }

    /**
     * Ak fotku nie je mozne pridat, stara zostava
     * @param requestCode kod ziadosti
     * @param resultCode kod vysledku
     * @param result vysledne data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        super.onActivityResult(requestCode, resultCode, result)
        if(resultCode == Activity.RESULT_OK && requestCode == IMG_REQUEST) {
            val uri = result?.data
            savePrefs { it.putString(IMAGE_URI, uri?.let {uri.toString()} ?: "") }
            if (uri == null) {
                scrImgFitBox.visibility = View.GONE
                imgRemove.visibility = View.GONE
                bgImage.x = 0F
                bgImage.y = 0F
                //niesom si isty ci to uzivatel zbada kedze tato udalost nastane pred onResume
                Toast.makeText(this,
                    R.string.image_loading_error, Toast.LENGTH_SHORT).show()
            }
            else {
                bgImage.setImageURI(uri)
                if (imgFit.checkedRadioButtonId < 0)
                    imgContain.isChecked = true
                savePrefs {
                    it.remove(IMAGE_X)
                    it.remove(IMAGE_Y)
                }
            }
        }
    }

    /**
     * nastavenia velkosti a polohy obrazka a polohy tabulky podla ulozenych preferencii
     */
    override fun onResume() {
        super.onResume()
        scrBox.post {
            tbleditor.scaleByWidth(timetable.measuredWidth)
            scaleFrame()
            accessPrefs  {
                timetable.y = it.getFloat(TABLE_Y, timetable.y)
                fit = fit
                //ak si system pamata poslednu poziciu, tak po nacitani a zobrazeni obrazku nastavit
                if (it.contains(IMAGE_X) && it.contains(IMAGE_Y)) {
                    bgImage.postDelayed ({
                        bgImage.x = it.getFloat(IMAGE_X, bgImage.x)
                        bgImage.y = it.getFloat(IMAGE_Y, bgImage.y)
                    }, 50)
                }
            }
        }
    }

    /**
     * Ulozenie informacii o pozicii a prisposobeni obrazka
     */
    override fun onPause() {
        super.onPause()
        savePrefs {
            it.putFloat(TABLE_Y, timetable.y)
            it.putFloat(IMAGE_X, bgImage.x)
            it.putFloat(IMAGE_Y, bgImage.y)
            it.putInt(IMAGE_FIT, fit.position)
        }
        toolsCollapsed = scrDesignBox.visibility == View.GONE

    }

    /**
     * Zavretie databazy
     */
    override fun onDestroy() {
        tbleditor.close()
        super.onDestroy()
    }

    /**
     * Ulozenie informacii o tom ci je rozbaleny panel nastrojov na zmeny farieb
     * @param outState zaloha
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CURR_TARGET, targets.target)
        outState.putBoolean(TOOLS_COLLAPSED, toolsCollapsed)
    }

    /**
     * Pytanie si povolenia na pridavanie obrazku
     * @param request Kod ziadosti
     * @param permissions zoznam ziadosti o povolenie
     * @param granted zoznam povoleni
     */
    override fun onRequestPermissionsResult(request: Int, permissions: Array<out String>, granted: IntArray) {
        super.onRequestPermissionsResult(request, permissions, granted)
        when (request) {
            IMG_PERMISSION -> {
                if (granted.isNotEmpty() && granted[0] == PackageManager.PERMISSION_GRANTED)
                    attemptToPickImage()
                else
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun viewColorTools() {
        toolsCollapsed = false
        targets.chooseTarget(targets.button ?: bgB)
        scaleFrame()
    }

    private fun hideColorTools() {
        toolsCollapsed = true
        targets.chooseTarget(null)
        scaleFrame()
    }

    private fun readColor() = Ahsl(rangeA.progress, rangeH.progress, rangeS.progress, rangeL.progress)

    private fun attemptToPickImage() {
        startActivityForResult(Intent(Intent.ACTION_PICK).also { it.type = "image/*" },
            IMG_REQUEST
        )
    }

    /**
     * Zdroj: https://stackoverflow.com/questions/9791714/take-a-screenshot-of-a-whole-view
     */
    private fun setAsWallpaper() {
        val b = Bitmap.createBitmap(scrBox.width, scrBox.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        scrBox.draw(c)
        WallpaperManager.getInstance(this).setBitmap(b)
        Toast.makeText(this, R.string.wallpaper_set_success, Toast.LENGTH_SHORT).show()
    }

    // prisposobenie staticky nastavenej velkosti tablky velkosti dostupneho 2D priestoru
    private fun scaleFrame() {
        scrBox.post {
            val scaleX = (container.measuredWidth + scrBox.marginStart + scrBox.marginEnd) / scrBox.measuredWidth.toFloat()
            val scaleY = (container.measuredHeight + scrBox.marginTop + scrBox.marginBottom) / scrBox.measuredHeight.toFloat()
            val scale = scaleX.coerceAtMost(scaleY)
            scrBox.scaleX = scale
            scrBox.scaleY = scale
        }
    }

    //k preferenciam mozem pristupovat kedykolvek, preco si funkcie neskratit
    private fun savePrefs(fn:(SharedPreferences.Editor)->Unit) {
        with(getSharedPreferences(COORDS, 0).edit()) {
            fn(this)
            commit()
        }
    }
    private fun accessPrefs(fn:(SharedPreferences)->Unit) {
        with(getSharedPreferences(COORDS, 0)) {
            fn(this)
        }
    }

    private fun setUpEvents() {
        bgB.setOnClickListener(pickGroupListener)
        bgH.setOnClickListener(pickGroupListener)
        bgP.setOnClickListener(pickGroupListener)
        bgC.setOnClickListener(pickGroupListener)

        rangeA.tag = valA
        rangeH.tag = valH
        rangeS.tag = valS
        rangeL.tag = valL

        rangeA.setOnSeekBarChangeListener(onRecolorListener)
        rangeH.setOnSeekBarChangeListener(onRecolorListener)
        rangeS.setOnSeekBarChangeListener(onRecolorListener)
        rangeL.setOnSeekBarChangeListener(onRecolorListener)

        //prepinanie vypinanie editorov
        scrDesign.setOnClickListener { viewColorTools() }
        scrDesignClose.setOnClickListener { hideColorTools() }

        /**
         *  pokus o pridanie obrazku do pozadia rozvrhu. Vypytat si povolenie ak ho este nemam
         *  https://www.youtube.com/watch?v=O6dWwoULFI8
         */
        gallery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        IMG_PERMISSION
                    )
                else attemptToPickImage()
            else attemptToPickImage()
        }
        imgRemove.setOnClickListener {
            bgImage.setImageURI(null)
            scrImgFitBox.visibility = View.GONE
            imgRemove.visibility = View.GONE
            savePrefs { it.putString(IMAGE_URI, "") }
        }

        //objektov
        timetable.setOnTouchListener(tableMovement)
        bgImage.setOnTouchListener(imageMovement)
        imgFit.setOnCheckedChangeListener { group, _ ->
            if (bgImage.drawable != null) fit = fit
            else if (group.checkedRadioButtonId > -1) group.clearCheck()
        }
    }
}