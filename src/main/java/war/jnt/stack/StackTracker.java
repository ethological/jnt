package war.jnt.stack;

import lombok.Setter;

// fuck all this shit, retarded tracker kill me fuckfuckfuckfuckfuck
@Setter
public class StackTracker {

    private int max = 0;

    private int sp = -1;

    public void simPush() {
        sp++; max = Math.max(max, sp);
    }

    public void simPop() {
        sp--;
    }

    public int dump() {
        return sp;
    }

    public int max() {
        return max;
    }
}
