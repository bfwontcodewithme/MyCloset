package com.example.mycloset

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mycloset.ui.MainActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before

@RunWith(AndroidJUnit4::class)
class SIdeBarMenuNavigationTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun confirmedLogout() {
        // Force Logout so that 'group_logged_out' is always visible
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        activityRule.scenario.onActivity { activity ->
            // This forces the menu to update immediately for the test
            (activity as MainActivity).updateDrawerMenuVisibility()
        }
    }
    @After
    fun cleanupLoggedUser(){
        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        activityRule.scenario.onActivity { activity ->
            // This forces the menu to update immediately for the test
            (activity as MainActivity).updateDrawerMenuVisibility()
        }
    }
    @Test
    fun testRegisterNav(){
        testNavigationFlow(R.id.nav_register, R.id.tvRegisterTitle)
    }
    @Test
    fun testLoginNav(){
        testNavigationFlow(R.id.nav_login, R.id.btnLogin)
    }
    @Test
    fun testProfileNav(){
        testNavigationFlow(R.id.nav_profile, R.id.tvProfileTitle)
    }
    @Test
    fun testSettingNav(){
        testNavigationFlow(R.id.nav_settings, R.id.tvSettingsTitle)
    }

    private fun testNavigationFlow(menuFragmentId: Int, uniqueViewId: Int){


        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(menuFragmentId))

        onView(withId(uniqueViewId)).check(matches(isDisplayed()))

        //activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        //pressBack()
        //onView(withId(R.id.tvHomeTitle)).check(matches(isDisplayed()))
    }
}