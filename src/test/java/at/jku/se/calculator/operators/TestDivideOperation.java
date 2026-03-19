package at.jku.se.calculator.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

/**
 * This test class performs tests for the {@link DivideOperation} class.
 * * @author Michael Vierhauser (adapted)
 */
public class TestDivideOperation {

    private DivideOperation divide;

    @Before
    public void setup() {
        divide = new DivideOperation();
    }

    /**
     * Tests a standard division.
     */
    @Test
    public void testCalculate() {
        String result = divide.calculate("6/3");
        assertEquals(2, Integer.parseInt(result));
    }

    /**
     * Tests division with leading zeros.
     */
    @Test
    public void testCalculateLeadingZeros() {
        String result = divide.calculate("009/3");
        assertEquals(3, Integer.parseInt(result));
    }

    /**
     * CRITICAL: Tests that division by zero throws an {@link IllegalArgumentException}.
     */
    @Test
    public void testDivisionByZero() {
        assertThrows(IllegalArgumentException.class, () -> divide.calculate("10/0"));
    }

    /**
     * Tests that an invalid first operand (string) throws an {@link IllegalArgumentException}.
     */
    @Test
    public void testCalculateExceptionFirstOperand() {
        assertThrows(IllegalArgumentException.class, () -> divide.calculate("xyz/3"));
    }

    /**
     * Tests that an invalid second operand (string) throws an {@link IllegalArgumentException}.
     */
    @Test
    public void testCalculateExceptionSecondOperand() {
        assertThrows(IllegalArgumentException.class, () -> divide.calculate("3/abc"));
    }

    /**
     * Tests that a malformed input with the wrong operator throws an {@link IllegalArgumentException}.
     */
    @Test
    public void testCalculateMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> divide.calculate("10+2"));
    }

    /**
     * Tests that an empty input or missing operands throw an {@link IllegalArgumentException}.
     */
    @Test
    public void testCalculateIncompleteInput() {
        assertThrows(IllegalArgumentException.class, () -> divide.calculate("10/"));
    }
}