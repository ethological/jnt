package war.metaphor.processor;

import lombok.ToString;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.metaphor.base.ObfuscatorContext;
import war.metaphor.tree.ClassField;
import war.metaphor.tree.ClassMethod;
import war.metaphor.tree.JClassNode;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adds visible‐annotation filtering support for member rules.
 */
public class FilterProcessor {

    private final ConfigurationSection rootSection;

    public FilterProcessor(ConfigurationSection rootSection) {
        this.rootSection = rootSection;
    }

    public void process(Collection<JClassNode> classes, ConfigurationSection section) {
        classes.forEach(JClassNode::removeExempt);

        List<FilterRule> rules = new ArrayList<>( loadRules(rootSection) );
        if (section != null) {
            rules.addAll(loadRules(section));
        }

        if (rules.isEmpty()) return;

        classes.forEach(cls -> applyFilters(cls, rules));
    }

    private List<FilterRule> loadRules(ConfigurationSection section) {
        List<FilterRule> rules = new ArrayList<>();
        rules.addAll(loadRules(section, FilterAction.EXEMPT));
        rules.addAll(loadRules(section, FilterAction.INCLUDE));
        return rules;
    }

    private List<FilterRule> loadRules(ConfigurationSection section, FilterAction action) {
        List<Map<String,Object>> entries = getOrEmpty(section.getMapList(action.name().toLowerCase()));
        return entries.stream().map(entry -> {
            String pkgGlob = (String) entry.get("package");
            List<Map<String,Object>> classMaps = getOrEmpty(entry.get("classes"));
            List<ClassRule> classRules = classMaps.stream()
                    .map(m -> new ClassRule(
                            (String) m.get("class"),
                            getOrEmpty(m.get("methods")).stream().map(Object::toString).collect(Collectors.toList()),
                            getOrEmpty(m.get("fields" )).stream().map(Object::toString).collect(Collectors.toList()),
                            getOrEmpty(m.get("annotations")).stream().map(Object::toString).collect(Collectors.toList())  // ← new
                    ))
                    .collect(Collectors.toList());
            return new FilterRule(action, pkgGlob, classRules);
        }).collect(Collectors.toList());
    }

    private void applyFilters(JClassNode cls, List<FilterRule> rules) {
        String name = cls.getRealName();
        String pkg  = extractPackage(name);

        List<FilterRule> exempt   = rules.stream().filter(r -> r.getAction() == FilterAction.EXEMPT).toList();
        List<FilterRule> include  = rules.stream().filter(r -> r.getAction() == FilterAction.INCLUDE).toList();

        // 1) Class‐level exclusion as before
        Predicate<FilterRule> matchesClass = r ->
                r.matchesPackage(pkg) && (r.matchesPackageOnly() || (r.isClassOnlyRule(name) && r.matchesClass(name)));
        Predicate<FilterRule> matchesClassAny = r ->
                r.matchesPackage(pkg) && (r.matchesPackageOnly() || r.matchesClass(name));
        boolean classExcluded = exempt.stream().anyMatch(matchesClass)
                || (!include.isEmpty() && include.stream().noneMatch(matchesClassAny));

//        System.out.println("PROCESSING: " + name + "  EXCLUDED: " + classExcluded);
//        System.out.println("RULES: " + rules);

        if (classExcluded) {
            cls.addExempt();
            return;
        }

//        System.out.println("\tCHECKING MEMBERS");

        // 2) Member‐level exclusion: name + annotation
        for (MethodNode m : cls.methods) {

            String m_name = m.name;

            if (m.signature != null && m.signature.startsWith("pass::jnt")) {
                String original = m.signature.substring(10);
                m_name = new String(Base64.getDecoder().decode(original));
            }

            String sig = m_name + m.desc;

            Predicate<FilterRule> nameMatch = r ->
                    r.matchesPackage(pkg) && r.matchesMember(name, sig, true);
            Predicate<FilterRule> annoMatch = r ->
                    r.matchesPackage(pkg) && r.matchesAnnotation(name, m.visibleAnnotations);

            boolean excludeByName = exempt.stream().anyMatch(nameMatch)
                    || (hasMemberRule(cls, include, true) && include.stream().noneMatch(nameMatch));
            boolean excludeByAnno = exempt.stream().anyMatch(annoMatch)
                    || (hasAnnotationRule(cls, include) && include.stream().noneMatch(annoMatch));

//            System.out.println("\t\tMETHOD: " + sig + "  EXCLUDE_BY_NAME: " + excludeByName + "  EXCLUDE_BY_ANNO: " + excludeByAnno);

            if (excludeByName || excludeByAnno) {
                cls.addExemptMember(m);
            }
        }

        for (FieldNode f : cls.fields) {

            String f_name = f.name;
            String sig = f_name + f.desc;

            Predicate<FilterRule> nameMatch = r ->
                    r.matchesPackage(pkg) && r.matchesMember(name, sig, false);
            Predicate<FilterRule> annoMatch = r ->
                    r.matchesPackage(pkg) && r.matchesAnnotation(name, f.visibleAnnotations);

            boolean excludeByName = exempt.stream().anyMatch(nameMatch)
                    || (hasMemberRule(cls, include, false) && include.stream().noneMatch(nameMatch));
            boolean excludeByAnno = exempt.stream().anyMatch(annoMatch)
                    || (hasAnnotationRule(cls, include) && include.stream().noneMatch(annoMatch));

            if (excludeByName || excludeByAnno) {
                cls.addExemptMember(f);
            }
        }
    }

