package de.bigboot.ggtools.fang.utils

data class Map(val id: String, val name: String, val prototype: Boolean = false)

object Maps {
    val CANYON = Map("canyon", "Ghost Reef")
    val MISTFORGE = Map("mistforge", "Sanctum Falls")
    val VALLEY = Map("valley", "Sirens Strand")
    val WIZARDWOODS = Map("wizardwoods", "Ember Grove", true)
    val CANYONNIGHT = Map("canyonnight", "Ghost Reef Night", true)
    val SKYCITYV2 = Map("skycityv2", "Sky City V2", true)
    val SKYTUGA = Map("skytuga", "Sky Tuga", true)

    val ALL = listOf(CANYON, MISTFORGE, VALLEY, WIZARDWOODS, CANYONNIGHT, SKYCITYV2, SKYTUGA)
    val FINISHED = ALL.filter { !it.prototype }

    fun fromId(id: String) = ALL.find { it.id == id }
}