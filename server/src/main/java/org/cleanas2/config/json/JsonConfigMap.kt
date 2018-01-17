package org.cleanas2.config.json

import org.apache.commons.lang3.StringUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import java.util.LinkedHashMap

/**
 * Wraps a Map instance with a "parent" kind of name, plus utility methods to simplify and reduce
 * the code needed when reading from a json config file.
 *
 * @author Andrew Backer awbacker@gmail.com / andrew.backer@powere2e.com
 */
class JsonConfigMap(private val parent: String, private val map: Map<*, *>) {

    @Throws(Exception::class)
    private fun requireAny(name: String) {
        if (name.contains(".")) {
            val parts = StringUtils.split(name, ".", 2) // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0]) // validate the first section
            getSection(parts[0]).requireAny(parts[1]) // validate the rest, which may result in > 1 further validations
        }
        if (!has(name))
            throw Exception(String.format("Required element \"%s\" missing", formatName(name)))
    }

    @Throws(Exception::class)
    fun requireList(name: String) {
        if (name.contains(".")) {
            val parts = StringUtils.split(name, ".", 2) // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0])
            getSection(parts[0]).requireSection(parts[1])
        } else {
            if (!has(name))
                throw Exception(String.format("Required List \"%s\" not found", formatName(name)))
            if (!hasList(name))
                throw Exception(String.format("Required List \"%s\" found, but got: %s", formatName(name), get(name)!!::class.java.getName()))
        }
    }

    @Throws(Exception::class)
    fun requireSection(name: String) {
        if (name.contains(".")) {
            val parts = StringUtils.split(name, ".", 2) // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0])
            getSection(parts[0]).requireSection(parts[1])
        } else {
            if (!has(name))
                throw Exception(String.format("Required Section \"%s\" not found", formatName(name)))
            if (!hasSection(name))
                throw Exception(String.format("Required Section \"%s\" found, but contains a value", formatName(name)))
        }
    }

    @Throws(Exception::class)
    fun requireValue(name: String) {
        if (name.contains(".")) {
            val parts = StringUtils.split(name, ".", 2) // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0])
            getSection(parts[0]).requireValue(parts[1])
        } else {
            if (!has(name))
                throw Exception(String.format("Required value '%s' (%s) not found", name, formatName(name)))
            if (hasSection(name))
                throw Exception(String.format("Required value \"%s\" found, but is a section '{}'", formatName(name)))
        }
    }

    @Throws(Exception::class)
    fun requireValues(vararg names: String) {
        for (name in names) {
            requireValue(name)
        }
    }

    fun has(name: String): Boolean {
        return map.containsKey(name)
    }

    fun hasSection(name: String): Boolean {
        return map.containsKey(name) && map[name] is Map<*, *>
    }

    fun hasList(name: String): Boolean {
        return map.containsKey(name) && map[name] is List<*>
    }

    @JvmOverloads operator fun get(name: String, defaultValue: Any? = null): Any? {
        if (name.contains(".")) {
            val parts = StringUtils.split(name, ".", 2) // split "a.b.c.d.e" => "[a, b.c.d.e]"
            try {
                requireSection(parts[0])
                return getSection(parts[0])[parts[1]]
            } catch (e: Exception) {
                return defaultValue
            }

        } else {
            return if (has(name)) map[name] else defaultValue
        }
    }

    fun getString(name: String): String {
        return get(name, "") as String
    }

    fun getString(name: String, defaultValue: String): String {
        return get(name, defaultValue) as String
    }

    fun getInt(name: String): Int {
        return get(name) as Int
    }

    fun formatName(name: String): String {
        return if (StringUtils.isBlank(parent)) {
            name
        } else {
            parent + "." + name
        }
    }

    /**
     * Returns a sub-section of the configuration.  Throws an error if it does not exist, or is a value type
     */
    @Throws(Exception::class)
    @JvmOverloads
    fun getSection(name: String, required: Boolean = false): JsonConfigMap {
        if (required) requireSection(name)
        var out = this
        for (n in StringUtils.split(name, ".")) {
            out = JsonConfigMap(formatName(name), out[n] as Map<*, *>)
        }
        return out
    }

    /**
     * Reads a value from the config map.  If it is another map, return that.  If it is a single value, use
     * the @defaultKey parameter and return a new config map with that as the key, and the value as the... value.
     *
     *
     * This is useful for configurations that can be "simple", e.g. a single value, but where you also want to allow
     * a more complex configuration.
     */
    @Throws(Exception::class)
    fun getSectionOrCreate(name: String, defaultKey: String): JsonConfigMap {
        requireAny(name)
        val v = get(name)
        return if (v is Map<*, *>) {
            JsonConfigMap(formatName(defaultKey), v)
        } else {
            JsonConfigMap(formatName(defaultKey), getStringObjectMap(defaultKey, v!!))
        }
    }

    private fun getStringObjectMap(defaultKey: String, `val`: Any): Map<String, Any> {
        val map1 = LinkedHashMap<String, Any>(5)
        map1.put(defaultKey, `val`)
        return map1
    }

    //    public void withValue(String name, Consumer<String> func) throws Exception {
    //        requireValue(name);
    //        func.accept(getString(name));
    //    }
    //
    //    public <T> void withValue(String name, Class<T> klass, Consumer<T> func) throws Exception {
    //        requireValue(name);
    //        Object value = get(name);
    //        if (klass.isAssignableFrom(value.getClass())) {
    //            //noinspection unchecked
    //            func.accept((T) value);
    //        }
    //    }
    //
    //    public void withValueIf(String name, Consumer<String> func) {
    //        if (has(name)) func.accept(getString(name));
    //    }

    fun getOrDefault(name: String, defaultValue: String): String {
        return if (has(name)) getString(name) else defaultValue
    }

    fun getOrDefault(name: String, defaultValue: Int): Int {
        return if (has(name)) getInt(name) else defaultValue
    }

    companion object {
        private val logger = LogFactory.getLog(JsonConfigMap::class.java.simpleName)
    }
}
