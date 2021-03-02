package net.alagris.cli.conv;

import net.alagris.cli.conv.Atomic.Set;
import net.alagris.cli.conv.Atomic.Str;
import net.alagris.core.IntSeq;
import net.alagris.core.NullTermIter;
import net.alagris.core.Pair;
import net.alagris.core.Specification.Range;

import java.util.ArrayList;

public interface Optimise {

    public static Str str(IntSeq seq) {
        return seq.size() == 1 ? new AtomicChar(seq.at(0)) : new AtomicStr(seq);
    }

    public static Church strRefl(IntSeq seq) {
        if (seq.size() == 1) {
            return new Church.ChRefl(new AtomicChar(seq.at(0)));
        } else {
            final AtomicStr s = new AtomicStr(seq);
            return new Church.ChConcat(s, new Church.ChProd(s));
        }
    }

    public static Set ranges(NullTermIter<Pair<Integer, Integer>> ranges) {
        Pair<Integer, Integer> r;
        ArrayList<Range<Integer, Boolean>> set = Kolmogorov.SPECS.makeEmptyRanges(false);
        while ((r = ranges.next()) != null) {
            Kolmogorov.SPECS.insertRange(set, r.l(), r.r(), false, true, (a, b) -> a || b);
        }
        return new AtomicSet(set);
    }

    public static Set range(int rangeFromExclusive,
                            int rangeToInclusive) {
        assert rangeFromExclusive < rangeToInclusive;
        return new AtomicSet(Kolmogorov.SPECS.makeSingletonRanges(true, false, rangeFromExclusive, rangeToInclusive));
    }

    public static Set rangeRefl(int rangeFromExclusive,
                                int rangeToInclusive) {
        assert rangeFromExclusive < rangeToInclusive;
        return new AtomicSet(Kolmogorov.SPECS.makeSingletonRanges(true, false, rangeFromExclusive, rangeToInclusive));
    }

    public static Solomonoff power(Solomonoff sol, int power) {
        if (power == 0)
            return Atomic.EPSILON;
        Solomonoff pow = sol;
        for (int i = 1; i < power; i++) {

            pow = concat(pow, sol);
        }
        return pow;
    }

    public static Solomonoff powerOptional(Solomonoff sol, int power) {
        if (power == 0)
            return Atomic.EPSILON;
        Solomonoff pow = new SolKleene(sol, '?');
        for (int i = 1; i < power; i++) {
            pow = concat(pow, new SolKleene(sol, '?'));
        }
        return pow;
    }

    public static Solomonoff concat(Solomonoff lhs, Solomonoff rhs) {
        if (lhs instanceof Str && rhs instanceof Str) {
            return str(((Str) lhs).str().concat(((Str) rhs).str()));
        } else if (lhs instanceof Str && ((Str) lhs).str().isEmpty()) {
            return rhs;
        } else if (rhs instanceof Str && ((Str) rhs).str().isEmpty()) {
            return lhs;
        } else if (rhs instanceof SolConcat) {
            final SolConcat r = (SolConcat) rhs;
            // TODO rewrite it as loop, instead of recursion. This way stack won't blow up
            return new SolConcat(concat(lhs, r.lhs), r.rhs);
        } else {
            return new SolConcat(lhs, rhs);
        }
    }

