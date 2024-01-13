package world.bentobox.level.calculators;

import java.text.ParseException;

/**
 * Utility class to evaluate equations
 */
public class EquationEvaluator {

    private static class Parser {
        private final String input;
        private int pos = -1;
        private int currentChar;

        @SuppressWarnings("unused")
        private Parser() {
            throw new IllegalStateException("Utility class");
        }

        public Parser(String input) {
            this.input = input;
            moveToNextChar();
        }

        private void moveToNextChar() {
            currentChar = (++pos < input.length()) ? input.charAt(pos) : -1;
        }

        private boolean tryToEat(int charToEat) {
            while (currentChar == ' ') {
                moveToNextChar();
            }
            if (currentChar == charToEat) {
                moveToNextChar();
                return true;
            }
            return false;
        }

        public double evaluate() throws ParseException {
            double result = parseExpression();
            if (pos < input.length()) {
                throw new ParseException("Unexpected character: " + (char) currentChar, pos);
            }
            return result;
        }

        private double parseExpression() throws ParseException {
            double result = parseTerm();
            while (true) {
                if (tryToEat('+')) {
                    result += parseTerm();
                } else if (tryToEat('-')) {
                    result -= parseTerm();
                } else {
                    return result;
                }
            }
        }

        private double parseFactor() throws ParseException {
            if (tryToEat('+')) {
                return parseFactor(); // unary plus
            }
            if (tryToEat('-')) {
                return -parseFactor(); // unary minus
            }
            double x;
            int startPos = this.pos;
            if (tryToEat('(')) { // parentheses
                x = parseExpression();
                tryToEat(')');
            } else if ((currentChar >= '0' && currentChar <= '9') || currentChar == '.') { // numbers
                while ((currentChar >= '0' && currentChar <= '9') || currentChar == '.') {
                    moveToNextChar();
                }
                x = Double.parseDouble(input.substring(startPos, this.pos));
            } else if (currentChar >= 'a' && currentChar <= 'z') { // functions
                while (currentChar >= 'a' && currentChar <= 'z') {
                    moveToNextChar();
                }
                String func = input.substring(startPos, this.pos);
                x = parseFactor();
                x = switch (func) {
                case "sqrt" -> Math.sqrt(x);
                case "sin" -> Math.sin(Math.toRadians(x));
                case "cos" -> Math.cos(Math.toRadians(x));
                case "tan" -> Math.tan(Math.toRadians(x));
                case "log" -> Math.log(x);
                default -> throw new ParseException("Unknown function: " + func, startPos);
                };
            } else {
                throw new ParseException("Unexpected: " + (char) currentChar, startPos);
            }

            if (tryToEat('^')) {
                x = Math.pow(x, parseFactor()); // exponentiation
            }

            return x;
        }

        private double parseTerm() throws ParseException {
            double x = parseFactor();
            for (;;) {
                if (tryToEat('*'))
                    x *= parseFactor(); // multiplication
                else if (tryToEat('/'))
                    x /= parseFactor(); // division
                else
                    return x;
            }
        }

    }

    public static double eval(final String equation) throws ParseException {
        return new Parser(equation).evaluate();
    }

}
