package war.jnt.dash;

import lombok.Getter;

@Getter
public enum Origin {

    CORE("jnt::core"),
    DASH("jnt::dash"),
    EXHAUST("jnt::exhaust"),
    INTAKE("jnt::intake"),
    FUSEBOX("jnt::fusebox"),
    TIMING("jnt::timing"),
    STACKMAN("jnt::stack_management"),
    ARGS("jnt::args"),
    METAPHOR("jnt::metaphor"),
    WORKER("jnt::worker");

    private final String origin;

    Origin(String origin) {
        this.origin = origin;
    }

}
