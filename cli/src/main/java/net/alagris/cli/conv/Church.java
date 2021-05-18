package net.alagris.cli.conv;

import java.util.function.Function;

public interface Church {

    public Church substituteCh(Function<ChVar, Church> argMap);

    public int precedence();

    public void toString(StringBuilder sb);

    interface TranslationQueries {
        Kolmogorov resolve(String id);

        default PushedBack resolveFreeVariable(ChVar i) {
            final Kolmogorov ref = resolve(i.id);
            final Kolmogorov var = PushedBack.var(i.id, ref);
            return PushedBack.wrap(var);
        }
    }

    public PushedBack toKolmogorov(TranslationQueries queries);

    public static class ChVar implements Church {
        final String id;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void toString(StringBuilder sb) {
            sb.append(id);
        }

        public ChVar(String id) {
            this.id = id;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
            return argMap.apply(this);
        }

        @Override
        public PushedBack toKolmogorov(TranslationQueries queries) {
            return queries.resolveFreeVariable(this);
        }

        @Override
        public int precedence() {
            return Integer.MAX_VALUE;
        }
    }

    public static class ChDiff implements Church {
        final Church lhs, rhs;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        public ChDiff(Church lhs, Church rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void toString(StringBuilder sb) {
            sb.append("cdrewrite[");
            replacement.toString(sb);
            sb.append(",");
            leftCntx.toString(sb);
            sb.append(",");
            rightCntx.toString(sb);
            sb.append(",");
            sigmaStar.toString(sb);
            sb.append("]");
        }

        @Override
        public int precedence() {
            return Integer.MAX_VALUE;
        }

        public ChCdRewrite(Church sigmaStar, final Church rightCntx, final Church leftCntx, final Church replacement) {
            this.sigmaStar = sigmaStar;
            this.rightCntx = rightCntx;
            this.leftCntx = leftCntx;
            this.replacement = replacement;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
            final Church subSigmaStar = sigmaStar.substituteCh(argMap);
            final Church subRightCntx = rightCntx.substituteCh(argMap);
            final Church subLeftCntx = leftCntx.substituteCh(argMap);
            final Church subReplacement = replacement.substituteCh(argMap);
            if (sigmaStar == subSigmaStar && subRightCntx == rightCntx && subLeftCntx == leftCntx
                    && subReplacement == replacement)
                return this;
            return new ChCdRewrite(subSigmaStar, subRightCntx, subLeftCntx, subReplacement);
        }

        @Override
        public PushedBack toKolmogorov(TranslationQueries queries) {
            final PushedBack sigmaStar = this.sigmaStar.toKolmogorov(queries);
            return replacement.toKolmogorov(queries).cdrewrite(leftCntx.toKolmogorov(queries),
                    rightCntx.toKolmogorov(queries), sigmaStar);
        }
    }

