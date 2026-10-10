package com.gymtracker.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.gymtracker.android.presentation.TrackerViewModel
import com.gymtracker.android.presentation.RunViewModel
import com.gymtracker.android.presentation.Tab
import com.gymtracker.android.presentation.composables.GymTrackerApp
import java.io.File

class MainActivity : ComponentActivity() {
    private var runOpenRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra("openRun", false) == true) runOpenRequest++
        val app = application as GymTrackerApplication
        setContent {
            val model: TrackerViewModel = viewModel(factory = TrackerViewModel.factory(
                app.trackerRepository, app.settingsRepository, app.runController
            ))
            val runModel: RunViewModel = viewModel(factory = RunViewModel.factory(app.runController, app.runRepository))
            LaunchedEffect(runOpenRequest) { if (runOpenRequest > 0) model.selectTab(Tab.RUN) }
            GymTrackerApp(model, runModel, onExport = ::shareCsv)
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("openRun", false)) runOpenRequest++
    }
    private fun shareCsv(csv: String) {
        val folder = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(folder, "gymtracker.csv").apply { writeText(csv) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Export CSV"))
    }
}
