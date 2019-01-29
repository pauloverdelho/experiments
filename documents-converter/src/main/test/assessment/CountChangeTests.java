package assessment;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CountChangeTests {
    @Test
    public void shouldHandleTheExampleCase() {
        Integer expected = 3;
        Integer actual = Challenge.countChange(4, Arrays.asList(1, 2));
        assertEquals(expected, actual);
    }

    @Test
    public void shouldHandleAnotherSimpleCase() {
        Integer expected = 4;
        long time1 = System.currentTimeMillis();
        Integer actual = Challenge.countChange(10, Arrays.asList(5, 2, 3));
        long time2 = System.currentTimeMillis();
        System.out.println("Demorou " + (time2 - time1) + " ms");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldHandleAnotherSimpleCase2() {
        Integer expected = 184;
        long time1 = System.currentTimeMillis();
        Integer actual = Challenge.countChange(100, Arrays.asList(5, 2, 3));
        long time2 = System.currentTimeMillis();
        System.out.println("Demorou " + (time2 - time1) + " ms");
        assertEquals(expected, actual);
    }

    @Test
    public void shouldHandleAnotherSimpleCase3() {
        Integer expected = 0;
        long time1 = System.currentTimeMillis();
        Integer actual = Challenge.countChange(10, Arrays.asList(50, 100, 200));
        long time2 = System.currentTimeMillis();
        System.out.println("Demorou " + (time2 - time1) + " ms");
        assertEquals(expected, actual);
    }
}
