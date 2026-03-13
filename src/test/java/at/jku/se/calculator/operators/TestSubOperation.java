package at.jku.se.calculator.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

/**
 * This test class performs tests for the {@link SubOperation} class.
 */
public class TestSubOperation {

	private SubOperation sub;

	@Before
	public void setup() {
		sub = new SubOperation();
	}

	/**
	 * Tests that subtracting a smaller number from a larger one yields a positive result.
	 */
	@Test
	public void testCalculatePositiveResult() {
		String result = sub.calculate("8-3");
		assertEquals(5, Integer.parseInt(result));
	}

	/**
	 * Tests that subtracting a larger number from a smaller one yields a negative result.
	 */
	@Test
	public void testCalculateNegativeResult() {
		String result = sub.calculate("3-8");
		assertEquals(-5, Integer.parseInt(result));
	}

	/**
	 * Tests that an invalid first operand throws an {@link IllegalArgumentException}.
	 */
	@Test
	public void testCalculateInvalidFirstOperand() {
		assertThrows(IllegalArgumentException.class, () -> sub.calculate("xyz-3"));
	}

	/**
	 * Tests that an invalid second operand throws an {@link IllegalArgumentException}.
	 */
	@Test
	public void testCalculateInvalidSecondOperand() {
		assertThrows(IllegalArgumentException.class, () -> sub.calculate("3-abc"));
	}
}
