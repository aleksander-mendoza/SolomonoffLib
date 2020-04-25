package hoarec;

public class Mealy {

    public static class Transition {
        /**
         * If inputFromInclusive>inputToInclusive then it's an epsilon transition.
         */
        int inputFromInclusive, inputToInclusive;
        String output;
    }
    
    
    
    public Mealy(Transition[][] matrix) {
    }
}