    public static class ChIdentity implements Church {
        final Church lhs;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        public ChIdentity(Church lhs) {
            this.lhs = lhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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
	/*
	public static class StringFile implements Church, Kolmogorov, Solomonoff {
		final String path;
		final Atomic.VarState state;
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			toString(sb);
			return sb.toString();
		}
		@Override
		public void toString(StringBuilder sb) {
			sb.append("stringFile!('");
			sb.append(path);
			sb.append("')");
		}

		@Override
		public int precedence() {
			return Integer.MAX_VALUE;
		}

		public StringFile(String path) {
			this(path, VarState.NONE);
		}
		
		public StringFile(String path, Atomic.VarState state) {
			this.path = path;
			this.state = state;
		}

		@Override
		public Church substituteCh(Function<ChVar, Church> argMap) {
			return this;
		}

		@Override
		public PushedBack toKolmogorov(TranslationQueries queries) {
			return PushedBack.wrap(this);
		}
		@Override
		public Solomonoff toSolomonoff(Function<Var, Kolmogorov> variableAssignment,
				Function<String, Solomonoff> variableDefinitions) {
			return this;
		}
		@Override
		public void forEachVar(Consumer<Var> variableAssignment) {
		}
		@Override
		public IntSeq representative(Function<Var, Kolmogorov> variableAssignment) {
			
			return null;
		}
		@Override
		public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap) {
			return null;
		}
		@Override
		public Kolmogorov inv() {
			return null;
		}
		@Override
		public boolean producesOutput() {
			return false;
		}
		@Override
		public boolean readsInput() {
			return false;
		}
		@Override
		public Kolmogorov clearOutput() {
			return null;
		}
		@Override
		public void countUsages(Consumer<Var> countUsage) {
			// TODO Auto-generated method stub
			
		}
		@Override
		public Weights toStringAutoWeightsAndAutoExponentials(StringBuilder sb, Function<String, VarMeta> usagesLeft) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	*/

    public static class ChClearOutput implements Church {
        final Church lhs;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void toString(StringBuilder sb) {
            sb.append("clearOutput[");
            lhs.toString(sb);
            sb.append("]");
        }

        @Override
        public int precedence() {
            return Integer.MAX_VALUE;
        }

        public ChClearOutput(Church lhs) {
            this.lhs = lhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
            final Church subLhs = lhs.substituteCh(argMap);
            if (subLhs == lhs)
                return this;
            return new ChClearOutput(subLhs);
        }

        @Override
        public PushedBack toKolmogorov(TranslationQueries queries) {
            return lhs.toKolmogorov(queries).clearOutput();
        }
    }

    public static class ChRefl implements Church {
        final Atomic.Set set;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void toString(StringBuilder sb) {
            sb.append(":<0>");
            set.toString(sb);
        }

        @Override
        public int precedence() {
            return Integer.MAX_VALUE;
        }

        public ChRefl(Atomic.Set set) {
            this.set = set;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
            return this;
        }

        @Override
        public PushedBack toKolmogorov(TranslationQueries queries) {
            return PushedBack.wrap(new KolRefl(set));
        }
    }

    public static class ChComp implements Church {
        final Church lhs, rhs;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        @Override
        public int precedence() {
            return 0;
        }

        public ChComp(Church lhs, Church rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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
            sb.append(" | ");
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
            return 2;
        }

        public ChUnion(Church lhs, Church rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        public ChConcat(Church lhs, Church rhs) {
            this.lhs = lhs;
            this.rhs = rhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
        }

        @Override
        public void toString(StringBuilder sb) {
            sb.append(":");
            if (rhs instanceof Atomic) {
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

        public ChProd(Church rhs) {
            this.rhs = rhs;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        @Override
        public int precedence() {
            return 4;
        }

        public ChKleene(Church lhs, char type) {
            this.lhs = lhs;
            this.type = type;
            assert type == '*' || type == '?' || type == '+';
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

    /**
     * Concatenates exactly n times
     */
    public static class ChPow implements Church {
        final Church lhs;
        final int power;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        @Override
        public int precedence() {
            return 4;
        }

        public ChPow(Church lhs, int power) {
            this.lhs = lhs;
            this.power = power;
            assert power >= 0;
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
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

    /**
     * Concatenates n times or less (optional concatenation)
     */
    public static class ChLePow implements Church {
        final Church lhs;
        final int power;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        @Override
        public int precedence() {
            return 4;
        }

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
        public Church substituteCh(Function<ChVar, Church> argMap) {
            final Church subLhs = lhs.substituteCh(argMap);
            if (subLhs == lhs)
                return this;
            return new ChLePow(subLhs, power);
        }

    }

    public static class ChInv implements Church {
        final Church lhs;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            toString(sb);
            return sb.toString();
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

        @Override
        public int precedence() {
            return 4;
        }

        public ChInv(Church lhs) {
            this.lhs = lhs;
        }

        @Override
        public PushedBack toKolmogorov(TranslationQueries queries) {
            return lhs.toKolmogorov(queries).inv();
        }

        @Override
        public Church substituteCh(Function<ChVar, Church> argMap) {
            final Church subLhs = lhs.substituteCh(argMap);
            if (subLhs == lhs)
                return this;
            return new ChInv(subLhs);
        }
    }

}
