package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Skills (
        @Json(name="Skill1") val lmb: Skill,
        @Json(name="Skill2") val rmb: Skill,
        @Json(name="Skill3") val q: Skill,
        @Json(name="Skill4") val e: Skill,
        @Json(name="Focus") val focus: Skill
): Serializable {
    operator fun get(id: SkillId) = when (id) {
        SkillId.LMB -> lmb
        SkillId.RMB -> rmb
        SkillId.Q -> q
        SkillId.E -> e
        SkillId.Focus -> focus
    }

    fun getSkills() = SkillId.values().map { Pair(it, get(it)) }
}

enum class SkillId(val imageId: Int, val slotId: Int): Serializable {
    LMB(1, 0),
    RMB(2, 1),
    Q(3, 3),
    E(4, 4),
    Focus(5, 2)
}