    private boolean hasMemberRule(JClassNode classNode, List<FilterRule> rules, boolean isMethod) {
        return rules.stream()
                .flatMap(r -> r.classRules.stream())
                .filter(r -> r.matches(classNode.getRealName()))
                .anyMatch(cr -> !cr.getMemberRules(isMethod).isEmpty());
    }

    private boolean hasAnnotationRule(JClassNode classNode, List<FilterRule> rules) {
        return rules.stream()
                .flatMap(r -> r.classRules.stream())
                .filter(r -> r.matches(classNode.getRealName()))
                .anyMatch(cr -> !cr.getAnnotationRules().isEmpty());
    }

    private String extractPackage(String name) {
        int idx = name.lastIndexOf('/');
        return (idx == -1) ? "" : name.substring(0, idx);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getOrEmpty(Object obj) {
        return obj instanceof List ? (List<T>) obj : Collections.emptyList();
    }

    private static String globToRegex(String glob) {
        String[] parts = glob.split("\\*", -1);
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < parts.length; i++) {
            sb.append(Pattern.quote(parts[i]));
            if (i < parts.length - 1) sb.append(".*");
        }
        return sb.append("$").toString();
    }

    private enum FilterAction { EXEMPT, INCLUDE }

    @ToString
    private static final class FilterRule {
        private final FilterAction action;
        private final Pattern packagePattern;
        private final List<ClassRule> classRules;

        FilterRule(FilterAction action, String pkgGlob, List<ClassRule> classRules) {
            this.action         = action;
            this.packagePattern = Pattern.compile(globToRegex(pkgGlob));
            this.classRules     = classRules;
        }

        FilterAction getAction()                 { return action; }
        boolean matchesPackage(String pkg)       { return packagePattern.matcher(pkg).matches(); }
        boolean matchesPackageOnly()             { return classRules.isEmpty(); }
        boolean matchesClass(String clsName)     { return classRules.stream().anyMatch(cr -> cr.matches(clsName)); }
        boolean matchesMember(String clsName,
                              String memberSig,
                              boolean isMethod)
        {
            return classRules.stream()
                    .filter(cr -> cr.matches(clsName))
                    .flatMap(cr -> cr.getMemberRules(isMethod).stream())
                    .anyMatch(mr -> mr.matches(memberSig));
        }

        boolean matchesAnnotation(String clsName,
                                  List<AnnotationNode> annos)
        {
            // look up any annotation‐rules for this class, then see if any match
            return classRules.stream()
                    .filter(cr -> cr.matches(clsName))
                    .flatMap(cr -> cr.getAnnotationRules().stream())
                    .anyMatch(ar -> ar.matchesAny(annos));
        }

        boolean isClassOnlyRule(String clsName) {
            return !classRules.isEmpty()
                    && classRules.stream()
                    .filter(cr -> cr.matches(clsName))
                    .allMatch(cr ->
                            cr.getMemberRules(true ).isEmpty() &&
                                    cr.getMemberRules(false).isEmpty() &&
                                    cr.getAnnotationRules().isEmpty()
                    );
        }
    }

    @ToString
    private static final class ClassRule {
        private final Pattern classPattern;
        private final List<MemberRule> methods, fields, annotations;

        ClassRule(String classGlob,
                  List<String> methodGlobs,
                  List<String> fieldGlobs,
                  List<String> annotationGlobs)  // ← new
        {
            this.classPattern = Pattern.compile(globToRegex(classGlob));
            this.methods      = methodGlobs.stream()
                    .map(g -> new MemberRule(g, true ))
                    .collect(Collectors.toList());
            this.fields       = fieldGlobs.stream()
                    .map(g -> new MemberRule(g, false))
                    .collect(Collectors.toList());
            this.annotations  = annotationGlobs.stream()
                    .map(g -> new MemberRule(g, true )) // reuse MemberRule but interpret against annos
                    .collect(Collectors.toList());
        }

        boolean matches(String name) {
            return classPattern.matcher(name).matches();
        }
        List<MemberRule> getMemberRules(boolean isMethod) {
            return isMethod ? methods : fields;
        }
        List<MemberRule> getAnnotationRules() {
            return annotations;
        }
    }

    @ToString
    private static final class MemberRule {
        private final Pattern pattern;
        private final boolean isMethod;  // true for method names, false for fields, true for annotation globs

        MemberRule(String glob, boolean isMethod) {
            this.pattern  = Pattern.compile(globToRegex(glob));
            this.isMethod = isMethod;
        }

        boolean matches(String sig) {
            return pattern.matcher(sig).matches();
        }

        boolean matchesAny(List<AnnotationNode> annos) {
            if (annos == null) return false;

            List<String> classes = new ArrayList<>();
            for (AnnotationNode anno : annos) {
                String desc = anno.desc;
                Type type = Type.getType(desc);
                String className = type.getInternalName();
                JClassNode cls = ObfuscatorContext.INSTANCE.loadClass(className);
                if (cls != null) {
                    classes.add(cls.getRealName());
                }
            }

            return classes.stream()
                    .anyMatch(desc -> pattern.matcher(desc).matches());
        }
    }
}
