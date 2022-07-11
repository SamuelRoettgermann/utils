package samuel.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

public class Graph {
    private final String equationString;
    private final Term equation;

    public Graph(final String equationString) {
        this.equationString = equationString.strip();
        this.equation = parseEquation(this.equationString);
    }

    /**
     * Same as {@link #evaluate(double, double, double)} but converts input first. Possibly useful for Restful API.
     **/
    public Map<Double, Double> evaluate(final String sfrom, final String sto, final String sstep) {
        double from = Double.parseDouble(sfrom), to = Double.parseDouble(sto);
        final double step = Math.abs(Double.parseDouble(sstep));

        return evaluate(from, to, step);
    }

    /**
     * Evaluates the term in a range [from, to]. step signals how much difference is between x-values.
     **/
    public Map<Double, Double> evaluate(double from, double to, final double step) {
        if (from > to) {
            // swap from <-> to
            var temp = from;
            from = to;
            to = temp;
        }

        Map<Double, Double> map = new HashMap<>();
        for (double cur = from; cur <= to; cur += step)
            map.put(cur, evaluate(cur));

        return map;
    }

    /**
     * Evaluates the equation for fixed x values and returns the y-values.
     * Useful when you already have the x-values and only want the y-values
     **/
    public double[] evaluate(double[] xs) {
        double[] ys = new double[xs.length];
        for (int i = 0; i < xs.length; i++)
            ys[i] = evaluate(xs[i]);

        return ys;
    }

    /**
     * Evaluate the equation for a certain x value
     **/
    public double evaluate(double x) {
        return equation.apply(x);
    }

    /**
     * Term is a function that can be evaluated.
     * <p>
     * To achieve this there are two subclasses. The first of which is {@link BiTerm} which takes a variable/constant,
     * an operand and another value which is a term again. By doing this we're able to create one big nested term.
     * The second is {@link TrivialTerm} which simply holds either a variable or constant, nothing else.
     * When evaluated it returns the constant or the current variable.
     */
    private static abstract class Term implements Function<Double, Double> {

        private static final Operand
                ADDITION = new Addition(),
                SUBTRACTION = new Subtraction(),
                MULTIPLICATION = new Multiplication(),
                DIVISION = new Division(),
                POWER = new Power();

        /**
         * Comfort conversion
         **/
        protected static Value parseValue(String s) {
            try {
                return new Constant(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return new Variable();
            }
        }

        /**
         * Comfort conversion
         **/
        protected static Operand parseOperand(String s) {
            if (s.length() > 1)
                throw new IllegalArgumentException("not more than one operand allowed");

            return switch (s.charAt(0)) {
                case '+' -> ADDITION;
                case '-' -> SUBTRACTION;
                case '*' -> MULTIPLICATION;
                case '/' -> DIVISION;
                case '^' -> POWER;
                default -> throw new IllegalArgumentException("Illegal operator %c".formatted(s.charAt(0)));
            };
        }

        protected interface Operand {
            double calculate(double v1, double v2);
        }

        protected static record Addition() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 + v2;
            }
        }

        protected static record Subtraction() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 - v2;
            }
        }

        protected static record Multiplication() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 * v2;
            }
        }

        protected static record Division() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return v1 / v2;
            }
        }

        protected static record Power() implements Operand {
            @Override
            public double calculate(double v1, double v2) {
                return Math.pow(v1, v2);
            }
        }

        protected interface Value {
            double get(double x);
        }

        protected static record Constant(double x) implements Value {
            @Override
            public double get(/* ignored */ double x) {
                return this.x;
            }
        }

        protected static record Variable() implements Value {
            @Override
            public double get(double x) {
                return x;
            }
        }
    }

    private static class BiTerm extends Term {
        private final Value v;
        private final Operand op;
        private final Term other;

        public BiTerm(String v, String op, Term other) {
            this.v = parseValue(v);
            this.op = parseOperand(op);
            this.other = other;
        }

        @Override
        public Double apply(Double x) {
            return op.calculate(v.get(x), other.apply(x));
        }
    }

    private static class TrivialTerm extends Term {
        private final Value v;

        public TrivialTerm(String v) {
            this.v = parseValue(v);
        }

        @Override
        public Double apply(Double x) {
            return v.get(x);
        }
    }

    /**
     * Pattern to match either a {@link TrivialTerm} or {@link BiTerm}
     **/
    protected static final Pattern pattern =
            Pattern.compile("(-?\\d+|x)\\s*([+\\-*/^]?)(.*)");

    /**
     * Called internally which constructs the (nested) term
     **/
    private static Term parseEquation(final String equation) {
        var matcher = pattern.matcher(equation);
        matcher.find();  // this has to be called before .group is possible
        String v = matcher.group(1);
        String op = matcher.group(2);

        if (op.isEmpty()) // no operator, just a trivial term
            return new TrivialTerm(v);

        return new BiTerm(v, op, parseEquation(matcher.group(3).strip()));
    }

    @Override
    public String toString() {
        return "f(x) = " + equationString;
    }

    public static void main(String... args) {
        var eq1 = new Graph("x - 1 + 2");
        var xs = genArr(0, 3, 0.5);
        System.out.println(eq1);
        System.out.println(Arrays.toString(xs));
        System.out.println(Arrays.toString(eq1.evaluate(xs)));
    }





    /**
     * Comfort method to quickly create an array of values within a certain range.
     * <p>
     * Meant for debugging purposes.
     **/
    private static double[] genArr(final double from, final double to, final double step) {
        return DoubleStream.iterate(from, x -> x <= to, x -> x + step).toArray();
    }
}
