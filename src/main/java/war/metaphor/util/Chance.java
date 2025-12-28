package war.metaphor.util;

import java.security.SecureRandom;

public class Chance {
    public static boolean chance(double percent) {
        if (percent <= 0) return false;
        if (percent >= 100) return true;
        return new SecureRandom().nextDouble() * 100 < percent;
    }
}
