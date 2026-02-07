package com.example.mycloset

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.example.mycloset.data.model.Item
import com.example.mycloset.ui.MainActivity
import com.example.mycloset.ui.item.ItemListFragment
import com.example.mycloset.util.Injection
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemFuncUITest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    private lateinit var fakeRepo : FakeItemRepository

    @Before
    fun setup(){
        fakeRepo = FakeItemRepository()
        fakeRepo.itemsToReturn = listOf(Item(name = "Test Item", type = "Test type",
            color = "Test", season = "Test"))
        Injection.setRepository(fakeRepo)

    }

    @After
    fun repoReset(){
        Injection.reset()
    }

    @Test
    fun testItemRendersInViewHolder(){
        val scenario = launchFragmentInContainer<ItemListFragment>(
            themeResId = R.style.Theme_MyCloset
        )
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onFragment { fragment ->
            fragment.viewModel.loadItems("test_user")
        }
        onView(withText("Test Item")).check(matches(isDisplayed()))
        onView(withText("Test type • Test • Test")).check(matches(isDisplayed()))
    }
     @Test
     fun testDisplayEmptyList(){
         val fakeRepo = FakeItemRepository()
         fakeRepo.itemsToReturn = emptyList()

         val scenario = launchFragmentInContainer<ItemListFragment>(
             themeResId = R.style.Theme_MyCloset
         )
         scenario.moveToState(Lifecycle.State.RESUMED)
         Thread.sleep(1000)
         onView(withId(R.id.tvEmpty))
             .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
     }

}