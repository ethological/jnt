package war.jnt.obfuscation;

import war.metaphor.engine.types.PolymorphicEngine;

public class MutatedString {

    private static PolymorphicEngine engine;
    private static int engineCounter;

    private final String str;

    public MutatedString(String str) {
        this.str = str;
    }

    public String compute(String varName) {
        return compute(varName, false);
    }

    public String compute(String varName, boolean wide) {

        if (engineCounter++ % 100 == 0)
            engine = new PolymorphicEngine(30);

        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++)
            chars[i] = (char) engine.run(chars[i]);

        StringBuilder sb = new StringBuilder();

        sb.append("\tjchar s_").append(varName).append("[] = {");
        for (char c : chars) {
            sb.append(" 0x")
                    .append(String.format("%04X", (int) c))
                    .append(", ");
        }
        if (chars.length != 0) sb.delete(sb.length() - 2, sb.length());
        sb.append(" };\n");

        if (wide) {
            sb.append("\tjchar* ").append(varName).append(" = jnt_decode_string(");
        } else {
            sb.append("\tchar* ").append(varName).append(" = jstr_to_utf8(jnt_decode_string(");
        }

        sb.append("s_").append(varName).append(", ")
                .append(chars.length)
                .append(", ")
                .append(StringLookup.add(engine))
                .append(")");

        if (wide) {
            sb.append(";\n");
        } else {
            sb.append(", ").append(chars.length).append(");\n");
        }

        return sb.toString();
    }

}