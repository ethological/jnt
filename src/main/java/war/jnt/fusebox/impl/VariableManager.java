package war.jnt.fusebox.impl;

public class VariableManager {
    public static int lookups = 0;
    public static int classes = 0;
    public static int stringBuffers = 0;
    public static int jstrings = 0;
    public static int fields = 0;

    public String newField() {
        fields++;
        return "vfield" + fields;
    }

    public String newLookup() {
        lookups++;
        return "lookup" + lookups;
    }

    public String newClass() {
        classes++;
        return "klass" + classes;
    }

    public String newBuffer() {
        stringBuffers++;
        return "buf" + stringBuffers;
    }

    public String newJString() {
        jstrings++;
        return "jstr"+ jstrings;
    }


    public String refClass(int id) {
        return "klass" + id;
    }

    public String refLookup(int id) {
        return "lookup" + id;
    }

    public String refBuffer(int id) {
        return "buf" + id;
    }

    public String refJString(int id) {
        return "jstr" + id;
    }

    public int classes() {
        return classes;
    }

    public int lookups() {
        return lookups;
    }
}