    public static Solomonoff union(Solomonoff lhs, Solomonoff rhs) {
        if (rhs instanceof SolUnion) {
            final SolUnion u = (SolUnion) rhs;
            return new SolUnion(union(lhs, u.lhs), u.rhs);
        } else {
            return new SolUnion(lhs, rhs);
        }
    }
//	public static Pipeline or(Pipeline lhs, Pipeline rhs) {
//		if(rhs instanceof SolOr){
//			final SolOr u = (SolOr) rhs; 
//			return new Pipeline.SolOr(or(lhs, u.lhs),u.rhs);
//		} else {
//			return new Pipeline.SolOr(lhs, rhs);
//		}
//	}
//	public static Pipeline lazyComp(Pipeline lhs, Pipeline rhs) {
//		if(rhs instanceof SolLazyComp){
//			final SolLazyComp u = (SolLazyComp) rhs; 
//			return new Pipeline.SolLazyComp(lazyComp(lhs, u.lhs),u.rhs);
//		} else {
//			return new Pipeline.SolLazyComp(lhs, rhs);
//		}
//	}
//	public static Pipeline and(Pipeline lhs, Pipeline rhs) {
//		if(rhs instanceof SolAnd){
//			final SolAnd u = (SolAnd) rhs; 
//			return new Pipeline.SolAnd(and(lhs, u.lhs),u.rhs);
//		} else {
//			return new Pipeline.SolAnd(lhs, rhs);
//		}
//	}

    public static Kolmogorov union(Kolmogorov lhs, Kolmogorov rhs) {
        if (lhs instanceof Set && rhs instanceof Set) {
            final Set l = (Set) lhs;
            final Set r = (Set) rhs;
            return new AtomicSet(Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a || b));
        }
        if (lhs instanceof KolRefl && rhs instanceof KolRefl) {
            final KolRefl l = (KolRefl) lhs;
            final KolRefl r = (KolRefl) rhs;
            return new KolRefl(new AtomicSet(Atomic.composeSets(l.set.ranges(), r.set.ranges(), (a, b) -> a || b)));
        }
        if (lhs instanceof KolProd && rhs instanceof KolProd) {
            final KolProd l = (KolProd) lhs;
            final KolProd r = (KolProd) rhs;
            return new KolProd(union(l, r));
        }
        if (lhs instanceof KolUnion) {
            final KolUnion l = (KolUnion) lhs;
            if (l.lhs.compositionHeight() > 1 && l.rhs.compositionHeight() == 1) {
                assert !(l.lhs instanceof KolUnion);
                return new KolUnion(l.lhs, union(l.rhs, rhs));
            }
        }
        if (rhs instanceof KolUnion) {
            final KolUnion r = (KolUnion) rhs;
            assert r.lhs.compositionHeight() != 1 || r.rhs.compositionHeight() <= 1 || !(r.rhs instanceof KolUnion);
            if (lhs.compositionHeight() == 1) {
                return new KolUnion(union(lhs, r.lhs), r.rhs);
            }
        }

