package war.metaphor.mutator.virtualization;

import lombok.Getter;
import org.objectweb.asm.tree.LabelNode;

public class HandlerRange {
    @Getter
    private final LabelNode start, end;

    public HandlerRange() {
        this.start = new LabelNode();
        this.end = new LabelNode();
    }
}
