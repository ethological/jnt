package war.jnt.innercache;

import lombok.RequiredArgsConstructor;
import war.jnt.cache.Cache;
import war.jnt.cache.struct.CachedField;
import war.jnt.cache.struct.CachedMethod;
import war.jnt.core.code.UnitContext;
import war.jnt.fusebox.impl.VariableManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class InnerCache {
    public final Map<String, String> ccMap = new HashMap<>(); // class
    public final Map<CacheMemberInfo, String> cmMap = new HashMap<>(); // static method
    public final Map<CacheMemberInfo, String> vmMap = new HashMap<>(); // virtual method
    public final Map<CacheMemberInfo, String> cfMap = new HashMap<>(); // static field
    public final Map<CacheMemberInfo, String> vfMap = new HashMap<>(); // virtual field

    public final UnitContext ctx;
    public final VariableManager vm;

    public void cleanMaps() {
        ccMap.clear();
        cmMap.clear();
        vmMap.clear();
        cfMap.clear();
        vfMap.clear();
    }

    public void generateJNILookups() {
        for (var entry : ccMap.entrySet()) {
            String klass = entry.getKey();
            String varName = entry.getValue();
            int idx = Cache.Companion.request_klass(klass);
            ctx.fmtAppend("\tconst jclass %s = request_klass(env, %d);\n", varName, idx);
        }

        for (var entry : cmMap.entrySet()) {
            CacheMemberInfo cmi = entry.getKey();
            String varName = entry.getValue();
            String kv = FindClass(cmi.owner());
            int methodIdx = Cache.Companion.request_method(new CachedMethod(
                    cmi.owner(),
                    cmi.name(),
                    cmi.desc(),
                    true
            ));
            ctx.fmtAppend("\tconst jmethodID %s = request_method(env, %s, %d);\n", varName, kv, methodIdx);
        }

        for (var entry : vmMap.entrySet()) {
            CacheMemberInfo cmi = entry.getKey();
            String varName = entry.getValue();
            String kv = FindClass(cmi.owner());
            int methodIdx = Cache.Companion.request_method(new CachedMethod(
                    cmi.owner(),
                    cmi.name(),
                    cmi.desc(),
                    false
            ));
            ctx.fmtAppend("\tconst jmethodID %s = request_virtual(env, %s, %d);\n", varName, kv, methodIdx);
        }

        for (var entry : cfMap.entrySet()) {
            CacheMemberInfo cmi = entry.getKey();
            String varName = entry.getValue();
            String kv = FindClass(cmi.owner());
            int fieldIdx = Cache.Companion.request_field(new CachedField(
                    cmi.owner(), cmi.name(), cmi.desc(), true
            ));
            ctx.fmtAppend("\tconst jfieldID %s = request_field(env, %s, %d);\n", varName, kv, fieldIdx);
        }

        for (var entry : vfMap.entrySet()) {
            CacheMemberInfo cmi = entry.getKey();
            String varName = entry.getValue();
            String kv = FindClass(cmi.owner());
            int fieldIdx = Cache.Companion.request_field(new CachedField(
                    cmi.owner(), cmi.name(), cmi.desc(), false
            ));
            ctx.fmtAppend("\tconst jfieldID %s = request_ifield(env, %s, %d);\n", varName, kv, fieldIdx);
        }
    }

    public Optional<String> lookupCF(CacheMemberInfo cmi) {
        return Optional.ofNullable(cfMap.get(cmi));
    }

    public Optional<String> lookupVF(CacheMemberInfo cmi) {
        return Optional.ofNullable(vfMap.get(cmi));
    }

    public Optional<String> lookupCC(String klass) {
        return Optional.ofNullable(ccMap.get(klass));
    }

    public Optional<String> lookupCM(CacheMemberInfo cmi) {
        return Optional.ofNullable(cmMap.get(cmi));
    }

    public Optional<String> lookupVM(CacheMemberInfo cmi) {
        return Optional.ofNullable(vmMap.get(cmi));
    }

    public String GetStaticField(CacheMemberInfo cmi) {
        return cfMap.computeIfAbsent(cmi, k -> vm.newField());
    }

    public String GetVirtualField(CacheMemberInfo cmi) {
        return vfMap.computeIfAbsent(cmi, k -> vm.newField());
    }

    public String GetStaticMethod(CacheMemberInfo cmi) {
        return cmMap.computeIfAbsent(cmi, k -> vm.newLookup());
    }

    public String GetVirtualMethod(CacheMemberInfo cmi) {
        return vmMap.computeIfAbsent(cmi, k -> vm.newLookup());
    }

    public String MutableFindClass(String klass) {
        return ccMap.computeIfAbsent(klass, k -> vm.newClass());
    }

    public String FindClass(String klass) {
        return ccMap.computeIfAbsent(klass, k -> vm.newClass());
    }
}
