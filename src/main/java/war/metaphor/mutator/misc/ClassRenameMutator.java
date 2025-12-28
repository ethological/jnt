package war.metaphor.mutator.misc;

import war.configuration.ConfigurationSection;
import war.jar.JarResource;
import war.jnt.annotate.Level;
import war.jnt.annotate.Stability;
import war.jnt.utility.mapping.Mapping;
import war.jnt.utility.mapping.impl.ClassIdentity;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.MappingMutator;
import war.metaphor.resources.FabricModResource;
import war.metaphor.resources.IResource;
import war.metaphor.tree.JClassNode;
import war.metaphor.util.Dictionary;
import war.metaphor.util.Purpose;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Stability(Level.HIGH)
public class ClassRenameMutator extends MappingMutator {

    public ClassRenameMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    boolean test = false;
    @Override
    public void run(ObfuscatorContext base) {
        Map<String, String> mapping = new HashMap<>();

        List<JClassNode> classesList = new ArrayList<>(base.getClasses());
        Collections.shuffle(classesList);

        for (JClassNode classNode : classesList) {
            if (classNode.isExempt()) continue;
            String newName = config.getString("prefix", "") + Dictionary.gen(1, Purpose.CLASS);
            String oldName = classNode.name;

            mapping.put(oldName, newName);

            base.getRepository().add(new Mapping(
                    new ClassIdentity(oldName),
                    new ClassIdentity(newName)
            ));
        }

        map(base, mapping);

        Manifest manifest = base.getManifest();
        if (manifest != null) {
            Attributes attrs = manifest.getMainAttributes();
            attrs.replaceAll((_, val) -> {
                if (val instanceof String) {
                    String normalized = ((String) val).replace('.', '/');
                    return mapping.getOrDefault(normalized, (String) val);
                }
                return val;
            });
        }

        Map<String, IResource> resourceHandlers = new HashMap<>();
        resourceHandlers.put("fabric.mod.json", new FabricModResource());

        for (JarResource resource : base.getResources()) {
            if (resourceHandlers.containsKey(resource.name())) {
                IResource handler = resourceHandlers.get(resource.name());
                String contents = new String(resource.content());
                resource.setContent(handler.handle(contents, mapping).getBytes(StandardCharsets.UTF_8));
            }
        }

    }
}
