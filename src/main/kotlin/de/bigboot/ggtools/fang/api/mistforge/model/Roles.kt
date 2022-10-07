package de.bigboot.ggtools.fang.api.mistforge.model

import com.squareup.moshi.*
import kotlin.collections.HashMap

class Roles(internal val heroRoles: Map<Int, List<String>>, val roles: Set<String>): Serializable {
    operator fun get(heroId: Int) = heroRoles[heroId] ?: throw RuntimeException("Invalid heroId: $heroId")
}

class RolesAdapter: JsonAdapter<Roles>() {
    override fun fromJson(reader: JsonReader): Roles {
        val heroRoles = HashMap<Int, List<String>>()
        val roles = HashSet<String>()

        reader.beginObject()

        while (reader.peek() == JsonReader.Token.NAME) {
            val id = reader.nextName().toInt()-1
            val list = ArrayList<String>()

            reader.beginArray()

            while (reader.peek() == JsonReader.Token.STRING) {
                val role = reader.nextString()
                list.add(role)
                roles.add(role)
            }

            reader.endArray()

            heroRoles[id] = list
        }

        reader.endObject()

        return Roles(heroRoles, roles)
    }

    override fun toJson(writer: JsonWriter, value: Roles?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.beginObject()
            for ((id, roles) in value.heroRoles) {
                writer.name((id+1).toString())
                writer.beginArray()

                for (role in roles) {
                    writer.value(role)
                }

                writer.endArray()
            }
            writer.endObject()
        }
    }
}