package war.jnt.core.source;

import org.objectweb.asm.tree.MethodNode;
import war.configuration.ConfigurationSection;
import war.jnt.core.code.CodeProcessor;
import war.jnt.core.header.HeaderProcessor;
import war.jnt.core.name.NameProcessor;
import war.jnt.fusebox.impl.Internal;
import war.metaphor.tree.JClassNode;

public class SourceProcessor {
    public static String forClass(JClassNode node, ConfigurationSection config) {
        var builder = new StringBuilder();

        String headerName = NameProcessor.forClass(node.name) + ".h";

        builder.append("#include \"").append(headerName).append("\"\n\n");

        for (MethodNode method : node.methods) {
            if (node.isExempt(method)) continue;
            if (Internal.disallowedTranspile(node, method)) continue;

            String signature = HeaderProcessor.signature(node, method);
            //builder.append("finline ").append(signature).append(" {\n");

            builder.append(signature).append(" {\n");

            String code = CodeProcessor.forMethod(node, method, config);
            builder.append(code);

            builder.append("}\n");
        }

        return builder.toString();
    }
}
