package net.alagris.ast;

import java.util.Map;

import net.alagris.IntSeq;
import net.alagris.Pair.IntPair;
import net.alagris.ast.Atomic.Set;
import net.alagris.ast.Atomic.Str;
import net.alagris.ast.Atomic.Var;
import net.alagris.ast.Kolmogorov.KolConcat;
import net.alagris.ast.Kolmogorov.KolIdentity;
import net.alagris.ast.Kolmogorov.KolInv;
import net.alagris.ast.Kolmogorov.KolProd;

public interface Optimise {

	public static Str str(IntSeq seq) {
		return seq.size() == 1 ? new Atomic.Char(seq.at(0)) : new Atomic.StrImpl(seq);
	}

	public static Kolmogorov identity(final Kolmogorov regex) {
		if (regex instanceof KolIdentity) {
			return regex;
		}
		return new KolIdentity(regex);
	}

	public static Kolmogorov cdrewrite(final Kolmogorov replacement, final Kolmogorov leftCntx,
			final Kolmogorov rightCntx, final Kolmogorov sigmaStar) {
		return new Kolmogorov.KolKleene(union(concat(prod(Atomic.REFLECT), sigmaStar),
				concat(identity(leftCntx), concat(replacement, identity(rightCntx)))), '*');
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

	public static Solomonoff powerOptional(Solomonoff sol, int power) {
		if (power == 0)
			return Atomic.EPSILON;
		Solomonoff pow = new Solomonoff.SolKleene(sol, '?');
		for (int i = 1; i < power; i++) {
			pow = new Solomonoff.SolConcat(sol, pow);
		}
		return pow;
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

	public static Kolmogorov concat(Kolmogorov lhs, Kolmogorov rhs) {
		if (lhs instanceof Str && rhs instanceof Str) {
			return str(((Str) lhs).str().concat(((Str) rhs).str()));
		} else if (rhs instanceof KolConcat) {
			final KolConcat r = (KolConcat) rhs;
			// TODO rewrite it as loop, instead of recursion. This way stack won't blow up
			return new KolConcat(concat(lhs, r.lhs), r.rhs);
		} else {
			return new KolConcat(lhs, rhs);
		}
	}

	public static Kolmogorov prod(Kolmogorov lhs) {
		if (lhs.readsInput()) {
			return Atomic.EPSILON;
		} else {
			return new KolProd(lhs);
		}
	}

	public static Kolmogorov inv(Kolmogorov lhs) {
		if (lhs instanceof KolInv) {
			return ((KolInv) lhs).lhs;
		} else {
			return new KolInv(lhs);
		}
	}

	public static Kolmogorov diff(Kolmogorov lhs, Kolmogorov rhs) {
		if (lhs instanceof Set && rhs instanceof Set) {
			final Set l = (Set) lhs;
			final Set r = (Set) rhs;
			return new Atomic.SetImpl(Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a || !b));
		} else {
			return new Kolmogorov.KolDiff(lhs, rhs);
		}
	}
}
