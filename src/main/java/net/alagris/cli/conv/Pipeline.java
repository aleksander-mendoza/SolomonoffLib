package net.alagris.cli.conv;

//public interface Pipeline extends Solomonoff{
//
//	public static class SolSplit implements Pipeline {
//		final Pipeline tuple;
//
//		public SolSplit(Pipeline  tuple) {
//			this.tuple = tuple;
//		}
//
//		@Override
//		public int precedence() {
//			return Integer.MAX_VALUE;
//		}
//
//		@Override
//		public void toString(StringBuilder sb) {
//			
//			sb.append("split ");
//			tuple.toString(sb);
//			sb.append(" on <"+Integer.MAX_VALUE+">:''");
//		}
//
//		@Override
//		public void countUsages(Consumer<Var> countUsage) {
//			tuple.countUsages(countUsage);
//		}
//
//		@Override
//		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
//			sb.append("split ");
//			tuple.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			sb.append(" on <"+Integer.MAX_VALUE+">:''");
//			return null;
//		}
//	}
//
//	public static class SolOr implements Pipeline {
//		final Pipeline lhs, rhs;
//
//		public SolOr(Pipeline lhs, Pipeline rhs) {
//			this.lhs = lhs;
//			this.rhs = rhs;
//			assert !(rhs instanceof SolOr);
//		}
//
//		@Override
//		public int precedence() {
//			return 0;
//		}
//
//		@Override
//		public void toString(StringBuilder sb) {
//			if (lhs.precedence() < precedence()) {
//				sb.append("@(");
//				lhs.toString(sb);
//				sb.append(")");
//			} else {
//				lhs.toString(sb);
//			}
//			sb.append("||");
//			if (rhs.precedence() < precedence()) {
//				sb.append("@(");
//				rhs.toString(sb);
//				sb.append(")");
//			} else {
//				rhs.toString(sb);
//			}
//		}
//		
//		@Override
//		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
//			if (lhs.precedence() < precedence()) {
//				sb.append("@(");
//				lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//				sb.append(")");
//			} else {
//				lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			}
//			sb.append("||");
//			if (rhs.precedence() < precedence()) {
//				sb.append("@(");
//				rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//				sb.append(")");
//			} else {
//				rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			}
//			return null;
//		}
//
//		@Override
//		public void countUsages(Consumer<Var> countUsage) {
//			lhs.countUsages(countUsage);
//			rhs.countUsages(countUsage);
//		}
//	}
//	
//
//	public static class SolAnd implements Pipeline {
//		final Pipeline lhs, rhs;
//
//		public SolAnd(Pipeline lhs, Pipeline rhs) {
//			this.lhs = lhs;
//			this.rhs = rhs;
//			assert !(rhs instanceof SolAnd);
//		}
//
//		@Override
//		public int precedence() {
//			return 1;
//		}
//
//		@Override
//		public void toString(StringBuilder sb) {
//			if (lhs.precedence() < precedence()) {
//				sb.append("@(");
//				lhs.toString(sb);
//				sb.append(")");
//			} else {
//				lhs.toString(sb);
//			}
//			sb.append("&&");
//			if (rhs.precedence() < precedence()) {
//				sb.append("@(");
//				rhs.toString(sb);
//				sb.append(")");
//			} else {
//				rhs.toString(sb);
//			}
//		}
//		@Override
//		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
//			if (lhs.precedence() < precedence()) {
//				sb.append("@(");
//				lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//				sb.append(")");
//			} else {
//				lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			}
//			sb.append("&&");
//			if (rhs.precedence() < precedence()) {
//				sb.append("@(");
//				rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//				sb.append(")");
//			} else {
//				rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			}
//			return null;
//		}
//		@Override
//		public void countUsages(Consumer<Var> countUsage) {
//			lhs.countUsages(countUsage);
//			rhs.countUsages(countUsage);
//		}
//	}
//	public static class SolLazyComp implements Pipeline {
//		final Pipeline lhs, rhs;
//
//		public SolLazyComp(Pipeline lhs, Pipeline rhs) {
//			this.lhs = lhs;
//			this.rhs = rhs;
//			assert !(rhs instanceof SolLazyComp);
//		}
//
//		@Override
//		public int precedence() {
//			return 2;
//		}
//		@Override
//		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
//			if (lhs.precedence() < precedence()) {
//				sb.append("@(");
//				lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//				sb.append(")");
//			} else {
//				lhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			}
//			sb.append(";");
//			if (rhs.precedence() < precedence()) {
//				sb.append("@(");
//				rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//				sb.append(")");
//			} else {
//				rhs.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//			}
//			return null;
//		}
//		@Override
//		public void toString(StringBuilder sb) {
//			if (lhs.precedence() < precedence()) {
//				sb.append("@(");
//				lhs.toString(sb);
//				sb.append(")");
//			} else {
//				lhs.toString(sb);
//			}
//			sb.append(";");
//			if (rhs.precedence() < precedence()) {
//				sb.append("@(");
//				rhs.toString(sb);
//				sb.append(")");
//			} else {
//				rhs.toString(sb);
//			}
//		}
//
//		@Override
//		public void countUsages(Consumer<Var> countUsage) {
//			lhs.countUsages(countUsage);
//			rhs.countUsages(countUsage);
//		}
//	}
//
//	public static class SolRegex implements Pipeline {
//		final Regex sol;
//
//		public SolRegex(Regex sol) {
//			this.sol = sol;
//		}
//
//		@Override
//		public int precedence() {
//			return 3;
//		}
//
//		@Override
//		public void toString(StringBuilder sb) {
//			sol.toString(sb);
//		}
//
//		@Override
//		public void countUsages(Consumer<Var> countUsage) {
//			sol.countUsages(countUsage);
//		}
//		@Override
//		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
//			return sol.toStringAutoWeightsAndAutoExponentials(sb,usagesLeft);
//		}
//	}
//}