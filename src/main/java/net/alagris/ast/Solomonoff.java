package net.alagris.ast;

import java.util.Map;
import java.util.function.Function;

import net.alagris.IntSeq;
import net.alagris.Pair;
import net.alagris.Pair.IntPair;

public interface Solomonoff {

	
	public static class VarMeta{
		int usagesLeft;
		IntPair postWeight;
		
	}
	public static class Weights{
		final int min;
		final int max;
		final Integer eps;
		public Weights(int min,int max,Integer eps) {
			assert min <= max;
			this.min = min;
			this.max = max;
			this.eps = eps;
		}
	}
	public static Integer add(Integer a,int b) {
		return a==null?null:a+b;
	}
	public static Weights w(int min,int max,Integer eps) {
		return new Weights(min, max, eps);
	}
	
	int precedence();
	void toString(StringBuilder sb);
	/**prints to string but with automatic inference of lexicographic weights and automatically prefixing
	 * variables with exponentials whenever copies are necessary. Returns the highest weight that was appended 
	 * to the regular expression. 
	 * @return max and min weight of any outgoing transition (according to Glushkov's construction)*/
	Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,Weights preWeight, Map<String,Integer> usagesLeft);
	
	public static class SolUnion implements Solomonoff{
		final Solomonoff lhs,rhs;
		public SolUnion(Solomonoff lhs,Solomonoff rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			assert !(rhs instanceof SolUnion);
		}
		@Override
		public int precedence() {
			return 0;
		}
		@Override
		public void toString(StringBuilder sb) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			}else {
				lhs.toString(sb);
			}
			sb.append("|");
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			}else {
				rhs.toString(sb);
			}
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb,Weights w, Map<String, Integer> usagesLeft) {
			final Weights lw;
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lw = lhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
				sb.append(")");
			}else {
				lw = lhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
			}
			sb.append("|");
			final Weights rw;
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				rw = rhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
				sb.append(")");
			}else {
				rw = rhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
			}
			
			final int diff;
			if(rw.min <= lw.max) {
				diff = lw.max-rw.min+1;
				sb.append(" ").append(diff);
			}else {
				diff = 0;
			}
			assert lw.max < rw.min+diff;
			return w(lw.min, rw.max+diff,add(rw.eps, diff));
		}
	}
	public static class SolConcat implements Solomonoff{
		final Solomonoff lhs,rhs;
		public SolConcat(Solomonoff lhs,Solomonoff rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
			assert !(rhs instanceof SolConcat);
		}
		@Override
		public int precedence() {
			return 1;
		}
		@Override
		public void toString(StringBuilder sb) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			}else {
				lhs.toString(sb);
			}
			sb.append(" ");
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				rhs.toString(sb);
				sb.append(")");
			}else {
				rhs.toString(sb);
			}
		}
		@Override
		public IntPair toStringAutoWeightsAndAutoExponentials(StringBuilder sb, IntPair w, Map<String, Integer> usagesLeft) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				w = lhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
				sb.append(")");
			}else {
				w = lhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
			}
			sb.append(" ");
			if(rhs.precedence()<precedence()) {
				sb.append("(");
				w = rhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
				sb.append(")");
			} else {
				w = rhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
			}
			return w;
		}
	}
	public static class SolProd implements Solomonoff{
		final IntSeq output;
		public SolProd(IntSeq output) {
			this.output = output;
		}
		@Override
		public int precedence() {
			return 3;
		}
		@Override
		public void toString(StringBuilder sb) {
			sb.append(":").append(output.toStringLiteral());
		}
		@Override
		public IntPair toStringAutoWeightsAndAutoExponentials(StringBuilder sb, IntPair w, Map<String, Integer> usagesLeft) {
			sb.append(":").append(output.toStringLiteral());
			return w;
		}
	}
	public static class SolKleene implements Solomonoff{
		final Solomonoff lhs;
		final char type;
		public SolKleene(Solomonoff lhs, char type) {
			this.lhs = lhs;
			this.type = type;
		}
		@Override
		public int precedence() {
			return 2;
		}
		@Override
		public void toString(StringBuilder sb) {
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				lhs.toString(sb);
				sb.append(")");
			}else {
				lhs.toString(sb);
			}
			sb.append(type);
		}
		@Override
		public IntPair toStringAutoWeightsAndAutoExponentials(StringBuilder sb, IntPair w, Map<String, Integer> usagesLeft) {
			final IntPair afterKleene; 
			if(lhs.precedence()<precedence()) {
				sb.append("(");
				afterKleene = lhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
				sb.append(")");
			} else {
				afterKleene = lhs.toStringAutoWeightsAndAutoExponentials(sb,w,usagesLeft);
			}
			final IntPair out;
			if(type=='+'||type=='*') {
				final int beforeMin = w.l;
				final int beforeMax = w.r;
				final int afterMin = afterKleene.l;
				final int afterMax = afterKleene.r;
				final int diff;
				if(afterMin<=beforeMax) {
					diff = beforeMax-afterMin+1;
					sb.append(diff);
				}else {
					diff = 0;
				}
				out = Pair.of(afterMin+diff,afterMax+diff);
			}else {
				out = afterKleene;
			}
			sb.append(type);
			return out;
		}
	}
	
	public static class SolFunc implements Solomonoff{
		final Solomonoff[] args;
		final String id;
		public SolFunc(Solomonoff[] args,String id) {
			this.args = args;
			this.id = id;
		}
		@Override
		public int precedence() {
			return 3;
		}
		@Override
		public void toString(StringBuilder sb) {
			sb.append(id).append("[");
			if(args.length>0) {
				args[0].toString(sb);
				for(int i=1;i<args.length;i++) {
					sb.append(",");
					args[1].toString(sb);
				}
			}
			sb.append("]");
		}
		@Override
		public IntPair toStringAutoWeightsAndAutoExponentials(StringBuilder sb, IntPair preWeight,
				Map<String, Integer> usagesLeft) {
			sb.append(id).append("[");
			final IntPair out;
			if(args.length>0) {
				final IntPair w = args[0].toStringAutoWeightsAndAutoExponentials(sb,preWeight,usagesLeft);
				int min = w.l;
				int max = w.r;
				for(int i=1;i<args.length;i++) {
					sb.append(",");
					final IntPair p = args[1].toStringAutoWeightsAndAutoExponentials(sb,preWeight,usagesLeft);
					min = p.l;
					max = p.r;
				}
				out = Pair.of(min, max);
			}else {
				out = NEUTRAL_WEIGHTS;
			}
			sb.append("]");
			return out;
		}
	}
	public static final IntPair NEUTRAL_WEIGHTS = Pair.of((int)Kolmogorov.SPECS.weightNeutralElement(),(int) Kolmogorov.SPECS.weightNeutralElement());
	public static class SolRange implements Solomonoff{
		final int fromInclusive,toInclusive;
		public SolRange(int fromInclusive,int toInclusive) {
			this.fromInclusive = fromInclusive;
			this.toInclusive = toInclusive;
		}
		@Override
		public int precedence() {
			return 3;
		}
		@Override
		public void toString(StringBuilder sb) {
			IntSeq.appendRange(sb, fromInclusive, toInclusive);
		}
		@Override
		public IntPair toStringAutoWeightsAndAutoExponentials(StringBuilder sb, IntPair preWeight,
				Map<String, Integer> usagesLeft) {
			IntSeq.appendRange(sb, fromInclusive, toInclusive);
			return NEUTRAL_WEIGHTS;
		}
	}
}