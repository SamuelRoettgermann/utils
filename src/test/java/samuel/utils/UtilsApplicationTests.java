package samuel.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.DoubleStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UtilsApplicationTests {

    Equation eq;


    @Test
    void addition() {
        eq = new Equation("1 + 3 + 2 + 4");
        assertEquals(eq.evaluate(0), 10.0);

        eq = new Equation("2 + -4 + 1");
        assertEquals(eq.evaluate(0), -1.0);

        eq = new Equation("x + -1 + x + 2");
        double from = 0, to = 3, step = 0.5;
        assertArrayEquals(eq.evaluate(from, to, step),
                DoubleStream.iterate(from, x -> x <= to, x -> x + step).map(x -> x + (-1) + x + 2).toArray());
    }

}
