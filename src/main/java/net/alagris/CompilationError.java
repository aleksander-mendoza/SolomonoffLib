package net.alagris;

import java.util.List;
import java.util.Stack;

public class CompilationError extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public CompilationError(String msg) {
        super(msg);
    }

    public CompilationError(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CompilationError(Throwable cause) {
        super(cause);
    }

    public static final class DuplicateFunction extends CompilationError {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final Pos firstDeclaration;
        private final Pos secondDeclaration;
        private final String name;

        public DuplicateFunction(Pos firstDeclaration, Pos secondDeclaration,
                                 String name) {
            super(name + " is implemented at " + firstDeclaration + " and " + secondDeclaration);
            this.firstDeclaration = firstDeclaration;
            this.secondDeclaration = secondDeclaration;
            this.name = name;
        }

        public Pos getFirstDeclaration() {
            return firstDeclaration;
        }

        public Pos getSecondDeclaration() {
            return secondDeclaration;
        }

        public String getName() {
            return name;
        }

    }

    public static class ParseException extends CompilationError {
        private final Pos node;

        public ParseException(Pos node, Throwable cause) {
            super(cause);
            this.node = node;
        }

        public Pos getNode() {
            return node;
        }
    }

    public static class TypecheckException extends CompilationError {
        private final Pos funcPos, typePos;
        private final String name;

        public TypecheckException(Pos funcPos, Pos typePos, String name) {
            super("Function "+name+" "+funcPos+" does not conform to type "+typePos);
            this.funcPos = funcPos;
            this.typePos = typePos;
            this.name = name;
        }
    }

    public static class CompositionTypecheckException extends CompilationError {
        private final Pos automatonPos;
        private final Pos typePos;

        public CompositionTypecheckException(Pos automatonPos,Pos typePos) {
            super("Composition of transduction at "+automatonPos+" violates assertion at "+typePos);
            this.automatonPos = automatonPos;
            this.typePos = typePos;
        }
    }

    public static class NondeterminismException extends CompilationError {
        private final Pos nondeterministicStatePos1, nondeterministicStatePos2;

        public NondeterminismException(Pos nondeterministicStatePos1, Pos nondeterministicStatePos2, String funcName) {
            super("Function "+funcName+" nondeterministically branches to "
                    +nondeterministicStatePos1+" and "+nondeterministicStatePos2);
            this.nondeterministicStatePos1 = nondeterministicStatePos1;
            this.nondeterministicStatePos2 = nondeterministicStatePos2;
            this.funcName = funcName;
        }

        private final String funcName;

    }

    public static class KleeneNondeterminismException extends CompilationError {
        private final Pos kleenePos;

        public KleeneNondeterminismException(Pos kleenePos) {
            super("Kleene closure on expression that prints output for empty input" +
                    "leading to nondeterminism at "
                    +kleenePos);
            this.kleenePos = kleenePos;
        }

    }

    public static class IllegalCharacter extends CompilationError {
        private final Pos pos;

        public IllegalCharacter(Pos pos, String s) {
            super(s);
            this.pos = pos;
        }
    }

    public static class WeightConflictingToThirdState extends CompilationError {

        private final LexUnicodeSpecification.FunctionalityCounterexampleToThirdState<
                LexUnicodeSpecification.E, LexUnicodeSpecification.P, ?> counterexample;

        public WeightConflictingToThirdState(LexUnicodeSpecification.FunctionalityCounterexampleToThirdState<
                LexUnicodeSpecification.E,LexUnicodeSpecification.P,?> counterexample) {
            super("Found weight conflicting transitions from "+counterexample.fromStateA+" and "+counterexample.fromStateB+" going to state "+counterexample.toStateC+
                    " over transitions "+counterexample.overEdgeA+" and "+counterexample.overEdgeB);
            this.counterexample = counterexample;
        }

    }

    public static class WeightConflictingFinal extends CompilationError {

        private final LexUnicodeSpecification.FunctionalityCounterexampleFinal<
                LexUnicodeSpecification.E, LexUnicodeSpecification.P, ?> counterexample;

        public WeightConflictingFinal(LexUnicodeSpecification.FunctionalityCounterexampleFinal<
                LexUnicodeSpecification.E,LexUnicodeSpecification.P,?> counterexample) {
            super("Found weight conflicting final states "+counterexample.fromStateA+" and "+counterexample.fromStateB+
                    " with state output "+counterexample.finalEdgeA+" and "+counterexample.finalEdgeA);
            this.counterexample = counterexample;
        }

    }

    public static class UndefinedExternalFunc extends CompilationError {
        public final String functionName;
        public final Pos pos;

        public UndefinedExternalFunc(String functionName, Pos pos) {
            super("Function "+functionName+" at "+pos+" not found");
            this.functionName = functionName;
            this.pos = pos;
        }
    }

    public static class MissingFunction extends CompilationError {
        public MissingFunction(Pos pos, String id) {
            super("Variable '" + id + "' not found at "+pos);
        }
    }

    public static class AmbiguousDictionary extends CompilationError {
        public AmbiguousDictionary(IntSeq in, IntSeq out1, IntSeq out2) {
            super("Input key '"+in.toUnicodeString()+"' has ambiguous outputs '"+out1+"' and '"+out2+"'");
        }
    }

    public static class IllegalInformantSize extends CompilationError {
        public IllegalInformantSize(List<Pair<IntSeq, IntSeq>> text, int expectedSize) {
            super("Expected informant containing "+expectedSize+" element(s) but was "+text.size());
        }
    }

    public static class IllegalOperandsNumber extends CompilationError {
        public IllegalOperandsNumber(List<?> automata, int expectedSize) {
            super("Expected "+expectedSize+" operands but had "+automata.size());
        }
    }

    public static class InvertionNotPossible extends CompilationError {

        public InvertionNotPossible(String msg) {
            super(msg);
        }
    }

    public static class DoubleReflectionOnOutput extends CompilationError {
        public DoubleReflectionOnOutput(Pos state, LexUnicodeSpecification.E edge) {
            super("State at "+state+" produces reflection twice. Inversion will lead to too large automata");
        }
    }
    public static class RangeWithoutReflection extends CompilationError {
        public RangeWithoutReflection(Pos state, LexUnicodeSpecification.E edge) {
            super("State at "+state+" is a range but its output is not reflected. Inversion leads to nondeterminism");
        }
    }
    public static class EpsilonTransitionCycle extends CompilationError {
        public EpsilonTransitionCycle(Pos state, IntSeq output, IntSeq output1) {
            super("State at "+state+" produces epsilon cycle. Example of cycling outputs "+output+" "+output1);
        }
    }
    public static class AmbiguousAcceptingState extends CompilationError {

        public AmbiguousAcceptingState(Pos sourceState, Pos firstAcceptingState,Pos secondAcceptingState, LexUnicodeSpecification.P firstFinalEdge, LexUnicodeSpecification.P secondFinalEdge) {
            super("State at "+sourceState+" leads nondeterministically to another accepting states "+firstAcceptingState+" with output "+firstFinalEdge+" and "+secondAcceptingState+" with output "+secondFinalEdge+" after inversion.");
        }
    }
}
