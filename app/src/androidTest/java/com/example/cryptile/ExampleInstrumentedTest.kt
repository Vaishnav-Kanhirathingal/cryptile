package com.example.cryptile

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private val sleepSmall: Long = 3_000
    private val sleepLong: Long = 8_000
    private val sleepEmailVerification: Long = 30_000

    private val email = "vaishnav.kanhira@gmail.com"
    private val accountPassword = "12345678"
    private val userName = "MyUserName"

    @Test
    fun signUpAndEnter() {
        signUpWithEmail()
        Thread.sleep(sleepEmailVerification)
        loginUsingEmail()
    }

    /**
     * run while on sign in screen
     */
    private fun signUpWithEmail() {
        onView(withId(R.id.email_sign_up_button)).perform(click())
        onView(withId(R.id.user_name_edit_text)).perform(typeText(userName))
        onView(withId(R.id.user_email_edit_text)).perform(typeText(email))
        onView(withId(R.id.user_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.user_confirm_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.sign_up_button)).perform(click())
        Thread.sleep(sleepSmall)
        onView(withId(R.id.dismiss_button)).perform(click())
    }

    /**
     * run while on sign in screen
     */
    private fun loginUsingEmail() {
        onView(withId(R.id.user_email_edit_text)).perform(typeText(email))
        onView(withId(R.id.user_password_edit_text)).perform(typeText(accountPassword))
        onView(withId(R.id.login_button)).perform(click())
    }
}