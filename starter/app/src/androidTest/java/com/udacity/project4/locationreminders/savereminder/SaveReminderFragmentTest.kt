package com.udacity.project4.locationreminders.savereminder

import android.app.Activity
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.util.DataBindingIdlingResource

import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.hamcrest.core.Is.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock

class SaveReminderFragmentTest : KoinTest{
    private lateinit var dataSource: ReminderDataSource
    private lateinit var viewModel: SaveReminderViewModel

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initializeRepository() {
        stopKoin()
        /**
         * Use Koin as a Service Locator
         * */
        val myModule = module {
            viewModel {
                SaveReminderViewModel(ApplicationProvider.getApplicationContext(), get() as ReminderDataSource)
            }

            // This needs to be manually casted (even though it seems useless)
            single { RemindersLocalRepository(get()) as ReminderDataSource}
            single { LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext()) }
        }

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(myModule))
        }
        dataSource = get()

        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)

        runBlocking {
            dataSource.deleteAllReminders()
        }
    }

    private fun DataBindingIdlingResource.monitorSaveReminderFragment(fragmentScenario: FragmentScenario<SaveReminderFragment>) {
        fragmentScenario.onFragment { fragment ->
            activity = fragment.requireActivity()
        }
    }

    @Test
    fun noTitle_ShowsTitleError(){
        val navController = mock(NavController::class.java)
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)

        dataBindingIdlingResource.monitorSaveReminderFragment(scenario)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))
    }

    @Test
    fun noDescription_ShowsDescriptionError() {
        val navController = mock(NavController::class.java)
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)
       dataBindingIdlingResource.monitorSaveReminderFragment(scenario)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.reminderTitle)).perform(typeText("title"))
        Espresso.closeSoftKeyboard()
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(R.id.snackbar_text)).check(matches(withText(R.string.err_enter_description)))
    }

    @Test
    fun validReminder_SavesReminder(){
        // GIVEN having launched the SaveReminderFragment
        val navController = mock(NavController::class.java)
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle.EMPTY, R.style.AppTheme)
        dataBindingIdlingResource.monitorSaveReminderFragment(scenario)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        // WHEN we have a valid reminder
        val reminder = ReminderDataItem("title", "description", "location", 10.0, 10.0)

        viewModel.validateAndSaveReminder(reminder)
        // WHEN clicking to save, it does so
        onView(withId(R.id.saveReminder)).perform(click())
        assertThat(viewModel.showToast.value, `is`("Reminder Saved !"))
    }


}