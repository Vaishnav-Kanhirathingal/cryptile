package com.example.cryptile

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)


    private val sleepSmall: Long = 3_000
    private val sleepMid: Long = 8_000
    private val sleepLong: Long = 20_000
    private val sleepEmailVerification: Long = 30_000

    private val email = "vaishnav.kanhira@gmail.com"
    private val accountPassword = "12345678"
    private val userName = "MyUserName"

    private val safeName = "SafeName123"
    private val safePasswordOne = "passwordOne"
    private val safePasswordTwo = "passwordTwo"

    @Test
    fun mainTask() {
//        signUpWithEmail()
//        Thread.sleep(sleepEmailVerification)
        loginUsingEmail()
        createSafe()
    }

    /** run while on sign in screen */
    private fun signUpWithEmail() {
        onView(withId(R.id.email_sign_up_button)).perform(click())
        onView(withId(R.id.user_name_edit_text)).perform(typeText(userName))
        onView(withId(R.id.user_email_edit_text)).perform(typeText(email))
        onView(withId(R.id.user_set_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.user_confirm_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.sign_up_button)).perform(click())
        Thread.sleep(sleepMid)
        onView(withId(R.id.dismiss_button)).perform(click())
    }

    /** run while on sign in screen */
    private fun loginUsingEmail() {
        onView(withId(R.id.user_email_edit_text)).perform(typeText(email))
        onView(withId(R.id.user_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.login_button)).perform(click())
    }

    /** call from main fragment */
    private fun createSafe() {
        onView(withId(R.id.add_safe_fab)).perform(click())
        onView(withParent(withId(R.id.safe_name_input_layout)))
            .perform(typeText(safeName))
        onView(withParent(withId(R.id.safe_password_one_input_layout)))
            .perform(typeText(safePasswordOne))
        onView(withId(R.id.use_multiple_passwords_switch))
            .perform(click())
        onView(withParent(withId(R.id.safe_password_two_input_layout)))
            .perform(typeText(safePasswordTwo))
        onView(withId(R.id.confirm_button)).perform(scrollTo(), click())
        Thread.sleep(sleepMid)
        // TODO: check if it matches
    }

    /** call from main fragment */
    private fun openSafe() {
        // TODO: open recycler with text from safe name
    }
}