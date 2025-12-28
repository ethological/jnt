package war.metaphor.tree;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import war.metaphor.base.ObfuscatorContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class Hierarchy {

    public static Hierarchy INSTANCE;

    private final ObfuscatorContext core = ObfuscatorContext.INSTANCE;

    private final ConcurrentMap<ClassMethod, Set<ClassMethod>> methodHierarchy = new ConcurrentHashMap<>();
    private final ConcurrentMap<ClassField,  Set<ClassField>> fieldHierarchy  = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> commonSuperCache = new ConcurrentHashMap<>();

    private final Set<JClassNode> computedClasses = ConcurrentHashMap.newKeySet();

    private volatile boolean graphBuilt = false;

    public Hierarchy() {
        INSTANCE = this;
    }

    public void reset() {
        core.getClasses().forEach(JClassNode::resetHierarchy);
        core.getLibraries().forEach(JClassNode::resetHierarchy);

        methodHierarchy.clear();
        fieldHierarchy.clear();
        commonSuperCache.clear();

        computedClasses.clear();
        graphBuilt = false;
    }

    public void iterateClass(JClassNode cls) {
        if (cls != null) {
            ensureClassLinked(cls);
        }
    }

    public Set<ClassMethod> getMethodHierarchy(ClassMethod m) {
        if (m == null) return Collections.emptySet();

        Set<ClassMethod> set = methodHierarchy.get(m);
        if (set != null) return set;

        ensureHierarchyComputed(m.getClassNode());

        return methodHierarchy.computeIfAbsent(m, k -> {
            Set<ClassMethod> s = ConcurrentHashMap.newKeySet();
            s.add(k);
            return s;
        });
    }

    public Set<ClassField> getFieldHierarchy(ClassField f) {
        if (f == null) return Collections.emptySet();

        Set<ClassField> set = fieldHierarchy.get(f);
        if (set != null) return set;

        ensureHierarchyComputed(f.getClassNode());

        return fieldHierarchy.computeIfAbsent(f, k -> {
            Set<ClassField> s = ConcurrentHashMap.newKeySet();
            s.add(k);
            return s;
        });
    }

    public Set<JClassNode> getClassParents(JClassNode start) {
        ensureClassLinked(start);

        Set<JClassNode> parents = new HashSet<>();
        parents.add(start);

        Deque<JClassNode> queue = new ArrayDeque<>();

        if (start.getParents() != null) {
            queue.addAll(start.getParents());
        }

        while (!queue.isEmpty()) {
            JClassNode parent = queue.poll();
            if (parents.add(parent)) {
                ensureClassLinked(parent);
                if (parent.getParents() != null) {
                    queue.addAll(parent.getParents());
                }
            }
        }

        return parents;
    }


    public Set<JClassNode> getClassHierarchy(JClassNode start) {
        ensureClassLinked(start);

        Set<JClassNode> hierarchy = new HashSet<>();
        hierarchy.add(start);

        Deque<JClassNode> upQueue = new ArrayDeque<>();
        if (start.getParents() != null) upQueue.addAll(start.getParents());

        while (!upQueue.isEmpty()) {
            JClassNode parent = upQueue.poll();
            if (hierarchy.add(parent)) {
                ensureClassLinked(parent);
                if (parent.getParents() != null) {
                    upQueue.addAll(parent.getParents());
                }
            }
        }

        Deque<JClassNode> downQueue = new ArrayDeque<>();

        if (start.getChildren() != null) downQueue.addAll(start.getChildren());

        while (!downQueue.isEmpty()) {
            JClassNode child = downQueue.poll();
            if (hierarchy.add(child)) {
                ensureClassLinked(child);
                if (child.getChildren() != null) {
                    downQueue.addAll(child.getChildren());
                }
            }
        }

        return hierarchy;
    }


    public String getCommonSuperClass(String t1, String t2) {
        String key = t1 + '#' + t2;
        String cached = commonSuperCache.get(key);
        if (cached != null) return cached;

        String result = computeCommonSuperClass(t1, t2);
        commonSuperCache.put(key, result);
        commonSuperCache.put(t2 + '#' + t1, result);
        return result;
    }

    public void ensureGraphBuilt() {
        if (graphBuilt) return;
        synchronized (this) {
            if (graphBuilt) return;
            Stream.concat(core.getClasses().parallelStream(), core.getLibraries().parallelStream())
                    .forEach(this::ensureClassLinked);
            graphBuilt = true;
        }
    }


    private void ensureClassLinked(JClassNode cls) {
        if (cls == null || cls.linked) return;

        synchronized (cls) {
            if (cls.linked) return;
            cls.linked = true;

            String superName = cls.superName;
            List<String> interfaces = cls.interfaces;

            if (superName != null) {
                JClassNode p = core.loadClass(superName);
                if (p != null) {
                    p.addChild(cls);
                    cls.addParent(p);
                }
            }

            if (interfaces != null) {
                for (String anInterface : interfaces) {
                    JClassNode p = core.loadClass(anInterface);
                    if (p != null) {
                        p.addChild(cls);
                        cls.addParent(p);
                    }
                }
            }


        }
    }

    private void ensureHierarchyComputed(JClassNode cls) {
        if (cls == null || computedClasses.contains(cls)) return;

        ensureClassLinked(cls);

        synchronized (cls) {
            if (computedClasses.contains(cls)) return;

            if (cls.superName != null) {
                JClassNode p = core.loadClass(cls.superName);
                if (p != null) ensureHierarchyComputed(p);
            }
            if (cls.interfaces != null) {
                for (String iface : cls.interfaces) {
                    JClassNode p = core.loadClass(iface);
                    if (p != null) ensureHierarchyComputed(p);
                }
            }

            if (cls.methods != null) {
                for (var m : cls.methods) {
                    if ((m.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) continue;

                    ClassMethod cm = ClassMethod.of(cls, m);

                    ClassMethod parentMethod = resolveMethodUpwards(cls, m);

                    if (parentMethod != null) {
                        mergeMethodSets(cm, parentMethod);
                    } else {
                        methodHierarchy.computeIfAbsent(cm, k -> {
                            Set<ClassMethod> s = ConcurrentHashMap.newKeySet();
                            s.add(k);
                            return s;
                        });
                    }
                }
            }

            if (cls.fields != null) {
                for (var f : cls.fields) {
                    if ((f.access & Opcodes.ACC_PRIVATE) != 0) continue;
                    ClassField cf = ClassField.of(cls, f);
                    ClassField parentField = resolveFieldUpwards(cls, f);

                    if (parentField != null) {
                        mergeFieldSets(cf, parentField);
                    } else {
                        fieldHierarchy.computeIfAbsent(cf, k -> {
                            Set<ClassField> s = ConcurrentHashMap.newKeySet();
                            s.add(k);
                            return s;
                        });
                    }
                }
            }

            if (cls.getChildren() != null) {
                for (JClassNode child : cls.getChildren()) {
                    if (computedClasses.contains(child)) {
                        linkChildrenMethods(cls, child);
                        linkChildrenFields(cls, child);
                    }
                }
            }

            computedClasses.add(cls);
        }
    }

    private void linkChildrenMethods(JClassNode parent, JClassNode child) {
        if (child.methods == null) return;

        for (var m : child.methods) {
            if ((m.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) continue;

            ClassMethod parentMethod = findMethodInNode(parent, m);

            if (parentMethod != null) {
                ClassMethod childMethod = ClassMethod.of(child, m);
                mergeMethodSets(childMethod, parentMethod);
            }
        }

        if (child.getChildren() != null) {
            for (JClassNode grandChild : child.getChildren()) {
                if (computedClasses.contains(grandChild)) {
                    linkChildrenMethods(parent, grandChild);
                }
            }
        }
    }

    private void linkChildrenFields(JClassNode parent, JClassNode child) {
        if (child.fields == null) return;

        for (var f : child.fields) {
            if ((f.access & Opcodes.ACC_PRIVATE) != 0) continue;

            if (parent.fields != null) {
                for (var pf : parent.fields) {
                    if (pf.name.equals(f.name) && pf.desc.equals(f.desc)) {
                        ClassField childField = ClassField.of(child, f);
                        ClassField parentField = ClassField.of(parent, pf);
                        mergeFieldSets(childField, parentField);
                        break;
                    }
                }
            }
        }

        if (child.getChildren() != null) {
            for (JClassNode grandChild : child.getChildren()) {
                if (computedClasses.contains(grandChild)) {
                    linkChildrenFields(parent, grandChild);
                }
            }
        }
    }

    private void mergeMethodSets(ClassMethod child, ClassMethod parent) {
        Set<ClassMethod> parentSet = getMethodHierarchy(parent);
        parentSet.add(child);
        methodHierarchy.put(child, parentSet);
    }

    private void mergeFieldSets(ClassField child, ClassField parent) {
        Set<ClassField> parentSet = getFieldHierarchy(parent);
        parentSet.add(child);
        fieldHierarchy.put(child, parentSet);
    }

    private ClassMethod resolveMethodUpwards(JClassNode start, MethodNode look) {
        if (start.superName != null) {
            JClassNode p = core.loadClass(start.superName);
            if (p != null) {
                ClassMethod found = findMethodInNode(p, look);
                if (found != null) return found;
                found = resolveMethodUpwards(p, look);
                if (found != null) return found;
            }
        }

        if (start.interfaces != null) {
            for (String iface : start.interfaces) {
                JClassNode p = core.loadClass(iface);
                if (p != null) {
                    ClassMethod found = findMethodInNode(p, look);
                    if (found != null) return found;
                    found = resolveMethodUpwards(p, look);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    private ClassMethod findMethodInNode(JClassNode node, MethodNode look) {
        if (node.methods == null) return null;
        for (var m : node.methods) {
            if (m.name.equals(look.name) && m.desc.equals(look.desc)) {
                return ClassMethod.of(node, m);
            }
        }
        return null;
    }

    private ClassField resolveFieldUpwards(JClassNode start, FieldNode look) {
        if (start.superName != null) {
            JClassNode p = core.loadClass(start.superName);
            if (p != null) {
                if (p.fields != null) {
                    for (var f : p.fields) {
                        if (f.name.equals(look.name) && f.desc.equals(look.desc)) {
                            return ClassField.of(p, f);
                        }
                    }
                }
                return resolveFieldUpwards(p, look);
            }
        }
        return null;
    }

    private String computeCommonSuperClass(String type1, String type2) {
        if (type1.startsWith("[") || type2.startsWith("[")) {
            if (!type1.startsWith("[") || !type2.startsWith("[")) return "java/lang/Object";
            String e1 = type1.substring(1);
            String e2 = type2.substring(1);
            if (e1.length() == 1 && !e1.equals(e2)) return "java/lang/Object";
            if (e2.length() == 1 && !e1.equals(e2)) return "java/lang/Object";
            String n1 = (e1.startsWith("L") && e1.endsWith(";")) ? e1.substring(1, e1.length() - 1) : e1;
            String n2 = (e2.startsWith("L") && e2.endsWith(";")) ? e2.substring(1, e2.length() - 1) : e2;
            return "[" + getCommonSuperClass(n1, n2);
        }

        JClassNode c1 = loadOrPrimitive(type1);
        JClassNode c2 = loadOrPrimitive(type2);
        if (c1 == null || c2 == null) return "java/lang/Object";

        ensureClassLinked(c1);
        ensureClassLinked(c2);

        if (c1.isAssignableFrom(c2)) return type1;
        if (c2.isAssignableFrom(c1)) return type2;

        if (!c1.isInterface() && !c2.isInterface()) {
            JClassNode curr = c1;
            while (curr != null && !curr.isAssignableFrom(c2)) {
                curr = core.loadClass(curr.superName);
                if (curr != null) ensureClassLinked(curr);
            }
            return curr != null ? curr.name.replace('.', '/') : "java/lang/Object";
        }
        return "java/lang/Object";
    }

    private JClassNode loadOrPrimitive(String name) {
        JClassNode cls = core.loadClass(name);
        if (cls != null) return cls;
        String boxed = switch (name) {
            case "B" -> "java/lang/Byte";
            case "C" -> "java/lang/Character";
            case "D" -> "java/lang/Double";
            case "F" -> "java/lang/Float";
            case "I" -> "java/lang/Integer";
            case "J" -> "java/lang/Long";
            case "S" -> "java/lang/Short";
            case "Z" -> "java/lang/Boolean";
            default -> name;
        };
        return core.loadClass(boxed);
    }

}
