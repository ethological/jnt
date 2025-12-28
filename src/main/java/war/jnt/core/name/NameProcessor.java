package war.jnt.core.name;

import lombok.experimental.UtilityClass;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class NameProcessor {

    Map<String, String> className = new HashMap<>();

    public String forClass(String name) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get SHA-256 instance", e);
        }
        digest.update(name.getBytes());
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash)
            sb.append(String.format("%02x", b));
        return "h" + sb;
//        return name.replace("/", "_").concat("_").concat(className.computeIfAbsent(name, _ -> String.valueOf(System.nanoTime())));
    }

    public String forMethod(ClassNode clazz, MethodNode method) {
        boolean isOverloaded = clazz.methods.stream().anyMatch(m -> m != method && m.name.equals(method.name));
        StringBuilder result = new StringBuilder(100);
        result.append(encode(clazz.name, EncoderType.JNI));
        result.append('_');
        result.append(encode(method.name, EncoderType.JNI));
        if (isOverloaded) {
            StringBuilder sig = new StringBuilder(100);
            Type[] args = Type.getArgumentTypes(method.desc);
            Type returnType = Type.getReturnType(method.desc);
            for (Type arg : args) sig.append(arg.getDescriptor());
            result.append("__").append(encode(sig, EncoderType.JNI)).append("__").append(encode(returnType.getDescriptor(), EncoderType.JNI));
        }
        return result.toString();
    }

    public enum EncoderType {
        CLASS,
        FIELDSTUB,
        FIELD,
        JNI,
        SIGNATURE
    }

    public String encode(CharSequence name, EncoderType mtype) {
        StringBuilder result = new StringBuilder(100);
        int length = name.length();

        for (int i = 0; i < length; i++) {
            char ch = name.charAt(i);
            if (isalnum(ch)) {
                result.append(ch);
                continue;
            }
            switch (mtype) {
                case CLASS:
                    switch (ch) {
                        case '.':
                        case '_':
                            result.append("_");
                            break;
                        case '$':
                            result.append("__");
                            break;
                        default:
                            result.append(encodeChar(ch));
                    }
                    break;
                case JNI:
                    switch (ch) {
                        case '/':
                        case '.':
                            result.append("_");
                            break;
                        case '_':
                            result.append("_1");
                            break;
                        case ';':
                            result.append("_2");
                            break;
                        case '[':
                            result.append("_3");
                            break;
                        default:
                            result.append(encodeChar(ch));
                    }
                    break;
                case SIGNATURE:
                    result.append(isprint(ch) ? ch : encodeChar(ch));
                    break;
                case FIELDSTUB:
                    result.append(ch == '_' ? ch : encodeChar(ch));
                    break;
                default:
                    result.append(encodeChar(ch));
            }
        }
        return result.toString();
    }

    String encodeChar(char ch) {
        String s = Integer.toHexString(ch);
        int nzeros = 5 - s.length();
        char[] result = new char[6];
        result[0] = '_';
        for (int i = 1; i <= nzeros; i++) {
            result[i] = '0';
        }
        for (int i = nzeros + 1, j = 0; i < 6; i++, j++) {
            result[i] = s.charAt(j);
        }
        return new String(result);
    }

    /* Warning: Intentional ASCII operation. */
    private boolean isalnum(char ch) {
        return ch <= 0x7f && /* quick test */
                ((ch >= 'A' && ch <= 'Z')  ||
                        (ch >= 'a' && ch <= 'z')  ||
                        (ch >= '0' && ch <= '9'));
    }

    /* Warning: Intentional ASCII operation. */
    private boolean isprint(char ch) {
        return ch >= 32 && ch <= 126;
    }
}
