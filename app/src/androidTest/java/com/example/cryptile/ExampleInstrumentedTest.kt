package com.example.cryptile

import android.os.Environment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.runner.screenshot.Screenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)


    private val sleepLow: Long = 3_000
    private val sleepMid: Long = 8_000
    private val sleepHigh: Long = 20_000
    private val sleepExtra: Long = 30_000

    private val email = "vaishnav.kanhira@gmail.com"
    private val accountPassword = "12345678"
    private val newAccountPassword = "1234567890"

    private val userName = "MyUserName"
    private val newUserName = "someNewUserName"

    private val safeName = "SafeName123"
    private val safePasswordOne = "passwordOne"
    private val safePasswordTwo = "passwordTwo"

    @Test
    fun mainTask() {
//        signUpWithEmail()
        loginUsingEmail(accountPassword)
        testSettings()
    }

    /** run while on sign in screen */
    private fun signUpWithEmail() {
        onView(withId(R.id.email_sign_up_button)).perform(click())
        onView(withId(R.id.user_name_edit_text)).perform(typeText(userName))
        onView(withId(R.id.user_email_edit_text)).perform(typeText(email))
        onView(withId(R.id.user_set_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.user_confirm_password_edit_text)).perform(typeText(accountPassword))
        saveScreenshot()
        onView(withId(R.id.sign_up_button)).perform(click())
        Thread.sleep(sleepMid)
        saveScreenshot()
        onView(withId(R.id.dismiss_button)).perform(click())
        Thread.sleep(sleepExtra)
    }

    /** run while on sign in screen */
    private fun loginUsingEmail(password: String) {
        onView(withId(R.id.user_email_edit_text)).perform(typeText(email))
        onView(withId(R.id.user_password_edit_text)).perform(typeText(password))
        saveScreenshot()
        onView(withId(R.id.login_button)).perform(click())
        Thread.sleep(sleepLow)
    }

    /** call from main fragment */
    private fun createSafe() {
        onView(withId(R.id.add_safe_fab)).perform(click())
        onView(withParent(withId(R.id.safe_name_input_layout))).perform(typeText(safeName))
        onView(withParent(withId(R.id.safe_password_one_input_layout)))
            .perform(typeText(safePasswordOne))
        onView(withId(R.id.use_multiple_passwords_switch)).perform(click())
        onView(withParent(withId(R.id.safe_password_two_input_layout)))
            .perform(typeText(safePasswordTwo))
        onView(withId(R.id.confirm_button)).perform(scrollTo(), click())
        saveScreenshot()
        Thread.sleep(sleepMid)
        saveScreenshot()
        // TODO: check if it matches
    }

    /** call from main fragment */
    private fun openSafe() {
        // TODO: open recycler with text from safe name
        saveScreenshot()
        onView(withText(safeName)).perform(click())
        onView(withParent(withId(R.id.password_one_text_layout))).perform(typeText(safePasswordOne))
        onView(withParent(withId(R.id.password_two_text_layout))).perform(typeText(safePasswordTwo))
        saveScreenshot()
        onView(withId(R.id.open_button)).perform(click())
        Thread.sleep(sleepMid)
        saveScreenshot()
    }

    private fun openSettings() {
        // TODO: open side menu
        onView(withContentDescription("side menu")).perform(click())
        onView(withId(R.id.settings)).perform(click())// TODO: fix scroll issue
        saveScreenshot()
    }

    private fun testSettings() {
        openSettings()
        onView(withParent(withId(R.id.new_user_name_text_layout))).perform(typeText(newUserName))
        onView(withContentDescription("New User Name")).perform(click())
        saveScreenshot()
        enterUserPassword()
        Thread.sleep(sleepMid)
        onView(withParent(withId(R.id.new_password_text_layout)))
            .perform(typeText(newAccountPassword))
        onView(withParent(withId(R.id.new_password_repeat_text_layout)))
            .perform(typeText(newAccountPassword))
        saveScreenshot()
        onView(withId(R.id.confirm_password_image_button)).perform(scrollTo(), click())
        enterUserPassword()
        loginUsingEmail(newAccountPassword)
        openSettings()
        onView(withId(R.id.condition_check_box)).perform(click())
        saveScreenshot()
        onView(withId(R.id.delete_account_button)).perform(click())
        enterUserPassword()
    }

    private fun enterUserPassword() {
        onView(withParent(withId(R.id.user_email_text_layout))).perform(typeText(newUserName))
        onView(withParent(withId(R.id.user_password_text_layout))).perform(typeText(newUserName))
        saveScreenshot()
        onView(withId(R.id.confirm_button)).perform(click())
    }

    private fun saveScreenshot() {
        // TODO: no action
        val timeString = SimpleDateFormat("dd/MM - HH:mm:ss.SSS").format(System.currentTimeMillis())
        val directory = File(Environment.getExternalStorageDirectory(), "TestScreenShots")
        if (!directory.exists()) directory.mkdirs()
        val captureFile = File(directory, "$timeString.png")

        val capture = Screenshot.capture()
        var out: BufferedOutputStream? = null
        try {
            out = BufferedOutputStream(FileOutputStream(captureFile))
            capture.bitmap.compress(capture.format, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                out?.close()
            } catch (ignored: IOException) {
                ignored.printStackTrace()
            }
        }
        Thread.sleep(sleepLow)
    }
}