        return new KolUnion(lhs, rhs);
    }

    public static Kolmogorov concat(Kolmogorov lhs, Kolmogorov rhs) {
        if (lhs instanceof KolRefl && ((KolRefl) lhs).set instanceof AtomicChar) {
            final AtomicChar l = (AtomicChar) ((KolRefl) lhs).set;
            if (rhs instanceof KolRefl && ((KolRefl) rhs).set instanceof AtomicChar) {
                final AtomicChar r = (AtomicChar) ((KolRefl) rhs).set;
                final Kolmogorov str = str(new IntSeq(l.character, r.character));
                return new KolConcat(str, new KolProd(str));
            } else {
                final Kolmogorov str = str(new IntSeq(l.character));
                lhs = new KolConcat(str, new KolProd(str));
            }
        }
        if (rhs instanceof KolRefl && ((KolRefl) rhs).set instanceof AtomicChar) {
            final AtomicChar r = (AtomicChar) ((KolRefl) rhs).set;
            final Kolmogorov str = str(new IntSeq(r.character));
            rhs = new KolConcat(str, new KolProd(str));
        }
        if (lhs instanceof Str && rhs instanceof Str) {
            return str(((Str) lhs).str().concat(((Str) rhs).str()));
        }
        if (lhs instanceof Str && ((Str) lhs).str().isEmpty()) {
            return rhs;
        }
        if (rhs instanceof Str && ((Str) rhs).str().isEmpty()) {
            return lhs;
        }
        if (lhs instanceof KolProd && rhs instanceof KolProd) {
            final KolProd l = (KolProd) lhs;
            final KolProd r = (KolProd) rhs;
            return new KolProd(concat(l, r));
        }
        if (lhs instanceof KolConcat) {
            final KolConcat l = (KolConcat) lhs;
            if (l.lhs.compositionHeight() > 1 && l.rhs.compositionHeight() == 1) {
                assert !(l.lhs instanceof KolConcat);
                return new KolConcat(l.lhs, concat(l.rhs, rhs));
            }
        }
        if (rhs instanceof KolConcat) {
            final KolConcat r = (KolConcat) rhs;
            assert r.lhs.compositionHeight() != 1 || r.rhs.compositionHeight() <= 1 || !(r.rhs instanceof KolConcat);
            if (lhs.compositionHeight() == 1) {
                return new KolConcat(concat(lhs, r.lhs), r.rhs);
            }
        }
        return new KolConcat(lhs, rhs);
    }

    public static Kolmogorov prod(Kolmogorov lhs) {
        if (lhs.readsInput()) {
            return new KolProd(lhs);
        } else {
            return Atomic.EPSILON;
        }
    }

    public static Kolmogorov inv(Kolmogorov lhs) {
        if (lhs instanceof KolInv) {
            return ((KolInv) lhs).lhs;
        } else {
            return new KolInv(lhs);
        }
    }

    public static Set diffSets(Set l, Set r) {
        final ArrayList<Range<Integer, Boolean>> u = Atomic.composeSets(l.ranges(), r.ranges(), (a, b) -> a && !b);
        if (u.size() == 2 && !u.get(0).edges()) {
            assert u.get(1).input().equals(Kolmogorov.SPECS.maximal());
            assert u.get(1).edges();
            return new AtomicChar(Kolmogorov.SPECS.maximal());
        } else if (u.size() == 3 && u.get(1).edges() && u.get(0).input().equals(u.get(1).input() - 1)) {
            assert u.get(2).input().equals(Kolmogorov.SPECS.maximal());
            assert !u.get(2).edges();
            assert !u.get(0).edges();
            return new AtomicChar(u.get(1).input());
        }
        return new AtomicSet(u);
    }

    public static Kolmogorov diff(Kolmogorov lhs, Kolmogorov rhs) {
        if (lhs instanceof Set && rhs instanceof Set) {
            return diffSets((Set) lhs, (Set) rhs);
        } else if (lhs instanceof KolRefl && rhs instanceof Set) {
            return new KolRefl(diffSets(((KolRefl) lhs).set, (Set) rhs));
        } else if (lhs instanceof Set && rhs instanceof KolRefl) {
            return diffSets((Set) lhs, ((KolRefl) rhs).set);
        } else if (lhs instanceof KolRefl && rhs instanceof KolRefl) {
            return new KolRefl(diffSets(((KolRefl) lhs).set, ((KolRefl) rhs).set));
        } else {
            return new KolDiff(lhs, rhs);
        }
    }

    public static Kolmogorov comp(Kolmogorov lhs, Kolmogorov rhs) {
        if (isIdentity(lhs)) return rhs;
        if (isIdentity(rhs)) return lhs;
        return new KolComp(lhs, rhs);
    }

    public static boolean isIdentity(Kolmogorov lhs) {
        if (lhs instanceof KolKleene) {
            final KolKleene k = (KolKleene) lhs;
            if (k.type == '*' && k.lhs instanceof KolRefl) {
                final KolRefl refl = (KolRefl) k.lhs;
                return refl.set.ranges() == Atomic.DOT;
            }
        }
        return false;
    }

    static Solomonoff kleene(Solomonoff sol,char type) {
        return new SolKleene(sol,type);
    }

    static Solomonoff refl(Solomonoff s) {
        assert s instanceof Atomic.Set;
        return new SolConcat(Atomic.REFLECT_PROD, s);
    }
}
