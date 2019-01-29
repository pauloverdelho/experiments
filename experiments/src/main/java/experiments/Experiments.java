package experiments;

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
