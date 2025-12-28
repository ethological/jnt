package war.jnt.dash;

import lombok.Getter;

@Getter
public enum Level {

    DEBUG("DEBUG"),
    INFO("INFO"),
    MEMORY("MEMORY"),
    WARNING("WARNING"),
    ERROR("ERROR"),
    FATAL("FATAL"),
    NONE("NONE");

    private final String level;

    Level(String level) {
        this.level = level;
    }
}
