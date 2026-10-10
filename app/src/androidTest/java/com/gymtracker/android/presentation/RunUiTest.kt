package com.gymtracker.android.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.presentation.composables.TrackerScaffold
import com.gymtracker.android.presentation.composables.previews.PreviewData
import com.gymtracker.android.presentation.composables.screens.*
import com.gymtracker.android.presentation.composables.theme.GymTrackerTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** UI fixtures avoid recording a run or editing the owner's real workout database. */
class RunUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allTabsAndEndRunAreVisibleAndNavigationDoesNotEndRun() {
        var selected by mutableStateOf(Tab.RUN)
        var ended = false
        val run = RunRecord(startedAt = 1000, clockStartMillis = 0, bootCount = 0, elapsedMillis = 60000)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GymTrackerTheme {
                    TrackerScaffold(PreviewData.state.copy(tab = selected), { selected = it }) {
                        if (selected == Tab.RUN) RunScreen(RunUiState(run = run, tracking = true, loading = false),
                            Settings(), {}, { _, _, _ -> }, { ended = true })
                        else Text("Other page", Modifier.fillMaxSize())
                    }
                }
            }
        }
        Tab.entries.forEach { tab -> compose.onNode(hasText(tab.label) and hasClickAction()).assertIsDisplayed() }
        compose.onNodeWithText("End Run").assertIsDisplayed()
        compose.onNode(hasText("Home") and hasClickAction()).performClick()
        assertFalse(ended)
        compose.onNode(hasText("Run") and hasClickAction()).performClick()
        compose.onNodeWithText("End Run").performClick()
        assertTrue(ended)
    }

    @Test fun strengthProgressIsLinkedFromHome() {
        var progress = false
        compose.setContent {
            GymTrackerTheme { HomeScreen(PreviewData.data, WeightUnit.LB, onProgress = { progress = true }, onStart = {}) }
        }
        compose.onNodeWithText("View Progress").performScrollTo().performClick()
        assertTrue(progress)
    }

    @Test fun historyFiltersRunsAndShowsRouteDetails() {
        val run = RunRecord(startedAt = 1000, clockStartMillis = 0, bootCount = 0,
            elapsedMillis = 600000, endedAt = 601000, distanceMeters = 1000.0, unit = DistanceUnit.KM)
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GymTrackerTheme { HistoryScreen(PreviewData.data, WeightUnit.LB, false, {}, {}, {}, runs = listOf(run)) }
            }
        }
        compose.onNodeWithText("Runs").performClick()
        compose.onNodeWithText("1.00 km • 10:00").assertIsDisplayed()
        compose.onNodeWithText("View Run").performClick()
        compose.onNodeWithText("Run Details").assertIsDisplayed()
        compose.onNodeWithText("Distance: 1.00 km").assertIsDisplayed()
        compose.onNodeWithText("Close").performClick()
    }
}
