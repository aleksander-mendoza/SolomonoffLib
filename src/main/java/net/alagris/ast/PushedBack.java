package net.alagris.ast;

import java.util.Map;

import net.alagris.IntSeq;
import net.alagris.ast.Atomic.Set;
import net.alagris.ast.Atomic.Str;
import net.alagris.ast.Atomic.Var;
import net.alagris.ast.Kolmogorov.KolConcat;
import net.alagris.ast.Kolmogorov.KolProd;

import net.alagris.Pair.IntPair;
public class PushedBack {
	private Kolmogorov lhs, pushedBackOutput;

	public PushedBack(Kolmogorov regex, Kolmogorov pushedBackOutput) {
		this.lhs = regex;
		this.pushedBackOutput = pushedBackOutput;
		assert !pushedBackOutput.producesOutput();
	}
	public static PushedBack range(IntPair pair) {
		return new PushedBack(Optimise.range(pair), Atomic.EPSILON);
	}
	public static PushedBack str(IntSeq seq) {
		return new PushedBack(Optimise.str(seq), Atomic.EPSILON);
	}
	public static PushedBack var(String id, Map<String, Kolmogorov> vars) {
		final Kolmogorov m = vars.get(id);
		if (m instanceof Str || m instanceof Set) {
			return new PushedBack(m, Atomic.EPSILON);
		} else if (m instanceof KolProd) {
			final KolProd prod = (KolProd) m;
			if (prod.rhs instanceof Str || prod.rhs instanceof Set) {
				return new PushedBack(Atomic.EPSILON, prod.rhs);
			}
		}else if(m instanceof KolConcat) {
			final KolConcat c = (KolConcat) m;
			if(c.rhs instanceof KolProd) {
				final KolProd prod = (KolProd) c.rhs;
				if ((c.lhs instanceof Str || c.lhs instanceof Set)&&(prod.rhs instanceof Str || prod.rhs instanceof Set)) {
					return new PushedBack(c.lhs, prod.rhs);
				}
			}
		}
		return new PushedBack(new Var(id, false, vars),Atomic.EPSILON);
	}

	public PushedBack union(PushedBack rhs) {
		concatPushedBackOutput();
		rhs.concatPushedBackOutput();
		lhs = Optimise.union(lhs, rhs.lhs);
		return this;
	}

	private void concatPushedBackOutput() {
		if (hasPushedBackOutput()) {
			lhs = new KolConcat(lhs, new KolProd(pushedBackOutput));
		}
		pushedBackOutput = Atomic.EPSILON;

	}

	/** output of lhs is fed as input to rhs */
	public PushedBack comp(PushedBack rhs) {
		concatPushedBackOutput();
		rhs.concatPushedBackOutput();
		lhs = new Kolmogorov.KolComp(lhs, rhs.lhs);
		return this;
	}

	public PushedBack concat(PushedBack rhs) {
		if(rhs.lhs.producesOutput()) {
			concatPushedBackOutput();
			pushedBackOutput = rhs.pushedBackOutput;
		}else {
			pushedBackOutput = Optimise.concat(pushedBackOutput, rhs.pushedBackOutput);
		}
		lhs = Optimise.concat(lhs,rhs.lhs);
		return this;
	}

	public PushedBack prod() {
		assert !producesOutput();
		pushedBackOutput = lhs;
		lhs = Atomic.EPSILON;
		return this;
	}

	public PushedBack kleene(char type) {
		concatPushedBackOutput();
		lhs = new Kolmogorov.KolKleene(lhs, type);
		return this;
	}

	public PushedBack pow(int num) {
		if (num > 0) {
			concatPushedBackOutput();//Well, theoretically we could try to push back
			//further, but readability of produced code might suffer.  
			lhs = new Kolmogorov.KolPow(lhs, num);
		} else if (num == 0) {
			pushedBackOutput = lhs = Atomic.EPSILON;
		} else {
			concatPushedBackOutput();
			final Kolmogorov inv = lhs.inv();
			lhs = num==-1 ? inv : new Kolmogorov.KolPow(inv, -num);
		}
		return this;
	}

	public PushedBack diff(PushedBack rhs) {
		assert !producesOutput();
		assert !rhs.producesOutput();
		if (lhs instanceof Set && rhs.lhs instanceof Set) {
			final Set l = (Set) lhs;
			final Set r = (Set) rhs;
			lhs = new Atomic.SetImpl(Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a || !b));
		} else {
			lhs = new Kolmogorov.KolDiff(lhs, rhs.lhs);
		}
		return this;
	}
	
	public Kolmogorov finish() {
		concatPushedBackOutput();
		return lhs;
	}

	public boolean hasPushedBackOutput() {
		return pushedBackOutput.readsInput();
	}

	public boolean producesOutput() {
		return pushedBackOutput.readsInput() || lhs.producesOutput();
	}
}