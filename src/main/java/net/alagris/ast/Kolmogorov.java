package net.alagris.ast;

import java.util.HashMap;
import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.ast.Atomic.Var;
import net.alagris.ast.Solomonoff;
import net.alagris.HashMapIntermediateGraph.LexUnicodeSpecification;

public interface Kolmogorov {

	public static final LexUnicodeSpecification SPECS = new LexUnicodeSpecification(false, 0, Integer.MAX_VALUE, null);

	public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment);

	public IntSeq representative(Function<Var, Kolmogorov> variableAssignment);

	public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap);

	public Kolmogorov inv();

	public boolean producesOutput();

	public boolean readsInput();

	public void toString(StringBuilder sb);

	public int precedence();
	
	public static class KolDiff implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean producesOutput, readsInput;

		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolDiff(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			producesOutput = lhs.producesOutput();
			readsInput = lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "subtract");
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return null;
		}

		@Override
		public Kolmogorov inv() {
			return new KolInv(this);
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subRhs = rhs.substitute(argMap);
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return Optimise.diff(subLhs, subRhs);
		}

		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append(" - ");
			if (rhs.precedence() < precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			} else {
				rhs.toString(sb);
			}
		}

		@Override
		public int precedence() {
			return 1;
		}
		
		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}

	}

	public static class KolIdentity implements Kolmogorov {
		final Kolmogorov lhs;
		final boolean readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return readsInput();
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolIdentity(Kolmogorov lhs) {
			this.lhs = lhs;
			readsInput = lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "identity");
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return lhs.representative(variableAssignment);
		}

		@Override
		public Kolmogorov inv() {
			return this;
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subLhs == lhs)
				return this;
			return new KolIdentity(subLhs);
		}

		@Override
		public void toString(StringBuilder sb) {
			sb.append("identity[");
			lhs.toString(sb);
			sb.append("]");

		}

		@Override
		public int precedence() {
			return Integer.MAX_VALUE;
		}
	}

	public static class KolComp implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return true;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolComp(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			readsInput = lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "compose");
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return null;
		}

		@Override
		public Kolmogorov inv() {
			return new KolComp(rhs.inv(), lhs.inv());
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subRhs = rhs.substitute(argMap);
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return new Kolmogorov.KolComp(subLhs, subRhs);
		}

		@Override
		public int precedence() {
			return 0;
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append(" @ ");
			if (rhs.precedence() < precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			} else {
				rhs.toString(sb);
			}
		}
	}

	public static class KolUnion implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean producesOutput, readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolUnion(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			producesOutput = lhs.producesOutput() || rhs.producesOutput();
			readsInput = lhs.readsInput() || rhs.readsInput();
			assert !(rhs instanceof KolUnion) : "Kolmogorov AST union not normalized";
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return Optimise.union(lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment));
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return rhs.representative(variableAssignment);
		}

		@Override
		public Kolmogorov inv() {
			return Optimise.union(lhs.inv(), rhs.inv());
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subRhs = rhs.substitute(argMap);
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return Optimise.union(subLhs, subRhs);
		}

		@Override
		public int precedence() {
			return 2;
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append("|");
			if (rhs.precedence() < precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			} else {
				rhs.toString(sb);
			}
		}
	}

	public static class KolConcat implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean producesOutput, readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolConcat(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			producesOutput = lhs.producesOutput() || rhs.producesOutput();
			readsInput = lhs.readsInput() || rhs.readsInput();
			assert !(rhs instanceof KolConcat) : "Kolmogorov AST concat not normalized";
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return Optimise.concat(lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment));
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return IntSeq.concatOpt(lhs.representative(variableAssignment), rhs.representative(variableAssignment));
		}

		@Override
		public Kolmogorov inv() {
			return Optimise.concat(lhs.inv(), rhs.inv());
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subRhs = rhs.substitute(argMap);
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return Optimise.concat(subLhs, subRhs);
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append(" ");
			if (rhs.precedence() < precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			} else {
				rhs.toString(sb);
			}
		}
		@Override
		public int precedence() {
			return 3;
		}
	}

	public static class KolProd implements Kolmogorov {
		final Kolmogorov rhs;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		public KolProd(Kolmogorov rhs) {
			assert !rhs.producesOutput();
			this.rhs = rhs;
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return new Solomonoff.SolProd(rhs.representative(variableAssignment));
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return IntSeq.Epsilon;
		}

		@Override
		public Kolmogorov inv() {
			assert !rhs.producesOutput();
			return rhs;
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subRhs = rhs.substitute(argMap);
			if (subRhs == rhs)
				return this;
			return Optimise.prod(subRhs);
		}

		@Override
		public boolean producesOutput() {
			return true;
		}

		@Override
		public boolean readsInput() {
			return false;
		}
		@Override
		public void toString(StringBuilder sb) {
			sb.append(":");
			if (!(rhs instanceof Atomic)) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			} else {
				rhs.toString(sb);
			}
		}

		@Override
		public int precedence() {
			return Integer.MAX_VALUE;
		}
	}

	public static class KolKleene implements Kolmogorov {
		final Kolmogorov lhs;
		final char type;
		final boolean producesOutput, readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolKleene(Kolmogorov lhs, char type) {
			this.lhs = lhs;
			this.type = type;
			assert type == '*' || type == '?' || type == '+';
			producesOutput = lhs.producesOutput();
			readsInput = lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return new Solomonoff.SolKleene(lhs.toSolomonoff(variableAssignment), type);
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return IntSeq.Epsilon;
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subLhs == lhs)
				return this;
			return new KolKleene(subLhs, type);
		}

		@Override
		public Kolmogorov inv() {
			return new KolKleene(lhs.inv(), type);
		}

		@Override
		public int precedence() {
			return 4;
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append(type);
		}
	}

	/** Concatenates exactly n times */
	public static class KolPow implements Kolmogorov {
		final Kolmogorov lhs;
		final int power;
		final boolean producesOutput, readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolPow(Kolmogorov lhs, int power) {
			this.lhs = lhs;
			this.power = power;
			assert power >= 0;
			producesOutput = lhs.producesOutput();
			readsInput = lhs.readsInput();
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subLhs == lhs)
				return this;
			return new KolPow(subLhs, power);
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return Optimise.power(lhs.toSolomonoff(variableAssignment), power);
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			final IntSeq one = lhs.representative(variableAssignment);
			return one == null ? null : one.pow(power);
		}

		@Override
		public Kolmogorov inv() {
			return new KolPow(lhs.inv(), power);
		}

		@Override
		public int precedence() {
			return 4;
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append("^").append(power);
		}
	}

	/** Concatenates n times or less (optional concatenation) */
	public static class KolLePow implements Kolmogorov {
		final Kolmogorov lhs;
		final int power;
		final boolean producesOutput, readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolLePow(Kolmogorov lhs, int power) {
			this.lhs = lhs;
			this.power = power;
			assert power >= 0;
			producesOutput = lhs.producesOutput();
			readsInput = lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			return Optimise.powerOptional(lhs.toSolomonoff(variableAssignment), power);
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			final IntSeq one = lhs.representative(variableAssignment);
			return one == null ? null : one.pow(power);
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subLhs == lhs)
				return this;
			return new KolLePow(subLhs, power);
		}

		@Override
		public Kolmogorov inv() {
			return new KolLePow(lhs.inv(), power);
		}

		@Override
		public int precedence() {
			return 4;
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append("^<=").append(power);
		}
	}

	public static class KolInv implements Kolmogorov {
		final Kolmogorov lhs;
		final boolean producesOutput, readsInput;

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public boolean producesOutput() {
			return producesOutput;
		}

		@Override
		public boolean readsInput() {
			return readsInput;
		}

		public KolInv(Kolmogorov lhs) {
			this.lhs = lhs;
			producesOutput = lhs.readsInput();
			readsInput = lhs.producesOutput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "inverse");
		}

		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			return null;
		}

		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			final Kolmogorov subLhs = lhs.substitute(argMap);
			if (subLhs == lhs)
				return this;
			return Optimise.inv(subLhs);
		}

		@Override
		public Kolmogorov inv() {
			return lhs;
		}

		@Override
		public int precedence() {
			return 4;
		}
		@Override
		public void toString(StringBuilder sb) {
			if (lhs.precedence() < precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			} else {
				lhs.toString(sb);
			}
			sb.append("^-1");
		}
	}

}
