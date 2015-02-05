package org.cleanas2.util;

import java.nio.file.*;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Helper class for random utils that don't have another place to live
 */
@SuppressWarnings("UnusedDeclaration")
public class AS2Util {
    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    public static Path getUniqueFileName(String dir, String fileName) {
        return getUniqueFileName(Paths.get(dir), Paths.get(fileName).getFileName());
    }

    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    public static Path getUniqueFileName(String dir, Path fileName) {
        return getUniqueFileName(Paths.get(dir), fileName.getFileName());
    }

    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    public static Path getUniqueFileName(Path dir, String fileName) {
        return getUniqueFileName(dir, Paths.get(fileName));
    }

    /**
     * Takes an existing path & filename and returns a FULL PATH to a unique file.  If the
     * file does not exist, it will return the same file.  If the file exists, will append
     * a suffix ( .1, .2, .3, etc) until a unique name is found.
     *
     * @param dir      Base directory to use (does not include file name)
     * @param fileName Starting filename.
     */
    public static Path getUniqueFileName(Path dir, Path fileName) {
        dir = dir.normalize();
        fileName = fileName.normalize().getFileName();
        Path newFile = dir.resolve(fileName);
        int suffix = 1;
        while (Files.exists(newFile)) {
            newFile = dir.resolve(fileName.toString() + "." + suffix);
        }
        return newFile;
    }

    /**
     * Combines a map into a string by turing each key:value into a line.
     *
     * @param lineDelimiter  Delimiter to place after each key:value pair (eg CRLF)
     * @param valueDelimiter Delimiter to place between each key:value pair (e.g. '=')
     * @param map            Map to combine
     */
    public static String combineMap(String lineDelimiter, String valueDelimiter, Map<String, String> map) {
        StringBuilder s = new StringBuilder();
        for (String key : map.keySet()) {
            s.append(key).append(valueDelimiter).append(map.get(key)).append(lineDelimiter);
        }
        return s.toString();
    }

    /**
     * Returns the value for the specified key in the map, or the default value if not found.
     */
    public static <T, V> V getOrDefault(Map<T, V> map, T key, V defaultValue) {
        if (!map.containsKey(key)) return defaultValue;
        V value = map.get(key);
        return (value == null) ? defaultValue : value;
    }

    public static <T> T getOrDefault(T input, T defaultValue) {
        return (input == null) ? defaultValue : input;
    }

    /**
     * Returns "" if the input string is NULL or all whitespace
     */
    public static String getOrBlank(String input) {
        // tests if the input is NULL or whitespace
        if (isBlank(input)) return "";
        return input;
    }

    /**
     * Get all items of the specified type from a list.  Useful for pulling one kind of object
     * out of a list that contains many.
     * <p/>
     * <pre>
     * List<MyInterface> items = ofType(listOfObjects, MyInterface.class)
     * </pre>
     */
    public static <T> List<T> ofType(List<?> list, Class<T> out) {
        List<T> matching = new ArrayList<>(list.size());
        for (Object value : list) {
            if (out.isAssignableFrom(value.getClass())) {
                if (out.isInstance(value)) {
                    //noinspection unchecked
                    matching.add((T) value);
                }
            }
        }
        return matching;
    }

    /**
     * Creates a "safe" filename by only allowing a-z, A-Z, 0-9, -._@#
     */
    public static String makeFileName(String fileName) {
        return makeFileName(fileName, "");
    }

    /**
     * Creates a "safe" filename by only allowing a-z, A-Z, 0-9, -._@#, and
     * appends the "extension" after the file name is fixed
     */
    public static String makeFileName(String fileName, String extension) {
        String fn = Paths.get(fileName).normalize().getFileName().toString();
        return fn.replaceAll("[^a-zA-Z0-9\\-\\._@# ]", "").trim() + extension;
    }

    public static <T> Map<String, T> newCaseInsensitiveMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
}
