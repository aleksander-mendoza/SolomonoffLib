package net.alagris;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.regex.Pattern;

import net.alagris.SolomonoffGrammarParser.*;
import net.alagris.Pair.IntPair;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class ParserListener<Pipeline, Var, V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>>
        implements SolomonoffGrammarListener {
    public final ParseSpecs<Pipeline, Var, V, E, P, A, O, W, N, G> specs;
    public final Stack<G> automata = new Stack<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, Future<?>> promisesMade = new ConcurrentHashMap<>();
    public ParserListener(ParseSpecs<Pipeline, Var, V, E, P, A, O, W, N, G> specs) {
        this.specs = specs;
        this.pipeline = specs.makeNewPipeline();
        
    }

    public G union(Pos pos, G lhs, G rhs) throws CompilationError {
        try {
            return specs.specification().union(lhs, rhs, specs.specification()::epsilonUnion);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.ParseException(pos, e);
        }
    }

    public G concat(G lhs, G rhs) {
        return specs.specification().concat(lhs, rhs);
    }

    public G kleene(Pos pos, G nested) throws CompilationError {
        try {
            return specs.specification().kleene(nested, specs.specification()::epsilonKleene);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.KleeneNondeterminismException(pos);
        }
    }

    public G kleeneSemigroup(Pos pos, G nested) throws CompilationError {
        try {
            return specs.specification().kleeneSemigroup(nested, specs.specification()::epsilonKleene);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.KleeneNondeterminismException(pos);
        }
    }

    public G kleeneOptional(Pos pos, G nested) throws CompilationError {
        try {
            return specs.specification().kleeneOptional(nested, specs.specification()::epsilonKleene);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new CompilationError.KleeneNondeterminismException(pos);
        }
    }

    public G product(G nested, O out) {
        return specs.specification().rightActionOnGraph(nested, specs.specification().partialOutputEdge(out));
    }

    public G weightBefore(W weight, G nested) {
        return specs.specification().leftActionOnGraph(specs.specification().partialWeightedEdge(weight), nested);
    }

    public G weightAfter(G nested, W weight) {
        return specs.specification().rightActionOnGraph(nested, specs.specification().partialWeightedEdge(weight));
    }

    public G epsilon() {
        return specs.specification().atomicEpsilonGraph();
    }

    public G atomic(V meta, Pair<A, A> range) {
        return specs.specification().atomicRangeGraph(meta, range);
    }

    public G empty() {
        return specs.specification().createEmptyGraph();
    }

    public Var var(Pos pos, String id, boolean makeCopy) throws CompilationError {
        Var g = makeCopy ? specs.copyVariable(id) : specs.consumeVariable(id);
        if (g == null) {
            throw new CompilationError.MissingFunction(pos, id);
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
    	return parseW(w.Weight());
    }
    private final W parseW(TerminalNode parseNode) throws CompilationError {
        return specs.specification().parseW(Integer.parseInt(parseNode.getText()));
    }

    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public static IntPair parseCodepointRange(TerminalNode node) {
        final String range = node.getText();
        final String part = range.substring(1, range.length() - 1);
        final int dashIdx = part.indexOf('-');
        final int from = Integer.parseInt(part.substring(0, dashIdx));
        final int to = Integer.parseInt(part.substring(dashIdx + 1));
        final int min = Math.min(from, to);
        final int max = Math.max(from, to);
        return Pair.of(min, max);
    }

    public G parseCodepointRangeAsG(TerminalNode node) {
        final IntPair range = parseCodepointRange(node);
        final Pair<A, A> rangeIn = specs.specification().parseRangeInclusive(range.l, range.r);
        final V meta = specs.specification().metaInfoGenerator(node);
        return atomic(meta, rangeIn);
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
            ints[i] = Integer.parseInt(parts[i]);
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
        final IntPair p = parseRange(node);
        final Pair<A, A> r = specs.specification().parseRangeInclusive(p.l, p.r);
        final V meta = specs.specification().metaInfoGenerator(node);
        return atomic(meta, r);
    }

    public static IntPair parseRange(TerminalNode node) {
        final int[] range = node.getText().codePoints().toArray();
        final int from, to;
        // [a-b] or [\a-b] or [a-\b] or [\a-\b]
        if (range[1] == '\\') {
            // [\a-b] or [\a-\b]
            from = escapeCharacter(range[2]);
            if (range[4] == '\\') {
                // [\a-\b]
                to = escapeCharacter(range[5]);
            } else {
                // [\a-b]
                to = range[4];
            }
        } else {
            // [a-b] or [a-\b]
            from = range[1];
            if (range[3] == '\\') {
                // [a-\b]
                to = escapeCharacter(range[4]);
            } else {
                // [a-b]
                to = range[3];
            }
        }
        final int min = Math.min(from, to);
        final int max = Math.max(to, from);
        return Pair.of(min, max);
    }

    @Override
    public void enterStart(StartContext ctx) {
    }

    @Override
    public void exitStart(StartContext ctx) {
    }

    @Override
    public void enterFuncDef(FuncDefContext ctx) {

    }

    @Override
    public void exitFuncDef(FuncDefContext ctx) {
        final String funcName = ctx.ID().getText();
        final G funcBody = automata.pop();
        assert automata.isEmpty();
        try {
            specs.introduceVariable(funcName, new Pos(ctx.ID().getSymbol()), funcBody, ctx.exponential != null);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enterHoarePipeline(HoarePipelineContext ctx) {

    }

    @Override
    public void exitHoarePipeline(HoarePipelineContext ctx) {
        try {
            specs.registerNewPipeline(new Pos(ctx.ID().getSymbol()), pipeline, ctx.ID().getText());
            pipeline = specs.makeNewPipeline();
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
                final G in = automata.pop();
                assert automata.isEmpty();
                specs.typecheckProduct(pos, funcName, in, epsilon());
            } else {
                final G out = automata.pop();
                final G in = automata.pop();
                assert automata.isEmpty();
                switch (ctx.type.getText()) {
                    case "&&":
                    case "⨯":
                        specs.typecheckProduct(pos, funcName, in, out);
                        break;
                    case "→":
                    case "->":
                        specs.typecheckFunction(pos, funcName, in, out);
                        break;
                }
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }

    }

    private Pipeline pipeline;

    @Override
    public void enterPipelineMealy(PipelineMealyContext ctx) {
    }

    @Override
    public void exitPipelineMealy(PipelineMealyContext ctx) {
        try {
            final G hoare = ctx.hoare == null ? null : automata.pop();
            final G tran = automata.pop();
            specs.appendAutomaton(new Pos(ctx.start), pipeline, tran);
            if (ctx.hoare != null)
                specs.appendLanguage(new Pos(ctx.hoare.start), pipeline, hoare);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enterPipelineExternal(PipelineExternalContext ctx) {
    }

    @Override
    public void exitPipelineExternal(PipelineExternalContext ctx) {
        try {
            specs.appendExternalFunction(new Pos(ctx.ID().getSymbol()), pipeline, ctx.ID().getText(),
                    parseInformant(ctx.informant()));
            if (ctx.hoare != null)
                specs.appendLanguage(new Pos(ctx.hoare.start), pipeline, automata.pop());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void enterPipelineNested(PipelineNestedContext ctx) {
    }

    @Override
    public void exitPipelineNested(PipelineNestedContext ctx) {
        try {
            specs.appendPipeline(new Pos(ctx.ID().getSymbol()), pipeline, ctx.ID().getText());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void enterPipelineBegin(PipelineBeginContext ctx) {

    }

    @Override
    public void exitPipelineBegin(PipelineBeginContext ctx) {
        try {
            if (ctx.hoare != null)
                specs.appendLanguage(new Pos(ctx.hoare.start), pipeline, automata.pop());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
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
    	for(final TerminalNode w : weights) {
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
            G lhs = automata.get(stackIdx++);
            int childIdx = 0;
            ParseTree concatOrWeight = ctx.children.get(childIdx);
            if (concatOrWeight instanceof WeightsContext) {
                lhs = weightBefore(parseW((WeightsContext) concatOrWeight), lhs);
                childIdx += 1;
            }
            assert ctx.children.get(childIdx) instanceof MealyConcatContext;
            childIdx += 1;
            while (childIdx < children) {
                final TerminalNode bar = (TerminalNode) ctx.children.get(childIdx);
                childIdx += 1;
                assert bar.getText().equals("|");
                assert stackIdx < automata.size();
                G rhs = automata.get(stackIdx++);
                concatOrWeight = ctx.children.get(childIdx);
                if (concatOrWeight instanceof WeightsContext) {
                    rhs = weightBefore(parseW((WeightsContext) concatOrWeight), rhs);
                    childIdx += 1;
                }
                assert ctx.children.get(childIdx) instanceof MealyConcatContext;
                lhs = union(new Pos(bar.getSymbol()), lhs, rhs);
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
            G lhs = automata.get(stackIdx++);
            ParseTree kleene = ctx.children.get(childIdx);
            assert kleene instanceof MealyKleeneClosureContext;
            if (childIdx + 1 < children) {
                final ParseTree kleeneOrWeightOrDot = ctx.children.get(childIdx + 1);
                if (kleeneOrWeightOrDot instanceof TerminalNode) {
                    final TerminalNode dot = (TerminalNode) kleeneOrWeightOrDot;
                        assert dot.getText().equals("∙");
                        childIdx += 2;
                } else if(kleeneOrWeightOrDot instanceof WeightsContext) {
                	 lhs = weightAfter(lhs, parseW((WeightsContext)kleeneOrWeightOrDot));
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
                G rhs = automata.get(stackIdx++);
                if (childIdx + 1 < children) {
                    final ParseTree kleeneOrWeightOrDot = ctx.children.get(childIdx + 1);
                    if (kleeneOrWeightOrDot instanceof TerminalNode) {
                        final TerminalNode dot = (TerminalNode) kleeneOrWeightOrDot;
                        assert dot.getText().equals("∙");
                        childIdx += 2;
                    } else if(kleeneOrWeightOrDot instanceof WeightsContext) {
                    	final WeightsContext w = (WeightsContext) kleeneOrWeightOrDot;
                    	rhs = weightAfter(rhs, parseW(w));
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
                lhs = concat(lhs, rhs);
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
                G nested = automata.pop();
                if (w == null) {
                    nested = kleeneOptional(new Pos(ctx.optional), nested);
                } else {
                    nested = kleeneOptional(new Pos(ctx.optional), weightAfter(nested, parseW(w)));
                }
                automata.push(nested);
            } else if (ctx.plus != null) {
                G nested = automata.pop();
                if (w == null) {
                    nested = kleeneSemigroup(new Pos(ctx.plus), nested);
                } else {
                    nested = kleeneSemigroup(new Pos(ctx.plus), weightAfter(nested, parseW(w)));
                }
                automata.push(nested);
            } else if (ctx.star != null) {
                G nested = automata.pop();
                if (w == null) {
                    nested = kleene(new Pos(ctx.star), nested);
                } else {
                    nested = kleene(new Pos(ctx.star), weightAfter(nested, parseW(w)));
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
            automata.push(g);
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
        automata.push(g);
    }

    @Override
    public void enterMealyAtomicCodepointRange(MealyAtomicCodepointRangeContext ctx) {

    }

    @Override
    public void exitMealyAtomicCodepointRange(MealyAtomicCodepointRangeContext ctx) {
        final G g = parseCodepointRangeAsG(ctx.CodepointRange());
        automata.push(g);
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
            automata.push(g);
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
            automata.push(specs.getGraph(g));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void enterMealyAtomicExternal(MealyAtomicExternalContext ctx) {
    }

    @Override
    public void exitMealyAtomicExternal(MealyAtomicExternalContext ctx) {
        final String functionName = ctx.ID().getText();
        final Pos pos = new Pos(ctx.ID().getSymbol());
        try {
            final G g = specs.externalFunction(pos, functionName,
                    parseInformant(ctx.informant()));
            automata.push(g);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
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
    public void enterMealyAtomicExternalOperation(MealyAtomicExternalOperationContext ctx) {

    }

    @Override
    public void exitMealyAtomicExternalOperation(MealyAtomicExternalOperationContext ctx) {
        final int unions = ctx.mealy_union().size();
        ArrayList<G> unionArray = new ArrayList<>(unions);
        for (int i = 0; i < unions; i++) {
            unionArray.add(automata.get(automata.size() - unions + i));
        }
        automata.setSize(automata.size() - unions);
        try {
            automata.push(specs.externalOperation(new Pos(ctx.ID().getSymbol()), ctx.ID().getText(), unionArray));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exitInformant(InformantContext ctx) {

    }

    @Override
    public void enterInformant(InformantContext ctx) {
    }

    ArrayList<Pair<O, O>> parseInformant(InformantContext ctx) {
        return parseInformant(ctx,seq-> {
            try {
                return specs.specification().parseStr(seq);
            } catch (CompilationError compilationError) {
                throw new RuntimeException(compilationError);
            }
        },specs.specification().outputNeutralElement(),null);
    }
    static <O> ArrayList<Pair<O, O>> parseInformant(InformantContext ctx, Function<IntSeq, O> parse, O neutral, O zero) {
        final ArrayList<Pair<O, O>> informant = new ArrayList<>();
        for (int i = 0; i < ctx.children.size(); ) {
            O in = parse.apply(parseQuotedLiteral((TerminalNode) ctx.children.get(i)));
            final O out;
            if (i + 1 < ctx.children.size()) {
                final TerminalNode next = (TerminalNode) ctx.children.get(i + 1);
                switch (next.getText()) {
                    case ":":// StringLiteral ':' (StringLiteral|ID) ','
                        TerminalNode outLiteral = (TerminalNode) ctx.children.get(i + 2);
                        final String outStr = outLiteral.getText();
                        if (outStr.equals("#") || outStr.equals("∅")) {// StringLiteral ':' ID ','
                            out = zero;
                        } else {// StringLiteral ':' StringLiteral ','
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
        specs.introduceVariable(".", Pos.NONE, DOT, true);
        specs.introduceVariable("Σ", Pos.NONE, DOT, true);
        specs.introduceVariable("#", Pos.NONE, HASH, true);
        specs.introduceVariable("∅", Pos.NONE, HASH, true);
        specs.introduceVariable("ε", Pos.NONE, EPS, true);
    }

    public static SolomonoffGrammarParser makeParser(CommonTokenStream tokens) throws CompilationError {
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
    public void enterIncludeFile(IncludeFileContext ctx) {

    }

    @Override
    public void exitIncludeFile(IncludeFileContext ctx) {
        final String quotedPath = ctx.StringLiteral().getText();
        final String path = quotedPath.substring(1, quotedPath.length() - 1);
        try {
            SolomonoffGrammarLexer lexer = new SolomonoffGrammarLexer(CharStreams.fromFileName(path));
            final SolomonoffGrammarParser parser = new SolomonoffGrammarParser(new CommonTokenStream(lexer));
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                        int charPositionInLine, String msg, RecognitionException e) {
                    System.err.println("line " + line + ":" + charPositionInLine + " " + msg + " " + e);
                }
            });
            if (ctx.ID() == null) {
                ParseTreeWalker.DEFAULT.walk(this, parser.start());
            } else {
                final String promiseId = ctx.ID().getText();
                final Future<?> future = pool.submit(() -> ParseTreeWalker.DEFAULT.walk(this, parser.start()));
                final Future<?> prevFuture = promisesMade.putIfAbsent(promiseId, future);
                if (prevFuture != null) {
                    throw new RuntimeException(new CompilationError.PromiseReused(promiseId));
                }
            }
        } catch (IOException e1) {
            throw new RuntimeException(new CompilationError(e1.getMessage()));
        }

    }

    @Override
    public void enterWaitForFile(WaitForFileContext ctx) {

    }

    @Override
    public void exitWaitForFile(WaitForFileContext ctx) {
        final String promiseId = ctx.ID().getText();
        final Future<?> future = promisesMade.get(promiseId);
        if (future == null) throw new RuntimeException(new CompilationError.PromiseNotMade(promiseId));
        try {
            future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException && e.getCause().getCause() instanceof CompilationError) {
                throw (RuntimeException) e.getCause();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(new CompilationError(e));
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