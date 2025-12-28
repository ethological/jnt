package war.jnt.dash;

import lombok.Getter;

public class Ansi {

    public static final String esc = "\u001B[";
    private final StringBuilder builder = new StringBuilder();
    private boolean reset = true;

    @Getter
    public enum Color {

        BLACK("30"),
        RED("31"),
        GREEN("32"),
        YELLOW("33"),
        BLUE("34"),
        MAGENTA("35"),
        CYAN("36"),
        WHITE("37"),
        BRIGHT_BLACK("90"),
        BRIGHT_RED("91"),
        BRIGHT_GREEN("92"),
        BRIGHT_YELLOW("93"),
        BRIGHT_BLUE("94"),
        BRIGHT_MAGENTA("95"),
        BRIGHT_CYAN("96"),
        BRIGHT_WHITE("97");

        private final String code;

        Color(String code) {
            this.code = code;
        }
    }

    @Getter
    public enum Attribute {
        RESET("0"),
        BOLD("1");

        private final String code;

        Attribute(String code) {
            this.code = code;
        }
    }

    public Ansi c(Color color) {
        builder.append(esc).append(color.code).append("m");
        return this;
    }

    public Ansi a(Attribute attribute) {
        builder.append(esc).append(attribute.code).append("m");
        return this;
    }

    public Ansi s(Object str) {
        builder.append(str);
        return this;
    }

    public Ansi r(boolean reset) {
        this.reset = reset;
        return this;
    }

    @Override
    public String toString() {
        if (reset) {
            builder.append(esc).append(Attribute.RESET.code).append("m");
        }
        return builder.toString();
    }
}