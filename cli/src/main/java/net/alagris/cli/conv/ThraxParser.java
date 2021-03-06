package net.alagris.cli.conv;

import net.alagris.ThraxGrammarLexer;
import net.alagris.ThraxGrammarListener;
import net.alagris.ThraxGrammarParser;
import net.alagris.ThraxGrammarParser.*;
import net.alagris.core.CompilationError;
import net.alagris.core.IntSeq;
import net.alagris.core.IntermediateGraph;
import net.alagris.core.LexUnicodeSpecification.E;
import net.alagris.core.LexUnicodeSpecification.P;
import net.alagris.core.Pos;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ThraxParser<N, G extends IntermediateGraph<Pos, E, P, N>> implements ThraxGrammarListener {

    public static final int TEMPORARY_THRAX_SYMBOL_BEGIN = 0x100000;
    public static final int EOS = 0x10FFFD;
    public static final int BOS = 0x10FFFC;
    int nextTemporarySymbolToCreate = TEMPORARY_THRAX_SYMBOL_BEGIN;

    public final HashMap<String, Integer> TEMPORARY_THRAX_SYMBOLS = new HashMap<>();

    {
        TEMPORARY_THRAX_SYMBOLS.put("BOS", BOS);
        TEMPORARY_THRAX_SYMBOLS.put("EOS", BOS);
    }

    int getTmpSymbol(String id) {
        return TEMPORARY_THRAX_SYMBOLS.computeIfAbsent(id, k -> nextTemporarySymbolToCreate++);
    }

    /**
     * Every file is automatically assigned some namespace. A namespace is nothing
     * more than a prefix that must be added to any variable that you want to access
     * (just like in C). For instance if there is variable f declared in
     * thrax_grammar.grm and assuming that alias for thrax_grammar.grm is
     * thrax_grammar, then the variable after conversion to Solomonoff becomes
     * thrax_grammar.f (note that in Solomonoff, dot is just a normal symbol like
     * any other and can be used as part of variable name).
     */
    public final HashMap<File, String> fileToNamespace = new HashMap<>();
    public final LinkedHashMap<String, V> globalVars = new LinkedHashMap<>();
    public final HashMap<String, Macro> macros = new LinkedHashMap<>();
    LinkedHashMap<String, V> macroScope;
    ArrayList<String> macroArgs;
    boolean useGlobalScope = true;
    public final Stack<FileScope> fileImportHierarchy = new Stack<>();
    /**
     * id of the variable currently being built
     */
    public String id;
    final Stack<Church> res = new Stack<>();
    public int numberOfSubVariablesCreated = 0;

    static class Macro {
        final List<String> args;
        final LinkedHashMap<String, V> localVars;

        public Macro(List<String> args, LinkedHashMap<String, V> localVars, Set<String> globalVars) {
            this.args = args;
            this.localVars = localVars;
            assert selfContained(globalVars);
        }

        private boolean selfContained(Set<String> globalVars) {
            final HashSet<String> vars = new HashSet<>(args);
            vars.addAll(globalVars);
            for (Entry<String, V> e : localVars.entrySet()) {
                e.getValue().re.substituteCh(var -> {
                    assert vars.contains(var.id) : vars + " " + var.id;
                    return var;
                });
                vars.add(e.getKey());
            }
            return true;
        }

        ;
    }

    public static class V {
        public final Church re;
        public final File introducedIn;

        public V(Church re, File introducedIn) {
            assert re != null;
            assert introducedIn != null;
            this.re = re;
            this.introducedIn = normalizeFile(introducedIn);
        }

        @Override
        public String toString() {
            return re.toString();
        }
    }

    public static class FileScope {
        public final File filePath;
        public final HashSet<String> export = new HashSet<>();
        public final HashMap<String, File> importAliasToFile = new HashMap<>();

        public FileScope(File filePath) {
            this.filePath = normalizeFile(filePath);
        }

        @Override
        public String toString() {
            return filePath.toString();
        }
    }


    public ThraxParser(File filePath) {
        final File normalized = normalizeFile(filePath);
        assert !fileToNamespace.containsKey(normalized);
        fileImportHierarchy.push(new FileScope(normalized));
        fileToNamespace.put(normalized, "root");
    }

    public IntSeq parseLiteral(String str) {
        final int[] arr = new int[str.codePointCount(1, str.length() - 1)];
        int arrEnd = 0;
        for (int i = 1; i < str.length() - 1; ) {// first and last are " characters
            final int c = str.codePointAt(i);
            if (c == '[') { // parse Thrax's special codes like "[32][0x20][040]"
                final int beginNum;
                final int base;
                final char firstInsideBrackets = str.charAt(i + 1);
                if (firstInsideBrackets == '0') {
                    if (str.charAt(i + 2) == 'x') {
                        base = 16;
                        beginNum = i + 3;// hex notation
                    } else if (str.charAt(i + 2) == ']') {// special case of [0]
                        base = 10;// decimal zero
                        beginNum = i + 1;
                    } else {
                        base = 8;// oct notation
                        beginNum = i + 2;
                    }
                } else if ('0' <= firstInsideBrackets && firstInsideBrackets <= '9') {
                    base = 10;// decimal notation
                    beginNum = i + 1;
                } else {
                    base = -1;// temporary thrax symbol
                    beginNum = i + 1;
                }
                int endNum = beginNum + 1;
                while (endNum < str.length() - 1 && str.charAt(endNum) != ']')
                    endNum++;
                final String insideBrackets = str.substring(beginNum, endNum);
                if (base == -1) {
                    arr[arrEnd++] = getTmpSymbol(insideBrackets);
                } else {
                    arr[arrEnd++] = Integer.parseInt(insideBrackets, base);
                }
                i = endNum + 1;
            } else if (c == '\\') {
                i++;
                final int escaped = str.codePointAt(i);
                i += Character.charCount(escaped);
                switch (escaped) {
                    case 'r':
                        arr[arrEnd++] = (int) '\r';
                        break;
                    case 't':
                        arr[arrEnd++] = (int) '\t';
                        break;
                    case 'n':
                        arr[arrEnd++] = (int) '\n';
                        break;
                    case '0':
                        arr[arrEnd++] = (int) '\0';
                        break;
                    case 'b':
                        arr[arrEnd++] = (int) '\b';
                        break;
                    case 'f':
                        arr[arrEnd++] = (int) '\f';
                        break;
                    default:
                        arr[arrEnd++] = escaped;
                        break;
                }

            } else {
                i += Character.charCount(c);
                arr[arrEnd++] = c;
            }

        }
        if (0 == arrEnd)
            return IntSeq.Epsilon;
        return new IntSeq(arr, 0, arrEnd);
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
        // pass
    }

    @Override
    public void enterStmtReturn(StmtReturnContext ctx) {
        id = "return";// this is the ID of return statement
    }

    @Override
    public void exitStmtReturn(StmtReturnContext ctx) {
        assert "return".equals(id);
        assert !useGlobalScope;
        introduceVarID("return", new V(res.pop(), fileImportHierarchy.peek().filePath));
    }

    @Override
    public void enterFstWithRange(FstWithRangeContext ctx) {

    }

    @Override
    public void exitFstWithRange(FstWithRangeContext ctx) {
        final Church re = res.pop();
        final int from, to;
        if (ctx.from == null) {
            from = to = Integer.parseInt(ctx.times.getText());
        } else {
            from = Integer.parseInt(ctx.from.getText());
            to = Integer.parseInt(ctx.to.getText());
        }
        if (to == 0 || from > to) {
            res.push(Atomic.EPSILON);
        } else {
            final Church repeating;
            if (re instanceof Atomic) {
                repeating = re;
            } else {
                assert id != null;
                final String subID = resolveNewID(id) + "." + "__" + (numberOfSubVariablesCreated++);
                introduceVarID(subID, new V(re, fileImportHierarchy.peek().filePath));
                repeating = new Church.ChVar(subID);
            }
            res.push(new Church.ChConcat(new Church.ChPow(repeating, from), new Church.ChLePow(repeating, to - from)));
        }
    }

    @Override
    public void enterFstWithOutput(FstWithOutputContext ctx) {

    }

    @Override
    public void exitFstWithOutput(FstWithOutputContext ctx) {
        final Church r = res.pop();
        final Church l = res.pop();
        res.push(new Church.ChConcat(new Church.ChClearOutput(l), new Church.ChProd(new Church.ChClearOutput(r))));
    }

    @Override
    public void enterFstWithoutUnion(FstWithoutUnionContext ctx) {

    }

    @Override
    public void exitFstWithoutUnion(FstWithoutUnionContext ctx) {
        // pass
    }

    @Override
    public void enterFstWithUnion(FstWithUnionContext ctx) {

    }

    @Override
    public void exitFstWithUnion(FstWithUnionContext ctx) {
        final Church r = res.pop();
        final Church l = res.pop();
        if (ctx.swap == null) {
            res.push(new Church.ChUnion(l, r));
        } else {
            res.push(new Church.ChUnion(r, l));
        }
    }

    @Override
    public void enterFstWithoutComposition(FstWithoutCompositionContext ctx) {

    }

    @Override
    public void exitFstWithoutComposition(FstWithoutCompositionContext ctx) {
        // pass
    }

    @Override
    public void enterStmtImport(StmtImportContext ctx) {
        assert id == null;
    }

    private static File normalizeFile(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
            return f;
        }
    }

    @Override
    public void exitStmtImport(StmtImportContext ctx) {
        assert id == null;
        final String quoted = ctx.StringLiteral().getText();
        final String noQuotes = quoted.substring(1, quoted.length() - 1);
        final File file = normalizeFile(new File(fileImportHierarchy.peek().filePath.getParentFile(), noQuotes));
        final String alias = ctx.ID().getText();
        final File prevFile = fileImportHierarchy.peek().importAliasToFile.put(alias, file);
        assert prevFile == null;
        assert useGlobalScope;
        if (!fileToNamespace.containsKey(file)) {
            String namespace = alias;
            if (fileToNamespace.containsValue(namespace)) {
                int i = 0;
                while (fileToNamespace.containsValue(namespace + i))
                    i++;
                namespace = namespace + i;
            }
            fileToNamespace.put(file, namespace);

            ThraxGrammarLexer lexer;
            try {
                lexer = new ThraxGrammarLexer(CharStreams.fromPath(file.toPath()));
                final ThraxGrammarParser parser = new ThraxGrammarParser(new CommonTokenStream(lexer));
                parser.addErrorListener(new BaseErrorListener() {
                    @Override
                    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                            int charPositionInLine, String msg, RecognitionException e) {
                        System.err.println("line " + line + ":" + charPositionInLine + " " + msg + " " + e);
                    }
                });
                final FileScope imported = new FileScope(file);
                assert fileToNamespace.containsKey(file);
                fileImportHierarchy.push(imported);
                ParseTreeWalker.DEFAULT.walk(this, parser.start());
                assert useGlobalScope;
                final FileScope popped = fileImportHierarchy.pop();
                assert popped == imported;
            } catch (IOException e1) {
                throw new RuntimeException(importTrace(),e1);
            }

        }
    }

    @Override
    public void enterStart(StartContext ctx) {

    }

    @Override
    public void exitStart(StartContext ctx) {
        // pass
    }

    @Override
    public void enterFunc_arguments(Func_argumentsContext ctx) {

    }

    @Override
    public void exitFunc_arguments(Func_argumentsContext ctx) {
        // pass
    }

    @Override
    public void enterFstWithWeight(FstWithWeightContext ctx) {

    }

    @Override
    public void exitFstWithWeight(FstWithWeightContext ctx) {
        final Church l = res.pop();
        throw new IllegalArgumentException("weights are not supported"+importTrace());
    }

    @Override
    public void enterFstWithoutOutput(FstWithoutOutputContext ctx) {

    }

    @Override
    public void exitFstWithoutOutput(FstWithoutOutputContext ctx) {
        // pass
    }

    @Override
    public void enterFstWithoutConcat(FstWithoutConcatContext ctx) {

    }

    @Override
    public void exitFstWithoutConcat(FstWithoutConcatContext ctx) {
        // pass
    }

    @Override
    public void enterFstWithKleene(FstWithKleeneContext ctx) {

    }

    @Override
    public void exitFstWithKleene(FstWithKleeneContext ctx) {
        if (ctx.closure != null) {
            res.push(new Church.ChKleene(res.pop(), ctx.closure.getText().charAt(0)));
        }

    }

    @Override
    public void enterStmtFunc(StmtFuncContext ctx) {
        useGlobalScope = false;
        macroScope = new LinkedHashMap<>();
        final List<TerminalNode> ids = ctx.func_arguments().ID();
        macroArgs = new ArrayList<>(ids.size());
        for (TerminalNode node : ids)
            macroArgs.add(node.getText());
    }

    @Override
    public void exitStmtFunc(StmtFuncContext ctx) {
        final FileScope file = fileImportHierarchy.peek();
        try {
            final Macro macro = new Macro(macroArgs, macroScope, globalVars.keySet());
            useGlobalScope = true;
            macroScope = null;
            macroArgs = null;
            macros.put(resolveFuncID(Collections.singletonList(ctx.ID()), false), macro);
        } catch (Throwable e) {
            throw new RuntimeException(ctx.ID() + " " + importTrace() + " " + globalVars + " " + fileImportHierarchy,
                    e);
        }
    }

    @Override
    public void enterFstWithoutDiff(FstWithoutDiffContext ctx) {

    }

    @Override
    public void exitFstWithoutDiff(FstWithoutDiffContext ctx) {
        // pass
    }

    @Override
    public void enterFstWithoutWeight(FstWithoutWeightContext ctx) {

    }

    @Override
    public void exitFstWithoutWeight(FstWithoutWeightContext ctx) {
        // pass
    }

    @Override
    public void enterFunccall_arguments(Funccall_argumentsContext ctx) {

    }

    @Override
    public void exitFunccall_arguments(Funccall_argumentsContext ctx) {

    }

    @Override
    public void enterStmtVarDef(StmtVarDefContext ctx) {
        id = ctx.ID().getText();
        numberOfSubVariablesCreated = 0;
    }

    @Override
    public void exitStmtVarDef(StmtVarDefContext ctx) {
        assert ctx.ID().getText().equals(id) : ctx.ID().getText() + " " + id;
        final String ID = resolveNewID(id);
        if (ctx.export != null) {
            fileImportHierarchy.peek().export.add(ID);
        }
        introduceVarID(ID, new V(res.pop(), fileImportHierarchy.peek().filePath));
        assert res.isEmpty() : res.toString();
        id = null;
    }

    @Override
    public void enterStmt_list(Stmt_listContext ctx) {

    }

    @Override
    public void exitStmt_list(Stmt_listContext ctx) {
        // pass
    }

    @Override
    public void enterFstWithComposition(FstWithCompositionContext ctx) {

    }

    @Override
    public void exitFstWithComposition(FstWithCompositionContext ctx) {
        final Church rhs = res.pop();
        final Church lhs = res.pop();
        res.push(new Church.ChComp(lhs, rhs));
    }

    @Override
    public void enterFstWithDiff(FstWithDiffContext ctx) {

    }

    @Override
    public void exitFstWithDiff(FstWithDiffContext ctx) {
        final Church rhs = res.pop();
        final Church lhs = res.pop();
        res.push(new Church.ChDiff(lhs, rhs));
    }

    @Override
    public void enterFstWithConcat(FstWithConcatContext ctx) {

    }

    @Override
    public void exitFstWithConcat(FstWithConcatContext ctx) {
        final Church rhs = res.pop();
        final Church lhs = res.pop();
        res.push(new Church.ChConcat(lhs, rhs));
    }

    @Override
    public void enterVar(VarContext ctx) {

    }

    @Override
    public void exitVar(VarContext ctx) {
        res.push(new Church.ChVar(resolveVarID(ctx.id().ID())));
    }

    @Override
    public void enterNested(NestedContext ctx) {

    }

    @Override
    public void exitNested(NestedContext ctx) {
        // pass
    }

    @Override
    public void enterSQuoteString(SQuoteStringContext ctx) {

    }

    @Override
    public void exitSQuoteString(SQuoteStringContext ctx) {
        if (id != null) {
            res.push(new AtomicStr(parseLiteral(ctx.StringLiteral().getText())));
        }
    }

    @Override
    public void enterDQuoteString(DQuoteStringContext ctx) {

    }

    @Override
    public void exitDQuoteString(DQuoteStringContext ctx) {
        res.push(Optimise.strRefl(parseLiteral(ctx.DStringLiteral().getText())));
    }

    @Override
    public void enterFuncCall(FuncCallContext ctx) {

    }

    @Override
    public void enterId(IdContext ctx) {
        // pass
    }

    @Override
    public void exitId(IdContext ctx) {
        // pass
    }

    String resolveID(String id) {
        if (useGlobalScope) {
            assert macroArgs == null;
            assert macroScope == null;
        } else {
            assert macroArgs != null;
            assert macroScope != null;
            if (macroScope.containsKey(id) || macroArgs.contains(id))
                return id;
        }
        final String globID = getNamespace(fileImportHierarchy.peek().filePath) + "." + id;
        assert globalVars.containsKey(globID) : globID + " " + fileImportHierarchy.peek().filePath + " " + globalVars;
        return globID;
    }

    String resolveNewID(String id) {
        if (useGlobalScope) {
            assert macroArgs == null;
            assert macroScope == null;
            final String globID = getNamespace(fileImportHierarchy.peek().filePath) + "." + id;
            assert !globalVars.containsKey(globID) : globID + " " + fileImportHierarchy.peek().filePath + " "
                    + globalVars;
            return globID;
        } else {
            assert macroArgs != null;
            assert macroScope != null;
            assert !globalVars.containsKey(id) : id + " " + fileImportHierarchy.peek().filePath + " " + globalVars;
            assert !macroArgs.contains(id) : id + " " + fileImportHierarchy.peek().filePath + " " + macroArgs;
            assert !macroScope.containsKey(id) : id + " " + fileImportHierarchy.peek().filePath + " " + macroScope;
            return id;
        }
    }

    String resolveVarID(List<TerminalNode> list) {
        if (list.size() == 1) {
            return resolveID(list.get(0).getText());
        } else {
            final String importAlias = list.get(0).getText();
            final File file = fileImportHierarchy.peek().importAliasToFile.get(importAlias);
            final String variableName = list.get(1).getText();
            final String globID = getNamespace(file) + "." + variableName;
            assert globalVars.containsKey(globID);
            return globID;
        }
    }

    String resolveFuncID(List<TerminalNode> list, boolean alreadyExists) {
        final String globID;
        if (list.size() == 1) {
            globID = getNamespace(fileImportHierarchy.peek().filePath) + "." + list.get(0).getText();
        } else {
            final String importAlias = list.get(0).getText();
            final File file = fileImportHierarchy.peek().importAliasToFile.get(importAlias);
            final String variableName = list.get(1).getText();
            globID = getNamespace(file) + "." + variableName;
        }
        assert alreadyExists == macros.containsKey(globID) : macros.keySet() + " " + globID + " " + alreadyExists;
        return globID;
    }

    private String getNamespace(File file) {
        assert file.equals(normalizeFile(file));
        final String ns = fileToNamespace.get(file);
        assert ns != null;
        return ns;
    }

    private void validateVarID(String id, V re) {
        assert id == null || !id.contains("null") : id;
        assert id == null || (id.length() > 0 && id.equals(id.trim()) && !id.startsWith("."));
        assert re.introducedIn != null;
        assert (re.introducedIn.equals(fileImportHierarchy.peek().filePath))
                || (getNamespace(re.introducedIn) != null && id.startsWith(getNamespace(re.introducedIn))) : id + " "
                + fileImportHierarchy.peek().filePath + " " + re.introducedIn + " "
                + getNamespace(re.introducedIn);
    }

    void introduceVarID(String id, V re) {
        validateVarID(id, re);
        if (useGlobalScope) {
            assert id.startsWith(getNamespace(re.introducedIn) + ".") : id + " " + getNamespace(re.introducedIn) + " "
                    + re.introducedIn;
            final V prev = globalVars.put(id, re);
            assert prev == null;
        } else {
            assert !id.startsWith(getNamespace(re.introducedIn) + ".") : id + " " + getNamespace(re.introducedIn) + " "
                    + re.introducedIn;
            final V prev = macroScope.put(id, re);
            assert prev == null;
        }
    }

    @Override
    public void exitFuncCall(FuncCallContext ctx) {
        final String rawFuncID = ctx.id().getText();
        final int argsNum = ctx.funccall_arguments().fst_with_weight().size();// tells us how many automata to pop
        switch (rawFuncID) {
            case "CDRewrite": {
                if (argsNum >= 6) {
                    res.pop();
                }
                if (argsNum >= 5) {
                    res.pop();
                }
                final Church sigmaStar = res.pop();
                final Church rightCntx = res.pop();
                final Church leftCntx = res.pop();
                final Church replacement = res.pop();
                res.push(new Church.ChCdRewrite(sigmaStar, rightCntx, leftCntx, replacement));
                break;
            }
            case "ArcSort":// All arcs are always sorted in Solomonoff
            case "Minimize":// Solomonoff already builds very small transducers
            case "Optimize":// Solomonoff decides better on its own when to optimize something
            case "Determinize":// This name is very misleading because even Thrax doesn't actually determinize
                // anything.
                // "Determinization" in Thrax merely makes all arcs have string outputs and
                // single-symbol inputs. It's always
                // enforced in Solomonoff
            case "RmEpsilon":// Solomonoff has no epsilons
                // pass
                break;
            case "Invert": {
                res.push(new Church.ChInv(res.pop()));
                break;
            }
            case "Concat": {
                final Church rhs = res.pop();
                final Church lhs = res.pop();
                res.push(new Church.ChConcat(lhs, rhs));
                break;
            }
            case "Union": {
                final Church rhs = res.pop();
                final Church lhs = res.pop();
                res.push(new Church.ChUnion(lhs, rhs));
                break;
            }
            case "Difference": {
                final Church rhs = res.pop();
                final Church lhs = res.pop();
                res.push(new Church.ChDiff(lhs, rhs));
                break;
            }
            case "Compose": {
                final Church rhs = res.pop();
                final Church lhs = res.pop();
                res.push(new Church.ChComp(lhs, rhs));
                break;
            }
            case "Project": {
                final Atomic.Str rhs = (Atomic.Str) res.pop();
                final Church lhs = res.pop();
                final String str = IntSeq.toUnicodeString(rhs.str());
                if(str.equals("input")){
                    res.push(new Church.ChIdentity(lhs));
                } else if(str.equals("output")) {
                    res.push(new Church.ChIdentity(new Church.ChInv(lhs)));
                }

                break;
            }
            case "StringFile": {
                final AtomicStr rhs = (AtomicStr) res.pop();
                final File f = new File(fileImportHierarchy.peek().filePath.getParentFile(), IntSeq.toUnicodeString(rhs.str));
                res.push(Optimise.stringFile(f));
                break;
            }
            default: {
                final String funcID = resolveFuncID(ctx.id().ID(), true);
                assert funcID != null;

                final Macro macro = macros.get(funcID);
                if (macro != null) {
                    assert argsNum == macro.args.size();
                    final HashMap<String, Church> argMap = new HashMap<String, Church>();
                    for (int i = macro.args.size() - 1; i >= 0; i--) {
                        argMap.put(macro.args.get(i), res.pop());
                    }
                    assert !funcID.startsWith(".");
                    final String mangledFuncID = resolveNewID(id) + ".__" + (numberOfSubVariablesCreated++) + "." + funcID;
                    for (Entry<String, V> localVar : macro.localVars.entrySet()) {
                        final String localVarID = localVar.getKey();
                        assert !fileImportHierarchy.isEmpty();
                        assert localVar.getValue() != null : mangledFuncID + " " + macro.localVars;
                        assert localVarID == null || !localVarID.startsWith(".");
                        final String mangledLocalVarID = mangledFuncID + "." + localVarID;
                        final Church sub = localVar.getValue().re.substituteCh(i -> {
                            final Church localRef = argMap.get(i.id);
                            if (localRef != null)
                                return localRef;
                            final V ref = globalVars.get(i.id);
                            assert ref != null : i.id + " " + globalVars;
                            return ref.re;
                        });
                        introduceVarID(mangledLocalVarID, new V(sub, fileImportHierarchy.peek().filePath));
                        argMap.put(localVarID, new Church.ChVar(mangledLocalVarID));
                    }
                    res.push(new Church.ChVar(mangledFuncID + ".return"));
                } else {
                    res.setSize(res.size() - argsNum);
                    assert false : funcID + " " + macros;
                    throw new IllegalArgumentException("Undefined function " + funcID+importTrace());
                }
            }
        }
    }

    private String importTrace() {
        final StringBuilder sb = new StringBuilder(" at ");
        sb.append(fileImportHierarchy.get(0).filePath);
        for (int i=1;i< fileImportHierarchy.size();i++) {
            sb.append(" -> ").append(fileImportHierarchy.get(i).filePath);
        }
        return sb.toString();
    }

    public static <N, G extends IntermediateGraph<Pos, E, P, N>> ThraxParser<N, G> parse(File filePath,
                                                                                         CharStream source) throws CompilationError {
        final ThraxGrammarLexer lexer = new ThraxGrammarLexer(source);
        final ThraxGrammarParser parser = new ThraxGrammarParser(new CommonTokenStream(lexer));
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) {
                System.err.println("line " + line + ":" + charPositionInLine + " " + msg + " " + e);
            }
        });
        try {
            ThraxParser<N, G> listener = new ThraxParser<N, G>(filePath);
            ParseTreeWalker.DEFAULT.walk(listener, parser.start());
            assert null != listener.getNamespace(normalizeFile(filePath));
            return listener;
        } catch (RuntimeException e) {
            if (e.getCause() instanceof CompilationError) {
                throw (CompilationError) e.getCause();
            } else {
                throw e;
            }
        }

    }

}
