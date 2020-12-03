package net.alagris.ast;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.Specification;
import net.alagris.Specification.NullTermIter;
import net.alagris.Specification.Range;

public interface Atomic {

	public static class Var implements Kolmogorov, Solomonoff {
		private final String id;
		private final boolean wasInverted;
		private final boolean readsInput;
		private final boolean producesOutput;
		
		public Var(String id, boolean wasInverted,
				Map<String, Kolmogorov> variableAssignment) {
			this(id,wasInverted,variableAssignment.get(id).readsInput(variableAssignment::get),variableAssignment.get(id).producesOutput(variableAssignment::get),variableAssignment);
		}
		public Var(String id, boolean wasInverted, boolean readsInput, boolean producesOutput,
				Map<String, Kolmogorov> variableAssignment) {
			this(id, wasInverted, readsInput, producesOutput);
			assert producesOutput(variableAssignment::get) == producesOutput;
			assert readsInput(variableAssignment::get) == readsInput;
		}

		/** literal constructor */
		private Var(String id, boolean wasInverted, boolean readsInput, boolean producesOutput) {
			this.id = id;
			this.wasInverted = wasInverted;
			this.readsInput = readsInput;
			this.producesOutput = producesOutput;
		}
		
		public String encodeID() {
			return (wasInverted ? 'i' : 'o') + (readsInput ? 'i' : 'n') + (producesOutput ? 'o' : 'n') + id;
		}
		
		public Var(String encodedID) {
			id = encodedID.substring(3);
			wasInverted = encodedID.charAt(0) == 'i';
			readsInput =  encodedID.charAt(1) == 'i';
			producesOutput = encodedID.charAt(2) == 'o';
		}

		@Override
		public Solomonoff toSolomonoff(Function<String, Kolmogorov> variableAssignment) {
			return this;
		}

		@Override
		public IntSeq representative(Function<String, Kolmogorov> variableAssignment) {
			return variableAssignment.apply(id).representative(variableAssignment);
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return variableAssignment.apply(id).producesOutput(variableAssignment);
		}

		@Override
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return new Var(id, !wasInverted, readsInput, producesOutput);
		}

		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return variableAssignment.apply(id).readsInput(variableAssignment);
		}
		
		@Override
		public int precedence() {
			return 3;
		}
		
		@Override
		public void toString(StringBuilder sb) {
			sb.append(id);
		}
	}

	static interface Set extends Kolmogorov {
		ArrayList<Range<Integer, Boolean>> ranges();
	}

	static interface Str extends Kolmogorov, Solomonoff {
		IntSeq str();

		@Override
		default public IntSeq representative(Function<String, Kolmogorov> variableAssignment) {
			return str();
		}

		@Override
		default public Solomonoff toSolomonoff(Function<String, Kolmogorov> variableAssignment) {
			return this;
		}

		@Override
		default public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.prod(this);
		}

		@Override
		default public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return false;
		}

		@Override
		default public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return true;
		}
		@Override
		default public int precedence() {
			return 3;
		}
		@Override
		default public void toString(StringBuilder sb) {
			sb.append(str().toStringLiteral());
		}
	}

	public static final Str EPSILON = new StrImpl(IntSeq.Epsilon);

	public static class StrImpl implements Str {
		final IntSeq str;

		public StrImpl(IntSeq str) {
			this.str = str;
		}

		@Override
		public IntSeq str() {
			return str;
		}

	}

	public static class SetImpl implements Set {
		final ArrayList<Range<Integer, Boolean>> ranges;

		public SetImpl(ArrayList<Range<Integer, Boolean>> ranges) {
			this.ranges = ranges;
		}

		@Override
		public Solomonoff toSolomonoff(Function<String, Kolmogorov> variableAssignment) {
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
				return new Var("#",false,false,false);
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
		public IntSeq representative(Function<String, Kolmogorov> variableAssignment) {
			final int min = min(ranges);
			return SPECS.minimal().equals(min) ? null : new IntSeq(min);
		}

		@Override
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.prod(this);
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return false;
		}

		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return !isEmpty(ranges);
		}
	}

	static class Char implements Set, Str {
		final int character;

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

	public static final ArrayList<Range<Integer, Boolean>> DOT = Kolmogorov.SPECS.makeSingletonRanges(true, false, 0,
			Integer.MAX_VALUE);

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
