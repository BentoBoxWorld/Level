package world.bentobox.level.calculators;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

import org.junit.Test;

/**
 * Test the equation evaluation
 */
public class EquationEvaluatorTest {

    /**
     * Test method for {@link world.bentobox.level.calculators.EquationEvaluator#eval(java.lang.String)}.
     * @throws ParseException 
     */
    @Test
    public void testEval() throws ParseException {
        assertEquals(4D, EquationEvaluator.eval("2+2"), 0D);
        assertEquals(0D, EquationEvaluator.eval("2-2"), 0D);
        assertEquals(1D, EquationEvaluator.eval("2/2"), 0D);
        assertEquals(4D, EquationEvaluator.eval("2*2"), 0D);
        assertEquals(8D, EquationEvaluator.eval("2+2+2+2"), 0D);
        assertEquals(5D, EquationEvaluator.eval("2.5+2.5"), 0D);
        assertEquals(1.414, EquationEvaluator.eval("sqrt(2)"), 0.001D);
        assertEquals(3.414, EquationEvaluator.eval("2 + sqrt(2)"), 0.001D);
        assertEquals(0D, EquationEvaluator.eval("sin(0)"), 0.1D);
        assertEquals(1D, EquationEvaluator.eval("cos(0)"), 0.1D);
        assertEquals(0D, EquationEvaluator.eval("tan(0)"), 0.1D);
        assertEquals(0D, EquationEvaluator.eval("log(1)"), 0.1D);
        assertEquals(27D, EquationEvaluator.eval("3^3"), 0.D);
        assertEquals(84.70332D, EquationEvaluator.eval("3^3 + 2 + 2.65 * (3 / 4) - sin(45) * log(10) + 55.344"),
                0.0001D);
    
    }
}
