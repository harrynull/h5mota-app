package tech.harrynull.h5mota.models

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.towerDataStore: DataStore<Preferences> by preferencesDataStore(name = "tower_data")

class TowerRepo(private val ctx: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun loadTower(id: String): Tower {
        val json = ctx.towerDataStore.data.map { pref -> pref[stringPreferencesKey("tower-$id")] }
        return this.json.decodeFromString(Tower.serializer(), json.first()!!)
    }

    suspend fun persistTowers(towers: List<Tower>) {
        towers.forEach { tower ->
            ctx.towerDataStore.edit { pref ->
                pref[stringPreferencesKey("tower-${tower.name}")] =
                    json.encodeToString(Tower.serializer(), tower)
            }
        }
    }

    suspend fun loadTowerDetails(id: String): TowerDetails {
        val json =
            ctx.towerDataStore.data.map { pref -> pref[stringPreferencesKey("tower-details-$id")] }
        return this.json.decodeFromString(TowerDetails.serializer(), json.first()!!)
    }

    suspend fun persistTowerDetails(id: String, details: TowerDetails) {
        ctx.towerDataStore.edit { pref ->
            pref[stringPreferencesKey("tower-$id-details")] =
                json.encodeToString(TowerDetails.serializer(), details)
        }
    }

    private val StarredKey = stringSetPreferencesKey("towers-starred")
    private val RecentKey = stringSetPreferencesKey("towers-recent")

    suspend fun setStarred(tower: Tower, starred: Boolean) {
        val id = tower.name
        ctx.towerDataStore.edit { pref ->
            pref[StarredKey] =
                pref[StarredKey]?.toMutableSet()?.apply {
                    if (starred) add(id) else remove(id)
                } ?: setOf(id)
        }
    }

    suspend fun getStarred(): Set<String> {
        return ctx.towerDataStore.data.map { pref -> pref[RecentKey] ?: setOf() }.first()
    }

    suspend fun isStarred(tower: Tower): Boolean {
        val id = tower.name
        return ctx.towerDataStore.data.map { pref -> id in (pref[StarredKey] ?: setOf()) }.first()
    }

    suspend fun addRecent(tower: Tower) {
        val id = tower.name
        ctx.towerDataStore.edit { pref ->
            val recent = pref[RecentKey]?.toMutableSet() ?: mutableSetOf()
            recent.remove(id)
            recent.add(id)
            if (recent.size > 50) {
                recent.remove(recent.first())
            }
            pref[RecentKey] = recent
        }
    }
}