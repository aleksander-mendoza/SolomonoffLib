//package net.alagris.cli.conv;
//
//import net.alagris.core.Util;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Function;
//
//public class Composed {
//    final HashMap<Integer, EncodedID> cascade = new HashMap<>();
//    boolean isPlainRegex(){
//        return cascade.isEmpty();
//    }
//    public int maxOccupiedGroupIndex(){
//        int max = -1;
//        for(int group: cascade.keySet()){
//            if(group>max){
//                max = group;
//            }
//        }
//        return max;
//    }
//    public Solomonoff sol;
//
//    public Composed(Solomonoff sol) {
//        this.sol = sol;
//        assert assertInvariants();
//    }
//
//    boolean assertInvariants() {
//        sol.countUsages(var -> {
//            assert false : var;
//        });
//        return true;
//    }
//
//    public Composed(Composed copy) {
//        assert copy.assertInvariants();
//        sol = copy.sol;
//        cascade.putAll(copy.cascade);
//    }
//
//    public Composed union(Composed rs, VarQuery query) {
//        final Composed c = this;
//        c.turnGroupZeroIntoSubgroup();
//        rs.turnGroupZeroIntoSubgroup();
//        c.sol = Optimise.union(c.sol, rs.sol);
//        assert Collections.disjoint(c.cascade.keySet(), rs.cascade.keySet()) : c.cascade + " " + rs.cascade;
//        c.cascade.putAll(rs.cascade);
//        assert c.assertInvariants();
//        return c;
//    }
//
//    public Composed concat(Composed rs, VarQuery query) {
//        final Composed c = this;
//        c.turnGroupZeroIntoSubgroup();
//        rs.turnGroupZeroIntoSubgroup();
//        c.sol = Optimise.concat(c.sol, rs.sol);
//        c.cascade.putAll(rs.cascade);
//        assert c.assertInvariants();
//        return c;
//    }
//
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        toString(sb);
//        return sb.toString();
//    }
//
//    public Composed kleene(char type, VarQuery query) {
//        final Composed c = this;
//        c.turnGroupZeroIntoSubgroup();
//        c.sol = Optimise.kleene(c.sol, type);
//        assert c.assertInvariants();
//        return c;
//    }
//
//    public Composed inv(VarQuery query) {
//        final Composed c = this;
//        if (c.cascade.isEmpty()) {
//            c.sol = new SolFunc(new Solomonoff[]{c.sol}, "inverse");
//            assert c.assertInvariants();
//            return c;
//        }
//        throw new IllegalStateException("Cannot invert composed");
//    }
//
//    public Composed identity(VarQuery query) {
//        final Composed c = this;
//        assert c.cascade.isEmpty();
//        c.sol = new SolFunc(new Solomonoff[]{c.sol}, "identity");
//        assert c.assertInvariants();
//        return c;
//    }
//
//
//    public Composed refl(VarQuery query) {
//        final Composed c = this;
//        assert c.sol instanceof Atomic.Set;
//        c.sol = ;
//        assert c.assertInvariants();
//        return c;
//    }
//
//    public Composed diff(Composed rs, VarQuery query) {
//        final Composed c = this;
//        assert rs.cascade.isEmpty();
//        c.sol = new SolFunc(new Solomonoff[]{c.sol, rs.sol}, "subtract");
//        assert c.assertInvariants();
//        return c;
//    }
//
//    public Composed power(int power, VarQuery query) {
//        final Composed c = this;
//        c.turnGroupZeroIntoSubgroup();
//        c.sol = Optimise.power(c.sol, power);
//        assert c.assertInvariants();
//        return c;
//    }
////
////    private Composed unwrapVariablesContainingCompositions(VarQuery query) {
////        if (sol instanceof AtomicVar) {
////            final AtomicVar v = (AtomicVar) sol;
////            assert v.compositionHeight() >= 1;
////            if (v.compositionHeight() > 1) {
////                return copyVariablesContainingCompositions(query);
////            }
////        }
////        return this;
////    }
////
////    private Composed copyVariablesContainingCompositions(VarQuery query) {
////        if (sol instanceof AtomicVar) {
////            final AtomicVar v = (AtomicVar) sol;
////            assert v.compositionHeight() >= 1;
////            if (v.compositionHeight() > 1) {
////                final Composed copy = query.variableDefinitions(v).copyVariablesContainingCompositions(query);
////                assert disjointExceptZeroGroup(copy, this);
////                for (Map.Entry<Integer, Composed> e : cascade.entrySet()) {
////                    if (e.getKey() != 0) {
////                        final Composed prev = copy.cascade.put(e.getKey(), e.getValue());
////                        assert prev == null;
////                    } else {
////                        copy.comp(new Composed(e.getValue()));
////                    }
////                }
////                return copy;
////            }
////        }
////        return new Composed(this);
////    }
//
//    private static boolean disjointExceptZeroGroup(Composed a, Composed b) {
//        final HashSet<Integer> h = new HashSet<>(a.cascade.keySet());
//        h.remove(0);
//        return Collections.disjoint(h, b.cascade.keySet());
//    }
//
//    private void turnGroupZeroIntoSubgroup() {
//        if (cascade.containsKey(0)) {
//            final EncodedID groupZero = cascade.remove(0);
//            maxOccupiedGroupIndex++;
//            cascade.put(maxOccupiedGroupIndex, groupZero);
//            sol = new SolSubmatch(sol, maxOccupiedGroupIndex);
//        }
//    }
//
//    public Composed powerOptional(int power, VarQuery query) {
//        final Composed c = this;
//        c.sol = Optimise.powerOptional(c.sol, power);
//        assert c.assertInvariants();
//        return c;
//    }
//
//    public Composed comp(Composed rs) {
//        Composed lastLhs = this;
//        while (lastLhs.cascade.containsKey(0)) {
//            lastLhs = lastLhs.cascade.get(0);
//        }
//        lastLhs.cascade.put(0, rs);
//        return this;
//    }
//
//    public Composed clearOutput(VarQuery query) {
//        final Composed c = this;
//        assert c.cascade.isEmpty();
//        c.sol = new SolFunc(new Solomonoff[]{c.sol}, "clearOutput");
//        assert c.assertInvariants();
//        return c;
//    }
//
//    public void toStringAutoWeightsAndAutoExponentials(StringBuilder sb, boolean nonfunc, Function<EncodedID, StringifierMeta> usagesLeft) {
//        if (nonfunc) {
//            sb.append("nonfunc ");
//        }
//        sol.toStringAutoWeightsAndAutoExponentials(sb, usagesLeft);
//        if (!cascade.isEmpty()) {
//            if (Util.find(cascade.keySet(), i -> i != 0) != null) {
//                sb.append("; {\n");
//                for (Map.Entry<Integer, Composed> e : cascade.entrySet()) {
//                    if (e.getKey() != 0) {
//                        sb.append(e.getKey()).append(" -> ");
//                        e.getValue().toStringAutoWeightsAndAutoExponentials(sb, nonfunc, usagesLeft);
//                    }
//                }
//                sb.append("}");
//            }
//            final Composed next = cascade.get(0);
//            if (next != null) {
//                sb.append(";\n");
//                next.toStringAutoWeightsAndAutoExponentials(sb, nonfunc, usagesLeft);
//            }
//        }
//        sb.append("\n");
//    }
//
//    public void toString(StringBuilder sb) {
//        sol.toString(sb);
//        if (!cascade.isEmpty()) {
//            if (Util.find(cascade.keySet(), i -> i != 0) != null) {
//                sb.append("; {\n");
//                for (Map.Entry<Integer, Composed> e : cascade.entrySet()) {
//                    if (e.getKey() != 0) {
//                        sb.append(e.getKey()).append(" -> ");
//                        e.getValue().toString(sb);
//                    }
//                }
//                sb.append("}");
//            }
//            final Composed next = cascade.get(0);
//            if (next != null) {
//                sb.append(";\n");
//                next.toString(sb);
//            }
//        }
//        sb.append("\n");
//    }
//
//    public void countUsages(Consumer<AtomicVar> countUsage) {
//        sol.countUsages(countUsage);
//        for (Composed e : cascade.values()) {
//            e.countUsages(countUsage);
//        }
//    }
//}
