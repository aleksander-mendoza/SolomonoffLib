package net.alagris.ast;

import java.util.Map;

import net.alagris.IntSeq;
import net.alagris.Pair.IntPair;
import net.alagris.ast.Atomic.Set;
import net.alagris.ast.Atomic.Str;
import net.alagris.ast.Atomic.Var;
import net.alagris.ast.Kolmogorov.KolConcat;
import net.alagris.ast.Kolmogorov.KolProd;

public interface Optimise {

	public static Str str(IntSeq seq) {
		return seq.size() == 1 ? new Atomic.Char(seq.at(0)) : new Atomic.StrImpl(seq);
	}

	public static Set range(IntPair range) {
		assert range.l < range.r;
		return new Atomic.SetImpl(Kolmogorov.SPECS.makeSingletonRanges(true, false, range.l, range.r));
	}

	public static Solomonoff power(Solomonoff sol, int power) {
		if (power == 0)
			return Atomic.EPSILON;
		Solomonoff pow = sol;
		for (int i = 1; i < power; i++) {
			pow = new Solomonoff.SolConcat(sol, pow);
		}
		return pow;
	}

	public static Kolmogorov var(String id, Map<String, Kolmogorov> vars) {
		final Kolmogorov m = vars.get(id);
		if (m instanceof Str || m instanceof Set) {
			return m;
		} else if (m instanceof KolProd) {
			final KolProd prod = (KolProd) m;
			if (prod.rhs instanceof Str || prod.rhs instanceof Set) {
				return m;
			}
		}
		return new Var(id,false,vars);
	}

	public static Kolmogorov union(Kolmogorov lhs, Kolmogorov rhs) {
		if (lhs instanceof Set && rhs instanceof Set) {
			final Set l = (Set) lhs;
			final Set r = (Set) rhs;
			return new Atomic.SetImpl(Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a || b));
		} else if (lhs instanceof KolProd && rhs instanceof KolProd) {
			final KolProd l = (KolProd) lhs;
			final KolProd r = (KolProd) rhs;
			return new KolProd(union(l, r));
		} else {
			return new Kolmogorov.KolUnion(lhs, rhs);
		}
	}

	/** output of lhs is fed as input to rhs */
	public static Kolmogorov comp(Kolmogorov lhs, Kolmogorov rhs) {
		return new Kolmogorov.KolComp(lhs, rhs);
	}

	/**
	 * should have form (((x1 x2) x3)...xn) :(y1 y2 y3...)
	 */
	public static boolean assertConcatenationIsNormalized(Kolmogorov k) {
		if (k instanceof Kolmogorov.KolConcat) {
			final Kolmogorov.KolConcat c = (Kolmogorov.KolConcat) k;
			return !(c.rhs instanceof KolConcat) && assertConcatenationIsNormalizedWithNoOutput(c.lhs);
		} else {
			return true;
		}
	}

	public static boolean assertConcatenationIsNormalizedWithNoOutput(Kolmogorov k) {
		if (k instanceof Kolmogorov.KolConcat) {
			final Kolmogorov.KolConcat c = (Kolmogorov.KolConcat) k;
			return !(c.rhs instanceof KolConcat || c.rhs instanceof KolProd)
					&& assertConcatenationIsNormalizedWithNoOutput(c.lhs);
		} else {
			return !(k instanceof KolProd);
		}
	}

	public static Kolmogorov concat(Kolmogorov lhs, Kolmogorov rhs) {
		assert assertConcatenationIsNormalized(lhs);
		assert assertConcatenationIsNormalized(rhs);
		final Kolmogorov out;
		if (lhs instanceof Str && rhs instanceof Str) {
			out = str(((Str) lhs).str().concat(((Str) rhs).str()));
		} else if (lhs instanceof Kolmogorov.KolProd) {
			final Kolmogorov.KolProd l = (Kolmogorov.KolProd) lhs;
			if (rhs instanceof Kolmogorov.KolProd) {
				final Kolmogorov.KolProd r = (Kolmogorov.KolProd) rhs;
				out = prod(concat(l.rhs, r.rhs));
			} else if (rhs instanceof Kolmogorov.KolConcat) {
				final Kolmogorov.KolConcat r = (Kolmogorov.KolConcat) rhs;
				out = new KolConcat(r.lhs, concat(l, r.rhs));
			} else {
				out = new KolConcat(rhs, l);
			}
		} else if (lhs instanceof Kolmogorov.KolConcat) {
			final Kolmogorov.KolConcat l = (Kolmogorov.KolConcat) lhs;
			if (rhs instanceof Kolmogorov.KolProd) {
				out = new KolConcat(l.lhs, concat(l.rhs, rhs));
			} else if (rhs instanceof Kolmogorov.KolConcat) {
				final Kolmogorov.KolConcat r = (Kolmogorov.KolConcat) rhs;
				out = concat(concat(l, r.lhs), r.rhs);
			} else {
				out = new KolConcat(lhs, rhs);
			}
		} else {
			if (rhs instanceof Kolmogorov.KolProd) {
				out = new KolConcat(lhs, rhs);
			} else if (rhs instanceof Kolmogorov.KolConcat) {
				final Kolmogorov.KolConcat r = (Kolmogorov.KolConcat) rhs;
				out = new KolConcat(concat(lhs, r.lhs), r.rhs);
			} else {
				out = new KolConcat(lhs, rhs);
			}
		}
		assert assertConcatenationIsNormalized(out);
		return out;
	}

	public static Kolmogorov prod(Kolmogorov rhs) {
		if(rhs instanceof Str) {
			if(((Str) rhs).str().isEmpty())return Atomic.EPSILON;
		}
		return new Kolmogorov.KolProd(rhs);
	}

	public static Kolmogorov kleene(Kolmogorov lhs, char type) {
		return new Kolmogorov.KolKleene(lhs, type);
	}

	public static Kolmogorov pow(Kolmogorov lhs, int num, Map<String, Kolmogorov> variableAssignment) {
		if (num > 0) {
			return new Kolmogorov.KolPow(lhs, num);
		} else if (num == 0) {
			return Atomic.EPSILON;
		} else {
			final Kolmogorov inv = lhs.inv(variableAssignment);
			return new Kolmogorov.KolPow(inv, -num);
		}
	}

	public static Kolmogorov diff(Kolmogorov lhs, Kolmogorov rhs) {
		if (lhs instanceof Set && rhs instanceof Set) {
			final Set l = (Set) lhs;
			final Set r = (Set) rhs;
			return new Atomic.SetImpl(Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a || !b));
		}
		return new Kolmogorov.KolDiff(lhs, rhs);
	}
}
