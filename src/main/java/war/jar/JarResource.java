package war.jar;

public class JarResource {

    private String name;
    private byte[] content;

    public JarResource(String name, byte[] content) {
        this.name = name;
        this.content = content != null ? content : new byte[0];
    }

    public void setContent(byte[] content) {
        this.content = content != null ? content : new byte[0];
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public byte[] content() {
        return content;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return String.format("%s(%s bytes)", name, content.length);
    }
}
