package com.gymtracker.android.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.gymtracker.android.MainActivity
import org.junit.Rule
import org.junit.Test

class WorkoutFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun logWorkoutThenFindItInHistory() {
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Start Workout").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start Workout").performScrollTo().performClick()
        compose.onNodeWithText("Bench Press").performScrollTo().performClick()
        compose.onNodeWithText("Weight (lb)").performScrollTo().performTextInput("185")
        compose.onNodeWithText("Reps").performTextInput("8")
        compose.onNodeWithText("Finish Workout").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Workout Saved").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("History").performClick()
        compose.onNodeWithText("View Details").performScrollTo().performClick()
        compose.onNodeWithText("Set 1: 185 lb × 8").assertIsDisplayed()
    }
}
