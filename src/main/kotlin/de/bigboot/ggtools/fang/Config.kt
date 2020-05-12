package de.bigboot.ggtools.fang

import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable
import java.nio.file.Paths
import kotlin.collections.ArrayList
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

sealed class ConfigException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ConfigMissingException(key: String)
        : ConfigException("Missing config value: $key")

    class ConfigValueException(key: String, expected: Class<*>, actual: Class<*>)
        : ConfigException("Invalid config value: $key (expected ${expected.simpleName}, got ${actual.simpleName})")
}

class ExceptionProperty<T>(private val ex: ConfigException) : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        throw ex
    }
}

class ValuePropery<T>(private val value: T) : ReadOnlyProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
}

private open class PropertyFactory<T>(private val config: TomlTable, private val returnType: Class<T>) {
    protected fun getValue(key: String): T? {
        val any = config.get(key)

        if (any != null && !returnType.isAssignableFrom(any.javaClass)) {
            throw ConfigException.ConfigValueException(
                key,
                returnType,
                any.javaClass
            )
        }

        @Suppress("UNCHECKED_CAST")
        return any as T?
    }
}

private class RequiredDelegateProvider<T>(config: TomlTable, returnType: Class<T>, private val  handler: (ConfigException) -> Unit) : PropertyFactory<T>(config, returnType) {
    operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): ReadOnlyProperty<Any, T> {
        val key = prop.name.toLowerCase()
        return try {
            ValuePropery(
                getValue(key)
                    ?: throw ConfigException.ConfigMissingException(key)
            )
        } catch (ex: ConfigException) {
            handler(ex)
            ExceptionProperty(ex)
        }
    }
}

private class OptionalDelegateProvider<T>(config: TomlTable, returnType: Class<T>, private val default: T, private val handler: (ConfigException) -> Unit) : PropertyFactory<T>(config, returnType) {
    operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): ReadOnlyProperty<Any, T> {
        val key = prop.name.toLowerCase()
        return try {
            ValuePropery(getValue(key) ?: default)
        } catch (ex: ConfigException) {
            handler(ex)
            ExceptionProperty(ex)
        }
    }
}

object Config {
    private val config: TomlParseResult = Toml.parse(Paths.get("config.toml"))
    private val exceptions: ArrayList<ConfigException> = ArrayList()

    private inline fun <reified T> required() = RequiredDelegateProvider(
        config,
        T::class.java
    ) {
        exceptions.add(it)
    }

    private inline fun <reified T> optional(default: T) =
        OptionalDelegateProvider(
            config,
            T::class.java,
            default
        ) {
            exceptions.add(it)
        }

    fun exceptions() = exceptions

    val BOT_TOKEN: String by required()
    val PREFIX: String by optional("!")
    val DB_DRIVER: String by optional("org.h2.Driver")
    val DB_URL: String by optional("jdbc:h2:./fang")
    val DB_USER: String by optional("")
    val DB_PASS: String by optional("")
    val DEFAULT_GROUP_NAME: String by optional("default")
    val DEFAULT_GROUP_PERMISSIONS: List<String> by optional(listOf())
    val ADMIN_GROUP_NAME: String by optional("admin")
    val ADMIN_GROUP_PERMISSIONS: List<String> by optional(listOf("*"))
    val ACCEPT_TIMEOUT: Int by optional(120)
}

