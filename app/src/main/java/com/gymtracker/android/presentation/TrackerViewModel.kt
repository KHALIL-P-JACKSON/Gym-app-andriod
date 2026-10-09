package com.gymtracker.android.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymtracker.android.domain.model.*
import com.gymtracker.android.domain.repository.*
import com.gymtracker.android.domain.service.WorkoutService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant

enum class Tab(val label: String, val symbol: String) {
    HOME("Home", "⌂"), WORKOUT("Workout", "＋"), PLAN("Plan", "▤"),
    HISTORY("History", "◷"), PROGRESS("Progress", "↗"), SETTINGS("Settings", "⚙")
}
data class TrackerUiState(
    val data: TrackerData = TrackerData(), val settings: Settings = Settings(),
    val session: WorkoutSession = WorkoutSession(), val tab: Tab = Tab.HOME,
    val loading: Boolean = true, val busy: Boolean = false, val error: String? = null,
    val completed: Workout? = null, val confirmFinish: Boolean = false,
    val historyEditor: Boolean = false
)
class TrackerViewModel(
    private val repository: TrackerRepository,
    private val preferences: SettingsRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(TrackerUiState())
    val state = mutableState.asStateFlow()
    private var liveSession: WorkoutSession? = null
    init {
        viewModelScope.launch {
            try {
                repository.seed()
                combine(repository.data, preferences.settings) { data, settings -> data to settings }
                    .collect { (data, settings) ->
                        mutableState.update { old ->
                            val session = old.session.copy(exercises = old.session.exercises.map { draft ->
                                draft.copy(sets = draft.sets.map { it.inUnit(settings.unit) })
                            })
                            old.copy(data = data, settings = settings, session = session, loading = false)
                        }
                    }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(loading = false, error = e.message ?: "Couldn't load your data.") } }
        }
    }
    private fun changeSession(transform: (WorkoutSession) -> WorkoutSession) {
        if (!state.value.busy) mutableState.update { it.copy(session = transform(it.session)) }
    }
    fun selectTab(tab: Tab) { mutableState.update { it.copy(tab = tab) } }
    fun name(value: String) = changeSession { it.copy(name = value) }
    fun notes(value: String) = changeSession { it.copy(notes = value) }
    fun date(value: Instant) = changeSession { it.copy(date = value) }
    fun addExercise(exercise: Exercise) = changeSession { session ->
        if (session.exercises.any { it.exercise.id == exercise.id }) session
        else session.copy(exercises = session.exercises + DraftExercise(
            exercise, listOf(DraftSet(entryUnit = state.value.settings.unit))
        ))
    }
    fun removeExercise(id: String) = changeSession { it.copy(exercises = it.exercises.filter { draft -> draft.exercise.id != id }) }
    private fun changeExercise(id: String, transform: (DraftExercise) -> DraftExercise) =
        changeSession { it.copy(exercises = it.exercises.map { draft -> if (draft.exercise.id == id) transform(draft) else draft }) }
    fun addSet(exerciseId: String) = changeExercise(exerciseId) {
        it.copy(isComplete = false, sets = it.sets + DraftSet(entryUnit = state.value.settings.unit))
    }
    fun removeSet(exerciseId: String, setId: String) = changeExercise(exerciseId) {
        it.copy(isComplete = false, sets = it.sets.filter { set -> set.id != setId })
    }
    fun editSet(exerciseId: String, setId: String, weight: String, reps: String) = changeExercise(exerciseId) {
        if (it.isComplete) it else it.copy(sets = it.sets.map { set ->
            if (set.id == setId) set.copy(weightText = weight, repsText = reps) else set
        })
    }
    fun completeExercise(id: String) = changeExercise(id) {
        it.copy(isComplete = if (it.isComplete) false else it.sets.isNotEmpty() && it.sets.all(DraftSet::isValid))
    }
    fun finish() {
        if (state.value.busy || state.value.session.validSetCount == 0) return
        if (state.value.session.invalidSetCount > 0 && state.value.settings.confirmDiscard)
            mutableState.update { it.copy(confirmFinish = true) }
        else save()
    }
    fun dismissFinish() { mutableState.update { it.copy(confirmFinish = false) } }
    fun save() {
        val session = state.value.session
        action {
            val workout = WorkoutService.build(session)
            repository.saveWorkout(workout)
            mutableState.update {
                it.copy(session = WorkoutSession(), completed = workout, confirmFinish = false)
            }
        }
    }
    fun dismissCompleted() {
        val restored = liveSession?.let { session ->
            session.copy(exercises = session.exercises.map { draft ->
                draft.copy(sets = draft.sets.map { it.inUnit(state.value.settings.unit) })
            })
        }
        mutableState.update {
            it.copy(completed = null, session = restored ?: it.session,
                tab = if (it.historyEditor) Tab.HISTORY else it.tab, historyEditor = false)
        }
        liveSession = null
    }
    fun startHistoryWorkout(workout: Workout? = null) {
        if (state.value.busy || state.value.historyEditor) return
        liveSession = state.value.session
        mutableState.update {
            it.copy(
                session = workout?.let { w -> WorkoutService.edit(w, it.data.exercises, it.settings.unit) } ?: WorkoutSession(),
                tab = Tab.WORKOUT, historyEditor = true
            )
        }
    }
    fun cancelHistoryEditor() {
        if (state.value.busy) return
        mutableState.update { it.copy(
            session = liveSession ?: WorkoutSession(), historyEditor = false, tab = Tab.HISTORY
        ) }
        liveSession = null
    }
    fun loadPlan(plan: WorkoutPlan) {
        plan.exerciseIds.mapNotNull { id -> state.value.data.exercises.find { it.id == id } }.forEach(::addExercise)
        selectTab(Tab.WORKOUT)
    }
    fun createExercise(name: String, group: MuscleGroup) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        action {
            val existing = state.value.data.exercises.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            val exercise = existing ?: Exercise(
                name = trimmed, muscleGroup = group,
                category = when (group) { MuscleGroup.BACK -> Category.PULL; MuscleGroup.LEGS -> Category.LEGS; else -> Category.PUSH },
                sortOrder = state.value.data.exercises.size
            )
            if (existing == null) repository.saveExercise(exercise)
            // This action owns busy, so mutate directly after the transaction.
            mutableState.update { old ->
                if (old.session.exercises.any { it.exercise.id == exercise.id }) old
                else old.copy(session = old.session.copy(exercises = old.session.exercises +
                    DraftExercise(exercise, listOf(DraftSet(entryUnit = old.settings.unit)))))
            }
        }
    }
    fun savePlan(plan: WorkoutPlan) = action { repository.savePlan(plan) }
    fun deletePlan(plan: WorkoutPlan) = action { repository.deletePlan(plan.id) }
    fun deleteWorkout(workout: Workout) = action { repository.deleteWorkout(workout.id) }
    fun settings(settings: Settings) = action { preferences.update(settings) }
    fun deleteAllData() = action {
        repository.deleteAllData()
        liveSession = null
        mutableState.update { it.copy(session = WorkoutSession(), historyEditor = false) }
    }
    fun dismissError() { mutableState.update { it.copy(error = null) } }
    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutableState.update { it.copy(error = e.message ?: "Couldn't save. Please try again.") } }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    companion object {
        fun factory(repository: TrackerRepository, preferences: SettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TrackerViewModel::class.java))
                return TrackerViewModel(repository, preferences) as T
            }
        }
    }
}
