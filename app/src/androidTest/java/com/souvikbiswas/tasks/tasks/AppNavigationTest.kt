/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.souvikbiswas.tasks.tasks

import android.app.Activity
import android.view.Gravity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.contrib.NavigationViewActions.navigateTo
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.souvikbiswas.tasks.R
import com.souvikbiswas.tasks.ServiceLocator
import com.souvikbiswas.tasks.data.Task
import com.souvikbiswas.tasks.data.source.TasksRepository
import com.souvikbiswas.tasks.util.DataBindingIdlingResource
import com.souvikbiswas.tasks.util.EspressoIdlingResource
import com.souvikbiswas.tasks.util.monitorActivity
import com.souvikbiswas.tasks.util.saveTaskBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the [DrawerLayout] layout component in [TasksActivity] which manages
 * navigation within the app.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest {

    private lateinit var tasksRepository: TasksRepository

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        tasksRepository = ServiceLocator.provideTasksRepository(getApplicationContext())
    }

    @After
    fun reset() {
        ServiceLocator.resetRepository()
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun drawerNavigationFromTasksToStatistics() {
        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.
            .perform(open()) // Open Drawer

        // Start statistics screen.
        onView(withId(R.id.nav_view))
            .perform(navigateTo(R.id.statisticsFragment))

        // Check that statistics screen was opened.
        onView(withId(R.id.statistics)).check(matches(isDisplayed()))

        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.
            .perform(open()) // Open Drawer

        // Start tasks screen.
        onView(withId(R.id.nav_view))
            .perform(navigateTo(R.id.tasksFragment))

        // Check that tasks screen was opened.
        onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun tasksScreen_clickOnAndroidHomeIcon_OpensNavigation() {
        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check that left drawer is closed at startup
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.

        // Open Drawer
        onView(
            withContentDescription(
                activityScenario
                    .getToolbarNavigationContentDescription()
            )
        ).perform(click())

        // Check if drawer is open
        onView(withId(R.id.drawer_layout))
            .check(matches(isOpen(Gravity.START))) // Left drawer is open open.
    }

    @Test
    fun statsScreen_clickOnAndroidHomeIcon_OpensNavigation() {
        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // When the user navigates to the stats screen
        activityScenario.onActivity {
            it.findNavController(R.id.nav_host_fragment).navigate(R.id.statisticsFragment)
        }

        // Then check that left drawer is closed at startup
        onView(withId(R.id.drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.

        // When the drawer is opened
        onView(
            withContentDescription(
                activityScenario
                    .getToolbarNavigationContentDescription()
            )
        ).perform(click())

        // Then check that the drawer is open
        onView(withId(R.id.drawer_layout))
            .check(matches(isOpen(Gravity.START))) // Left drawer is open open.
    }

    @Test
    fun taskDetailScreen_doubleUIBackButton() {
        val task = Task("UI <- button", "Description")
        tasksRepository.saveTaskBlocking(task)

        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the task on the list
        onView(withText("UI <- button")).perform(click())
        // Click on the edit task button
        onView(withId(R.id.fab_edit_task)).perform(click())

        // Confirm that if we click "<-" once, we end up back at the task details page
        onView(
          withContentDescription(
            activityScenario
              .getToolbarNavigationContentDescription()
          )
        ).perform(click())
        onView(withId(R.id.task_detail_title)).check(matches(isDisplayed()))

        // Confirm that if we click "<-" a second time, we end up back at the home screen
        onView(
          withContentDescription(
            activityScenario
              .getToolbarNavigationContentDescription()
          )
        ).perform(click())
        onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()))
    }

    @Test
    fun taskDetailScreen_doubleBackButton() {
        val task = Task("Back button", "Description")
        tasksRepository.saveTaskBlocking(task)

        // start up Tasks screen
        val activityScenario = ActivityScenario.launch(TasksActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the task on the list
        onView(withText("Back button")).perform(click())
        // Click on the edit task button
        onView(withId(R.id.fab_edit_task)).perform(click())

        // Confirm that if we click back once, we end up back at the task details page
        pressBack()
        onView(withId(R.id.task_detail_title)).check(matches(isDisplayed()))

        // Confirm that if we click back a second time, we end up back at the home screen
        pressBack()
        onView(withId(R.id.tasksContainer)).check(matches(isDisplayed()))
    }

    private fun <T : Activity> ActivityScenario<T>.getToolbarNavigationContentDescription()
        : String {
        var description = ""
        onActivity {
            description =
                it.findViewById<Toolbar>(R.id.toolbar).navigationContentDescription as String
        }
        return description
    }
}
