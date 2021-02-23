package net.alagris.cli.conv;

import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.util.*;

public class Compiler {


    public static String toStringAutoWeightsAndAutoExponentials(boolean exportAll,boolean nonfunc, Map<EncodedID, ASTMeta<Stacked>> vars) {
        final StringBuilder sb = new StringBuilder();
        final HashMap<EncodedID, StringifierMeta> meta = buildMeta(exportAll,vars);
        final DirectedAcyclicGraph<EncodedID, Object> dependencyOf = buildDependencyGraph(vars);
        removeUnused(vars, dependencyOf);
        countUsages(vars, meta, dependencyOf);

        final TopologicalOrderIterator<EncodedID, Object> dependencyOrder = new TopologicalOrderIterator<>(dependencyOf);
        while (dependencyOrder.hasNext()) {
            final EncodedID id = dependencyOrder.next();
            final StringifierMeta var = meta.get(id);
            assert var.usagesLeft > 0;
            final ASTMeta<Stacked> s = vars.get(id);
            if (s.re.compositionHeight()>1) {
                sb.append("@").append(id).append(" = \n");
                s.re.toStringAutoWeightsAndAutoExponentials(sb,nonfunc, j -> {
                    final StringifierMeta k = meta.get(j);
                    assert k.usagesLeft >= 0;
                    assert k.weights != null : j + "\n" + sb;
                    return k;
                });
            } else {
                assert s.re.compositionHeight() == 1;
                final Solomonoff sol = vars.get(id).re.get(0);
                if(nonfunc){
                    sb.append("nonfunc ");
                }
                sb.append(id).append(" = ");
                assert meta.containsKey(id) : id + " " + meta;
                var.weights = sol.toStringAutoWeightsAndAutoExponentials(sb, meta::get);
                sb.append("\n");
            }
            assert var.usagesLeft >= 0;
        }
        return sb.toString();
    }

    static HashMap<EncodedID, StringifierMeta> buildMeta(boolean exportAll, Map<EncodedID, ASTMeta<Stacked>> vars) {
        final HashMap<EncodedID, StringifierMeta> meta = new HashMap<>(vars.size());
        for (Map.Entry<EncodedID, ASTMeta<Stacked>> e : vars.entrySet()) {
            meta.put(e.getKey(), new StringifierMeta(e.getValue().export || exportAll ? 1 : 0));
        }
        return meta;
    }

    static DirectedAcyclicGraph<EncodedID, Object> buildDependencyGraph(Map<EncodedID, ASTMeta<Stacked>> vars) {
        final DirectedAcyclicGraph<EncodedID, Object> dependencyOf = new DirectedAcyclicGraph<>(null, null, false);
        for (Map.Entry<EncodedID, ASTMeta<Stacked>> e : vars.entrySet()) {
            dependencyOf.addVertex(e.getKey());
        }
        for (Map.Entry<EncodedID, ASTMeta<Stacked>> e : vars.entrySet()) {
            final EncodedID id = e.getKey();
            e.getValue().re.countUsages(reference -> dependencyOf.addEdge(reference, id, new Object()));
        }
        return dependencyOf;
    }

    static void countUsages(Map<EncodedID, ASTMeta<Stacked>> vars,
                            HashMap<EncodedID, StringifierMeta> meta,
                            DirectedAcyclicGraph<EncodedID, Object> dependencyOf) {
        for (EncodedID id : dependencyOf.vertexSet()) {
            vars.get(id).re.countUsages(reference -> {
                final StringifierMeta m = meta.get(reference);
                m.increment();
                assert dependencyOf.containsVertex(reference) : reference + " " + id;
            });
        }
    }

