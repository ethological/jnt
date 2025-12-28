package war.mayday.intrin.match;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class SwitchStmt {
    @Getter
    private final String value;
    public final List<MatchArm> arms = new ArrayList<>();

    public SwitchStmt(String value) {
        this.value = value;
    }
}
