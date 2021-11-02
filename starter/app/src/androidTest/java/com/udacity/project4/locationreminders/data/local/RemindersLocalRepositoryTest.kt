package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Error

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var repository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    private val reminder = ReminderDTO("title", "description", "location", 10.0, 10.0)

    @get:Rule
    var instantExecutorRule= InstantTaskExecutorRule()

    @Before
    fun setUpDb() {
        // Create and use an in- memory database which is only used for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RemindersDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        repository = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() { database.close()}

    @Test
    fun saveReminderRetrievesReminder_ReturnsSuccess()= runBlocking {
        repository.saveReminder(reminder)

        // WHEN retrieving the reminder
        val result = repository.getReminder(reminder.id)
        // retrieval is successful
        assertThat(result is Result.Success, `is`(true))
        result as Result.Success

        assertThat(result.data.title, `is`("title"))
        assertThat(result.data.description, `is`("description"))
        assertThat(result.data.location, `is`("location"))
    }

    @Test
    fun emptyReminders_returnsError() = runBlocking {
        repository.deleteAllReminders()

        val result = repository.getReminder(reminder.id)
        assertThat(result is Result.Error, `is`(true))

        result as Result.Error
        assertThat(result.message, `is`("Reminder not found!"))
    }
}