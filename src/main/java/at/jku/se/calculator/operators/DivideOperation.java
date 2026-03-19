package at.jku.se.calculator.operators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.jku.se.calculator.factory.ICalculationOperation;

/**
 * {@link ICalculationOperation} that divides two integer operands.
 *
 * <p>Expects an input string of the form {@code "a/b"} where both {@code a}
 * and {@code b} are valid integers. Throws {@link IllegalArgumentException}
 * if the input is malformed or division by zero is attempted.</p>
 */
public class DivideOperation implements ICalculationOperation {

    private static final Logger LOGGER = LogManager.getLogger(DivideOperation.class);

    @Override
    public String calculate(String txt) {
        LOGGER.info("Divide Operation executed: " + txt);

        // Wir splitten beim Schrägstrich "/"
        String[] terms = txt.split("/");

        if (terms.length == 2) {
            if (!isInteger(terms[0])) {
                LOGGER.error("Invalid Numerator: " + terms[0]);
                throw new IllegalArgumentException(String.format("%s is not a valid number", terms[0]));
            }
            if (!isInteger(terms[1])) {
                LOGGER.error("Invalid Denominator: " + terms[1]);
                throw new IllegalArgumentException(String.format("%s is not a valid number", terms[1]));
            }

            int dividend = Integer.parseInt(terms[0]);
            int divisor = Integer.parseInt(terms[1]);

            // Wichtig: Division durch Null abfangen
            if (divisor == 0) {
                LOGGER.error("Division by zero attempted");
                throw new IllegalArgumentException("Division by zero is not allowed!");
            }

            // Rückgabe als String (Ganzzahldivision)
            return (dividend / divisor) + "";
        } else {
            LOGGER.error("Malformed input: " + txt);
            throw new IllegalArgumentException("Input not correct! Expected format: a/b");
        }
    }

    private boolean isInteger(String value) {
        if (value == null) return false;
        try {
            Integer.parseInt(value.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}