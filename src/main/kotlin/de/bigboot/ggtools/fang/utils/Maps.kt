package de.bigboot.ggtools.fang.utils

data class Map(val id: String, val name: String, val prototype: Boolean = false)

object Maps {
    val LV_CANYON = Map("lv_canyon",        "[Clash] Ghost Reef")
    val LV_MISTFORGE = Map("lv_mistforge",     "[Clash] Sanctum Falls")
    val LV_VALLEY = Map("lv_valley",        "[Clash] Sirens Strand")
    val LV_WIZARDWOODS = Map("lv_wizardwoods",   "[Clash] Ember Grove", false)
    val LV_SKYCITYV2 = Map("lv_skycityv2",     "[Clash] Sky City V2", false)
    val LV_MODCITY = Map("lv_modcity",       "[Clash] Sky Tuga", false)

    val RS_CANYON2 = Map("rs_canyon2",        "[Rush] Ghost Reef", false)
    val RS_MISTFORGE2 = Map("rs_mistforge2",     "[Rush] Sanctum Falls", false)
    val RS_VALLEY3 = Map("rs_valley3",        "[Rush] Sirens Strand", false)
    val RS_WIZARDWOODS2 = Map("rs_wizardwoods2",   "[Rush] Ember Grove", false)
    val RS_SKYCITY1 = Map("rs_skycity1",       "[Rush] Sky City", false)
    val RS_MODCITY1 = Map("rs_modcity1",       "[Rush] Mod City", false)

    val ALL = listOf(
        LV_CANYON,
        LV_MISTFORGE,
        LV_VALLEY,
        LV_WIZARDWOODS,
        LV_SKYCITYV2,
        LV_MODCITY,
        RS_CANYON2,
        RS_MISTFORGE2,
        RS_VALLEY3,
        RS_WIZARDWOODS2,
        RS_SKYCITY1,
        RS_MODCITY1,
    )
    val FINISHED = ALL.filter { !it.prototype }

    fun fromId(id: String) = ALL.find { it.id == id }
}
