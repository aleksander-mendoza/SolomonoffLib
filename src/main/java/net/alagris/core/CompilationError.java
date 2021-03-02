package net.alagris.core;

import java.util.List;

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
        public ParseException(Pos node, String msg) {
            super(msg);
            this.node = node;
        }
        public Pos getNode() {
            return node;
        }
    }

    public static class PipelineSizeMismatchException extends CompilationError {

        public final Pos pos;
        public final int expected;
        public final int got;

        public PipelineSizeMismatchException(Pos pos, int expected, int got) {
            super("Expected pipeline of size "+expected+" but was "+got+", "+pos);
            this.pos = pos;
            this.expected = expected;
            this.got = got;
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

        public final Pos automatonPos;

        public <E,P> WeightConflictingToThirdState(Pos automatonPos,LexUnicodeSpecification.FunctionalityCounterexampleToThirdState<
                E,P,?> counterexample) {
            super(counterexample.getMessage(automatonPos));
            assert automatonPos!=null;
			this.automatonPos = automatonPos;
        }

    }


    public static class PseudoMinimisationNondeterminism extends CompilationError {
        public <N> PseudoMinimisationNondeterminism(Pos pos, N stateA, N stateB, LexUnicodeSpecification.P finA, LexUnicodeSpecification.P finB) {
            super("Automaton "+pos+" failed minimisation due to nondeterministic edge "+finA+" from "+stateA+" and edge "+finB+" from "+stateB);
        }
    }

    public static class EdgeReductionNondeterminism extends CompilationError {
        public EdgeReductionNondeterminism(Pos pos, Pos state, LexUnicodeSpecification.E edge, LexUnicodeSpecification.E edge1, Pos targetState) {
            super("Automaton "+pos+" has nondeterministic edges "+edge+" and "+edge1+" coming from "+state+" going to "+targetState);
        }
    }

    public static class WeightConflictingFinal extends CompilationError {

        public final Pos automatonPos;

        public <E,P> WeightConflictingFinal(Pos automatonPos,LexUnicodeSpecification.FunctionalityCounterexampleFinal<
                E,P,?> counterexample) {
            super(counterexample.getMessage(automatonPos));
            assert automatonPos!=null;
			this.automatonPos = automatonPos;
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
            super("Input key '"+IntSeq.toUnicodeString(in)+"' has ambiguous outputs '"+out1+"' and '"+out2+"'");
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

    public static class TooManyOperands extends CompilationError {
        public final List<?> automata;
        public final int expectedSize;

        public TooManyOperands(Pos pos,List<?> automata, int expectedSize) {
            super(pos+ " expected "+expectedSize+" operands but had "+automata.size());
            this.automata = automata;
            this.expectedSize = expectedSize;
        }
    }

    public static class IllegalOperandType extends CompilationError {

        public IllegalOperandType(Pos pos,int operandIndex,Class<?> expectedType,Class<?> actualType) {
            super(pos+" expected "+expectedType+" at argument "+operandIndex+" but was "+actualType);
        }
    }

    public static class NotEnoughOperands extends CompilationError {

        public NotEnoughOperands(Pos pos,int operandIndex,Class<?> expectedType,int actualSize) {
            super(pos+" expected "+expectedType+" at argument "+operandIndex+" but was only "+actualSize+" operands were provided");
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
