package org.cleanas2.util

import java.nio.file.*
import java.util.*

import org.apache.commons.lang3.StringUtils.isBlank

/**
 * Helper class for random utils that don't have another place to live
 */
object AS2Util {
    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    fun getUniqueFileName(dir: String, fileName: String): Path {
        return getUniqueFileName(Paths.get(dir), Paths.get(fileName).fileName)
    }

    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    fun getUniqueFileName(dir: String, fileName: Path): Path {
        return getUniqueFileName(Paths.get(dir), fileName.fileName)
    }

    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    fun getUniqueFileName(dir: Path, fileName: String): Path {
        return getUniqueFileName(dir, Paths.get(fileName))
    }

    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    fun getUniqueFileName(dir: Path, fileName: Path): Path {
        val normalizedDir = dir.normalize()
        val normalizedFileName = fileName.normalize().fileName
        var newFile = normalizedDir.resolve(normalizedFileName)
        val suffix = 1
        while (Files.exists(newFile)) {
            newFile = normalizedDir.resolve(normalizedFileName.toString() + "." + suffix)
        }
        return newFile
    }

    /**
     * Combines a map into a string by turing each key:value into a line.
     *
     * @param lineDelimiter  Delimiter to place after each key:value pair (eg CRLF)
     * @param valueDelimiter Delimiter to place between each key:value pair (e.g. '=')
     * @param map            Map to combine
     */
    fun combineMap(lineDelimiter: String, valueDelimiter: String, map: Map<String, String>): String {
        val s = StringBuilder()
        for (key in map.keys) {
            s.append(key).append(valueDelimiter).append(map[key]).append(lineDelimiter)
        }
        return s.toString()
    }

    /**
     * Returns the value for the specified key in the map, or the default value if not found.
     */
    fun <T, V> getOrDefault(map: Map<T, V>, key: T, defaultValue: V): V {
        if (!map.containsKey(key)) return defaultValue
        val value = map[key]
        return value ?: defaultValue
    }

    fun <T> getOrDefault(input: T?, defaultValue: T): T {
        return input ?: defaultValue
    }

    /**
     * Returns "" if the input string is NULL or all whitespace
     */
    fun getOrBlank(input: String): String {
        // tests if the input is NULL or whitespace
        return if (isBlank(input)) "" else input
    }

    /**
     * Get all items of the specified type from a list.  Useful for pulling one kind of object
     * out of a list that contains many.
     *
     *
     * <pre>
     * List<MyInterface> items = ofType(listOfObjects, MyInterface.class)
    </MyInterface></pre> *
     */
    fun <T> ofType(list: List<*>, out: Class<T>): List<T> {
        val matching = ArrayList<T>(list.size)
        for (value in list) {
            if (out.isAssignableFrom(value?.javaClass)) {
                if (out.isInstance(value)) {
                    matching.add(value as T)
                }
            }
        }
        return matching
    }

    /**
     * Creates a "safe" filename by only allowing a-z, A-Z, 0-9, -._@#, and
     * appends the "extension" after the file name is fixed
     */
    @JvmOverloads
    fun makeFileName(fileName: String, extension: String = ""): String {
        val fn = Paths.get(fileName).normalize().fileName.toString()
        return fn.replace("[^a-zA-Z0-9\\-\\._@# ]".toRegex(), "").trim { it <= ' ' } + extension
    }

    fun <T> newCaseInsensitiveMap(): AbstractMap<String, T> {
        return TreeMap(String.CASE_INSENSITIVE_ORDER)
    }
}
/**
 * Creates a "safe" filename by only allowing a-z, A-Z, 0-9, -._@#
 */
