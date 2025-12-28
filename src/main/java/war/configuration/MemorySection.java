package war.configuration;

import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.util.*;
import java.util.Map.Entry;

public class MemorySection implements ConfigurationSection {

    protected final Map<String, Object> map = new LinkedHashMap<>();
    @Getter
    private final Configuration root;
    @Getter
    private final ConfigurationSection parent;
    private final String path;
    private final String fullPath;

    protected MemorySection() {
        if (!(this instanceof Configuration)) {
            throw new IllegalStateException("Cannot construct a root MemorySection when not a Configuration");
        } else {
            this.path = "";
            this.fullPath = "";
            this.parent = null;
            this.root = (Configuration) this;
        }
    }

    protected MemorySection(ConfigurationSection parent, String path) {
        Validate.notNull(parent, "Parent cannot be null");
        Validate.notNull(path, "Path cannot be null");
        this.path = path;
        this.parent = parent;
        this.root = parent.getRoot();
        Validate.notNull(this.root, "Path cannot be orphaned");
        this.fullPath = createPath(parent, path);
    }

    public static String createPath(ConfigurationSection section, String key) {
        return createPath(section, key, section == null ? null : section.getRoot());
    }

    public static String createPath(ConfigurationSection section, String key, ConfigurationSection relativeTo) {
        Validate.notNull(section, "Cannot create path without a section");
        Optional<Configuration> root = Optional.ofNullable(section.getRoot());
        if (root.isPresent()) {
            char separator = root.get().options().pathSeparator();
            StringBuilder builder = new StringBuilder();
            for (ConfigurationSection parent = section; parent != null && parent != relativeTo; parent = parent.getParent()) {
                if (!builder.isEmpty()) {
                    builder.insert(0, separator);
                }

                builder.insert(0, parent.getName());
            }

            if (key != null && !key.isEmpty()) {
                if (!builder.isEmpty()) {
                    builder.append(separator);
                }

                builder.append(key);
            }

            return builder.toString();
        }
        throw new IllegalStateException("Cannot create path without a root");
    }

    public Set<String> getKeys(boolean deep) {
        Set<String> result = new LinkedHashSet<>();
        Configuration root = getRoot();
        if (root != null && root.options().copyDefaults()) {
            ConfigurationSection defaults = getDefaultSection();
            if (defaults != null) {
                result.addAll(defaults.getKeys(deep));
            }
        }

        mapChildrenKeys(result, this, deep);
        return result;
    }

    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new LinkedHashMap<>();
        Configuration root = getRoot();
        if (root != null && root.options().copyDefaults()) {
            ConfigurationSection defaults = getDefaultSection();
            if (defaults != null) {
                result.putAll(defaults.getValues(deep));
            }
        }

        mapChildrenValues(result, this, deep);
        return result;
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    public boolean isSet(String path) {
        Configuration root = getRoot();
        if (root == null) {
            return false;
        } else if (root.options().copyDefaults()) {
            return contains(path);
        } else {
            return get(path, null) != null;
        }
    }

    public String getCurrentPath() {
        return fullPath;
    }

    public String getName() {
        return path;
    }

