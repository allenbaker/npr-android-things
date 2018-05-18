package org.npr.npr_android_things

import android.app.Activity
import android.os.Bundle
import android.media.MediaPlayer
import android.util.Log
import android.view.KeyEvent
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.button.ButtonInputDriver
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import java.io.IOException


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class HomeActivity : Activity() {

    private val TAG = HomeActivity::class.java!!.simpleName
    private val SEEK_AMOUNT = 5000;
    private var mLedGpio: Gpio? = null
    private var buttonA: ButtonInputDriver? = null
    private var buttonB: ButtonInputDriver? = null
    private var buttonC: ButtonInputDriver? = null
    private val mediaPlayer = MediaPlayer()
    private var lastPosition : Int = 0
    private var display: AlphanumericDisplay? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val pioService = PeripheralManager.getInstance()
        try {
            display = AlphanumericDisplay(BoardDefaults.getI2cBus())
            display!!.setEnabled(true)
            display!!.clear()
//            display!!.display("NPR")
            display!!.writeColumn(0, 55)

            display!!.writeColumn(1, 243)
            display!!.writeColumn(2, 49)


//            display!!.writeColumn(0, 1)

            Log.i(TAG, "Configuring GPIO pins")
            mLedGpio = pioService.openGpio(BoardDefaults.getGPIOForLED())
            mLedGpio!!.value = true
            //mLedGpio!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)

        } catch (e: IOException) {
            Log.e(TAG, "Error configuring GPIO pins", e)
        }

        try {
            Log.i(TAG, "Registering button driver " + BoardDefaults.getGPIOForButton())

            buttonA = ButtonInputDriver(
                    "GPIO6_IO14",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            buttonA!!.register()

            buttonB = ButtonInputDriver(
                    "GPIO6_IO15",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_MEDIA_REWIND)
            buttonB!!.register()

            buttonC = ButtonInputDriver(
                    "GPIO2_IO07",
                    Button.LogicState.PRESSED_WHEN_LOW,
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
            buttonC!!.register()

        } catch (e: IOException) {
            Log.e(TAG, "Error configuring GPIO pins", e)
        }
    }

    override fun onResume() {
        super.onResume()

        val mp3File = "raw/baby_talk.mp3"
        val assetMan = assets
        val mp3Stream = assetMan.openFd(mp3File).createInputStream()
        mediaPlayer.setDataSource(mp3Stream.fd)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
                lastPosition = mediaPlayer.currentPosition
            }
            else {
                mediaPlayer.prepare()
                if(lastPosition != 0){
                    mediaPlayer.seekTo(lastPosition)
                }
                mediaPlayer.start()
            }
            return true
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if(!mediaPlayer.isPlaying) {
                mediaPlayer.prepare()
            }
            mediaPlayer.seekTo(mediaPlayer.currentPosition + SEEK_AMOUNT)
            mediaPlayer.start()
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            lastPosition = mediaPlayer.currentPosition
            val unverifiedPosition = lastPosition - SEEK_AMOUNT

            if(unverifiedPosition < 0) {
                lastPosition = 0
            }
            else {
                lastPosition = unverifiedPosition
            }
            if(!mediaPlayer.isPlaying) {
                mediaPlayer.prepare()
            }
            mediaPlayer.seekTo(lastPosition)
            mediaPlayer.start()
        }
        return super.onKeyUp(keyCode, event)
    }
}
