package war.jnt.core.header;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Header {
    private String name;
    private StringBuilder buffer = new StringBuilder();

    public Header(String name, StringBuilder buffer) {
        this.name = name;
        this.buffer = buffer;
    }

    public Header(String name, String buffer) {
        this.name = name;
        this.buffer.append(buffer);
    }

    public Header() {}

}
