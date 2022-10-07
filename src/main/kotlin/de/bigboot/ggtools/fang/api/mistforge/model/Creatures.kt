package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.*

data class Creature(val id: Int, val name: LocalizedValue, val type: CreatureType): Serializable
data class Creatures(val list: List<Creature>): Serializable {
    operator fun get(creatureId: Int) = list.getOrElse(creatureId) { throw RuntimeException("Invalid creatureId: $creatureId") }
    fun getOrNull(creatureId: Int) = list.getOrNull(creatureId)
}
enum class CreatureType {
    Bloomer,
    Cerberus,
    Cyclops,
    Drake,
    Obelisk,
}

class CreaturesAdapter(moshi: Moshi): JsonAdapter<Creatures>() {
    val delegate = moshi.adapter(LocalizedValue::class.java)

    override fun fromJson(reader: JsonReader): Creatures {
        val creatures = ArrayList<Creature>()

        reader.beginObject()

        while (reader.peek() == JsonReader.Token.NAME) {
            val id = reader.nextName().toInt()-1

            val name = delegate.fromJson(reader)

            val type = when (id) {
                0, 5 -> CreatureType.Bloomer
                1, 6 -> CreatureType.Cerberus
                2, 7 -> CreatureType.Cyclops
                3, 8 -> CreatureType.Drake
                else -> CreatureType.Obelisk
            }

            creatures.add(Creature(id, name!!, type))
        }

        reader.endObject()

        return Creatures(creatures)
    }

    override fun toJson(writer: JsonWriter, value: Creatures?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.beginObject()
            value.list.forEachIndexed { index, subvalue ->
                writer.name((index + 1).toString())

                delegate.toJson(writer, subvalue.name)
            }
            writer.endObject()
        }
    }

    companion object {
        val Factory = Factory { type, _, moshi ->
            if (Types.getRawType(type) == Creatures::class.java) CreaturesAdapter(moshi)
            else null
        }
    }
}