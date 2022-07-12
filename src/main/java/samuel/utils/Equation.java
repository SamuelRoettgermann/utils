package samuel.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;

public class Equation {
    private final String equationString;
    private final Term equation;

    public Equation(final String equationString) {
        this.equationString = equationString.strip();
        this.equation = parseEquation(this.equationString);
    }

    /**
     * Same as {@link #evaluate(double, double, double)} but converts input first. Possibly useful for Restful API.
     **/
    public double[] evaluate(final String sfrom, final String sto, final String sstep) {
        double from = Double.parseDouble(sfrom), to = Double.parseDouble(sto);
        final double step = Math.abs(Double.parseDouble(sstep));

        return evaluate(from, to, step);
    }

    /**
     * Evaluates the term in a range [from, to]. step signals how much difference is between x-values.
     **/
    public double[] evaluate(double from, double to, final double step) {
        if (from > to) {
            // swap from <-> to
            var temp = from;
            from = to;
            to = temp;
        }

        return evaluate(genArr(from, to, step));
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

        protected final Value v;

        protected Term(Value v) {
            this.v = v;
        }

        /**
         * Protected for easier access in subclasses.
         **/
        protected abstract int getPrecedence();

        /**
         * This method isn't supposed to change the value, but rather return a clone with the value adapted
         **/
        protected abstract Term newValue(double val);

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

        @FunctionalInterface
        protected interface Operand {
            double calculate(double v1, double v2);
        }

        @FunctionalInterface
        protected interface Value {
            double get(double x);
        }

        protected static record Constant(double x) implements Value {
            @Override
            public double get(/* ignored */ double x) {
                return this.x;
            }
        }

        protected static class Variable implements Value {
            @Override
            public double get(double x) {
                return x;
            }
        }
    }

    private static class BiTerm extends Term {

        private static final Map<Operand, Integer> precedences = new HashMap<>();
        private static final Operand
                ADDITION = Double::sum,
                SUBTRACTION = (v1, v2) -> v1 - v2,
                MULTIPLICATION = (v1, v2) -> v1 * v2,
                DIVISION = (v1, v2) -> v1 / v2,
                POWER = Math::pow;

        static {
            precedences.put(ADDITION, 1);
            precedences.put(SUBTRACTION, 1);
            precedences.put(MULTIPLICATION, 2);
            precedences.put(DIVISION, 2);
            precedences.put(POWER, 3);
        }

        private final Operand op;
        private final Term other;

        public BiTerm(String v, String op, Term other) {
            this(parseValue(v), parseOperand(op), other);
        }

        private BiTerm(Value v, Operand op, Term other) {
            super(v);
            this.op = op;
            this.other = other;
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

        @Override
        public Double apply(Double x) {
            if (this.getPrecedence() >= other.getPrecedence())
                // precedence left is higher -> calculate this immediately
                return other.newValue(op.calculate(this.v.get(x), other.v.get(x))).apply(x);
            else
                // precedence right is higher -> evaluate other term first
                return op.calculate(this.v.get(x), other.apply(x));
        }

        @Override
        protected int getPrecedence() {
            return precedences.get(this.op);
        }

        @Override
        protected Term newValue(double val) {
            return new BiTerm(new Constant(val), op, other);
        }
    }

    private static class TrivialTerm extends Term {

        public TrivialTerm(String v) {
            this(parseValue(v));
        }

        private TrivialTerm(Value v) {
            super(v);
        }

        @Override
        public Double apply(Double x) {
            return v.get(x);
        }

        @Override
        protected int getPrecedence() {
            return 0;
        }

        @Override
        protected Term newValue(double val) {
            return new TrivialTerm(new Constant(val));
        }
    }

    /**
     * Pattern to match either a {@link TrivialTerm} or {@link BiTerm}
     **/
    protected static final Pattern pattern =
            Pattern.compile("(-?\\d+|x)\\s*([+\\-*/^]?)(.*)");

    /**
     * Called internally which constructs the (nested) term
     * <p>
     * equation is guaranteed to be stripped
     **/
    private static Term parseEquation(final String equation) {
        if (equation.startsWith("(")) {
            // TODO: Find the closing paranthesis and use that term there
        }

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
        var eq1 = new Equation("3 * 2 + 1 / 2 * 2");
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
