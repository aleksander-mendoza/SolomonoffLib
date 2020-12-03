package net.alagris.ast;

import net.alagris.IntSeq;

public interface Solomonoff {

	int precedence();
	void toString(StringBuilder sb);
	
	public static class SolUnion implements Solomonoff{
		final Solomonoff lhs,rhs;
		public SolUnion(Solomonoff lhs,Solomonoff rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
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
	}
	public static class SolConcat implements Solomonoff{
		final Solomonoff lhs,rhs;
		public SolConcat(Solomonoff lhs,Solomonoff rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
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
			sb.append("*");
		}
	}
	public static class SolWeightAfter implements Solomonoff{
		final Solomonoff lhs;
		final int w;
		public SolWeightAfter(Solomonoff lhs, int w) {
			this.lhs = lhs;
			this.w = w;
		}
		@Override
		public int precedence() {
			return lhs.precedence();
		}
		@Override
		public void toString(StringBuilder sb) {
			lhs.toString(sb);
			sb.append(" ").append(w);
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
	}
	
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
	}
}