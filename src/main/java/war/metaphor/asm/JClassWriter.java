package war.metaphor.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.SymbolTable;
import war.jnt.dash.Level;
import war.jnt.dash.Logger;
import war.jnt.dash.Origin;
import war.metaphor.tree.Hierarchy;

public class JClassWriter extends ClassWriter {

    private final Logger logger = Logger.INSTANCE;

    public JClassWriter(int flags, SymbolTable symbolTable) {
        super(flags, symbolTable);
    }

    @Override
    public String getCommonSuperClass(String type1, String type2) {
        try {
            return Hierarchy.INSTANCE.getCommonSuperClass(type1, type2);
        } catch (Exception e) {
            logger.logln(Level.WARNING, Origin.METAPHOR, e.getMessage());
            return "java/lang/Object";
        }
    }
}