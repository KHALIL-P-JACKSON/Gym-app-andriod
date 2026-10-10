package com.gymtracker.android.domain.repository

import com.gymtracker.android.domain.model.RunRecord
import kotlinx.coroutines.flow.Flow

interface RunRepository {
    val completed: Flow<List<RunRecord>>
    suspend fun unfinished(): RunRecord?
    suspend fun save(run: RunRecord)
    suspend fun delete(id: String)
}
