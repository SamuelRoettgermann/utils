package samuel.utils;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class Graph {
    private final String equation;
    private final double from, to, step;

    public Graph(final String equation,
                 final String from,
                 final String to,
                 final String step) {
        this.equation = equation;
        this.from = Double.parseDouble(from);
        this.to = Double.parseDouble(to);
        this.step = Double.parseDouble(step);
    }

    public Map<Double, Double> evaluate() {
        return null;
    }

    private static class Term implements Function<Double, Double> {

        private static final Pattern pattern =
                Pattern.compile("(-?\\d+)\\s*([+\\-*/]|root).*");
        private final Value v;
        private final Operand op;
        private final Term before;

        Term(String equation) {
            pattern.matcher(equation).group(0)
        }

        @Override
        public Double apply(Double x) {
            return op.calculate(v.get(x), before.apply(x));
        }

        private interface Operand {
            double calculate(double v1, double v2);
        }

        private static record NoOp() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1;
            }
        }

        private static record Addition() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 + v2;
            }
        }

        private static record Subtraction() implements Operand {

            @Override
            public double calculate(double v1, double v2) {
                return v1 - v2;
            }
        }

        private static record Multiplication() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 * v2;
            }
        }

        private static record Division() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 / v2;
            }
        }

        private static record Root() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return Math.pow(v1, 1 / v2);
            }
        }

        private interface Value {
            double get(double x);
        }

        private static record Constant(double x) implements Value {
            @Override
            public double get(/* ignored */ double x) {
                return this.x;
            }
        }

        private static record Variable() implements Value {
            @Override
            public double get(double x) {
                return x;
            }
        }
    }

    private static Function<Double, Double> parseEquation(final String equation) {

    }
}
