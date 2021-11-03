package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantExecutorRule= InstantTaskExecutorRule()

    @Before
    fun setUpDb(){
        // Create and use an in- memory database which is only used for testing
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(), RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb(){ database.close() }

    private val reminder = ReminderDTO("title", "description", "location", 10.0, 10.0)

    @Test
    fun saveReminderToDatabase()= runBlockingTest{
        // INSERT the reminder
        database.reminderDao().saveReminder(reminder)
        // GET the reminder from the database
        val loadedReminder = database.reminderDao().getReminderById(reminder.id)

        // CHECK that the loaded reminder contains the correct values
        assertThat(loadedReminder as ReminderDTO, notNullValue())
        assertThat(loadedReminder.id, `is`(reminder.id))
        assertThat(loadedReminder.title, `is`(reminder.title))
        assertThat(loadedReminder.description, `is`(reminder.description))
        assertThat(loadedReminder.location, `is`(reminder.location))
    }

    @Test
    fun deleteRemindersFromDatabase() = runBlockingTest {
        database.reminderDao().saveReminder(reminder)

        database.reminderDao().deleteAllReminders()
        assertThat(database.reminderDao().getReminders(), `is`(emptyList()))
    }

}