package net.alagris.cli.conv;

import net.alagris.core.Pair;
import net.alagris.core.Util;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;
import java.util.function.Predicate;

public class Compiler {


    public static String toStringAutoWeightsAndAutoExponentials(Predicate<EncodedID> export, boolean nonfunc, Map<EncodedID, Solomonoff> vars, Map<Integer, Solomonoff> auxiliaryVars) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<EncodedID, StringifierMeta> meta = buildMeta(export, vars);
        final DirectedAcyclicGraph<EncodedID, Object> dependencyOf = buildDependencyGraph(vars, auxiliaryVars);
        removeUnused(export, dependencyOf);
        countUsages(vars, auxiliaryVars, meta, dependencyOf);

        final TopologicalOrderIterator<EncodedID, Object> dependencyOrder = new TopologicalOrderIterator<>(dependencyOf);
        class Stringifier implements Solomonoff.SolStringifier {
            final HashSet<Integer> usedGroupIndices = new HashSet<>();
            @Override
            public StringifierMeta usagesLeft(EncodedID id) {
                final StringifierMeta k = meta.get(id);
                assert k.usagesLeft >= 0;
                assert k.weights != null : id + "\n" + sb;
                usedGroupIndices.addAll(k.usedGroupIndices);
                return k;
            }

            @Override
            public void useSubmatch(int groupIndex) {
                usedGroupIndices.add(groupIndex);
            }
        }
        while (dependencyOrder.hasNext()) {
            final EncodedID id = dependencyOrder.next();
            final Stringifier str = new Stringifier();
            if(id.state == VarState.LAZY){
                final Solomonoff sol = auxiliaryVars.get(idToAuxiliaryVar(id));

                sb.append(id).append(" = ");
                if(nonfunc){
                    sb.append("nonfunc ");
                }
                final int finalGroupIndex;
                if(sol instanceof SolSubmatch){
                    final SolSubmatch sm = (SolSubmatch) sol;
                    sm.nested.toStringAutoWeightsAndAutoExponentials(sb, str);
                    finalGroupIndex = sm.groupIndex;
                }else {
                    sol.toStringAutoWeightsAndAutoExponentials(sb, str);
                    finalGroupIndex = -1;
                }
                assert !str.usedGroupIndices.contains(finalGroupIndex);
                if(!str.usedGroupIndices.isEmpty()) {
                    sb.append("; { ");
                    for (int groupIndex : str.usedGroupIndices) {
                        sb.append(groupIndex).append(" -> ").append(auxiliaryVarToID(groupIndex)).append(" ");
                    }
                    sb.append("}");
                }
                if(finalGroupIndex>-1){
                    sb.append(" ; ").append(auxiliaryVarToID(finalGroupIndex));

                }
            }else{
                final StringifierMeta varMeta = meta.get(id);
                assert varMeta.usagesLeft > 0;
                final Solomonoff sol = vars.get(id);
                if (nonfunc) {
                    sb.append("nonfunc ");
                }
                sb.append(id).append(" = ");
                varMeta.weights = sol.toStringAutoWeightsAndAutoExponentials(sb, str);
                varMeta.usedGroupIndices = str.usedGroupIndices;

                assert varMeta.usagesLeft >= 0;
                if(export.test(id)){
                    sb.append("\n");
                    assert varMeta.usagesLeft > 0;

                    sb.append("@").append(id).append(" = ");
                    varMeta.usagesLeft--;
                    if(varMeta.usagesLeft>0) {
                        sb.append("!!");
                    }
                    sb.append(id);
                    if(!str.usedGroupIndices.isEmpty()) {
                        sb.append("; { ");
                        for (int groupIndex : str.usedGroupIndices) {
                            sb.append(groupIndex).append(" -> ").append(auxiliaryVarToID(groupIndex)).append(" ");
                        }
                        sb.append(" }");
                    }
                }
            }
            sb.append("\n");
        }
        assert Util.forall(meta.values(),m->m.usagesLeft==0);
        return sb.toString();
    }

    static HashMap<EncodedID, StringifierMeta> buildMeta(Predicate<EncodedID> export, Map<EncodedID, Solomonoff> vars) {
        final HashMap<EncodedID, StringifierMeta> meta = new HashMap<>(vars.size());
        for (Map.Entry<EncodedID, Solomonoff> e : vars.entrySet()) {
            meta.put(e.getKey(), new StringifierMeta(export.test(e.getKey()) ? 1 : 0));
        }
        return meta;
    }

    static EncodedID auxiliaryVarToID(int groupIndex) {
        return new EncodedID("_" + groupIndex, VarState.LAZY);
    }

    static int idToAuxiliaryVar(EncodedID auxiliaryVar) {
        return Integer.parseInt(auxiliaryVar.id.substring(1));
    }

    static DirectedAcyclicGraph<EncodedID, Object> buildDependencyGraph(Map<EncodedID, Solomonoff> vars, Map<Integer, Solomonoff> auxiliaryVars) {
        final DirectedAcyclicGraph<EncodedID, Object> dependencyOf = new DirectedAcyclicGraph<>(null, null, false);
        for (EncodedID e : vars.keySet()) {
            dependencyOf.addVertex(e);
        }
        for (Integer e : auxiliaryVars.keySet()) {
            dependencyOf.addVertex(auxiliaryVarToID(e));
        }
        class Walker implements Solomonoff.SolWalker<Void> {
            private final EncodedID id;

            Walker(EncodedID id) {

                this.id = id;
            }

            @Override
            public Void atomicVar(AtomicVar var) {
                dependencyOf.addEdge(var, id, new Object());
                return null;
            }

            @Override
            public Void submatch(SolSubmatch sub) {
                dependencyOf.addEdge(auxiliaryVarToID(sub.groupIndex), id, new Object());
                return null;
            }
        }
        for (Map.Entry<EncodedID, Solomonoff> e : vars.entrySet()) {
            final EncodedID id = e.getKey();
            e.getValue().walk(new Walker(id));
        }
        for (Map.Entry<Integer, Solomonoff> e : auxiliaryVars.entrySet()) {
            final EncodedID id = auxiliaryVarToID(e.getKey());
            e.getValue().walk(new Walker(id));
        }
        return dependencyOf;
    }

    static DirectedAcyclicGraph<EncodedID, Object> buildDependencyGraphKol(Map<EncodedID, Kolmogorov> vars) {
        final DirectedAcyclicGraph<EncodedID, Object> dependencyOf = new DirectedAcyclicGraph<>(null, null, false);
        for (EncodedID e : vars.keySet()) {
            dependencyOf.addVertex(e);
        }
        for (Map.Entry<EncodedID, Kolmogorov> e : vars.entrySet()) {
            final EncodedID id = e.getKey();
            e.getValue().forEachVar(var-> dependencyOf.addEdge(var, id, new Object()));
        }
        return dependencyOf;
    }

    static void countUsages(Map<EncodedID, Solomonoff> vars,
                            Map<Integer, Solomonoff> auxiliaryVars,
                            HashMap<EncodedID, StringifierMeta> meta,
                            DirectedAcyclicGraph<EncodedID, Object> dependencyOf) {
        for (EncodedID id : dependencyOf.vertexSet()) {
            final Solomonoff sol;
            if (id.state == VarState.LAZY) {
                sol = auxiliaryVars.get(idToAuxiliaryVar(id));
            } else {
                sol = vars.get(id);
            }
            sol.walk(new Solomonoff.SolWalker<Void>() {
                @Override
                public Void atomicVar(AtomicVar var) {
                    final StringifierMeta m = meta.get(var);
                    m.increment();
                    assert dependencyOf.containsVertex(var) : var + " " + id;
                    return null;
                }

                @Override
                public Void submatch(SolSubmatch sub) {
                    return null;
                }
            });
        }
    }

    static void removeUnused(Predicate<EncodedID> export, DirectedAcyclicGraph<EncodedID, Object> dependencyOf) {
        while (true) {
            final HashSet<EncodedID> toRemove = new HashSet<>();
            for (EncodedID vertex : dependencyOf.vertexSet()) {
                if (!export.test(vertex) && dependencyOf.outDegreeOf(vertex) == 0) {
                    toRemove.add(vertex);
                }
            }
            if (toRemove.isEmpty()) break;
            for (EncodedID vertex : toRemove) {
                final boolean b = dependencyOf.removeVertex(vertex);
                assert b;
            }
        }
    }

    public static <M extends Map<EncodedID, Kolmogorov>> M defineMissingRegexesRequiredByActions(M vars) {
        final Stack<AtomicVar> missingToBeDefined = new Stack<>();
        for (Map.Entry<EncodedID, Kolmogorov> var : vars.entrySet()) {
            var.getValue().forEachVar(v -> {
                if (!vars.containsKey(v)) {
                    missingToBeDefined.push(v);
                }
            });
        }
        while (!missingToBeDefined.isEmpty()) {
            final AtomicVar var = missingToBeDefined.pop();
            if(vars.containsKey(var))continue;
            assert var.state != VarState.NONE;
            final EncodedID originaID = new EncodedID(var, VarState.NONE);
            final Kolmogorov originalKolm = vars.get(originaID);
            assert originalKolm != null : originaID + " " + var.encodeID();
            final Kolmogorov newKolm = var.state.actOn(originalKolm);
            final Kolmogorov prev = vars.put(var, newKolm);
            assert prev==null;
            newKolm.forEachVar(v -> {
                if (!vars.containsKey(v)) {
                    missingToBeDefined.push(v);
                }
            });
        }
        return vars;
    }

    public static Pair<HashMap<EncodedID, Solomonoff>, HashMap<Integer, Solomonoff>>
    toSolomonoff(Map<EncodedID, Kolmogorov> vars) {
        final DirectedAcyclicGraph<EncodedID, Object> dependencyOf = buildDependencyGraphKol(vars);
        final HashMap<EncodedID, Solomonoff> sol = new LinkedHashMap<>();
        final HashMap<Integer, Solomonoff> auxiliaryGroupVars = new HashMap<>();
        final TopologicalOrderIterator<EncodedID, Object> iter = new TopologicalOrderIterator<>(dependencyOf);
        while (iter.hasNext()) {
            final EncodedID encodedID = iter.next();
            final Kolmogorov kol = vars.get(encodedID);
            final VarQuery q = new VarQuery() {

                @Override
                public Kolmogorov variableAssignment(EncodedID id) {
                    return vars.get(id);
                }

                @Override
                public Solomonoff variableDefinitions(EncodedID id) {
                    return sol.get(id);
                }

                @Override
                public int introduceAuxiliaryVar(Solomonoff definition) {
                    final int id = auxiliaryGroupVars.size() + 1;
                    auxiliaryGroupVars.put(id, definition);
                    return id;
                }
            };
            final Solomonoff s = kol.toSolomonoff(q);
            assert s.validateSubmatches(q)!=0;
            sol.put(encodedID, s);
        }
        return Pair.of(sol, auxiliaryGroupVars);
    }


    public static String compileSolomonoff(boolean exportAll, boolean nonfunc, ThraxParser<?, ?> parser) {
        final LinkedHashMap<EncodedID, Kolmogorov> kol = toKolmogorov(parser.globalVars);
        final HashSet<String> export = parser.fileImportHierarchy.peek().export;
        final LinkedHashMap<EncodedID, Kolmogorov> afterActions = defineMissingRegexesRequiredByActions(
                kol);
        final Pair<HashMap<EncodedID, Solomonoff>, HashMap<Integer, Solomonoff>> sol = toSolomonoff(afterActions);
        final String str = toStringAutoWeightsAndAutoExponentials(id->exportAll||(id.state==VarState.NONE && export.contains(id.id)), nonfunc, sol.l(),sol.r());
        return str;
    }

    public static LinkedHashMap<EncodedID, Kolmogorov> toKolmogorov(LinkedHashMap<String, ThraxParser.V> globalVars) {
        final LinkedHashMap<EncodedID, Kolmogorov> kol = new LinkedHashMap<>();
        for (Map.Entry<String, ThraxParser.V> e : globalVars.entrySet()) {
            final String id = e.getKey();
            final PushedBack compiled = e.getValue().re.toKolmogorov(varid -> {
                final EncodedID encodedVarId = new EncodedID(varid, VarState.NONE);
                final Kolmogorov kolm = kol.get(encodedVarId);
                assert kolm != null : id + " " + encodedVarId + " " + varid + " " + globalVars.entrySet() + " " + kol;
                return kolm;
            });
            final Kolmogorov kolm = compiled.finish();
            kol.put(new EncodedID(id, VarState.NONE), kolm);
        }
        return kol;
    }
}
