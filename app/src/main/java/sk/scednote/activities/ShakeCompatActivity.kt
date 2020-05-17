package sk.scednote.activities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import sk.scednote.R
import sk.scednote.fragments.VoiceRecognitionDialog
import sk.scednote.sensors.ShakeSensor

/**
 * Aktivita s implementovanými senzormi pohybu a
 */
abstract class ShakeCompatActivity: AppCompatActivity() {
    companion object {
        private const val GIMME_VOICE = 10
        private const val VOICE_FRAGMENT = "VOICE_FRAGMENT"
        private const val UNSHAKEN = "UNSHAKEN"
    }
    private val shake = ShakeSensor()

    /**
     * Keď zatrasiem, potrebujem povolenie zvukového nahrávania, aby aby bolo možné zadávať hlasové príkazy
     * @param savedInstanceState uložená záloha
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shake.threshold = 10
        shake.setOnShake { showVoiceGUI() }
        val unshaken = getSharedPreferences(UNSHAKEN, Context.MODE_PRIVATE).getBoolean(UNSHAKEN, true)
        if (unshaken)
            Toast.makeText(this, resources.getString(R.string.unshaken), Toast.LENGTH_SHORT).show()
    }

    /**
     * Po zatrasení sa zobrazí rozhranie pre zadávanie hlasových pokynov. Aby to malo význam, musí byť povolené nahrávanie hlasu
     * @param request kód žiadosti
     * @param permissions zoznam potrebných povolení
     * @param granted zoznam povolených poolení
     */
    override fun onRequestPermissionsResult(request: Int, permissions: Array<out String>, granted: IntArray) {
        super.onRequestPermissionsResult(request, permissions, granted)
        if (request == GIMME_VOICE)
            if (granted.isNotEmpty() && granted[0] == PackageManager.PERMISSION_GRANTED) showVoiceGUI()
            else Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
    }

    /**
     * Kontrola povolení
     * Zdroj: https://www.youtube.com/watch?v=0bLwXw5aFOs
     */
    private fun checkPermission() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.
        checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun showVoiceGUI () {
        if (!SpeechRecognizer.isRecognitionAvailable(this))
            Toast.makeText(this, R.string.voice_err_unavailable, Toast.LENGTH_SHORT).show()
        else if (supportFragmentManager.fragments.size == 0) {
            if (!checkPermission())
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), GIMME_VOICE)
            else
                VoiceRecognitionDialog().show(supportFragmentManager, VOICE_FRAGMENT)
            getSharedPreferences(UNSHAKEN, Context.MODE_PRIVATE).edit().also {
                it.putBoolean(UNSHAKEN, false)
                it.apply()
            }
        }
    }

    /**
     * Aktivácia senzoru trasenia
     */
    override fun onResume() {
        super.onResume()
        shake.enable()
    }

    /**
     * Aktivácia senzoru trasenia
     */
    override fun onPause() {
        super.onPause()
        shake.disable()
    }
}