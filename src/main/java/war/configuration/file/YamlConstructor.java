package war.configuration.file;

import war.configuration.serialization.ConfigurationSerialization;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

public class YamlConstructor extends SafeConstructor {

    public YamlConstructor() {
        super(new LoaderOptions());
        this.yamlConstructors.put(Tag.MAP, new ConstructCustomObject());
    }

    private class ConstructCustomObject extends ConstructYamlMap {

        public Object construct(Node node) {
            if (node.isTwoStepsConstruction()) {
                throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
            } else {
                Map<?, ?> raw = (Map<?, ?>) super.construct(node);
                if (!raw.containsKey("==")) {
                    return raw;
                } else {
                    Map<String, Object> typed = new LinkedHashMap<>(raw.size());

                    for (Map.Entry<?, ?> value : raw.entrySet()) {
                        typed.put(value.getKey().toString(), value.getValue());
                    }

                    try {
                        return ConfigurationSerialization.deserializeObject(typed);
                    } catch (IllegalArgumentException var6) {
                        throw new YAMLException("Could not deserialize object", var6);
                    }
                }
            }
        }

        public void construct2ndStep(Node node, Object object) {
            throw new YAMLException("Unexpected referential mapping structure. Node: " + node);
        }
    }
}
