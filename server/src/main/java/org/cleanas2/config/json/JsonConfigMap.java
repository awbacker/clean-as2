package org.cleanas2.config.json;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps a Map instance with a "parent" kind of name, plus utility methods to simplify and reduce
 * the code needed when reading from a json config file.
 *
 * @author Andrew Backer {@literal awbacker@gmail.com / andrew.backer@powere2e.com}
 */
public class JsonConfigMap {
    private static final Log logger = LogFactory.getLog(JsonConfigMap.class.getSimpleName());

    private final Map map;
    private final String parent;

    public JsonConfigMap(String parent, Map map) {
        this.parent = parent;
        this.map = map;
    }

    private void requireAny(String name) throws Exception {
        if (name.contains(".")) {
            String[] parts = StringUtils.split(name, ".", 2); // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0]); // validate the first section
            getSection(parts[0]).requireAny(parts[1]); // validate the rest, which may result in > 1 further validations
        }
        if (!has(name))
            throw new Exception(String.format("Required element \"%s\" missing", formatName(name)));
    }

    public void requireList(String name) throws Exception {
        if (name.contains(".")) {
            String[] parts = StringUtils.split(name, ".", 2); // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0]);
            getSection(parts[0]).requireSection(parts[1]);
        } else {
            if (!has(name))
                throw new Exception(String.format("Required List \"%s\" not found", formatName(name)));
            if (!hasList(name))
                throw new Exception(String.format("Required List \"%s\" found, but it is not a list : %s", formatName(name), get(name).getClass().getName()));
        }
    }

    public void requireSection(String name) throws Exception {
        if (name.contains(".")) {
            String[] parts = StringUtils.split(name, ".", 2); // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0]);
            getSection(parts[0]).requireSection(parts[1]);
        } else {
            if (!has(name))
                throw new Exception(String.format("Required Section \"%s\" not found", formatName(name)));
            if (!hasSection(name))
                throw new Exception(String.format("Required Section \"%s\" found, but contains a value", formatName(name)));
        }
    }

    public void requireValue(String name) throws Exception {
        if (name.contains(".")) {
            String[] parts = StringUtils.split(name, ".", 2); // split "a.b.c.d.e" => "[a, b.c.d.e]"
            requireSection(parts[0]);
            getSection(parts[0]).requireValue(parts[1]);
        } else {
            if (!has(name))
                throw new Exception(String.format("Required value '%s' (%s) not found", name, formatName(name)));
            if (hasSection(name))
                throw new Exception(String.format("Required value \"%s\" found, but is a section '{}'", formatName(name)));
        }
    }

    public void requireValues(String... names) throws Exception {
        for (String name : names) {
            requireValue(name);
        }
    }

    public boolean has(String name) {
        return map.containsKey(name);
    }

    public boolean hasSection(String name) {
        return map.containsKey(name) && map.get(name) instanceof Map;
    }

    public boolean hasList(String name) {
        return map.containsKey(name) && map.get(name) instanceof List<?>;
    }

    public Object get(String name) {
        return get(name, null);
    }

    public Object get(String name, Object defaultValue) {
        if (name.contains(".")) {
            String[] parts = StringUtils.split(name, ".", 2); // split "a.b.c.d.e" => "[a, b.c.d.e]"
            try {
                requireSection(parts[0]);
                return getSection(parts[0]).get(parts[1]);
            } catch (Exception e) {
                return defaultValue;
            }
        } else {
            return has(name) ? map.get(name) : defaultValue;
        }
    }

    public String getString(String name) {
        return (String) get(name, "");
    }

    public String getString(String name, String defaultValue) {
        return (String) get(name, defaultValue);
    }

    public int getInt(String name) {
        return (int) get(name);
    }

    public String formatName(String name) {
        if (StringUtils.isBlank(parent)) {
            return name;
        } else {
            return parent + "." + name;
        }
    }

    /**
     * Returns a sub-section of the configuration.  Throws an error if it does not exist, or is a value type
     */
    public JsonConfigMap getSection(String name, boolean required) throws Exception {
        if (required) requireSection(name);
        JsonConfigMap out = this;
        for (String n : StringUtils.split(name, ".")) {
            out = new JsonConfigMap(formatName(name), (Map) out.get(n));
        }
        return out;
    }

    public JsonConfigMap getSection(String name) throws Exception {
        return getSection(name, false);
    }

    /**
     * Reads a value from the config map.  If it is another map, return that.  If it is a single value, use
     * the @defaultKey parameter and return a new config map with that as the key, and the value as the... value.
     * <p/>
     * This is useful for configurations that can be "simple", e.g. a single value, but where you also want to allow
     * a more complex configuration.
     */
    public JsonConfigMap getSectionOrCreate(String name, String defaultKey) throws Exception {
        requireAny(name);
        Object val = get(name);
        if (val instanceof Map) {
            return new JsonConfigMap(formatName(defaultKey), (Map) val);
        } else {
            return new JsonConfigMap(formatName(defaultKey), getStringObjectMap(defaultKey, val));
        }
    }

    private Map<String, Object> getStringObjectMap(String defaultKey, Object val) {
        Map<String, Object> map1 = new LinkedHashMap<>(5);
        map1.put(defaultKey, val);
        return map1;
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

    public String getOrDefault(String name, String defaultValue) {
        if (has(name)) return getString(name);
        return defaultValue;
    }

    public int getOrDefault(String name, int defaultValue) {
        if (has(name)) return getInt(name);
        return defaultValue;
    }
}
