package war.metaphor.mutator.misc;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.annotate.Stability;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.mutator.Mutator;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.Hierarchy;
import war.metaphor.tree.JClassNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Stability(war.jnt.annotate.Level.VERY_LOW)
public class InternalClassIntegrateMutator extends Mutator {

    private static final Logger LOGGER = new Logger();

    public InternalClassIntegrateMutator(ObfuscatorContext base, ConfigurationSection config) {
        super(base, config);
    }

    @Override
    public void run(ObfuscatorContext base) {
        LOGGER.logln(Level.INFO, Origin.METAPHOR, "Building hierarchy of the full jdk... (this might take a bit)");
        Hierarchy.INSTANCE.ensureGraphBuilt();
        Map<String, JClassNode> classes = new HashMap<>();

        for (JClassNode classNode : base.getClasses()) {
            if (classNode.isExempt()) continue;
            if (classNode.isInterface()) continue;

            handle(base, classNode, classes);
        }

        int hashCode = 0;
        int iter = 0;

        do {
            if (hashCode != 0) {
                iter++;

                if (iter > 3) {
                    break;
                }
            }
            hashCode = hash(classes);

            for (JClassNode classNode : new ArrayList<>(classes.values())) {
                handle(base, classNode, classes);
            }
        } while (hashCode != hash(classes));

        for (JClassNode value : classes.values()) {
            base.getClasses().add(value);
        }
    }

    private int hash(Map<String, JClassNode> classes) {
        int hash = 0;

        for (Map.Entry<String, JClassNode> entry : classes.entrySet()) {
            hash += entry.getKey().hashCode();

            JClassNode value = entry.getValue();
            hash += value.name.hashCode();
            for (MethodNode method : value.methods) {
                hash += method.name.hashCode() + method.desc.hashCode();
            }
        }

        return hash;
    }

    private void handle(ObfuscatorContext base, JClassNode classNode, Map<String, JClassNode> classes) {
        Map<JClassNode, ArrayList<MethodNode>> toAdd = new HashMap<>();

        ConfigurationSection cfg = base.getConfig();
        String libPath = cfg.getString("jnt-path", "war/jnt");

        for (MethodNode method : classNode.methods) {
            if (Modifier.isAbstract(method.access)) continue;
            if (classNode.isExempt(method)) continue;

            for (AbstractInsnNode ain : method.instructions) {
                if (ain instanceof MethodInsnNode min) {
                    if (min.itf) continue;
                    if (min.opcode != INVOKESTATIC && min.opcode != INVOKEVIRTUAL) continue;
//                    if (!JavaInternalClasses.classes.containsKey(min.owner)) continue;
                    if (min.owner.startsWith("jdk")) continue;
                    JClassNode jClassNode = base.loadClass(min.owner);
                    if (jClassNode == null) continue;
                    MethodNode orig = jClassNode.getMethod(min.name, min.desc);
                    if (orig == null) continue;
                    if (jClassNode.isInterface()) continue;
                    if (Modifier.isAbstract(orig.access)) continue;
                    if (Modifier.isNative(orig.access)) continue;
                    // if (hasNonPublicCalls(base, orig)) continue;
                    if (min.opcode != INVOKESTATIC) {
                        if (Hierarchy.INSTANCE.getMethodHierarchy(new ClassMethod(jClassNode, orig)).stream().filter(cm -> !base.classes.contains(cm.getClassNode())).count() != 1)
                            continue;
                    }

                    if (!classes.containsKey(min.owner)) {
                        JClassNode node = new JClassNode();
                        node.visit(V1_8, ACC_PUBLIC, libPath + "/internal/" + min.owner, null, "java/lang/Object", null);

                        node.methods.clear(); // idk just to be sure?

                        classes.put(min.owner, node);
                    }

                    JClassNode holderNode = classes.get(min.owner);

                    if (!toAdd.containsKey(holderNode)) {
                        toAdd.put(holderNode, new ArrayList<>());
                    }

                    boolean foundMethod = false;
                    for (MethodNode methodNode : holderNode.methods) {
                        if (methodNode.name == null) continue;
                        if (methodNode.desc == null) continue;
                        boolean descMatch = false;
                        if (min.opcode == INVOKEVIRTUAL) {
                            if (methodNode.desc.equals("(L" + min.owner + ";" + min.desc.substring(1))) descMatch = true;
                        }
                        if (methodNode.desc.equals(min.desc)) descMatch = true;
                        if (!descMatch) continue;
                        if (!methodNode.name.equals(min.name)) continue;

                        foundMethod = true;
                        break;
                    }
                    for (ArrayList<MethodNode> value : toAdd.values()) {
                        for (MethodNode methodNode : value) {
                            if (methodNode.name == null) continue;
                            if (methodNode.desc == null) continue;
                            boolean descMatch = false;
                            if (min.opcode == INVOKEVIRTUAL) {
                                if (methodNode.desc.equals("(L" + min.owner + ";" + min.desc.substring(1))) descMatch = true;
                            }
                            if (methodNode.desc.equals(min.desc)) descMatch = true;
                            if (!descMatch) continue;
                            if (!methodNode.name.equals(min.name)) continue;

                            foundMethod = true;
                        }
                        if (foundMethod) break;
                    }

                    if (!foundMethod) {
                        MethodNode newNode = new MethodNode();

                        newNode.name = min.name;
                        newNode.desc = min.desc;
                        newNode.access = orig.access;
                        orig.accept(newNode);

                        if (min.opcode == INVOKEVIRTUAL) {
                            newNode.desc = "(L" + min.owner + ";" + newNode.desc.substring(1);
                            newNode.access |= ACC_STATIC;
                        }

                        for (AbstractInsnNode instruction : newNode.instructions) {
                            if (instruction instanceof MethodInsnNode patch) {
                                if (patch.opcode == INVOKESPECIAL && !patch.name.contains("<")) {
                                    patch.opcode = INVOKEVIRTUAL; // java tends to do that for some odd reason
                                }
                            }
                        }

                        toAdd.get(holderNode).add(newNode);
                    }

                    if (min.opcode == INVOKEVIRTUAL) {
                        min.desc = "(L" + min.owner + ";" + min.desc.substring(1);
                    }
                    min.owner = libPath + "/internal/" + min.owner;
                    min.opcode = INVOKESTATIC;
                }
            }
        }

        for (Map.Entry<JClassNode, ArrayList<MethodNode>> entry : toAdd.entrySet()) {
            for (MethodNode methodNode : entry.getValue()) {
                entry.getKey().methods.add(methodNode);
            }
        }
    }

    private boolean hasNonPublicCalls(ObfuscatorContext base, MethodNode orig) {
        for (AbstractInsnNode ain : orig.instructions) {
            if (ain instanceof MethodInsnNode min) {
                if (min.itf) {
                    return true;
                }
                JClassNode jClassNode = base.loadClass(min.owner);
                if (!Modifier.isPublic(jClassNode.access)) {
                    return true;
                }
                if (!Modifier.isPublic(jClassNode.getMethod(min.name, min.desc).access)) {
                    return true;
                }
            } else if (ain instanceof FieldInsnNode fin) {
                JClassNode jClassNode = base.loadClass(fin.owner);
                if (!Modifier.isPublic(jClassNode.access)) {
                    return true;
                }
                if (!Modifier.isPublic(jClassNode.getField(fin.name, fin.desc).access)) {
                    return true;
                }
            }
        }

        return false;
    }
}
