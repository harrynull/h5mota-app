package tech.harrynull.h5mota.models

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

val Context.towerDataStore: DataStore<Preferences> by preferencesDataStore(name = "tower_data")

class TowerRepo(private val ctx: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val StarredKey = stringSetPreferencesKey("towers-starred")
    private val RecentKey = stringPreferencesKey("towers-recent")
    private fun towerKey(name: String) = stringPreferencesKey("tower-$name")
    private fun towerDetailsKey(name: String) = stringPreferencesKey("tower-details-$name")

    private fun String.parseToTower() = json.decodeFromString(Tower.serializer(), this)
    private fun String.parseToTowerDetails() =
        json.decodeFromString(TowerDetails.serializer(), this)

    private fun Tower.encodeToJson() = json.encodeToString(Tower.serializer(), this)
    private fun TowerDetails.encodeToJson() = json.encodeToString(TowerDetails.serializer(), this)

    suspend fun loadTower(id: String): Tower {
        return ctx.towerDataStore.data.map { pref ->
            pref[stringPreferencesKey("tower-$id")]!!.parseToTower()
        }.first()
    }

    suspend fun persistTowers(towers: List<Tower>) {
        towers.forEach { tower ->
            ctx.towerDataStore.edit { pref ->
                pref[towerKey(tower.name)] = tower.encodeToJson()
            }
        }
    }

    suspend fun loadTowerDetails(id: String): TowerDetails? {
        return ctx.towerDataStore.data.map { pref ->
            pref[towerDetailsKey(id)]?.parseToTowerDetails()
        }.firstOrNull()
    }

    suspend fun persistTowerDetails(id: String, details: TowerDetails) {
        ctx.towerDataStore.edit { pref ->
            pref[towerDetailsKey(id)] = details.encodeToJson()
        }
    }

    suspend fun setStarred(tower: Tower, starred: Boolean) {
        val id = tower.name
        ctx.towerDataStore.edit { pref ->
            pref[StarredKey] =
                pref[StarredKey]?.toMutableSet()?.apply {
                    if (starred) add(id) else remove(id)
                } ?: setOf(id)
        }
    }

    suspend fun getStarred(): List<Tower> {
        return ctx.towerDataStore.data.map { pref ->
            (pref[StarredKey] ?: setOf()).map { pref[towerKey(it)]!!.parseToTower() }
        }.first()
    }

    suspend fun isStarred(tower: Tower): Boolean {
        val id = tower.name
        return ctx.towerDataStore.data.map { pref -> id in (pref[StarredKey] ?: setOf()) }.first()
    }

    suspend fun addRecent(tower: Tower) {
        val id = tower.name
        ctx.towerDataStore.edit { pref ->
            val recent = pref[RecentKey]?.split("|")?.toMutableList() ?: mutableListOf()
            recent.remove(id)
            recent.add(id)
            if (recent.size > 50) {
                recent.remove(recent.first())
            }
            pref[RecentKey] = recent.joinToString("|")
        }
    }

    suspend fun getRecent(): List<Tower> {
        return ctx.towerDataStore.data.map { pref ->
            (pref[RecentKey]?.split("|")?.toList() ?: emptyList())
                .map { pref[towerKey(it)]!!.parseToTower() }
        }.first()
    }
}