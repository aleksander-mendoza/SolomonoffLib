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
import net.alagris.ast.Kolmogorov.KolUnion;
import net.alagris.ast.Solomonoff.SolConcat;
import net.alagris.ast.Solomonoff.SolProd;
import net.alagris.ast.Solomonoff.SolUnion;

public interface Optimise {

	public static Str str(IntSeq seq) {
		return seq.size() == 1 ? new Atomic.Char(seq.at(0)) : new Atomic.StrImpl(seq);
	}



	public static Set range(int rangeFromExclusive,
	           int rangeToInclusive) {
		assert rangeFromExclusive < rangeToInclusive;
		return new Atomic.SetImpl(Kolmogorov.SPECS.makeSingletonRanges(true, false, rangeFromExclusive, rangeToInclusive));
	}

	public static Solomonoff power(Solomonoff sol, int power) {
		if (power == 0)
			return Atomic.EPSILON;
		Solomonoff pow = sol;
		for (int i = 1; i < power; i++) {
			
			pow = concat(pow,sol );
		}
		return pow;
	}

	public static Solomonoff powerOptional(Solomonoff sol, int power) {
		if (power == 0)
			return Atomic.EPSILON;
		Solomonoff pow = new Solomonoff.SolKleene(sol, '?');
		for (int i = 1; i < power; i++) {
			pow = concat(pow,new Solomonoff.SolKleene(sol,'?'));
		}
		return pow;
	}

	public static Solomonoff concat(Solomonoff lhs, Solomonoff rhs) {
		if (lhs instanceof Str && rhs instanceof Str) {
			return str(((Str) lhs).str().concat(((Str) rhs).str()));
		}else if(lhs instanceof Str && ((Str)lhs).str().isEmpty()){
			return rhs;
		}else if(rhs instanceof Str && ((Str)rhs).str().isEmpty()){
			return lhs;
		}else if (rhs instanceof SolConcat) {
			final SolConcat r = (SolConcat) rhs;
			// TODO rewrite it as loop, instead of recursion. This way stack won't blow up
			return new SolConcat(concat(lhs, r.lhs), r.rhs);
		} else {
			return new SolConcat(lhs, rhs);
		}
	}
	
	public static Solomonoff union(Solomonoff lhs, Solomonoff rhs) {
		if(rhs instanceof SolUnion){
			final SolUnion u = (SolUnion) rhs; 
			return new Solomonoff.SolUnion(union(lhs, u.lhs),u.rhs);
		} else {
			return new Solomonoff.SolUnion(lhs, rhs);
		}
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
		} else if(rhs instanceof KolUnion){
			final KolUnion u = (KolUnion) rhs; 
			return new Kolmogorov.KolUnion(union(lhs, u.lhs),u.rhs);
		} else {
			return new Kolmogorov.KolUnion(lhs, rhs);
		}
	}

	public static Kolmogorov concat(Kolmogorov lhs, Kolmogorov rhs) {
		if (lhs instanceof Str && rhs instanceof Str) {
			return str(((Str) lhs).str().concat(((Str) rhs).str()));
		}else if(lhs instanceof Str && ((Str)lhs).str().isEmpty()){
			return rhs;
		}else if(rhs instanceof Str && ((Str)rhs).str().isEmpty()){
			return lhs;
		}else if (rhs instanceof KolConcat) {
			final KolConcat r = (KolConcat) rhs;
			// TODO rewrite it as loop, instead of recursion. This way stack won't blow up
			return new KolConcat(concat(lhs, r.lhs), r.rhs);
		} else {
			return new KolConcat(lhs, rhs);
		}
	}

	public static Kolmogorov prod(Kolmogorov lhs) {
		if (lhs.readsInput()) {
			return new KolProd(lhs);
		} else {
			return Atomic.EPSILON;
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
			return new Atomic.SetImpl(Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a && !b));
		} else {
			return new Kolmogorov.KolDiff(lhs, rhs);
		}
	}
}
