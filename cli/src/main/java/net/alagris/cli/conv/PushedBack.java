package net.alagris.cli.conv;

import net.alagris.cli.conv.Atomic.Set;
import net.alagris.cli.conv.Atomic.Str;
import net.alagris.core.IntSeq;
import net.alagris.core.NullTermIter;
import net.alagris.core.Pair;
import net.alagris.core.Pair.IntPair;

public class PushedBack {
	private Kolmogorov lhs, pushedBackOutput;

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		lhs.toString(sb);
		sb.append(" PUSH ");
		pushedBackOutput.toString(sb);
		return sb.toString();
	}

	public PushedBack(Kolmogorov regex, Kolmogorov pushedBackOutput) {
		this.lhs = regex;
		this.pushedBackOutput = pushedBackOutput;
		assert !pushedBackOutput.producesOutput();
	}

	public static PushedBack wrap(Kolmogorov regex) {
		if (regex instanceof Atomic) {
			return new PushedBack(regex, Atomic.EPSILON);
		} else if (regex instanceof KolProd) {
			final KolProd prod = (KolProd) regex;
			if (prod.rhs instanceof Atomic) {
				return new PushedBack(Atomic.EPSILON, prod.rhs);
			}
		} else if (regex instanceof KolConcat) {
			final KolConcat c = (KolConcat) regex;
			if (c.rhs instanceof KolProd) {
				final KolProd prod = (KolProd) c.rhs;
				if ((c.lhs instanceof Str || c.lhs instanceof Set)
						&& (prod.rhs instanceof Str || prod.rhs instanceof Set)) {
					return new PushedBack(c.lhs, prod.rhs);
				}
			}
		}
		return new PushedBack(regex, Atomic.EPSILON);
	}

	public static PushedBack range(NullTermIter<IntPair> rangeInclusive) {
		final NullTermIter<Pair<Integer,Integer>> exclusiveRange = () -> {
			IntPair i = rangeInclusive.next();
			if (i == null) return null;
			return Kolmogorov.SPECS.parseRangeInclusive(i.l, i.r);
		};
		return new PushedBack(Optimise.ranges(exclusiveRange), Atomic.EPSILON);
	}

	public static PushedBack str(IntSeq seq) {
		return new PushedBack(Optimise.str(seq), Atomic.EPSILON);
	}

	public static PushedBack prod(IntSeq seq) {
		return new PushedBack(Atomic.EPSILON, Optimise.str(seq));
	}

	public static PushedBack eps() {
		return new PushedBack(Atomic.EPSILON, Atomic.EPSILON);
	}

	public static Kolmogorov var(String id, Kolmogorov referenced) {
		if (referenced instanceof Atomic || referenced instanceof KolRefl) {
			return referenced;
		}
		if (referenced instanceof KolConcat) {
			final KolConcat c = (KolConcat) referenced;
			if (c.lhs instanceof Atomic && c.rhs instanceof KolProd
					&& ((KolProd) c.rhs).rhs instanceof Atomic) {
				return referenced;
			}
		}
		return new AtomicVar(id, VarState.NONE, referenced);
	}

	public PushedBack cdrewrite(final PushedBack leftCntx, final PushedBack rightCntx, final PushedBack sigmaStar) {
		return sigmaStar.union(leftCntx.concat(this).concat(rightCntx)).kleene('*');
	}

	public PushedBack union(PushedBack rhs) {
		concatPushedBackOutput();
		rhs.concatPushedBackOutput();
		lhs = Optimise.union(regex(), rhs.regex());
		return this;
	}

	private void concatPushedBackOutput() {
		if (hasPushedBackOutput()) {
			lhs = Optimise.concat(regex(), new KolProd(getPushedBackOutput()));
		}
		pushedBackOutput = Atomic.EPSILON;

	}

	/** output of lhs is fed as input to rhs */
	public PushedBack comp(PushedBack rhs) {
		concatPushedBackOutput();
		rhs.concatPushedBackOutput();
		lhs = Optimise.comp(regex(), rhs.regex());
		return this;
	}



	public PushedBack concat(PushedBack rhs) {
		if (rhs.regex() instanceof KolRefl && ((KolRefl) rhs.regex()).set instanceof AtomicChar) {
			final AtomicChar r = (AtomicChar) ((KolRefl) rhs.regex()).set;
			if (regex() instanceof KolRefl && ((KolRefl) regex()).set instanceof AtomicChar) {
				final AtomicChar l = (AtomicChar) ((KolRefl) regex()).set;
				lhs = pushedBackOutput = new AtomicStr(new IntSeq(l.character,r.character));
			}else {
				pushedBackOutput = Optimise.concat(getPushedBackOutput(), r);
				lhs = Optimise.concat(regex(), r);
			}
		} else {
			if (rhs.regex().producesOutput()) {// non-commutative case
				concatPushedBackOutput();
				pushedBackOutput = rhs.getPushedBackOutput();
			} else {// commutative case
				pushedBackOutput = Optimise.concat(getPushedBackOutput(), rhs.getPushedBackOutput());
			}
			lhs = Optimise.concat(regex(), rhs.regex());
		}
		return this;
	}

	public PushedBack prod() {
		assert !producesOutput();
		pushedBackOutput = regex();
		lhs = Atomic.EPSILON;
		return this;
	}

	public PushedBack identity() {
		if (lhs instanceof KolIdentity) {
			pushedBackOutput = Atomic.EPSILON;
		} else if (!lhs.readsInput()) {
			lhs = pushedBackOutput = Atomic.EPSILON;
		} else if (lhs instanceof Str) {
			pushedBackOutput = lhs;
		} else {
			lhs = lhs.identity();
			pushedBackOutput = Atomic.EPSILON;
		}
		return this;
	}

	public PushedBack clearOutput() {
		pushedBackOutput = Atomic.EPSILON;
		lhs = lhs.clearOutput();
		return this;
	}

	public PushedBack kleene(char type) {
		concatPushedBackOutput();
		lhs = new KolKleene(regex(), type);
		return this;
	}

	public PushedBack pow(int num) {
		if (num > 0) {
			concatPushedBackOutput();// Well, theoretically we could try to push back
			// further, but readability of produced code might suffer.
			lhs = new KolPow(regex(), num);
		} else if (num == 0) {
			pushedBackOutput = lhs = Atomic.EPSILON;
		} else {
			concatPushedBackOutput();
			final Kolmogorov inv = regex().inv();
			lhs = num == -1 ? inv : new KolPow(inv, -num);
		}
		return this;
	}

	public PushedBack powLe(int num) {
		if (num > 0) {
			concatPushedBackOutput();// Well, theoretically we could try to push back
			// further, but readability of produced code might suffer.
			lhs = new KolLePow(regex(), num);
		} else if (num == 0) {
			pushedBackOutput = lhs = Atomic.EPSILON;
		} else {
			concatPushedBackOutput();
			final Kolmogorov inv = regex().inv();
			lhs = num == -1 ? inv : new KolLePow(inv, -num);
		}
		return this;
	}

	public PushedBack diff(PushedBack rhs) {
//		assert !producesOutput();
		lhs = Optimise.diff(lhs, rhs.lhs);
		return this;
	}

	public Kolmogorov finish() {
		concatPushedBackOutput();
		return regex();
	}

	public boolean hasPushedBackOutput() {
		return getPushedBackOutput().readsInput();
	}

	public boolean producesOutput() {
		return getPushedBackOutput().readsInput() || regex().producesOutput();
	}

	public Kolmogorov regex() {
		return lhs;
	}

	public Kolmogorov getPushedBackOutput() {
		return pushedBackOutput;
	}

	public PushedBack copy() {
		return new PushedBack(lhs, pushedBackOutput);
	}

	public PushedBack inv() {
		concatPushedBackOutput();
		lhs = lhs.inv();
		return this;
	}
}