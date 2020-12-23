package net.alagris.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.Pair.IntPair;
import net.alagris.Specification;
import net.alagris.Specification.NullTermIter;
import net.alagris.Specification.Range;

public interface Atomic {

	public static class Var implements Kolmogorov, Solomonoff, Atomic {
		private final String id;
		private final boolean wasInverted;
		private final boolean readsInput;
		private final boolean producesOutput;

		@Override
		public void countUsages(Consumer<Var> countUsage) {
			countUsage.accept(this);
		}

		public Var(String id, boolean wasInverted, Kolmogorov referenced) {
			assert referenced != null : id;
			this.id = id;
			this.wasInverted = wasInverted;
			this.readsInput = referenced.readsInput();
			this.producesOutput = referenced.producesOutput();
		}

		public Var(String id, boolean wasInverted, boolean readsInput, boolean producesOutput,
				Function<Var, Kolmogorov> variableAssignment) {
			this(id, wasInverted, readsInput, producesOutput);
			assert variableAssignment.apply(this).producesOutput() == producesOutput;
			assert variableAssignment.apply(this).readsInput() == readsInput;
		}

		/** literal constructor */
		private Var(String id, boolean wasInverted, boolean readsInput, boolean producesOutput) {
			this.id = id;
			this.wasInverted = wasInverted;
			this.readsInput = readsInput;
			this.producesOutput = producesOutput;
		}
		@Override
		public String toString() {
			return encodeID();
		}

		public String encodeID() {
			return encodeID(id, wasInverted, readsInput, producesOutput);
		}
		public String encodeInvID() {
			return encodeID(id, !wasInverted, producesOutput, readsInput);
		}
		@Override
		public Kolmogorov inv() {
			return new Var(id, !wasInverted(), producesOutput, readsInput);
		}
		public static String encodeID(String id, boolean wasInverted, boolean readsInput, boolean producesOutput) {
			return (wasInverted ? "i" : "o") + (readsInput ? "i" : "n") + (producesOutput ? "o" : "n") + id;
		}
		public Var(String encodedID) {
			id = encodedID.substring(3);
			wasInverted = encodedID.charAt(0) == 'i';
			readsInput = encodedID.charAt(1) == 'i';
			producesOutput = encodedID.charAt(2) == 'o';
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			variableAssignment.apply(this);//just a callback. Do nothing with it
			return this;
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return variableAssignment.apply(this).representative(variableAssignment);
		}

		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		@Override
		public int precedence() {
			return 3;
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append(id);
		}

		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
			
			final VarMeta usages = usagesLeft.apply(encodeID());
			assert usages.usagesLeft > 0;
			if(--usages.usagesLeft>0) {
				sb.append("!!");
			}
			sb.append(encodeID());
			assert usages.weights!=null:id;
			return new Weights(usages.weights);
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			return argMap.get(encodeID());
		}

		public boolean wasInverted() {
			return wasInverted;
		}

