package war.metaphor.mutator.integrity;

import java.util.Arrays;

public record TableIntegrity(String[] table) {
    @Override
    public String[] table() {
        return table;
    }

    public int getHash() {
        return Arrays.hashCode(table);
    }
}
