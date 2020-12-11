package net.alagris.ast;

import java.util.Map;
import java.util.function.Function;

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
	
	
	public static PushedBack wrap(Kolmogorov regex) {
		if (regex instanceof Atomic) {
			return new PushedBack(regex, Atomic.EPSILON);
		} else if (regex instanceof KolProd) {
			final KolProd prod = (KolProd) regex;
			if (prod.rhs instanceof Atomic) {
				return new PushedBack(Atomic.EPSILON, prod.rhs);
			}
		}else if(regex instanceof KolConcat) {
			final KolConcat c = (KolConcat) regex;
			if(c.rhs instanceof KolProd) {
				final KolProd prod = (KolProd) c.rhs;
				if ((c.lhs instanceof Str || c.lhs instanceof Set)&&(prod.rhs instanceof Str || prod.rhs instanceof Set)) {
					return new PushedBack(c.lhs, prod.rhs);
				}
			}
		}
		return new PushedBack(regex, Atomic.EPSILON);
	}
	
	public static PushedBack range(IntPair pair) {
		return new PushedBack(Optimise.range(pair), Atomic.EPSILON);
	}
	public static PushedBack str(IntSeq seq) {
		return new PushedBack(Optimise.str(seq), Atomic.EPSILON);
	}
	public static PushedBack eps() {
		return new PushedBack(Atomic.EPSILON, Atomic.EPSILON);
	}
	public static Kolmogorov var(String id, Function<String, Kolmogorov> vars) {
		return new Var(id, false, vars);
	}
	
	public PushedBack cdrewrite(final PushedBack leftCntx,
			final PushedBack rightCntx, final PushedBack sigmaStar) {
		concatPushedBackOutput();
		lhs = Optimise.cdrewrite(lhs, leftCntx.finish(), rightCntx.finish(), sigmaStar.finish());
		return this;
	}
	
	public PushedBack union(PushedBack rhs) {
		concatPushedBackOutput();
		rhs.concatPushedBackOutput();
		lhs = Optimise.union(getLhs(), rhs.getLhs());
		return this;
	}

	private void concatPushedBackOutput() {
		if (hasPushedBackOutput()) {
			lhs = new KolConcat(getLhs(), new KolProd(getPushedBackOutput()));
		}
		pushedBackOutput = Atomic.EPSILON;

	}

	/** output of lhs is fed as input to rhs */
	public PushedBack comp(PushedBack rhs) {
		concatPushedBackOutput();
		rhs.concatPushedBackOutput();
		lhs = new Kolmogorov.KolComp(getLhs(), rhs.getLhs());
		return this;
	}

	public PushedBack concat(PushedBack rhs) {
		if(rhs.getLhs().producesOutput()) {
			concatPushedBackOutput();
			pushedBackOutput = rhs.getPushedBackOutput();
		}else {
			pushedBackOutput = Optimise.concat(getPushedBackOutput(), rhs.getPushedBackOutput());
		}
		lhs = Optimise.concat(getLhs(),rhs.getLhs());
		return this;
	}

	public PushedBack prod() {
		assert !producesOutput();
		pushedBackOutput = getLhs();
		lhs = Atomic.EPSILON;
		return this;
	}

	public PushedBack kleene(char type) {
		concatPushedBackOutput();
		lhs = new Kolmogorov.KolKleene(getLhs(), type);
		return this;
	}

	public PushedBack pow(int num) {
		if (num > 0) {
			concatPushedBackOutput();//Well, theoretically we could try to push back
			//further, but readability of produced code might suffer.  
			lhs = new Kolmogorov.KolPow(getLhs(), num);
		} else if (num == 0) {
			pushedBackOutput = lhs = Atomic.EPSILON;
		} else {
			concatPushedBackOutput();
			final Kolmogorov inv = getLhs().inv();
			lhs = num==-1 ? inv : new Kolmogorov.KolPow(inv, -num);
		}
		return this;
	}
	
	public PushedBack powLe(int num) {
		if (num > 0) {
			concatPushedBackOutput();//Well, theoretically we could try to push back
			//further, but readability of produced code might suffer.  
			lhs = new Kolmogorov.KolLePow(getLhs(), num);
		} else if (num == 0) {
			pushedBackOutput = lhs = Atomic.EPSILON;
		} else {
			concatPushedBackOutput();
			final Kolmogorov inv = getLhs().inv();
			lhs = num==-1 ? inv : new Kolmogorov.KolLePow(inv, -num);
		}
		return this;
	}

	public PushedBack diff(PushedBack rhs) {
		assert !producesOutput();
		assert !rhs.producesOutput();
		lhs = Optimise.diff(lhs,rhs.lhs);
		return this;
	}
	
	public Kolmogorov finish() {
		concatPushedBackOutput();
		return getLhs();
	}

	public boolean hasPushedBackOutput() {
		return getPushedBackOutput().readsInput();
	}

	public boolean producesOutput() {
		return getPushedBackOutput().readsInput() || getLhs().producesOutput();
	}
	public Kolmogorov getLhs() {
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
		lhs = Optimise.inv(lhs);
		return this;
	}
}