		public String id() {
			return id;
		}
	}

	static interface Set extends Kolmogorov, Atomic , Church{
		ArrayList<Range<Integer, Boolean>> ranges();

	}

	static interface Str extends Kolmogorov, Solomonoff, Atomic, Church{
		IntSeq str();

		default public int precedence() {
			return Integer.MAX_VALUE;
		}

		@Override
		public default void countUsages(Consumer<Var> countUsage) {
		}

		@Override
		default public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return str();
		}

		@Override
		default public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return this;
		}

		@Override
		default public Kolmogorov inv() {
			return Optimise.prod(this);
		}

		@Override
		default public boolean producesOutput() {
			return false;
		}

		@Override
		public default Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			return this;
		}
		@Override
		default PushedBack toKolmogorov(TranslationQueries resolveFreeVariable) {
			return PushedBack.wrap(this);
		}
		@Override
		public default Church substituteCh(Function<ChVar, Church> argMap) {
			return this;
		}

		@Override
		default public void toString(StringBuilder sb) {
			sb.append(str().toStringLiteral());
		}
		
		@Override
		public default Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,
				Function<String, VarMeta> usagesLeft) {
			sb.append(str().toStringLiteral());
			if (str().isEmpty())
				return Solomonoff.eps();
			return Solomonoff.str();
		}
	}

	public static final Str EPSILON = new StrImpl(IntSeq.Epsilon);
	public static final IntSeq REFLECT_STR = new IntSeq(Kolmogorov.SPECS.minimal());
	public static final Str REFLECT = new StrImpl(REFLECT_STR);

	public static class StrImpl implements Str {
		final IntSeq str;

		public StrImpl(IntSeq str) {
			this.str = str;
		}

		@Override
		public IntSeq str() {
			return str;
		}

		@Override
		public boolean readsInput() {
			return !str.isEmpty();
		}

	}

	public static class SetImpl implements Set {
		final ArrayList<Range<Integer, Boolean>> ranges;

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			return this;
		}

		@Override
		public Church substituteCh(Function<ChVar, Church> argMap) {
			return this;
		}
		
		@Override
		public PushedBack toKolmogorov(TranslationQueries resolveFreeVariable) {
			return PushedBack.wrap(this);
		}
		
		public SetImpl(ArrayList<Range<Integer, Boolean>> ranges) {
			this.ranges = ranges;
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			assert SPECS.isFullSigmaCovered(ranges);
			int fromExclusive;
			int i;
			if (ranges.get(0).edges()) {
				i = 0;
				fromExclusive = SPECS.minimal();
			} else if (ranges.size() > 1) {
				fromExclusive = ranges.get(0).input();
				i = 1;
			} else {
				return new Var("#", false, false, false);
			}
			Range<Integer, Boolean> range = ranges.get(i);
			assert range.edges() : ranges;
			int toInclusive = range.input();
			Solomonoff sol = new Solomonoff.SolRange(fromExclusive + 1, toInclusive);
			for (i++; i < ranges.size(); i++) {
				Range<Integer, Boolean> rangeExcluded = ranges.get(i);
				fromExclusive = rangeExcluded.input();
				assert !rangeExcluded.edges() : ranges;
				i++;
				if (i == ranges.size()) {
					assert SPECS.maximal().equals(fromExclusive) : ranges;
					break;
				}
				range = ranges.get(i);
				assert range.edges() : ranges;
				toInclusive = range.input();
				sol = new Solomonoff.SolUnion(sol, new Solomonoff.SolRange(fromExclusive + 1, toInclusive));
			}
			return sol;
		}

		@Override
		public ArrayList<Range<Integer, Boolean>> ranges() {
			return ranges;
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			final int min = min(ranges);
			return SPECS.minimal().equals(min) ? null : new IntSeq(min);
		}

		@Override
		public Kolmogorov inv() {
			return Optimise.prod(this);
		}

		@Override
		public boolean producesOutput() {
			return false;
		}

		@Override
		public boolean readsInput() {
			return !isEmpty(ranges);
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append(ranges);
		}

		@Override
		public int precedence() {
			return Integer.MAX_VALUE;
		}
	}

	static class Char implements Set, Str {
		final int character;

		@Override
		public boolean readsInput() {
			return true;
		}

		public Char(int character) {
			this.character = character;
		}

		@Override
		public IntSeq str() {
			return new IntSeq(character);
		}

		@Override
		public ArrayList<Range<Integer, Boolean>> ranges() {
			return SPECS.makeSingletonRanges(true, false, character - 1, character);
		}

	}

	public static final ArrayList<Range<Integer, Boolean>> DOT = Kolmogorov.SPECS.makeSingletonRanges(true, false,
			Kolmogorov.SPECS.minimal(), Kolmogorov.SPECS.maximal());

	public static ArrayList<Range<Integer, Boolean>> composeSets(ArrayList<Range<Integer, Boolean>> lhs,
			ArrayList<Range<Integer, Boolean>> rhs, BiFunction<Boolean, Boolean, Boolean> f) {
		final NullTermIter<Specification.RangeImpl<Integer, Boolean>> i = Kolmogorov.SPECS.zipTransitionRanges(
				Specification.fromIterable(lhs), Specification.fromIterable(rhs),
				(from, to, lhsTran, rhsTran) -> new Specification.RangeImpl<>(to, f.apply(lhsTran, rhsTran)));
		final ArrayList<Range<Integer, Boolean>> ranges = new ArrayList<>(lhs.size() + rhs.size());
		Specification.RangeImpl<Integer, Boolean> next;
		Specification.RangeImpl<Integer, Boolean> prev = null;
		while ((next = i.next()) != null) {
			if (prev != null && !prev.edges().equals(next.edges())) {
				ranges.add(prev);
			}
			prev = next;
		}
		assert prev != null;
		assert prev.input().equals(Kolmogorov.SPECS.maximal()) : prev.input() + "==" + Kolmogorov.SPECS.maximal();
		ranges.add(prev);
		if (ranges.size() == 2 && ranges.get(0).input().equals(255) && ranges.get(0).edges()) {
			return DOT;
		} else {
			return ranges;
		}
	}

	public static int min(ArrayList<Range<Integer, Boolean>> ranges) {
		assert Kolmogorov.SPECS.isFullSigmaCovered(ranges);
		if (ranges.get(0).edges()) {
			return Kolmogorov.SPECS.successor(Kolmogorov.SPECS.minimal());
		} else if (ranges.size() > 1) {
			assert ranges.get(1).edges() : ranges;
			return Kolmogorov.SPECS.successor(ranges.get(0).input());
		}
		return Kolmogorov.SPECS.minimal();
	}

	public static boolean isEmpty(ArrayList<Range<Integer, Boolean>> ranges) {
		return Kolmogorov.SPECS.minimal().equals(min(ranges));
	}
}