    public void addDefault(String path, Object value) {
        Validate.notNull(path, "Path cannot be null");
        Configuration root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot add default without root");
        } else if (root == this) {
            throw new UnsupportedOperationException("Unsupported addDefault(String, Object) implementation");
        } else {
            root.addDefault(createPath(this, path), value);
        }
    }

    public ConfigurationSection getDefaultSection() {
        Configuration root = getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();
        return defaults != null && defaults.isConfigurationSection(getCurrentPath()) ? defaults.getConfigurationSection(getCurrentPath()) : null;
    }

    public void set(String path, Object value) {
        Validate.notEmpty(path, "Cannot set to an empty path");
        Configuration root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot use section without a root");
        } else {
            char separator = root.options().pathSeparator();
            int i1 = -1;
            ConfigurationSection section = this;

            int i2;
            String key;
            while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
                key = path.substring(i2, i1);
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection == null) {
                    section = section.createSection(key);
                } else {
                    section = subSection;
                }
            }

            key = path.substring(i2);
            if (section == this) {
                if (value == null) {
                    map.remove(key);
                } else {
                    map.put(key, value);
                }
            } else {
                section.set(key, value);
            }
        }
    }

    public Object get(String path) {
        return get(path, getDefault(path));
    }

    public Object get(String path, Object def) {
        Validate.notNull(path, "Path cannot be null");
        if (path.isEmpty()) {
            return this;
        } else {
            Optional<Configuration> root = Optional.ofNullable(getRoot());
            if (root.isPresent()) {
                char separator = root.get().options().pathSeparator();
                int i1 = -1;
                ConfigurationSection section = this;

                do {
                    int i2;
                    if ((i1 = path.indexOf(separator, i2 = i1 + 1)) == -1) {
                        String key = path.substring(i2);
                        if (section == this) {
                            Object result = map.get(key);
                            return result == null ? def : result;
                        }

                        return section.get(key, def);
                    }

                    section = section.getConfigurationSection(path.substring(i2, i1));
                } while (section != null);

                return def;
            }
        }
        throw new IllegalStateException("Cannot access section without a root");
    }

    public ConfigurationSection createSection(String path) {
        Validate.notEmpty(path, "Cannot create section at empty path");
        Configuration root = getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create section without a root");
        } else {
            char separator = root.options().pathSeparator();
            int i1 = -1;
            ConfigurationSection section = this;

            int i2;
            String key;
            while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
                key = path.substring(i2, i1);
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection == null) {
                    section = section.createSection(key);
                } else {
                    section = subSection;
                }
            }

            key = path.substring(i2);
            if (section == this) {
                ConfigurationSection result = new MemorySection(this, key);
                map.put(key, result);
                return result;
            } else {
                return section.createSection(key);
            }
        }
    }

    public ConfigurationSection createSection(String path, Map<?, ?> map) {
        ConfigurationSection section = createSection(path);

        for (Entry<?, ?> value : map.entrySet()) {
            if (value.getValue() instanceof Map) {
                section.createSection(value.getKey().toString(), (Map<?, ?>) value.getValue());
            } else {
                section.set(value.getKey().toString(), value.getValue());
            }
        }

        return section;
    }

    public String getString(String path) {
        Object def = getDefault(path);
        return getString(path, def != null ? def.toString() : null);
    }

    public String getString(String path, String def) {
        Object val = get(path, def);
        return val != null ? val.toString() : def;
    }

    public boolean isString(String path) {
        return get(path) instanceof String;
    }

    public int getInt(String path) {
        Object def = getDefault(path);
        return getInt(path, def instanceof Number ? NumberConversions.toInt(def) : 0);
    }

    public int getInt(String path, int def) {
        Object val = get(path, def);
        return val instanceof Number ? NumberConversions.toInt(val) : def;
    }

    public boolean isInt(String path) {
        return get(path) instanceof Integer;
    }

    public boolean getBoolean(String path) {
        Object def = getDefault(path);
        return this.getBoolean(path, def instanceof Boolean ? (Boolean) def : false);
    }

    public boolean getBoolean(String path, boolean def) {
        Object val = get(path, def);
        return val instanceof Boolean ? (Boolean) val : def;
    }

    public boolean isBoolean(String path) {
        return get(path) instanceof Boolean;
    }

    public double getDouble(String path) {
        Object def = getDefault(path);
        return this.getDouble(path, def instanceof Number ? NumberConversions.toDouble(def) : 0.0D);
    }

    public double getDouble(String path, double def) {
        Object val = get(path, def);
        return val instanceof Number ? NumberConversions.toDouble(val) : def;
    }

    public boolean isDouble(String path) {
        return get(path) instanceof Double;
    }

    public float getFloat(String path) {
        Object def = getDefault(path);
        return this.getFloat(path, def instanceof Float ? NumberConversions.toFloat(def) : 0.0F);
    }

    public float getFloat(String path, float def) {
        Object val = get(path, def);
        return val instanceof Float ? NumberConversions.toFloat(val) : def;
    }

    public boolean isFloat(String path) {
        return get(path) instanceof Float;
    }

    public long getLong(String path) {
        Object def = getDefault(path);
        return getLong(path, def instanceof Number ? NumberConversions.toLong(def) : 0L);
    }

    public long getLong(String path, long def) {
        Object val = get(path, def);
        return val instanceof Number ? NumberConversions.toLong(val) : def;
    }

    public boolean isLong(String path) {
        return get(path) instanceof Long;
    }

    public List<?> getList(String path) {
        Object def = getDefault(path);
        return getList(path, def instanceof List ? (List<?>) def : null);
    }

    public List<?> getList(String path, List<?> def) {
        Object val = get(path, def);
        return (List<?>) (val instanceof List ? val : def);
    }

    public boolean isList(String path) {
        return get(path) instanceof List;
    }

    public List<String> getStringList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<String> result = new ArrayList<>();

            for (Object object : list.get()) {
                if (object instanceof String str) {
                    result.add(str);
                } else if (isPrimitiveWrapper(object)) {
                    result.add(String.valueOf(object));
                }
            }
            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Integer> getIntegerList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Integer> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Integer i -> result.add(i);
                    case String str -> {
                        try {
                            result.add(Integer.valueOf(str));
                        } catch (Exception _) {}
                    }
                    case Character c -> result.add(Integer.valueOf(c));
                    case Number n -> result.add(n.intValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Boolean> getBooleanList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Boolean> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Boolean b -> result.add(b);
                    case String str -> {
                        if (Boolean.TRUE.toString().equals(str)) {
                            result.add(true);
                        } else if (Boolean.FALSE.toString().equals(str)) {
                            result.add(false);
                        }
                    }
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Double> getDoubleList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Double> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Double d -> result.add(d);
                    case String str -> {
                        try {
                            result.add(Double.valueOf(str));
                        } catch (Exception _) {}
                    }
                    case Character c -> result.add((double) c);
                    case Number n -> result.add(n.doubleValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Float> getFloatList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Float> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Float f -> result.add(f);
                    case String str -> {
                        try {
                            result.add(Float.valueOf(str));
                        } catch (Exception _) {}
                    }
                    case Character c -> result.add((float) c);
                    case Number n -> result.add(n.floatValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Long> getLongList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Long> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Long l -> result.add(l);
                    case String str -> {
                        try {
                            result.add(Long.valueOf(str));
                        } catch (Exception _) {}
                    }
                    case Character c -> result.add((long) c);
                    case Number n -> result.add(n.longValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Byte> getByteList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Byte> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Byte b -> result.add(b);
                    case String str -> {
                        try {
                            result.add(Byte.valueOf(str));
                        } catch (Exception _) {}
                    }
                    case Character c -> result.add((byte) c.charValue());
                    case Number n -> result.add(n.byteValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Character> getCharacterList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Character> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Character c -> result.add(c);
                    case String str -> {
                        if (str.length() == 1) {
                            result.add(str.charAt(0));
                        }
                    }
                    case Number n -> result.add((char) n.intValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Short> getShortList(String path) {
        Optional<List<?>> list = Optional.ofNullable(getList(path));
        if (list.isPresent()) {
            List<Short> result = new ArrayList<>();

            for (Object object : list.get()) {
                switch (object) {
                    case Short s -> result.add(s);
                    case String str -> {
                        try {
                            result.add(Short.valueOf(str));
                        } catch (Exception _) {}
                    }
                    case Character c -> result.add((short) c.charValue());
                    case Number n -> result.add(n.shortValue());
                    default -> {}
                }
            }

            return result;
        }
        return new ArrayList<>(0);
    }

    public List<Map<?, ?>> getMapList(String path) {
        List<?> list = this.getList(path);
        List<Map<?, ?>> result = new ArrayList<>();

        if (list != null) {
            for (Object object : list) {
                if (object instanceof Map<?, ?> m) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    public ConfigurationSection getConfigurationSection(String path) {
        Object val = get(path, null);
        if (val != null) {
            return val instanceof ConfigurationSection config ? config : null;
        } else {
            return get(path, getDefault(path)) instanceof ConfigurationSection ? createSection(path) : null;
        }
    }

    public boolean isConfigurationSection(String path) {
        return get(path) instanceof ConfigurationSection;
    }

    protected boolean isPrimitiveWrapper(Object input) {
        return input instanceof Integer || input instanceof Boolean || input instanceof Character || input instanceof Byte || input instanceof Short || input instanceof Double || input instanceof Long || input instanceof Float;
    }

    protected Object getDefault(String path) {
        Validate.notNull(path, "Path cannot be null");
        Configuration root = getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();
        return defaults == null ? null : defaults.get(createPath(this, path));
    }

    protected void mapChildrenKeys(Set<String> output, ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection sec) {
            for (Entry<String, Object> entry : sec.map.entrySet()) {
                output.add(createPath(section, entry.getKey(), this));
                if (deep && entry.getValue() instanceof ConfigurationSection subsection) {
                    mapChildrenKeys(output, subsection, true);
                }
            }
        } else {
            Set<String> keys = section.getKeys(deep);
            for (String key : keys) {
                output.add(createPath(section, key, this));
            }
        }
    }

    protected void mapChildrenValues(Map<String, Object> output, ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection sec) {
            for (Entry<String, Object> entry : sec.map.entrySet()) {
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
                if (deep && entry.getValue() instanceof ConfigurationSection subsection) {
                    mapChildrenValues(output, subsection, true);
                }
            }
        } else {
            Map<String, Object> values = section.getValues(deep);
            for (Entry<String, Object> entry : values.entrySet()) {
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
            }
        }
    }

    public String toString() {
        Configuration root = getRoot();
        return getClass().getSimpleName() + "[path='" + getCurrentPath() + "', root='" + (root == null ? null : root.getClass().getSimpleName()) + "']";
    }
}
