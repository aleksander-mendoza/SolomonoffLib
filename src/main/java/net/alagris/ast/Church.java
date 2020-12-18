package net.alagris.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

public interface Church {

	public Church substituteCh(HashMap<String, Church> argMap);

	interface TranslationQueries {
		PushedBack resolveFreeVariable(ChVar var);
	}

	public PushedBack toKolmogorov(TranslationQueries queries);

	public static class ChVar implements Church {
		final String id;

		public ChVar(String id) {
			this.id = id;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			return argMap.getOrDefault(id,this);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return queries.resolveFreeVariable(this);
		}
	}

	public static class ChDiff implements Church {
		final Church lhs, rhs;

		public ChDiff(Church lhs, Church rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subRhs = rhs.substituteCh(argMap);
			final Church subLhs = lhs.substituteCh(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return new ChDiff(subLhs, subRhs);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).diff(rhs.toKolmogorov(queries));
		}
	}

	public static class ChCdRewrite implements Church {
		private Church sigmaStar;
		private Church rightCntx;
		private Church leftCntx;
		private Church replacement;

		public ChCdRewrite(Church sigmaStar, final Church rightCntx, final Church leftCntx, final Church replacement) {
			this.sigmaStar = sigmaStar;
			this.rightCntx = rightCntx;
			this.leftCntx = leftCntx;
			this.replacement = replacement;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subSigmaStar = sigmaStar.substituteCh(argMap);
			final Church subRightCntx = rightCntx.substituteCh(argMap);
			final Church subLeftCntx = leftCntx.substituteCh(argMap);
			final Church subReplacement = replacement.substituteCh(argMap);
			if (sigmaStar == subSigmaStar && subRightCntx == rightCntx && subLeftCntx==leftCntx && subReplacement== replacement)
				return this;
			return new ChCdRewrite(subSigmaStar,subRightCntx,subLeftCntx,subReplacement);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return replacement.toKolmogorov(queries).cdrewrite(leftCntx.toKolmogorov(queries), rightCntx.toKolmogorov(queries), sigmaStar.toKolmogorov(queries));
		}
	}

	public static class ChIdentity implements Church {
		final Church lhs;

		public ChIdentity(Church lhs) {
			this.lhs = lhs;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subLhs = lhs.substituteCh(argMap);
			if (subLhs == lhs)
				return this;
			return new ChIdentity(subLhs);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).identity();
		}
	}

	public static class ChComp implements Church {
		final Church lhs, rhs;

		public ChComp(Church lhs, Church rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subRhs = rhs.substituteCh(argMap);
			final Church subLhs = lhs.substituteCh(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return new ChComp(subLhs, subRhs);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).comp(rhs.toKolmogorov(queries));
		}
	}

	public static class ChUnion implements Church {
		final Church lhs, rhs;

		public ChUnion(Church lhs, Church rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subRhs = rhs.substituteCh(argMap);
			final Church subLhs = lhs.substituteCh(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return new ChUnion(subLhs, subRhs);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).union(rhs.toKolmogorov(queries));
		}
	}

	public static class ChConcat implements Church {
		final Church lhs, rhs;

		public ChConcat(Church lhs, Church rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subRhs = rhs.substituteCh(argMap);
			final Church subLhs = lhs.substituteCh(argMap);
			if (subRhs == rhs && subLhs == lhs)
				return this;
			return new ChConcat(subLhs, subRhs);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).concat(rhs.toKolmogorov(queries));
		}
	}

	public static class ChProd implements Church {
		final Church rhs;

		public ChProd(Church rhs) {
			this.rhs = rhs;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subRhs = rhs.substituteCh(argMap);
			if (subRhs == rhs)
				return this;
			return new ChProd(subRhs);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return rhs.toKolmogorov(queries).prod();
		}
	}

	public static class ChKleene implements Church {
		final Church lhs;
		final char type;

		public ChKleene(Church lhs, char type) {
			this.lhs = lhs;
			this.type = type;
			assert type == '*' || type == '?' || type == '+';
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subLhs = lhs.substituteCh(argMap);
			if (subLhs == lhs)
				return this;
			return new ChKleene(subLhs, type);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).kleene(type);
		}
	}

	/** Concatenates exactly n times */
	public static class ChPow implements Church {
		final Church lhs;
		final int power;

		public ChPow(Church lhs, int power) {
			this.lhs = lhs;
			this.power = power;
			assert power >= 0;
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subLhs = lhs.substituteCh(argMap);
			if (subLhs == lhs)
				return this;
			return new ChPow(subLhs, power);
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).pow(power);
		}
	}

	/** Concatenates n times or less (optional concatenation) */
	public static class ChLePow implements Church {
		final Church lhs;
		final int power;

		public ChLePow(Church lhs, int power) {
			this.lhs = lhs;
			this.power = power;
			assert power >= 0;
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).powLe(power);
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subLhs = lhs.substituteCh(argMap);
			if (subLhs == lhs)
				return this;
			return new ChLePow(subLhs, power);
		}

	}

	public static class ChInv implements Church {
		final Church lhs;

		public ChInv(Church lhs) {
			this.lhs = lhs;
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return lhs.toKolmogorov(queries).inv();
		}

		@Override
		public Church substituteCh(HashMap<String, Church> argMap) {
			final Church subLhs = lhs.substituteCh(argMap);
			if (subLhs == lhs)
				return this;
			return new ChInv(subLhs);
		}
	}

}
