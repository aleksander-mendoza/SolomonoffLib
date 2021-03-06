package net.alagris.core;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import net.alagris.SolomonoffGrammarLexer;
import net.alagris.SolomonoffGrammarListener;
import net.alagris.SolomonoffGrammarParser;
import net.alagris.SolomonoffGrammarParser.*;
import net.alagris.core.Pair.IntPair;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class ParserListener<Var, V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>>
        implements SolomonoffGrammarListener {
    public final ParseSpecs<Var, V, E, P, A, O, W, N, G> specs;
    public static class AutomatonAndGroup<G>{
        G g;
        int groupIndex;
        public AutomatonAndGroup(G g, int groupIndex){
            this.g = g;
            this.groupIndex = groupIndex;
        }
    }
    public final Stack<AutomatonAndGroup<G>> automata = new Stack<>();
    public final Stack<Pipeline<V, A, E, P, N, G>> pipelines = new Stack<>();
    /**
     * If false, then exponential means consume
     */
    public final boolean exponentialMeansCopy;
    private String currFuncName;

    public ParserListener(ParseSpecs<Var, V, E, P, A, O, W, N, G> specs, boolean exponentialMeansCopy) {
        this.specs = specs;
        this.exponentialMeansCopy = exponentialMeansCopy;

    }

    public G union(Pos pos, G lhs, G rhs) throws CompilationError {
        try {
            return specs.specification().union(lhs, rhs, specs.specification()::epsilonUnion);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.ParseException(pos, e);
        }
    }

    public AutomatonAndGroup<G> unionAndGroup(Pos pos, AutomatonAndGroup<G> lhs, AutomatonAndGroup<G> rhs) throws CompilationError {
        lhs.g = union(pos,lhs.g,rhs.g);
        lhs.groupIndex = Math.max(lhs.groupIndex,rhs.groupIndex);
        return lhs;
    }

    public G concat(G lhs, G rhs) {
        return specs.specification().concat(lhs, rhs);
    }

    public AutomatonAndGroup<G> concatAndGroup( AutomatonAndGroup<G> lhs, AutomatonAndGroup<G> rhs) {
        lhs.g = concat(lhs.g,rhs.g);
        lhs.groupIndex = Math.max(lhs.groupIndex,rhs.groupIndex);
        return lhs;
    }

    public G kleene(Pos pos, G nested) throws CompilationError {
        try {
            return specs.specification().kleene(nested, specs.specification()::epsilonKleene);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.KleeneNondeterminismException(pos);
        }
    }

    public AutomatonAndGroup<G> kleeneAndGroup(Pos pos, AutomatonAndGroup<G> lhs) throws CompilationError {
        lhs.g = kleene(pos,lhs.g);
        return lhs;
    }

    public G kleeneSemigroup(Pos pos, G nested) throws CompilationError {
        try {
            return specs.specification().kleeneSemigroup(nested, specs.specification()::epsilonKleene);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.KleeneNondeterminismException(pos);
        }
    }

    public AutomatonAndGroup<G> kleeneSemigroupAndGroup(Pos pos, AutomatonAndGroup<G> lhs) throws CompilationError {
        lhs.g = kleeneSemigroup(pos,lhs.g);
        return lhs;
    }

    public G kleeneOptional(Pos pos, G nested) throws CompilationError {
        try {
            return specs.specification().kleeneOptional(nested, specs.specification()::epsilonKleene);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.KleeneNondeterminismException(pos);
        }
    }
    public AutomatonAndGroup<G> kleeneOptionalAndGroup(Pos pos, AutomatonAndGroup<G> lhs) throws CompilationError {
        lhs.g = kleeneOptional(pos,lhs.g);
        return lhs;
    }
    public G product(G nested, O out) {
        return specs.specification().rightActionOnGraph(nested, specs.specification().partialOutputEdge(out));
    }

    public G weightBefore(W weight, G nested) {
        return specs.specification().leftActionOnGraph(specs.specification().partialWeightedEdge(weight), nested);
    }
    public AutomatonAndGroup<G> weightBeforeAndGroup(W weight, AutomatonAndGroup<G> nested) {
        nested.g = weightBefore(weight,nested.g);
        return nested;
    }

    public G weightAfter(G nested, W weight) {
        return specs.specification().rightActionOnGraph(nested, specs.specification().partialWeightedEdge(weight));
    }

    public AutomatonAndGroup<G> weightAfterAndGroup( AutomatonAndGroup<G> nested,W weight) {
        nested.g = weightAfter(nested.g,weight);
        return nested;
    }

    public AutomatonAndGroup<G> epsilonAndGroup() {
        return new AutomatonAndGroup<G>(epsilon(),0);
    }

    public G epsilon() {
        return specs.specification().atomicEpsilonGraph();
    }


    public G atomic(V meta, NullTermIter<Pair<A, A>> range) {
        return specs.specification().atomicRangesGraph(meta, range);
    }

    public G atomic(V meta, Pair<A, A> range) {
        return specs.specification().atomicRangeGraph(meta, range);
    }

    public G empty() {
        return specs.specification().createEmptyGraph();
    }

    public Var var(Pos pos, String id, boolean makeCopy) throws CompilationError {

        Var g = (makeCopy == exponentialMeansCopy) ? specs.copyVariable(id) : specs.consumeVariable(id);
        if (g == null) {
            throw new CompilationError.MissingTransducer(pos, id);
        } else {
            return g;
        }
    }

    public G fromOutputString(V meta, O str) {
        return specs.specification().atomicEpsilonGraph(specs.specification().partialOutputEdge(str));
    }

    public G fromString(V meta, Iterable<A> string) {
        return fromString(meta, string.iterator());
    }

    public G fromString(V meta, Iterator<A> string) {
        if (string.hasNext()) {
            G concatenated = atomic(meta, specs.specification().symbolAsRange(string.next()));
            while (string.hasNext()) {
                concatenated = concat(concatenated, atomic(meta, specs.specification().symbolAsRange(string.next())));
            }
            return concatenated;
        } else {
            return epsilon();
        }
    }

    public static IntSeq parseQuotedLiteral(TerminalNode literal) {
        return parseQuotedLiteral(literal.getText());
    }

    public static IntSeq parseQuotedLiteral(String quotedLiteral) {
        assert quotedLiteral.startsWith("'");
        assert quotedLiteral.endsWith("'");
        final String unquotedLiteral = quotedLiteral.substring(1, quotedLiteral.length() - 1);
        final int[] escaped = new int[unquotedLiteral.length()];
        int j = 0;
        boolean isAfterBackslash = false;
        for (int c : (Iterable<Integer>) unquotedLiteral.codePoints()::iterator) {
            if (isAfterBackslash) {
                switch (c) {
                    case '0':
                        escaped[j++] = '\0';
                        break;
                    case 'b':
                        escaped[j++] = '\b';
                        break;
                    case 't':
                        escaped[j++] = '\t';
                        break;
                    case 'n':
                        escaped[j++] = '\n';
                        break;
                    case 'r':
                        escaped[j++] = '\r';
                        break;
                    case 'f':
                        escaped[j++] = '\f';
                        break;
                    default:
                        escaped[j++] = c;
                        break;
                }
                isAfterBackslash = false;
            } else {
                switch (c) {
                    case '\\':
                        isAfterBackslash = true;
                        break;
                    default:
                        escaped[j++] = c;
                        break;
                }

            }
        }
        return new IntSeq(escaped, 0, j);
    }

    private final W parseW(WeightsContext w) {
        return parseW(w.Num());
    }

    private final W parseW(TerminalNode parseNode) throws CompilationError {
        return specs.specification().parseW(Integer.parseInt(parseNode.getText()));
    }

    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static NullTermIter<IntPair> parseCodepointRange(TerminalNode node) {
        final String range = node.getText();
        assert range.endsWith("]>");
        assert range.startsWith("<[");
        final String part = range.substring(2, range.length() - 2);
        final String[] ranges = part.trim().split(" +", 0);
        return new NullTermIter<IntPair>() {
            int i = 0;

            @Override
            public IntPair next() {
                if (i == ranges.length) return null;
                final String range = ranges[i];
                final int dashIdx = range.indexOf('-');
                final int from, to;
                if (dashIdx == -1) {
                    to = from = Integer.parseUnsignedInt(range);
                } else {
                    from = Integer.parseUnsignedInt(range.substring(0, dashIdx));
                    to = Integer.parseUnsignedInt(range.substring(dashIdx + 1));
                }

                final int min;
                final int max;
                if (Integer.compareUnsigned(from, to) < 0) {
                    min = from;
                    max = to;
                } else {
                    min = to;
                    max = from;
                }
                i++;
                return Pair.of(min, max);
            }
        };
    }

    public G parseCodepointRangeAsG(TerminalNode node) {
        final NullTermIter<IntPair> p = parseCodepointRange(node);
        final NullTermIter<Pair<A, A>> r = () -> {
            IntPair i = p.next();
            if (i == null) return null;
            return specs.specification().parseRangeInclusive(i.l, i.r);
        };
        final V meta = specs.specification().metaInfoGenerator(node);
        return atomic(meta, r);
    }

    public static IntSeq parseCodepointOrStringLiteral(String quotedStringOrCodepointLiteral) {
        quotedStringOrCodepointLiteral = quotedStringOrCodepointLiteral.trim();
        if (quotedStringOrCodepointLiteral.startsWith("<")) {
            assert quotedStringOrCodepointLiteral.endsWith(">");
            return parseCodepoint(quotedStringOrCodepointLiteral);
        } else {
            assert quotedStringOrCodepointLiteral.startsWith("'");
            assert quotedStringOrCodepointLiteral.endsWith("'");
            return parseQuotedLiteral(quotedStringOrCodepointLiteral);
        }
    }

    public static IntSeq parseCodepoint(TerminalNode node) {
        return parseCodepoint(node.getText());
    }

    public static IntSeq parseCodepoint(String codepointString) {
        final String[] parts = WHITESPACE.split(codepointString.substring(1, codepointString.length() - 1));
        if (parts.length == 0)
            return IntSeq.Epsilon;
        final int[] ints = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ints[i] = Integer.parseUnsignedInt(parts[i]);
        }
        return new IntSeq(ints);
    }

    public static int escapeCharacter(int c) {
        switch (c) {
            case 'b':
                return '\b';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 'f':
                return '\f';
        }
        return c;
    }

    public G parseRangeAsG(TerminalNode node) {
        final NullTermIter<IntPair> p = parseRange(node);
        final NullTermIter<Pair<A, A>> r = () -> {
            IntPair i = p.next();
            if (i == null) return null;
            return specs.specification().parseRangeInclusive(i.l, i.r);
        };
        final V meta = specs.specification().metaInfoGenerator(node);
        return atomic(meta, r);
    }

    public static NullTermIter<IntPair> parseRange(TerminalNode node) {
        return parseRange(node.getText());
    }
    public static NullTermIter<IntPair> parseRange(String node) {
        final int[] range = node.codePoints().toArray();
        assert range[0] == '[';
        assert range[range.length - 1] == ']';
        return new NullTermIter<IntPair>() {
            int i = 1;

            @Override
            public IntPair next() {
                if (range[i] == ']') return null;
                final int from, to;
                if (range[i] == '\\') {
                    // \a-b or \a-\b or \a
                    from = escapeCharacter(range[i + 1]);
                    if (range[i + 2] == '-') {
                        if (range[i + 3] == '\\') {
                            // \a-\b
                            to = escapeCharacter(range[i + 4]);
                            i = i + 5;
                        } else {
                            // \a-b
                            to = range[i + 3];
                            i = i + 4;
                        }
                    } else {
                        // \a
                        to = from;
                        i = i + 2;
                    }
                } else {
                    // a-b or a-\b or a
                    from = range[i];
                    if (range[i + 1] == '-') {
                        if (range[i + 2] == '\\') {
                            // a-\b
                            to = escapeCharacter(range[i + 3]);
                            i = i + 4;
                        } else {
                            // a-b
                            to = range[i + 2];
                            i = i + 3;
                        }
                    } else {
                        // a
                        to = from;
                        i = i + 1;
                    }
                }
                final int min = Math.min(from, to);
                final int max = Math.max(to, from);
                return Pair.of(min, max);
            }
        };

    }

    @Override
    public void enterStart(StartContext ctx) {
    }

    @Override
    public void exitStart(StartContext ctx) {
    }


    @Override
    public void enterFuncDef(FuncDefContext ctx) {
        currFuncName = ctx.ID().getText();
    }

    @Override
    public void exitFuncDef(FuncDefContext ctx) {
        final String funcName = ctx.ID().getText();
        final AutomatonAndGroup<G> funcBody = automata.pop();
        assert automata.isEmpty();
        try {
            final Pos pos = new Pos(ctx.ID().getSymbol());
            final Var var = specs.introduceVariable(funcName, pos, funcBody.g,funcBody.groupIndex, ctx.exponential != null);
            if (ctx.nonfunctional == null) {
                Specification.RangedGraph<V, A, E, P> g = specs.getOptimised(var);
                specs.specification().checkFunctionality(g, pos);
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void enterTypeJudgement(TypeJudgementContext ctx) {
    }

    @Override
    public void exitTypeJudgement(TypeJudgementContext ctx) {

        final String funcName = ctx.ID().getText();
        final Pos pos = new Pos(ctx.ID().getSymbol());
        try {
            if (ctx.type == null) {
                final AutomatonAndGroup<G> in = automata.pop();
                assert automata.isEmpty();
                specs.typecheckInputOnly(pos, funcName, in.g);
            } else {
                final AutomatonAndGroup<G> out = automata.pop();
                final AutomatonAndGroup<G> in = automata.pop();
                assert automata.isEmpty();
                switch (ctx.type.getText()) {
                    case "&&":
                    case "⨯":
                        specs.typecheckProduct(pos, funcName, in.g, out.g);
                        break;
                    case "→":
                    case "->":
                        specs.typecheckFunction(pos, funcName, in.g, out.g);
                        break;
                }
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }

    }


    @Override
    public void enterPipelineDef(PipelineDefContext ctx) {
        assert pipelines.isEmpty();
        currFuncName = ctx.ID().getText();
    }

    @Override
    public void exitPipelineDef(PipelineDefContext ctx) {
        final Pipeline<V, A, E, P, N, G> p = pipelines.pop();
        assert currFuncName.equals(ctx.ID().getText());
        final String name = ctx.ID().getText();
        try {
            specs.registerNewPipeline(p, name);
        } catch (CompilationError compilationError) {
            throw new RuntimeException(compilationError);
        }
        currFuncName = null;
        assert pipelines.isEmpty();
    }


    @Override
    public void enterPipelineCompose(PipelineComposeContext ctx) {

    }

    @Override
    public void exitPipelineCompose(PipelineComposeContext ctx) {
        final int size = ctx.pipeline_atomic().size();
        Pipeline<V, A, E, P, N, G> p = pipelines.pop();
        assert size * 2 - 1 == ctx.children.size();
        for (int i = 1; i < size; i++) {
            final TerminalNode semicolon = (TerminalNode) ctx.children.get(size * 2 - 1 - i * 2);
            assert semicolon.getText().equals(";");
            final V meta = specs.specification().metaInfoGenerator(semicolon);
            p = new Pipeline.Composition<>(meta, pipelines.pop(), p);
        }
        pipelines.push(p);
    }

    @Override
    public void enterPipelineOr(PipelineOrContext ctx) {

    }

    @Override
    public void exitPipelineOr(PipelineOrContext ctx) {
        final int size = ctx.pipeline_compose().size();
        Pipeline<V, A, E, P, N, G> p = pipelines.pop();
        assert size * 2 - 1 == ctx.children.size();
        for (int i = 1; i < size; i++) {
            final TerminalNode semicolon = (TerminalNode) ctx.children.get(size * 2 - 1 - i * 2);
            assert semicolon.getText().equals("||");
            final V meta = specs.specification().metaInfoGenerator(semicolon);
            p = new Pipeline.Alternative<>(meta, pipelines.pop(), p);
        }
        pipelines.push(p);
    }

    @Override
    public void enterPipelineMealy(PipelineMealyContext ctx) {

    }

    @Override
    public void exitPipelineMealy(PipelineMealyContext ctx) {
        final AutomatonAndGroup<G> g = automata.pop();
        final Specification.RangedGraph<V, A, E, P> r = specs.specification().optimiseGraph(g.g);
        final V meta = specs.specification().metaInfoGenerator(ctx);
        try {
            specs.specification().reduceEdges(meta, r);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        if (ctx.nonfunctional == null) {
            try {
                specs.specification().checkFunctionality(r, new Pos(ctx.start));
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        pipelines.push(new Pipeline.Automaton<>(r, meta));
    }

    @Override
    public void enterPipelineAssertion(PipelineAssertionContext ctx) {

    }

    @Override
    public void exitPipelineAssertion(PipelineAssertionContext ctx) {
        final AutomatonAndGroup<G> g = automata.pop();
        final Specification.RangedGraph<V, A, E, P> r = specs.specification().optimiseGraph(g.g);
        final V meta = specs.specification().metaInfoGenerator(ctx);
        try {
            specs.specification().testDeterminism('@' + currFuncName, r);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        pipelines.push(new Pipeline.Assertion<>(r, meta, ctx.runtime != null));
    }

    @Override
    public void enterPipelineExternal(PipelineExternalContext ctx) {

    }

    @Override
    public void exitPipelineExternal(PipelineExternalContext ctx) {
        final V meta = specs.specification().metaInfoGenerator(ctx.ID());
        final Pos pos = new Pos(ctx.ID().getSymbol());
        try {
            pipelines.push(new Pipeline.External<>(meta, specs.externalPipeline(pos, ctx.ID().getText(), parseFuncArgs(ctx.func_arg()).g)));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enterPipelineReuse(PipelineReuseContext ctx) {

    }

    @Override
    public void exitPipelineReuse(PipelineReuseContext ctx) {
        final String name = ctx.ID().getText();
        final Pipeline<V, A, E, P, N, G> p = specs.getPipeline(name);
        pipelines.push(p);
    }

    @Override
    public void enterPipelineNested(PipelineNestedContext ctx) {

    }

    @Override
    public void exitPipelineNested(PipelineNestedContext ctx) {

    }

    @Override
    public void enterPipelineSubmatch(PipelineSubmatchContext ctx) {

    }

    @Override
    public void exitPipelineSubmatch(PipelineSubmatchContext ctx) {
        final V meta = specs.specification().metaInfoGenerator(ctx);
        final HashMap<A, Pipeline<V, A, E, P, N, G>> handlers = new HashMap<>();
        for (int i = ctx.pipeline_or().size() - 1; i >= 0; i--) {
            final Pipeline<V, A, E, P, N, G> p = pipelines.pop();
            final int group = Integer.parseInt(ctx.Num(i).getSymbol().getText());
            if (group < 0) throw new RuntimeException(
                    new CompilationError.ParseException(
                            new Pos(ctx.Num(i).getSymbol()),
                            "Submatch group index can't be negative"));
            final A marker = specs.specification().groupIndexToMarker(group);
            handlers.put(marker, p);
        }
        pipelines.push(new Pipeline.Submatch<V, A, E, P, N, G>(meta, handlers));
    }

    @Override
    public void enterMealyUnion(MealyUnionContext ctx) {

    }

    @Override
    public void enterWeights(WeightsContext ctx) {
    }


    @Override
    public void exitWeights(WeightsContext ctx) {

    }

    public W parseW(List<TerminalNode> weights) {
        W weight = specs.specification().weightNeutralElement();
        for (final TerminalNode w : weights) {
            try {
                weight = specs.specification().multiplyWeights(weight, parseW(w));
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        return weight;
    }

    @Override
    public void exitMealyUnion(MealyUnionContext ctx) {
        try {
            final int elements = ctx.mealy_concat().size();
            final int children = ctx.children.size();
            assert elements > 0;
            int stackIdx = automata.size() - elements;
            AutomatonAndGroup<G> lhs = automata.get(stackIdx++);
            int childIdx = 0;
            ParseTree concatOrWeight = ctx.children.get(childIdx);
            if (concatOrWeight instanceof WeightsContext) {
                lhs = weightBeforeAndGroup(parseW((WeightsContext) concatOrWeight), lhs);
                childIdx += 1;
            }
            assert ctx.children.get(childIdx) instanceof MealyConcatContext;
            childIdx += 1;
            while (childIdx < children) {
                final TerminalNode bar = (TerminalNode) ctx.children.get(childIdx);
                childIdx += 1;
                assert bar.getText().equals("|");
                assert stackIdx < automata.size();
                AutomatonAndGroup<G> rhs = automata.get(stackIdx++);
                concatOrWeight = ctx.children.get(childIdx);
                if (concatOrWeight instanceof WeightsContext) {
                    rhs = weightBeforeAndGroup(parseW((WeightsContext) concatOrWeight), rhs);
                    childIdx += 1;
                }
                assert ctx.children.get(childIdx) instanceof MealyConcatContext;
                lhs = unionAndGroup(new Pos(bar.getSymbol()), lhs, rhs);
                childIdx += 1;
            }
            assert childIdx == children;
            assert stackIdx == automata.size();
            automata.setSize(automata.size() - elements);
            automata.push(lhs);
        } catch (CompilationError compilationError) {
            compilationError.printStackTrace();
        }


    }

    @Override
    public void enterMealyConcat(MealyConcatContext ctx) {

    }

    @Override
    public void exitMealyConcat(MealyConcatContext ctx) {
        final int elements = ctx.mealy_Kleene_closure().size();
        final int children = ctx.children.size();
        assert elements > 0;
        int stackIdx = automata.size() - elements;
        int childIdx = 0;
        AutomatonAndGroup<G> lhs = automata.get(stackIdx++);
        ParseTree kleene = ctx.children.get(childIdx);
        assert kleene instanceof MealyKleeneClosureContext;
        if (childIdx + 1 < children) {
            final ParseTree kleeneOrWeightOrDot = ctx.children.get(childIdx + 1);
            if (kleeneOrWeightOrDot instanceof TerminalNode) {
                final TerminalNode dot = (TerminalNode) kleeneOrWeightOrDot;
                assert dot.getText().equals("∙");
                childIdx += 2;
            } else if (kleeneOrWeightOrDot instanceof WeightsContext) {
                lhs = weightAfterAndGroup(lhs, parseW((WeightsContext) kleeneOrWeightOrDot));
                if (childIdx + 2 < children && ctx.children.get(childIdx + 2) instanceof TerminalNode) {
                    assert ctx.children.get(childIdx + 2).getText().equals("∙");
                    childIdx += 3;
                } else {
                    childIdx += 2;
                }
            } else {
                childIdx += 1;
            }
        } else {
            childIdx += 1;
        }
        while (childIdx < children) {
            kleene = ctx.children.get(childIdx);
            assert kleene instanceof MealyKleeneClosureContext;
            assert stackIdx < automata.size();
            AutomatonAndGroup<G> rhs = automata.get(stackIdx++);
            if (childIdx + 1 < children) {
                final ParseTree kleeneOrWeightOrDot = ctx.children.get(childIdx + 1);
                if (kleeneOrWeightOrDot instanceof TerminalNode) {
                    final TerminalNode dot = (TerminalNode) kleeneOrWeightOrDot;
                    assert dot.getText().equals("∙");
                    childIdx += 2;
                } else if (kleeneOrWeightOrDot instanceof WeightsContext) {
                    final WeightsContext w = (WeightsContext) kleeneOrWeightOrDot;
                    rhs = weightAfterAndGroup(rhs, parseW(w));
                    if (childIdx + 2 < children && ctx.children.get(childIdx + 2) instanceof TerminalNode) {
                        assert ctx.children.get(childIdx + 2).getText().equals("∙");
                        childIdx += 3;
                    } else {
                        childIdx += 2;
                    }
                } else {
                    childIdx += 1;
                }
            } else {
                childIdx += 1;
            }
            lhs = concatAndGroup(lhs, rhs);
        }
        assert stackIdx == automata.size();
        assert childIdx == children;
        automata.setSize(automata.size() - elements);
        automata.push(lhs);
    }

    @Override
    public void enterMealyKleeneClosure(MealyKleeneClosureContext ctx) {

    }

    @Override
    public void exitMealyKleeneClosure(MealyKleeneClosureContext ctx) {
        final WeightsContext w = ctx.weights();
        try {
            if (ctx.optional != null) {
                AutomatonAndGroup<G> nested = automata.pop();
                if (w == null) {
                    nested = kleeneOptionalAndGroup(new Pos(ctx.optional), nested);
                } else {
                    nested = kleeneOptionalAndGroup(new Pos(ctx.optional), weightAfterAndGroup(nested, parseW(w)));
                }
                automata.push(nested);
            } else if (ctx.plus != null) {
                AutomatonAndGroup<G> nested = automata.pop();
                if (w == null) {
                    nested = kleeneSemigroupAndGroup(new Pos(ctx.plus), nested);
                } else {
                    nested = kleeneSemigroupAndGroup(new Pos(ctx.plus), weightAfterAndGroup(nested, parseW(w)));
                }
                automata.push(nested);
            } else if (ctx.star != null) {
                AutomatonAndGroup<G> nested = automata.pop();
                if (w == null) {
                    nested = kleeneAndGroup(new Pos(ctx.star), nested);
                } else {
                    nested = kleeneAndGroup(new Pos(ctx.star), weightAfterAndGroup(nested, parseW(w)));
                }
                automata.push(nested);
            } else {
                assert w == null;
                //pass
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enterMealyAtomicLiteral(MealyAtomicLiteralContext ctx) {

    }

    @Override
    public void exitMealyAtomicLiteral(MealyAtomicLiteralContext ctx) {
        try {
            final V meta = specs.specification().metaInfoGenerator(ctx.StringLiteral());
            final O in = specs.specification().parseStr(parseQuotedLiteral(ctx.StringLiteral()));
            final G g = ctx.colon == null ? fromString(meta, in) : fromOutputString(meta, in);
            automata.push(new AutomatonAndGroup<>(g,0));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enterMealyAtomicRange(MealyAtomicRangeContext ctx) {

    }

    @Override
    public void exitMealyAtomicRange(MealyAtomicRangeContext ctx) {
        final G g = parseRangeAsG(ctx.Range());
        automata.push(new AutomatonAndGroup<>(g,0));
    }

    @Override
    public void enterMealyAtomicCodepointRange(MealyAtomicCodepointRangeContext ctx) {

    }

    @Override
    public void exitMealyAtomicCodepointRange(MealyAtomicCodepointRangeContext ctx) {
        final G g = parseCodepointRangeAsG(ctx.CodepointRange());
        automata.push(new AutomatonAndGroup<>(g,0));
    }

    @Override
    public void enterMealyAtomicCodepoint(MealyAtomicCodepointContext ctx) {

    }

    @Override
    public void exitMealyAtomicCodepoint(MealyAtomicCodepointContext ctx) {
        try {
            final V meta = specs.specification().metaInfoGenerator(ctx.Codepoint());
            O str = specs.specification().parseStr(parseCodepoint(ctx.Codepoint()));
            final G g = ctx.colon == null ? fromString(meta, str) : fromOutputString(meta, str);
            automata.push(new AutomatonAndGroup<>(g,0));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void enterMealyAtomicVarID(MealyAtomicVarIDContext ctx) {

    }

    @Override
    public void exitMealyAtomicVarID(MealyAtomicVarIDContext ctx) {
        try {
            final Var g = var(new Pos(ctx.start), ctx.ID().getText(), ctx.exponential != null);
            automata.push(new AutomatonAndGroup<>(specs.getGraph(g),specs.getMaxGroupIndex(g)));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void enterMealyAtomicExternal(MealyAtomicExternalContext ctx) {
    }

    @Override
    public void exitFunc_arg(Func_argContext ctx) {

    }

    @Override
    public void enterFunc_arg(Func_argContext ctx) {

    }

    public AutomatonAndGroup<ArrayList<FuncArg<G, O>>> parseFuncArgs(Func_argContext ctx) {
        final int expressions = ctx.mealy_union().size();
        final int references = ctx.ID().size();
        final int informants = ctx.informant().size();
        final int argCount = expressions + references + informants;
        final ArrayList<FuncArg<G, O>> args = new ArrayList<>(argCount);
        final Iterator<ParseTree> i = ctx.children.iterator();
        int exprIdx = 0;
        int maxGroup = 0;
        while (i.hasNext()) {
            final ParseTree child = i.next();
            if (child instanceof InformantContext) {
                final InformantContext inf = (InformantContext) child;
                args.add(parseInformant(inf));
            } else if (child instanceof MealyUnionContext) {
                final AutomatonAndGroup<G> expr = automata.get(automata.size() - expressions + exprIdx);
                exprIdx++;
                args.add(new FuncArg.Expression<>(expr.g));
                maxGroup = Math.max(maxGroup,expr.groupIndex);
            } else if (child instanceof TerminalNode) {
                final TerminalNode terminal = (TerminalNode) child;
                if (terminal.getSymbol().getType() == SolomonoffGrammarLexer.ID) {
                    final G ref = specs.getGraph(specs.borrowVariable(terminal.getText()));
                    args.add(new FuncArg.VarRef<>(ref));
                }
            }
        }
        assert args.size() == argCount:args.size() +" == "+argCount;
        automata.setSize(automata.size() - expressions);
        return new AutomatonAndGroup<>(args,maxGroup);
    }

    @Override
    public void exitMealyAtomicExternal(MealyAtomicExternalContext ctx) {
        final String functionName = ctx.funcName.getText();
        final Pos pos = new Pos(ctx.funcName);
        final AutomatonAndGroup<ArrayList<FuncArg<G, O>>> args = parseFuncArgs(ctx.func_arg());
        try {
            final G g = specs.externalFunction(pos, functionName, args.g);
            automata.push(new AutomatonAndGroup<>(g,args.groupIndex));
        } catch (CompilationError compilationError) {
            throw new RuntimeException(compilationError);
        }

    }

    @Override
    public void enterMealyAtomicNested(MealyAtomicNestedContext ctx) {

    }

    @Override
    public void exitMealyAtomicNested(MealyAtomicNestedContext ctx) {
        //pass
    }

    @Override
    public void enterMealyAtomicSubmatchGroup(MealyAtomicSubmatchGroupContext ctx) {

    }

    @Override
    public void exitMealyAtomicSubmatchGroup(MealyAtomicSubmatchGroupContext ctx) {
        final int group = Integer.parseInt(ctx.Num().getSymbol().getText());
        final Pos pos = new Pos(ctx.Num().getSymbol());
        if (group <= 0)
            throw new RuntimeException(new CompilationError.ParseException(pos, "The group index must be positive"));
        final A marker = specs.specification().groupIndexToMarker(group);
        final O out = specs.singletonOutput(marker);
        final P partial = specs.specification().partialOutputEdge(out);
        final AutomatonAndGroup<G> g = automata.pop();
        if(g.groupIndex<group){
            g.groupIndex = group;
        }else{
            specs.handleNonDecreasingGroupIndex( group,g.groupIndex,pos);
        }
        final G g2 = specs.specification().leftActionOnGraph(partial, g.g);
        final G g3 = specs.specification().rightActionOnGraph(g2, partial);
        g.g = g3;
        automata.push(g);
    }

    @Override
    public void exitInformant(InformantContext ctx) {

    }

    @Override
    public void enterInformant(InformantContext ctx) {
    }

    public FuncArg.Informant<G, O> parseInformant(InformantContext ctx) {
        return parseInformant(ctx, seq -> {
            try {
                return specs.specification().parseStr(seq);
            } catch (CompilationError compilationError) {
                throw new RuntimeException(compilationError);
            }
        }, specs.specification().outputNeutralElement(), null);
    }

    static <G, O> FuncArg.Informant<G, O> parseInformant(InformantContext ctx, Function<IntSeq, O> parse, O neutral, O zero) {
        final FuncArg.Informant<G, O> informant = new FuncArg.Informant<>();
        if(ctx.children==null)return informant;
        for (int i = 0; i < ctx.children.size(); ) {
            O in = parse.apply(parseQuotedLiteral((TerminalNode) ctx.children.get(i)));
            final O out;
            if (i + 1 < ctx.children.size()) {
                final TerminalNode next = (TerminalNode) ctx.children.get(i + 1);
                switch (next.getText()) {
                    case ":":// StringLiteral ':' (StringLiteral|ID) ','
                        TerminalNode outLiteral = (TerminalNode) ctx.children.get(i + 2);
                        final String outStr = outLiteral.getText();
                        if (outLiteral.getSymbol().getType() == SolomonoffGrammarParser.ID) {// StringLiteral ':' ID ','
                            if (outStr.equals("∅")) {
                                out = zero;
                            } else {
                                throw new RuntimeException(new CompilationError.ParseException(new Pos(next.getSymbol()), "Expected ∅ but was " + outStr));
                            }
                        } else if (outLiteral.getSymbol().getType() == SolomonoffGrammarParser.Range) {// StringLiteral ':' Range ','
                            if (outStr.equals("[]")) {
                                out = zero;
                            } else {
                                throw new RuntimeException(new CompilationError.ParseException(new Pos(next.getSymbol()), "Expected [] but was " + outStr));
                            }
                        } else {// StringLiteral ':' StringLiteral ','
                            assert outLiteral.getSymbol().getType() == SolomonoffGrammarParser.StringLiteral;
                            out = parse.apply(parseQuotedLiteral(outLiteral));
                        }
                        i = i + 4;
                        break;
                    case ",":// StringLiteral ','
                        i = i + 2;
                        out = neutral;
                        break;
                    default:
                        throw new IllegalStateException("Expected one of , ∅ # : at " + new Pos(next.getSymbol()));
                }
            } else {// StringLiteral
                i = i + 1;
                out = neutral;
            }
            informant.add(Pair.of(in, out));
        }
        return informant;

    }

    @Override
    public void visitTerminal(TerminalNode node) {

    }

    @Override
    public void visitErrorNode(ErrorNode node) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {

    }

    public void addDotAndHashtag() throws CompilationError {
        final G DOT = atomic(specs.specification().metaInfoNone(), specs.specification().dot());
        final G EPS = epsilon();
        final G HASH = empty();
        specs.introduceVariable(".", Pos.NONE, DOT,0, true);
        specs.introduceVariable("Σ", Pos.NONE, DOT,0, true);
//        specs.introduceVariable("#", Pos.NONE, HASH, true);
        specs.introduceVariable("∅", Pos.NONE, HASH,0, true);
        specs.introduceVariable("ε", Pos.NONE, EPS,0, true);
    }

    public static SolomonoffGrammarParser makeParser(CommonTokenStream tokens) {
        final SolomonoffGrammarParser parser = new SolomonoffGrammarParser(tokens);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                System.err.println("line " + line + ":" + charPositionInLine + " " + msg + " " + e);
            }
        });
        return parser;

    }

    public void runCompiler(SolomonoffGrammarParser parser) throws CompilationError {
        try {
            ParseTreeWalker.DEFAULT.walk(this, parser.start());
            assert automata.isEmpty();
        } catch (RuntimeException e) {
            automata.clear();
            if (e.getCause() instanceof CompilationError) {
                throw (CompilationError) e.getCause();
            } else {
                throw e;
            }
        }
    }

    public void runREPL(SolomonoffGrammarParser parser) throws CompilationError {
        try {
            ParseTreeWalker.DEFAULT.walk(this, parser.repl());
            assert automata.isEmpty();
        } catch (RuntimeException e) {
            automata.clear();
            if (e.getCause() instanceof CompilationError) {
                throw (CompilationError) e.getCause();
            } else {
                throw e;
            }
        }
    }

    @Override
    public void enterFuncs(FuncsContext ctx) {

    }

    @Override
    public void exitFuncs(FuncsContext ctx) {

    }

    @Override
    public void enterRepl(ReplContext ctx) {

    }

    @Override
    public void exitRepl(ReplContext ctx) {

    }
}