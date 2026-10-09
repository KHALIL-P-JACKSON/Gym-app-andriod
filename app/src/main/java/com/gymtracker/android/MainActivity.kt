package com.gymtracker.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.android.presentation.TrackerViewModel
import com.gymtracker.android.presentation.composables.GymTrackerApp
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GymTrackerApplication
        setContent {
            val model: TrackerViewModel = viewModel(factory = TrackerViewModel.factory(
                app.trackerRepository, app.settingsRepository
            ))
            GymTrackerApp(model, onExport = ::shareCsv)
        }
    }
    private fun shareCsv(csv: String) {
        val folder = File(cacheDir, "exports").apply { mkdirs() }
        val file = File(folder, "gymtracker.csv").apply { writeText(csv) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Export workouts"))
    }
}
