package experiments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Experiments {

    public static void main(String[] args) {
        try {

            Integer i = null;
            i.intValue();
        }catch (NullPointerException e) {
            String message = e.getMessage();
            System.out.println(message);
        }
    }

}
