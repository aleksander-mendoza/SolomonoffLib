package net.alagris;

import java.util.*;
import java.util.regex.Pattern;

import net.alagris.GrammarParser.*;
import net.automatalib.commons.util.Pair;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class ParserListener<Pipeline, V, E, P, A, O extends Seq<A>, W, N, G extends IntermediateGraph<V, E, P, N>> implements GrammarListener {

    public enum TypeConstructor {
        FUNCTION, PRODUCT
    }

    public static class Type<V, E, P, N, G extends IntermediateGraph<V, E, P, N>> {
        public final G lhs, rhs;
        public final V meta;
        public final String name;
        public final TypeConstructor constructor;

        public Type(V meta, String name, G lhs, G rhs, TypeConstructor constructor) {
            this.meta = meta;
            this.name = name;
            this.rhs = rhs;
            this.lhs = lhs;
            this.constructor = constructor;
        }
    }

    private final Collection<Type<V, E, P, N, G>> types;
    private final ParseSpecs<Pipeline, V, E, P, A, O, W, N, G> specs;
    private final Stack<G> automata = new Stack<>();

    public ParserListener(Collection<Type<V, E, P, N, G>> types, ParseSpecs<Pipeline, V, E, P, A, O, W, N, G> specs) {
        this.types = types;
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

    public G atomic(V meta, A from, A to) {
        return specs.specification().atomicRangeGraph(from, meta, to);
    }

    public G empty() {
        return specs.specification().createEmptyGraph();
    }

    public G atomic(V meta, A symbol) {
        return specs.specification().atomicRangeGraph(symbol, meta, symbol);
    }

    public GMeta<V, E, P, N, G> var(Pos pos, String id, boolean makeCopy) throws CompilationError {
        GMeta<V, E, P, N, G> g = makeCopy ? specs.copyVariable(id) : specs.consumeVariable(id);
        if (g == null) {
            throw new CompilationError.MissingFunction(pos, id);
        } else {
            return g;
        }
    }

    public G fromString(V meta, Iterable<A> string) {
        return fromString(meta, string.iterator());
    }

    public G fromString(V meta, Iterator<A> string) {
        if (string.hasNext()) {
            G concatenated = atomic(meta, string.next());
            while (string.hasNext()) {
                concatenated = concat(concatenated, atomic(meta, string.next()));
            }
            return concatenated;
        } else {
            return epsilon();
        }
    }


    private IntSeq parseQuotedLiteral(TerminalNode literal) throws CompilationError {
        final String quotedLiteral = literal.getText();
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
        return new IntSeq(escaped, j);
    }

    private final W parseW(TerminalNode parseNode) throws CompilationError {
        return specs.specification().parseW(Integer.parseInt(parseNode.getText()));
    }


    public static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public G parseCodepoint(TerminalNode node) {
        String range = node.getText();
        range = range.substring(1, range.length() - 1);
        String[] parts = WHITESPACE.split(range);
        G concatenated = null;
        for (String part : parts) {
            final int dashIdx = part.indexOf('-');
            final int from;
            final int to;
            if (dashIdx == -1) {
                from = to = Integer.parseInt(part);
            } else {
                from = Integer.parseInt(range.substring(0, dashIdx));
                to = Integer.parseInt(range.substring(dashIdx + 1));
            }
            final Pair<A, A> rangeIn = specs.specification().parseRange(from, to);
            final V meta = specs.specification().metaInfoGenerator(node);
            G atom = atomic(meta, rangeIn.getFirst(), rangeIn.getSecond());
            if (concatenated == null) {
                concatenated = atom;
            } else {
                concatenated = concat(concatenated, atom);
            }
        }
        return concatenated == null ? epsilon() : concatenated;
    }

    public IntSeq parseCodepointNoRanges(TerminalNode node) {
        String range = node.getText();
        range = range.substring(1, range.length() - 1);
        final String[] parts = WHITESPACE.split(range);
        if (parts.length == 0) return IntSeq.Epsilon;
        final int[] ints = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            ints[i] = Integer.parseInt(parts[i]);
        }
        return new IntSeq(ints, ints.length);
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

    public G parseRange(TerminalNode node) {
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
        Pair<A, A> r = specs.specification().parseRange(from, to);
        V meta = specs.specification().metaInfoGenerator(node);
        return atomic(meta, r.getFirst(), r.getSecond());
    }

    @Override
    public void enterStart(StartContext ctx) {
    }

    @Override
    public void exitStart(StartContext ctx) {
    }

    @Override
    public void enterEndFuncs(EndFuncsContext ctx) {

    }

    @Override
    public void exitEndFuncs(EndFuncsContext ctx) {

    }

    @Override
    public void enterFuncDef(FuncDefContext ctx) {

    }

    @Override
    public void exitFuncDef(FuncDefContext ctx) {
        final String funcName = ctx.ID().getText();
        final G funcBody = automata.pop();
        try {
            specs.introduceVariable(new GMeta<>(funcBody, funcName, new Pos(ctx.ID().getSymbol()), ctx.exponential!=null));
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
        final G out = automata.pop();
        final G in = automata.pop();
        final String funcName = ctx.ID().getText();
        switch (ctx.type.getText()) {
            case "&&":
            case "⨯":
                types.add(new Type<>(specs.specification().metaInfoGenerator(ctx.ID()), funcName, in, out, TypeConstructor.PRODUCT));
                break;
            case "→":
            case "->":
                types.add(new Type<>(specs.specification().metaInfoGenerator(ctx.ID()), funcName, in, out, TypeConstructor.FUNCTION));
                break;
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
            if (ctx.hoare != null) specs.appendLanguage(new Pos(ctx.hoare.start), pipeline, hoare);
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
            specs.appendExternalFunction(new Pos(ctx.ID().getSymbol()), pipeline, ctx.ID().getText(), ctx.informant() == null ? Collections.emptyList() : Collections.unmodifiableList(informant));
            if (ctx.hoare != null) specs.appendLanguage(new Pos(ctx.hoare.start), pipeline, automata.pop());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        } finally {
            informant.clear();
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
            if (ctx.hoare != null) specs.appendLanguage(new Pos(ctx.hoare.start), pipeline, automata.pop());
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void enterMealyUnion(MealyUnionContext ctx) {

    }

    @Override
    public void exitMealyUnion(MealyUnionContext ctx) {
        G last = automata.pop();
        G prev = null;
        TerminalNode bar = null;
        try {
            for (int i = ctx.children.size() - 2; i >= 0; i--) {
                final ParseTree child = ctx.children.get(i);
                if (child instanceof TerminalNode) {
                    final TerminalNode term = (TerminalNode) child;
                    if (!term.getText().equals("|")) {
                        if (prev == null) {
                            last = weightBefore(parseW(term), last);
                        } else {
                            prev = weightBefore(parseW(term), prev);
                        }
                    }
                } else if (child instanceof MealyEndConcatContext || child instanceof MealyMoreConcatContext) {
                    if (prev != null) {
                        last = union(new Pos(bar.getSymbol()), last, prev);
                    }
                    bar = (TerminalNode) ctx.children.get(i + 1);
                    assert bar.getSymbol().getText().equals("|") : bar.getText();
                    prev = automata.pop();

                }
            }
            if (prev != null) {
                last = union(new Pos(bar.getSymbol()), last, prev);
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }

        automata.push(last);
    }

    @Override
    public void enterMealyEndConcat(MealyEndConcatContext ctx) {

    }

    @Override
    public void exitMealyEndConcat(MealyEndConcatContext ctx) {
        final TerminalNode w = ctx.Weight();
        G lhs = automata.pop();
        if (w != null) {
            try {
                lhs = weightAfter(lhs, parseW(w));
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        automata.push(lhs);
    }

    @Override
    public void enterMealyMoreConcat(MealyMoreConcatContext ctx) {

    }

    @Override
    public void exitMealyMoreConcat(MealyMoreConcatContext ctx) {
        G rhs = automata.pop();
        G lhs = automata.pop();

        final TerminalNode w = ctx.Weight();
        if (w == null) {
            lhs = concat(lhs, rhs);
        } else {
            try {
                lhs = concat(lhs, weightAfter(rhs, parseW(w)));
            } catch (CompilationError e) {
                throw new RuntimeException(e);
            }
        }
        automata.push(lhs);
    }

    @Override
    public void enterMealyKleeneClosure(MealyKleeneClosureContext ctx) {

    }

    @Override
    public void exitMealyKleeneClosure(MealyKleeneClosureContext ctx) {
        final TerminalNode w = ctx.Weight();
        G nested = automata.pop();
        try {
            if (ctx.optional != null) {
                if (w == null) {
                    nested = kleeneOptional(new Pos(ctx.optional), nested);
                } else {
                    nested = kleeneOptional(new Pos(ctx.optional), weightAfter(nested, parseW(w)));
                }
            } else if (ctx.plus != null) {
                if (w == null) {
                    nested = kleeneSemigroup(new Pos(ctx.plus), nested);
                } else {
                    nested = kleeneSemigroup(new Pos(ctx.plus), weightAfter(nested, parseW(w)));
                }
            } else {
                assert ctx.star != null;
                if (w == null) {
                    nested = kleene(new Pos(ctx.star), nested);
                } else {
                    nested = kleene(new Pos(ctx.star), weightAfter(nested, parseW(w)));
                }
            }

        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        automata.push(nested);
    }

    @Override
    public void enterMealyNoKleeneClosure(MealyNoKleeneClosureContext ctx) {

    }

    @Override
    public void exitMealyNoKleeneClosure(MealyNoKleeneClosureContext ctx) {

    }

    @Override
    public void enterMealyProduct(MealyProductContext ctx) {

    }

    @Override
    public void exitMealyProduct(MealyProductContext ctx) {
        G nested = automata.pop();
        try {
            nested = product(nested, specs.specification().parseStr(parseQuotedLiteral(ctx.StringLiteral())));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        automata.push(nested);
    }

    @Override
    public void enterMealyProductCodepoints(MealyProductCodepointsContext ctx) {

    }

    @Override
    public void exitMealyProductCodepoints(MealyProductCodepointsContext ctx) {
        G nested = automata.pop();
        try {
            nested = product(nested, specs.specification().parseStr(parseCodepointNoRanges(ctx.Codepoint())));
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
        automata.push(nested);
    }

    @Override
    public void enterMealyEpsilonProduct(MealyEpsilonProductContext ctx) {

    }

    @Override
    public void exitMealyEpsilonProduct(MealyEpsilonProductContext ctx) {

    }

    @Override
    public void enterMealyAtomicLiteral(MealyAtomicLiteralContext ctx) {

    }

    @Override
    public void exitMealyAtomicLiteral(MealyAtomicLiteralContext ctx) {
        try {
            final V meta = specs.specification().metaInfoGenerator(ctx.StringLiteral());
            final O in = specs.specification().parseStr(parseQuotedLiteral(ctx.StringLiteral()));
            final G g = fromString(meta, in);
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
        final G g = parseRange(ctx.Range());
        automata.push(g);
    }

    @Override
    public void enterMealyAtomicCodepoint(MealyAtomicCodepointContext ctx) {

    }

    @Override
    public void exitMealyAtomicCodepoint(MealyAtomicCodepointContext ctx) {
        final G g = parseCodepoint(ctx.Codepoint());
        automata.push(g);
    }

    @Override
    public void enterMealyAtomicVarID(MealyAtomicVarIDContext ctx) {

    }

    @Override
    public void exitMealyAtomicVarID(MealyAtomicVarIDContext ctx) {
        try {

            final GMeta<V, E, P, N, G> g = var(new Pos(ctx.start), ctx.ID().getText(), ctx.exponential!=null);
            automata.push(g.graph);
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
    }

    private final ArrayList<Pair<O, O>> informant = new ArrayList<>();

    @Override
    public void enterMealyAtomicExternal(MealyAtomicExternalContext ctx) {
    }

    @Override
    public void exitMealyAtomicExternal(MealyAtomicExternalContext ctx) {
        final String functionName = ctx.ID().getText();
        final Pos pos = new Pos(ctx.ID().getSymbol());
        try {
            final G g = specs.externalFunction(pos, functionName, ctx.informant() == null ? Collections.emptyList() : Collections.unmodifiableList(informant));
            informant.clear();
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

    }

    @Override
    public void exitInformant(InformantContext ctx) {

    }

    @Override
    public void enterInformant(InformantContext ctx) {
        try {
            for (int i = 0; i < ctx.children.size(); ) {
                O in = specs.specification().parseStr(parseQuotedLiteral((TerminalNode)ctx.children.get(i)));
                final O out;
                if(i + 1<ctx.children.size()) {
                    final TerminalNode next = (TerminalNode) ctx.children.get(i + 1);
                    switch (next.getText()) {
                        case ":":// StringLiteral ':' (StringLiteral|ID) ','
                            TerminalNode outLiteral = (TerminalNode) ctx.children.get(i + 2);
                            if(outLiteral.getText().equals("#")){// StringLiteral ':' ID ','
                                out = null;
                            }else{// StringLiteral ':' StringLiteral ','
                                out = specs.specification().parseStr(parseQuotedLiteral(outLiteral));
                            }
                            i = i + 4;
                            break;
                        case ",":// StringLiteral ','
                            i = i + 2;
                            out = specs.specification().outputNeutralElement();
                            break;
                        default:
                            throw new IllegalStateException("Expected one of , # : at " + new Pos(next.getSymbol()));
                    }
                }else{// StringLiteral
                    i = i + 1;
                    out = specs.specification().outputNeutralElement();
                }
                informant.add(Pair.of(in, out));
            }
        } catch (CompilationError e) {
            throw new RuntimeException(e);
        }
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
        Pair<A, A> dot = specs.specification().dot();
        final G DOT = atomic(specs.specification().metaInfoNone(), specs.specification().dot().getFirst(), specs.specification().dot().getSecond());
        final G HASH = empty();
        specs.introduceVariable(new GMeta<>(DOT, ".", Pos.NONE, true));
        specs.introduceVariable(new GMeta<>(HASH, "#", Pos.NONE, true));
    }

    public void parse(CharStream source) throws CompilationError {
        final GrammarLexer lexer = new GrammarLexer(source);
        final GrammarParser parser = new GrammarParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                System.err.println("line " + line + ":" + charPositionInLine + " " + msg + " " + e);
            }
        });
        try {
            ParseTreeWalker.DEFAULT.walk(this, parser.start());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CompilationError) {
                throw (CompilationError) e.getCause();
            } else {
                throw e;
            }
        }

    }
}