package com.gymtracker.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymtracker.android.domain.model.DistanceUnit
import com.gymtracker.android.domain.repository.RunRepository
import com.gymtracker.android.tracking.RunController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RunViewModel(private val controller: RunController, private val repository: RunRepository) : ViewModel() {
    val state = controller.state
    val runs = repository.completed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun start(unit: DistanceUnit, goal: Double?, audio: Boolean) = controller.requestStart(unit, goal, audio)
    fun end() = controller.end()
    fun dismissError() = controller.dismissError()
    fun dismissSaved() = controller.dismissSaved()
    fun delete(id: String) { viewModelScope.launch {
        try { repository.delete(id) } catch (e: Exception) { controller.report("Couldn't delete run: ${e.message}") }
    } }
    companion object {
        fun factory(controller: RunController, repository: RunRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(RunViewModel::class.java))
                return RunViewModel(controller, repository) as T
            }
        }
    }
}
