package war.metaphor.util.asm;

import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Col-E
 * completely stolen, just don't want to include SimAnalyzer
 */
public class TypeUtil {

    public static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
    public static final Type EXCEPTION_TYPE = Type.getObjectType("java/lang/Exception");

    private static final List<Integer> SORT_ORDER = new ArrayList<>();

    public static int getPromotionIndex(int sort) {
        return SORT_ORDER.indexOf(sort);
    }

    static {
        SORT_ORDER.add(0);
        SORT_ORDER.add(1);
        SORT_ORDER.add(3);
        SORT_ORDER.add(4);
        SORT_ORDER.add(2);
        SORT_ORDER.add(5);
        SORT_ORDER.add(6);
        SORT_ORDER.add(8);
        SORT_ORDER.add(7);
        SORT_ORDER.add(9);
        SORT_ORDER.add(10);
    }
}
