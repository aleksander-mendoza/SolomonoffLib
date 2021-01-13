package net.alagris;

import java.util.List;
import java.util.Stack;

public class CompilationError extends Exception {

    
	/**
     *
     */
    public static final long serialVersionUID = 1L;

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
        public static final long serialVersionUID = 1L;
        public final Pos firstDeclaration;
        public final Pos secondDeclaration;
        public final String name;

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
        public final Pos node;

        public ParseException(Pos node, Throwable cause) {
            super(cause);
            this.node = node;
        }

        public Pos getNode() {
            return node;
        }
    }

    public static class TypecheckException extends CompilationError {
        public final Pos funcPos, typePos;
        public final String name;

        public TypecheckException(Pos funcPos, Pos typePos, String name) {
            super("Function "+name+" "+funcPos+" does not conform to type "+typePos);
            assert funcPos!=null;
            assert typePos!=null;
            this.funcPos = funcPos;
            this.typePos = typePos;
            this.name = name;
        }
    }

    public static class CompositionTypecheckException extends CompilationError {
        public final Pos automatonPos;
        public final Pos typePos;

        public CompositionTypecheckException(Pos automatonPos,Pos typePos) {
            super("Composition of transduction at "+automatonPos+" violates assertion at "+typePos);
            assert automatonPos!=null;
            assert typePos!=null;
            this.automatonPos = automatonPos;
            this.typePos = typePos;
        }
    }

    public static class NondeterminismException extends CompilationError {
        public final Pos nondeterministicStatePos1, nondeterministicStatePos2;

        public NondeterminismException(Pos nondeterministicStatePos1, Pos nondeterministicStatePos2, String funcName) {
            super("Function "+funcName+" nondeterministically branches to "
                    +nondeterministicStatePos1+" and "+nondeterministicStatePos2);
            this.nondeterministicStatePos1 = nondeterministicStatePos1;
            this.nondeterministicStatePos2 = nondeterministicStatePos2;
            assert nondeterministicStatePos1!=null;
            assert nondeterministicStatePos2!=null;
            this.funcName = funcName;
        }

        public final String funcName;

    }

    public static class KleeneNondeterminismException extends CompilationError {
        public final Pos kleenePos;

        public KleeneNondeterminismException(Pos kleenePos) {
            super("Kleene closure on expression that prints output for empty input" +
                    "leading to nondeterminism at "
                    +kleenePos);
            assert kleenePos!=null;
            this.kleenePos = kleenePos;
        }

    }

    public static class IllegalCharacter extends CompilationError {
        public final Pos pos;

        public IllegalCharacter(Pos pos, String s) {
            super(s);
            assert pos!=null;
            this.pos = pos;
        }
    }

    public static class WeightConflictingToThirdState extends CompilationError {

        public final LexUnicodeSpecification.FunctionalityCounterexampleToThirdState<
                LexUnicodeSpecification.E, LexUnicodeSpecification.P, ?> counterexample;
        public final Pos automatonPos;

        public WeightConflictingToThirdState(Pos automatonPos,LexUnicodeSpecification.FunctionalityCounterexampleToThirdState<
                LexUnicodeSpecification.E,LexUnicodeSpecification.P,?> counterexample) {
            super("Automaton "+automatonPos+" contains weight conflicting transitions from "+counterexample.fromStateA+" and "+counterexample.fromStateB+" going to state "+counterexample.toStateC+
                    " over transitions "+counterexample.overEdgeA+" and "+counterexample.overEdgeB+". Example of input "+counterexample.strTrace());
            assert automatonPos!=null;
			this.automatonPos = automatonPos;
            this.counterexample = counterexample;
        }

    }

    public static class WeightConflictingFinal extends CompilationError {

        public final LexUnicodeSpecification.FunctionalityCounterexampleFinal<
                LexUnicodeSpecification.E, LexUnicodeSpecification.P, ?> counterexample;
        public final Pos automatonPos;

        public WeightConflictingFinal(Pos automatonPos,LexUnicodeSpecification.FunctionalityCounterexampleFinal<
                LexUnicodeSpecification.E,LexUnicodeSpecification.P,?> counterexample) {
            super("Automaton "+automatonPos+" contains weight conflicting final states "+counterexample.fromStateA+" and "+counterexample.fromStateB+
                    " with state output "+counterexample.finalEdgeA+" and "+counterexample.finalEdgeA+". Example of input "+counterexample.strTrace());
            assert automatonPos!=null;
			this.automatonPos = automatonPos;
            this.counterexample = counterexample;
        }

    }

    public static class UndefinedExternalFunc extends CompilationError {
        public final String functionName;
        public final Pos pos;

        public UndefinedExternalFunc(String functionName, Pos pos) {
            super("Function "+functionName+" at "+pos+" not found");
            this.functionName = functionName;
            assert pos!=null;
            this.pos = pos;
        }
    }

    public static class MissingFunction extends CompilationError {
        public final Pos pos;
        public final String id;

        public MissingFunction(Pos pos, String id) {
            super("Variable '" + id + "' not found at "+pos);
            assert pos!=null;
            this.pos = pos;
            this.id = id;
        }
    }

    public static class AmbiguousDictionary extends CompilationError {
        public final IntSeq in;
        public final IntSeq out1;
        public final IntSeq out2;

        public AmbiguousDictionary(IntSeq in, IntSeq out1, IntSeq out2) {
            super("Input key '"+in.toUnicodeString()+"' has ambiguous outputs '"+out1+"' and '"+out2+"'");
            this.in = in;
            this.out1 = out1;
            this.out2 = out2;
        }
    }

    public static class IllegalInformantSize extends CompilationError {
        public final List<Pair<IntSeq, IntSeq>> text;
        public final int expectedSize;

        public IllegalInformantSize(List<Pair<IntSeq, IntSeq>> text, int expectedSize) {
            super("Expected informant containing "+expectedSize+" element(s) but was "+text.size());
            this.text = text;
            this.expectedSize = expectedSize;
        }
    }

    public static class IllegalOperandsNumber extends CompilationError {
        public final List<?> automata;
        public final int expectedSize;

        public IllegalOperandsNumber(List<?> automata, int expectedSize) {
            super("Expected "+expectedSize+" operands but had "+automata.size());
            this.automata = automata;
            this.expectedSize = expectedSize;
        }
    }

    public static class InvertionNotPossible extends CompilationError {

        public final String msg;

        public InvertionNotPossible(String msg) {
            super(msg);
            this.msg = msg;
        }
    }

    public static class DoubleReflectionOnOutput extends CompilationError {
        public final Pos state;
        public final LexUnicodeSpecification.E edge;

        public DoubleReflectionOnOutput(Pos state, LexUnicodeSpecification.E edge) {
            super("State at "+state+" produces reflection twice. Inversion will lead to too large automata");
            this.state = state;
            this.edge = edge;
        }
    }
    public static class RangeWithoutReflection extends CompilationError {
        public final Pos state;
        public final LexUnicodeSpecification.E edge;

        public RangeWithoutReflection(Pos state, LexUnicodeSpecification.E edge) {
            super("State at "+state+" is a range but its output is not reflected. Inversion leads to nondeterminism");
            this.state = state;
            this.edge = edge;
        }
    }
    public static class EpsilonTransitionCycle extends CompilationError {
        public final Pos state;
        public final IntSeq output;
        public final IntSeq output1;

        public EpsilonTransitionCycle(Pos state, IntSeq output, IntSeq output1) {
            super("State at "+state+" produces epsilon cycle. Example of cycling outputs "+output+" "+output1);
            this.state = state;
            this.output = output;
            this.output1 = output1;
        }
    }
    public static class AmbiguousAcceptingState extends CompilationError {

        public final Pos sourceState;
        public final Pos firstAcceptingState;
        public final Pos secondAcceptingState;
        public final LexUnicodeSpecification.P firstFinalEdge;
        public final LexUnicodeSpecification.P secondFinalEdge;

        public AmbiguousAcceptingState(Pos sourceState, Pos firstAcceptingState, Pos secondAcceptingState, LexUnicodeSpecification.P firstFinalEdge, LexUnicodeSpecification.P secondFinalEdge) {
            super("State at "+sourceState+" leads nondeterministically to another accepting states "+firstAcceptingState+" with output "+firstFinalEdge+" and "+secondAcceptingState+" with output "+secondFinalEdge+" after inversion.");
            this.sourceState = sourceState;
            this.firstAcceptingState = firstAcceptingState;
            this.secondAcceptingState = secondAcceptingState;
            this.firstFinalEdge = firstFinalEdge;
            this.secondFinalEdge = secondFinalEdge;
        }
    }
    public static class PromiseReused extends CompilationError {

		public String promise;

		public PromiseReused(String promise) {
			super("Promise identifier "+promise+" was used more than once! Promise ID must be unique across entire project!");
			this.promise = promise;
		}


	}
    public static class PromiseNotMade extends CompilationError {
		public String promise;
		public PromiseNotMade(String promise) {
			super("Promise identifier "+promise+" was not found!");
			this.promise = promise;
		}
	}
}
