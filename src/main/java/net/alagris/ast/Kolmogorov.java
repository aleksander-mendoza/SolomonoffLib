package net.alagris.ast;

import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.ast.Solomonoff;
import net.alagris.HashMapIntermediateGraph.LexUnicodeSpecification;

public interface Kolmogorov {

	
	public static final LexUnicodeSpecification SPECS = new LexUnicodeSpecification(false, 0,Integer.MAX_VALUE,null);

	public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment);
	public IntSeq representative(Function<String,Kolmogorov> variableAssignment);
	public Kolmogorov inv();
	public boolean producesOutput();
	public boolean readsInput();
	
	public static class KolDiff implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean producesOutput,readsInput;
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
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "subtract");
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			return null;
		}

		@Override
		public Kolmogorov inv() {
			return new KolInv(this);
		}

		
	}

	public static class KolComp implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean readsInput;
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
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "compose");
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			return null;
		}

		@Override
		public Kolmogorov inv() {
			return new KolComp(rhs.inv(), lhs.inv());
		}

	}

	public static class KolUnion implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean producesOutput,readsInput;
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
			producesOutput = lhs.producesOutput()||rhs.producesOutput();
			readsInput =  lhs.readsInput()||rhs.readsInput();
			assert !(lhs instanceof KolUnion):"Kolmogorov AST union not normalized";
		}

		@Override
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			return new Solomonoff.SolUnion(lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment));
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			return lhs.representative(variableAssignment);
		}

		@Override
		public Kolmogorov inv() {
			return Optimise.union(lhs.inv(), rhs.inv());
		}

	}

	public static class KolConcat implements Kolmogorov {
		final Kolmogorov lhs, rhs;
		final boolean producesOutput,readsInput;
		
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
			producesOutput = lhs.producesOutput()||rhs.producesOutput();
			readsInput = lhs.readsInput()||rhs.readsInput();
			assert !(rhs instanceof KolConcat):"Kolmogorov AST concat not normalized";
		}

		@Override
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			return new Solomonoff.SolConcat(lhs.toSolomonoff(variableAssignment), rhs.toSolomonoff(variableAssignment));
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			return IntSeq.concatOpt(lhs.representative(variableAssignment),rhs.representative(variableAssignment));
		}

		@Override
		public Kolmogorov inv() {
			return Optimise.concat(lhs.inv(), rhs.inv());
		}

		
	}

	public static class KolProd implements Kolmogorov {
		final Kolmogorov  rhs;

		public KolProd( Kolmogorov rhs) {
			assert !rhs.producesOutput();
			this.rhs = rhs;
		}

		@Override
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			return new Solomonoff.SolProd(rhs.representative(variableAssignment));
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			return IntSeq.Epsilon;
		}

		@Override
		public Kolmogorov inv() {
			assert !rhs.producesOutput();
			return rhs;
		}

		@Override
		public boolean producesOutput() {
			return true;
		}
		@Override
		public boolean readsInput() {
			return false;
		}
	}

	public static class KolKleene implements Kolmogorov {
		final Kolmogorov lhs;
		final char type;
		final boolean producesOutput,readsInput;
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
			readsInput =  lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			return new Solomonoff.SolKleene(lhs.toSolomonoff(variableAssignment), type);
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			return IntSeq.Epsilon;
		}

		@Override
		public Kolmogorov inv() {
			return new KolKleene(lhs.inv(), type);
		}

	
	}
	/**Concatenates exactly n times*/
	public static class KolPow implements Kolmogorov {
		final Kolmogorov lhs;
		final int power;
		final boolean producesOutput,readsInput;
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
			assert power>=0;
			producesOutput = lhs.producesOutput();
			readsInput = lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			return Optimise.power(lhs.toSolomonoff(variableAssignment), power);
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			final IntSeq one = lhs.representative(variableAssignment);
			return one==null?null:one.pow(power);
		}

		@Override
		public Kolmogorov inv() {
			return new KolPow(lhs.inv(), power);
		}

	}
	
	/**Concatenates n times or less (optional concatenation)*/
	public static class KolLePow implements Kolmogorov {
		final Kolmogorov lhs;
		final int power;
		final boolean producesOutput,readsInput;
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
			assert power>=0;
			producesOutput = lhs.producesOutput();
			readsInput =  lhs.readsInput();
		}

		@Override
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			return Optimise.powerOptional(lhs.toSolomonoff(variableAssignment), power);
		}

		@Override
		public IntSeq representative(Function<String,Kolmogorov> variableAssignment) {
			final IntSeq one = lhs.representative(variableAssignment);
			return one==null?null:one.pow(power);
		}

		@Override
		public Kolmogorov inv() {
			return new KolLePow(lhs.inv(), power);
		}

	}

	public static class KolInv implements Kolmogorov {
		final Kolmogorov lhs;
		final boolean producesOutput,readsInput;
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
		public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment) {
			final Solomonoff[] args = { lhs.toSolomonoff(variableAssignment) };
			return new Solomonoff.SolFunc(args, "inverse");
		}

		@Override
		public IntSeq representative(Function<String, Kolmogorov> variableAssignment) {
			return null;
		}

		@Override
		public Kolmogorov inv() {
			return lhs;
		}
	}
}
