package war.metaphor.tree;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.SymbolTable;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import war.jnt.dash.Ansi;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.asm.JClassWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static war.jnt.dash.Ansi.Color.YELLOW;

@Getter
public class JClassNode extends ClassNode implements Opcodes {

    public volatile boolean linked = false;

    private final Set<JClassNode> children;
    private final Set<JClassNode> parents;

    private final Set<String> exemptMembers = new HashSet<>();
    private boolean exemptSelf;

    private final boolean library;
    public SymbolTable symbolTable;
    public SymbolTable cachedSymbolTable;

    @Setter
    private String realName;

    @Setter
    private String liftedInitializer;

    public JClassNode() {
        this(false);
    }

    public JClassNode(boolean library) {
        super(Opcodes.ASM8);
        this.library = library;
        this.children = ConcurrentHashMap.newKeySet();
        this.parents = ConcurrentHashMap.newKeySet();
        this.symbolTable = new SymbolTable(null);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        if (realName == null) realName = name;
    }

    public Set<JClassNode> getParents() {
        Hierarchy.INSTANCE.iterateClass(this);
        return parents;
    }

    public Set<JClassNode> getChildren() {
        Hierarchy.INSTANCE.iterateClass(this);
        return children;
    }

    public void addExempt() {
        exemptSelf = true;
    }

    public void addExemptMember(String member) {
        exemptMembers.add(member);
    }

    public void addExemptMember(MethodNode member) {
        exemptMembers.add(name + "." + member.name + member.desc);
    }

    public void addExemptMember(FieldNode member) {
        exemptMembers.add(name + "." + member.name + member.desc);
    }

    public boolean isExempt() {
        return exemptSelf;
    }

    public boolean isExempt(MethodNode method) {
        String name = this.name + "." + method.name + method.desc;
        return exemptMembers.contains(name);
    }

    public boolean isExempt(FieldNode field) {
        String name = this.name + "." + field.name + field.desc;
        return exemptMembers.contains(name);
    }

    public void addChild(JClassNode child) {
        children.add(child);
    }

    public void addParent(JClassNode parent) {
        parents.add(parent);
    }

    public boolean isFinal() {
        return (access & ACC_FINAL) != 0;
    }

    public boolean isInterface() {
        return (access & ACC_INTERFACE) != 0;
    }

    public boolean isEnum() {
        return (access & ACC_ENUM) != 0;
    }

    public boolean isAnnotation() {
        return (access & ACC_ANNOTATION) != 0;
    }

    public boolean hasAnnotation(String annotation) {
        if (visibleAnnotations != null && visibleAnnotations.stream().anyMatch(a -> a.desc.equals(annotation)))
            return true;
        return invisibleAnnotations != null && invisibleAnnotations.stream().anyMatch(a -> a.desc.equals(annotation));
    }

    public boolean isPublic() {
        return (access & ACC_PUBLIC) != 0;
    }

    public boolean isPrivate() {
        return (access & ACC_PRIVATE) != 0;
    }

    public String getPackage() {
        if (!name.contains("/")) return "";
        return name.substring(0, name.lastIndexOf('/') + 1);
    }

    public void cacheSymbolTable() {
        cachedSymbolTable = symbolTable.clone();
    }

    public void resetSymbolTable() {
        symbolTable = cachedSymbolTable;
        cachedSymbolTable = null;
    }

    public void removeExempt() {
        exemptMembers.clear();
        exemptSelf = false;
    }

    public byte[] compute() {
        JClassWriter writer;
        try {
            cacheSymbolTable();
            writer = new JClassWriter(ClassWriter.COMPUTE_FRAMES, symbolTable);
            symbolTable.classWriter = writer;
            accept(writer);
            return writer.toByteArray();
        } catch (Exception ex) {
            Logger.INSTANCE.logln(Level.WARNING, Origin.METAPHOR,
                    String.format("Could not compute class %s -> %s (%s)", ex.getMessage(), new Ansi().c(YELLOW).s(name).r(false).c(Ansi.Color.BRIGHT_YELLOW),
                        new Ansi().c(YELLOW).s(realName).r(false).c(Ansi.Color.BRIGHT_YELLOW)));
            resetSymbolTable();
            writer = new JClassWriter(ClassWriter.COMPUTE_MAXS, symbolTable);
            symbolTable.classWriter = writer;
            accept(writer);
            return writer.toByteArray();
        }
    }

    public MethodNode getStaticInit() {
        String name = "<clinit>";

        for (MethodNode method : methods) {
            if (method.name.equals(name) && method.desc.equals("()V")) {
                return method;
            }
        }

        MethodNode method = new MethodNode(ACC_STATIC, name, "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        methods.add(method);

        return method;
    }

    public MethodNode getLiftedInit() {
        String name = getLiftedName("<clinit>");

        for (MethodNode method : methods) {
            if (method.name.equals(name) && method.desc.equals("()V")) {
                return method;
            }
        }

        MethodNode method = new MethodNode(ACC_STATIC, name, "()V", null, null);
        method.instructions.add(new InsnNode(RETURN));
        methods.add(method);

        return method;
    }

    public boolean isAssignableFrom(JClassNode class2) {
        if (this.equals(class2))
            return true;
        return Hierarchy.INSTANCE.getClassParents(class2).contains(this);
    }

    public void resetHierarchy() {
        children.clear();
        parents.clear();
        linked = false;
    }

    public MethodNode getMethod(String name, String desc) {
        Hierarchy.INSTANCE.iterateClass(this);
        MethodNode method = methods.stream().filter(m -> (name == null || m.name.equals(name)) && (desc == null || m.desc.equals(desc)))
                .findFirst().orElse(null);
        if (method == null) {
            for (JClassNode parent : parents) {
                method = parent.getMethod(name, desc);
                if (method != null)
                    return method;
            }
        }
        return method;
    }

    public FieldNode getField(String name, String desc) {
        Hierarchy.INSTANCE.iterateClass(this);
        FieldNode field = fields.stream().filter(f -> f.name.equals(name) && f.desc.equals(desc)).findFirst().orElse(null);
        if (field == null) {
            for (JClassNode parent : parents) {
                field = parent.getField(name, desc);
                if (field != null) return field;
            }
        }
        return field;
    }

    public void update(JClassNode use) {
        this.interfaces = new ArrayList<>();
        this.innerClasses = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.visibleAnnotations = null;
        this.invisibleAnnotations = null;
        this.visibleTypeAnnotations = null;
        this.invisibleTypeAnnotations = null;
        this.attrs = null;
        this.signature = null;
        this.sourceDebug = null;
        this.sourceFile = null;
        this.module = null;
        this.nestHostClass = null;
        this.nestMembers = null;
        this.permittedSubclasses = null;
        this.setRealName(use.getRealName());
        use.accept(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof JClassNode other)) return false;
        return this.name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public String getLiftedInitializer() {
        if (liftedInitializer == null || liftedInitializer.isEmpty()) {
            return "<clinit>";
        }

        return liftedInitializer;
    }

    public String getLiftedName(String name) {
        if (name.equals("<clinit>")) return getLiftedInitializer();
        return name;
    }

}
