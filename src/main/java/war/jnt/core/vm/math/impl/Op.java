package war.jnt.core.vm.math.impl;

import war.metaphor.util.interfaces.IRandom;

public class Op implements IRandom {
        public final String op = getRandomOp();
        public final int num;

        public Op(int num) {
            this.num = num;
        }

        private String getRandomOp() {
            return switch (RANDOM.nextInt(3)) {
                case 0 -> "^";
                case 1 -> "+";
                case 2 -> "-";
                default -> throw new IllegalStateException("Unexpected value");
            };
        }

        public String getOpposite() {
            return switch (op) {
                case "+" -> "-";
                case "-" -> "+";
                default -> op;
            };
        }
    }