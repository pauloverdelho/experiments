package assessment;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PaginationHelperTest {
    List<Integer> collection = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24);
    PaginationHelper<Integer> helper = new PaginationHelper<>(collection, 10);

    @Test
    public void testExample() throws Exception {
        assertEquals("pageCount is returning incorrect value", 3, helper.pageCount());
        assertEquals("pageIndex is returning incorrect value", 2, helper.pageIndex(23));
        assertEquals("itemCount is returning incorrect value", 24, helper.itemCount());


        for (int i = -2; i < 30; i++) {
            System.out.println(i + ": " + helper.pageIndex(i));
        }
        for (int i = -2; i < 5; i++) {
            System.out.println(i + ": " + helper.pageItemCount(i));
        }
    }
}
