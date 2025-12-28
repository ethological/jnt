package war.jnt.core.source;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Source {

    private String name;
    private StringBuilder buffer = new StringBuilder();

    public Source(String name, StringBuilder buffer) {
        this.name = name;
        this.buffer = buffer;
    }

    public Source(String name, String buffer) {
        this.name = name;
        this.buffer.insert(0, buffer);
    }

    public Source() {}

    public void clear() {
        this.buffer = new StringBuilder();
    }
}
