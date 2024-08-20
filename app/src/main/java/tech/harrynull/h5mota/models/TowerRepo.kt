package tech.harrynull.h5mota.models

object TowerRepo {
    val towers: MutableMap<String, Tower> = mutableMapOf()

    fun getTower(id: String): Tower {
        return towers[id] ?: throw IllegalArgumentException("Tower not found")
    }

    fun addTowers(towers: List<Tower>) {
        towers.forEach { tower ->
            this.towers[tower.name] = tower
        }
    }
}