    static void removeUnused(Map<EncodedID, ASTMeta<Stacked>> vars, DirectedAcyclicGraph<EncodedID, Object> dependencyOf) {
        while (true) {
            final HashSet<EncodedID> toRemove = new HashSet<>();
            for (EncodedID vertex : dependencyOf.vertexSet()) {
                if (!vars.get(vertex).export && dependencyOf.outDegreeOf(vertex) == 0) {
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

    public static <M extends Map<EncodedID, ASTMeta<Kolmogorov>>> M defineMissingRegexesRequiredByActions(M vars){
        final Stack<AtomicVar> missingToBeDefined = new Stack<>();
        for(Map.Entry<EncodedID, ASTMeta<Kolmogorov>> var:vars.entrySet()) {
            var.getValue().re.forEachVar(v->{
                if(!vars.containsKey(v)) {
                    missingToBeDefined.push(v);
                }
            });
        }
        while(!missingToBeDefined.isEmpty()) {
            final AtomicVar var = missingToBeDefined.pop();
            assert var.state!=VarState.NONE;
            final EncodedID originaID = new EncodedID(var,VarState.NONE);
            final ASTMeta<Kolmogorov> originalKolm = vars.get(originaID);
            assert originalKolm!=null:originaID+" "+var.encodeID();
            final Kolmogorov newKolm  = var.state.actOn(originalKolm.re);
            vars.put(var, new ASTMeta<>(newKolm, false));
            newKolm.forEachVar(v->{
                if(!vars.containsKey(v)) {
                    missingToBeDefined.push(v);
                }
            });
        }
        return vars;
    }

    public static HashMap<EncodedID, ASTMeta<Stacked>> toSolomonoff(Map<EncodedID, ASTMeta<Kolmogorov>> vars){
        final HashMap<EncodedID, ASTMeta<Stacked>> sol = new LinkedHashMap<>();
        for(Map.Entry<EncodedID, ASTMeta<Kolmogorov>> e:vars.entrySet()) {
            final EncodedID encodedID = e.getKey();
            final Kolmogorov kol = e.getValue().re;
            final Stacked s =  kol.toSolomonoff(new VarQuery() {
                @Override
                public Kolmogorov variableAssignment(EncodedID id) {
                    return vars.get(id).re;
                }

                @Override
                public Stacked variableDefinitions(EncodedID id) {
                    return sol.get(id).re;
                }
            });
            sol.put(encodedID,new ASTMeta<>(s,e.getValue().export));
        }
        return sol;
    }




    public static String compileSolomonoff(boolean exportAll,boolean nonfunc,ThraxParser<?,?> parser) {
        final LinkedHashMap<EncodedID, ASTMeta<Kolmogorov>> kol = toKolmogorov(parser);
        final LinkedHashMap<EncodedID, ASTMeta<Kolmogorov>> afterActions = defineMissingRegexesRequiredByActions(
                kol);
        final HashMap<EncodedID, ASTMeta<Stacked>> sol = toSolomonoff(afterActions);
        final String str =  toStringAutoWeightsAndAutoExponentials(exportAll,nonfunc,sol);
        return str;
    }

    public static LinkedHashMap<EncodedID, ASTMeta<Kolmogorov>> toKolmogorov(ThraxParser<?,?> parser) {
        return toKolmogorov(parser.globalVars,parser.fileImportHierarchy.peek().export);
    }

    public static LinkedHashMap<EncodedID, ASTMeta<Kolmogorov>> toKolmogorov(LinkedHashMap<String, ThraxParser.V> globalVars,
                                                                   HashSet<String> toExport) {
        final LinkedHashMap<EncodedID, ASTMeta<Kolmogorov>> kol = new LinkedHashMap<>();
        for (Map.Entry<String, ThraxParser.V> e : globalVars.entrySet()) {
            final String id = e.getKey();
            final PushedBack compiled = e.getValue().re.toKolmogorov(varid -> {
                final EncodedID encodedVarId = new EncodedID(varid, VarState.NONE);
                final ASTMeta<Kolmogorov> inter = kol.get(encodedVarId);
                assert inter != null : id + " " + encodedVarId + " " + varid + " " + globalVars.entrySet() + " " + kol;
                return inter.re;
            });
            final Kolmogorov kolm = compiled.finish();
            kol.put(new EncodedID(id, VarState.NONE), new ASTMeta<>(kolm, toExport.contains(id)));
        }
        return kol;
    }
}
