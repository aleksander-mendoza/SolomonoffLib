package net.alagris.ast;

import java.util.Map;
import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.ast.Solomonoff;
import net.alagris.HashMapIntermediateGraph.LexUnicodeSpecification;

public interface Kolmogorov {

	public static final LexUnicodeSpecification SPECS = new LexUnicodeSpecification(false, null);

	public Solomonoff toSolomonoff(Function<String,Kolmogorov> variableAssignment);
	public IntSeq representative(Function<String,Kolmogorov> variableAssignment);
	public Kolmogorov inv(Map<String,Kolmogorov> variableAssignment);
	public boolean producesOutput(Function<String, Kolmogorov> variableAssignment);
	public boolean readsInput(Function<String, Kolmogorov> variableAssignment);
	
	public static class KolDiff implements Kolmogorov {
		final Kolmogorov lhs, rhs;

		public KolDiff(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return new KolInv(this);
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.producesOutput(variableAssignment);
		}

		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment);
		}
	}

	public static class KolComp implements Kolmogorov {
		final Kolmogorov lhs, rhs;

		public KolComp(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.comp(rhs.inv(variableAssignment), lhs.inv(variableAssignment));
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return true;
		}
		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment);
		}
	}

	public static class KolUnion implements Kolmogorov {
		final Kolmogorov lhs, rhs;

		public KolUnion(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.union(lhs.inv(variableAssignment), rhs.inv(variableAssignment));
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.producesOutput(variableAssignment)||rhs.producesOutput(variableAssignment);
		}
		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment)||rhs.readsInput(variableAssignment);
		}
	}

	public static class KolConcat implements Kolmogorov {
		final Kolmogorov lhs, rhs;

		public KolConcat(Kolmogorov lhs, Kolmogorov rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.concat(lhs.inv(variableAssignment), rhs.inv(variableAssignment));
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.producesOutput(variableAssignment)||rhs.producesOutput(variableAssignment);
		}
		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment)||rhs.readsInput(variableAssignment);
		}
	}

	public static class KolProd implements Kolmogorov {
		final Kolmogorov  rhs;

		public KolProd( Kolmogorov rhs) {
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			assert !rhs.producesOutput(variableAssignment::get);
			return rhs;
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return true;
		}
		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return false;
		}
	}

	public static class KolKleene implements Kolmogorov {
		final Kolmogorov lhs;
		final char type;

		public KolKleene(Kolmogorov lhs, char type) {
			this.lhs = lhs;
			this.type = type;
			assert type == '*' || type == '?' || type == '+';
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.kleene(lhs.inv(variableAssignment), type);
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.producesOutput(variableAssignment);
		}

		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment);

		}
	}

	public static class KolPow implements Kolmogorov {
		final Kolmogorov lhs;
		final int power;

		public KolPow(Kolmogorov lhs, int power) {
			this.lhs = lhs;
			this.power = power;
			assert power>=0;
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return Optimise.pow(lhs.inv(variableAssignment), power, variableAssignment);
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.producesOutput(variableAssignment);
		}
		
		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment);
		}
	}

	public static class KolInv implements Kolmogorov {
		final Kolmogorov lhs;

		public KolInv(Kolmogorov lhs) {
			this.lhs = lhs;
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
		public Kolmogorov inv(Map<String, Kolmogorov> variableAssignment) {
			return lhs;
		}

		@Override
		public boolean producesOutput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.readsInput(variableAssignment);
		}
		
		@Override
		public boolean readsInput(Function<String, Kolmogorov> variableAssignment) {
			return lhs.producesOutput(variableAssignment);

		}
	}

}
