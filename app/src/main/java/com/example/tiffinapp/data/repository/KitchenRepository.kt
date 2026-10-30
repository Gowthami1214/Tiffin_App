package com.example.tiffinapp.data.repository

import android.content.Context
import com.example.tiffinapp.data.model.Kitchen
import com.example.tiffinapp.data.model.KitchenState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json

/**
 * Reads and deserialises [assets/kitchens.json] into a [Flow] of [KitchenState].
 *
 * The flow is cold — a new emission sequence starts on each collection.
 * Call [getKitchens] to trigger a (re)load; pass the returned Flow into
 * the ViewModel's retry channel for refresh behaviour.
 */
class KitchenRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Returns a cold [Flow] that:
     * 1. Emits [KitchenState.Loading] immediately.
     * 2. Reads `assets/kitchens.json` on an IO dispatcher.
     * 3. Emits [KitchenState.Success] or [KitchenState.Empty] / [KitchenState.Error].
     */
    fun getKitchens(): Flow<KitchenState> = flow {
        emit(KitchenState.Loading)
        try {
            val jsonString = context.assets
                .open("kitchens.json")
                .bufferedReader()
                .use { it.readText() }

            val kitchens: List<Kitchen> = json.decodeFromString(jsonString)

            if (kitchens.isEmpty()) {
                emit(KitchenState.Empty)
            } else {
                emit(KitchenState.Success(kitchens))
            }
        } catch (e: Exception) {
            emit(KitchenState.Error(e.message ?: "Unknown error loading kitchens"))
        }
    }.flowOn(Dispatchers.IO)
}
