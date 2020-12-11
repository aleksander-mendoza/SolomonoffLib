package net.alagris.ast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.function.BiFunction;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import net.alagris.IntSeq;
import net.alagris.IntermediateGraph;
import net.alagris.LexUnicodeSpecification.E;
import net.alagris.LexUnicodeSpecification.P;
import net.alagris.Pos;
import net.alagris.Specification.NullTermIter;
import net.alagris.Specification.Range;
import net.alagris.ThraxGrammarLexer;
import net.alagris.ThraxGrammarListener;
import net.alagris.ThraxGrammarParser;
import net.alagris.ThraxGrammarParser.DQuoteStringContext;
import net.alagris.ThraxGrammarParser.FstWithCompositionContext;
import net.alagris.ThraxGrammarParser.FstWithConcatContext;
import net.alagris.ThraxGrammarParser.FstWithDiffContext;
import net.alagris.ThraxGrammarParser.FstWithKleeneContext;
import net.alagris.ThraxGrammarParser.FstWithOutputContext;
import net.alagris.ThraxGrammarParser.FstWithRangeContext;
import net.alagris.ThraxGrammarParser.FstWithUnionContext;
import net.alagris.ThraxGrammarParser.FstWithWeightContext;
import net.alagris.ThraxGrammarParser.FstWithoutCompositionContext;
import net.alagris.ThraxGrammarParser.FstWithoutConcatContext;
import net.alagris.ThraxGrammarParser.FstWithoutDiffContext;
import net.alagris.ThraxGrammarParser.FstWithoutOutputContext;
import net.alagris.ThraxGrammarParser.FstWithoutUnionContext;
import net.alagris.ThraxGrammarParser.FstWithoutWeightContext;
import net.alagris.ThraxGrammarParser.FuncCallContext;
import net.alagris.ThraxGrammarParser.Func_argumentsContext;
import net.alagris.ThraxGrammarParser.Funccall_argumentsContext;
import net.alagris.ThraxGrammarParser.IdContext;
import net.alagris.ThraxGrammarParser.NestedContext;
import net.alagris.ThraxGrammarParser.SQuoteStringContext;
import net.alagris.ThraxGrammarParser.StartContext;
import net.alagris.ThraxGrammarParser.StmtFuncContext;
import net.alagris.ThraxGrammarParser.StmtImportContext;
import net.alagris.ThraxGrammarParser.StmtReturnContext;
import net.alagris.ThraxGrammarParser.StmtVarDefContext;
import net.alagris.ThraxGrammarParser.Stmt_listContext;
import net.alagris.ThraxGrammarParser.VarContext;

public class ThraxParser<N, G extends IntermediateGraph<Pos, E, P, N>> implements ThraxGrammarListener {

	public static final int TEMPORARY_THRAX_SYMBOL_BEGIN = 0x100000;
	public static final int EOS = 0x10FFFD;
	public static final int BOS = 0x10FFFC;
	int nextTemporarySymbolToCreate = TEMPORARY_THRAX_SYMBOL_BEGIN;
	
	final HashMap<String, Integer> TEMPORARY_THRAX_SYMBOLS = new HashMap<>();
	{
		TEMPORARY_THRAX_SYMBOLS.put("BOS", BOS);
		TEMPORARY_THRAX_SYMBOLS.put("EOS", BOS);
	}
	int getTmpSymbol(String id){
		return TEMPORARY_THRAX_SYMBOLS.computeIfAbsent(id, k-> nextTemporarySymbolToCreate++ );
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
	final HashMap<File, String> fileToNamespace = new HashMap<>();

	static class Macro {
		final List<String> args;
		final LinkedHashMap<String, V> localVars;

		public Macro(List<String> args, LinkedHashMap<String, V> localVars) {
			this.args = args;
			this.localVars = localVars;
		};
	}

	static class V{
		final Kolmogorov re;
		final boolean export;
		final File introducedIn;
		public V(Kolmogorov re, boolean export, File introducedIn) {
			this.re = re;
			this.export = export;
			this.introducedIn = introducedIn;
		}
	}
	static class FileScope {
		final File filePath;
		final LinkedHashMap<String, V> globalVars = new LinkedHashMap<>();
		final HashMap<String, Macro> macros = new LinkedHashMap<>();
		LinkedHashMap<String, V> currentScope = globalVars;
		final HashMap<String, File> importAliasToFile = new HashMap<>();

		public FileScope(File filePath) {
			this.filePath = filePath.getAbsoluteFile();
		}
		public HashMap<String, Integer> countUsages() {
			final HashMap<String, Integer> usages = new HashMap<>(globalVars.size());
				
			for (Entry<String, V> var : globalVars.entrySet()) {
				if(var.getValue().export)usages.put(var.getKey(), 1);
				var.getValue().re.countUsages(usages);
			}
			return usages;
		}
	}

	final Stack<FileScope> fileImportHierarchy = new Stack<>();
	/** id of the variable currently being built */
	public String id;
	final Stack<Kolmogorov> res = new Stack<>();
	public int numberOfSubVariablesCreated = 0;

	public ThraxParser(File filePath) {
		fileImportHierarchy.push(new FileScope(filePath));
	}

	static class SerializationContext {
		final HashMap<String, Integer> consumedUsages;

		public SerializationContext(HashMap<String, Integer> usages) {
			consumedUsages = new HashMap<>(usages);
		}
	}

	public String toSolomonoff() {
		final FileScope file = fileImportHierarchy.peek();
		final StringBuilder sb = new StringBuilder();
		SerializationContext ctx = new SerializationContext(file.countUsages());
		
		for (Entry<String, V> e : file.globalVars.entrySet()) {
			final Integer usagesLeft = ctx.consumedUsages.get(e.getKey());
			if (usagesLeft != null && usagesLeft > 0) {
				sb.append(e.getKey()).append(" = ");
				e.getValue().re.toSolomonoff(sb, ctx);
				sb.append("\n");
			}
		}
		return sb.toString();
	}


	public IntSeq parseLiteral(String str) {
		final int[] arr = new int[str.codePointCount(1, str.length()-1)];
		int arrEnd = 0;
		for (int i = 1; i < str.length() - 1;) {// first and last are " characters
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
				} else if('0'<=firstInsideBrackets && firstInsideBrackets<='9'){
					base = 10;// decimal notation
					beginNum = i + 1;
				}else {
					base = -1;//temporary thrax symbol
					beginNum = i + 1;
				}
				int endNum = beginNum + 1;
				while (endNum < str.length() - 1 && str.charAt(endNum) != ']')
					endNum++;
				final String insideBrackets = str.substring(beginNum, endNum);
				if(base==-1) {
					arr[arrEnd++]=getTmpSymbol(insideBrackets);
				}else {
					arr[arrEnd++]=Integer.parseInt(insideBrackets, base);
				}
				i = endNum+1;
			} else if (c == '\\') {
				i++;
				final int escaped = str.codePointAt(i);
				i+=Character.charCount(escaped);
				switch (escaped) {
				case 'r':
					arr[arrEnd++]=(int)'\r';
					break;
				case 't':
					arr[arrEnd++]=(int)'\t';
					break;
				case 'n':
					arr[arrEnd++]=(int)'\n';
					break;
				case '0':
					arr[arrEnd++]=(int)'\0';
					break;
				case 'b':
					arr[arrEnd++]=(int)'\b';
					break;
				case 'f':
					arr[arrEnd++]=(int)'\f';
					break;
				default:
					arr[arrEnd++]=escaped;
					break;
				}
				
			} else {
				i+=Character.charCount(c);
				arr[arrEnd++]=c;
			}
			
		}
		if(0==arrEnd)return IntSeq.Epsilon;
		return new IntSeq(arr,0,arrEnd);
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
		id = null;// this is the ID of return statement
	}

	@Override
	public void exitStmtReturn(StmtReturnContext ctx) {
		assert id == null;
		introduceVar(/* this is a completely valid key BTW */null,new V(res.pop(),false,fileImportHierarchy.peek().filePath));
	}

	@Override
	public void enterFstWithRange(FstWithRangeContext ctx) {

	}

	@Override
	public void exitFstWithRange(FstWithRangeContext ctx) {
		final Kolmogorov re = res.pop();
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
			final Kolmogorov repeating;
			if (re instanceof Atomic.Str) {
				repeating = re;
			} else {
				final String subID = (id == null ? "" : (id + ".")) + "__" + (numberOfSubVariablesCreated++);
				introduceVar(subID, new V(re,false,fileImportHierarchy.peek().filePath));
				repeating = new Atomic.Var(subID);
			}
			Kolmogorov repeated = repeating;
			for (int i = 1; i < from; i++) {
				repeated = concat(repeating, repeated);
			}
			for (int i = from; i < to; i++) {
				repeated = new Concat(repeated, new Kleene(repeating, Kleene.ZERO_OR_ONE));
			}

			res.push(repeated);
		}
	}

	@Override
	public void enterFstWithOutput(FstWithOutputContext ctx) {

	}

	@Override
	public void exitFstWithOutput(FstWithOutputContext ctx) {
		final RE r = res.pop();
		final RE l = res.pop();
		res.push(output(l, r));
	}

	RE output(RE lhs,RE rhs) {
		return output(lhs,rhs.representative());
	}
	RE output(RE lhs,IntSeq out ) {
		if (out.isEmpty())
			return lhs;
		if (lhs instanceof Output) {
			final Output lOut = (Output) lhs;
			return new OutputImpl(lOut.re(), lOut.out().concat(out));
		} else {
			return new OutputImpl(lhs, out);
		}
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
		final RE r = res.pop();
		final RE l = res.pop();
		res.push(union(l, r));
	}

	RE union(RE lhs, RE rhs) {
		if (lhs instanceof Set && rhs instanceof Set) {
			return composeSets(((Set) lhs).ranges(), ((Set) rhs).ranges(), (a, b) -> a || b);
		} else {
			return new Union(lhs, rhs);
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

	}

	@Override
	public void exitStmtImport(StmtImportContext ctx) {
		final String quoted = ctx.StringLiteral().getText();
		final String noQuotes = quoted.substring(1,quoted.length()-1);
		final File file = new File(fileImportHierarchy.peek().filePath.getParentFile(),noQuotes).getAbsoluteFile();
		final String alias = ctx.ID().getText();
		final File prevFile = fileImportHierarchy.peek().importAliasToFile.put(alias, file);
		assert prevFile==null;
		
		if (!fileToNamespace.containsKey(file)) {
			String namespace = alias;
			if(fileToNamespace.containsValue(namespace)) {
				int i=0;
				while(fileToNamespace.containsValue(namespace+i))i++;
				namespace = namespace+i;
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
				fileImportHierarchy.push(new FileScope(file));
				ParseTreeWalker.DEFAULT.walk(this, parser.start());
				final FileScope imported = fileImportHierarchy.pop();
				final String prefix = namespace+".";
				for(Entry<String, V> var:imported.globalVars.entrySet()) {
					final V importedVar = new V(var.getValue().re,false,var.getValue().introducedIn);
					introduceVar((var.getValue().introducedIn.equals(file)?prefix:"")+var.getKey(),importedVar);
				}
				for(Entry<String, Macro> macro:imported.macros.entrySet()) {
					fileImportHierarchy.peek().macros.put(prefix+macro.getKey(), macro.getValue());
				}
				
			} catch (IOException e1) {
				throw new RuntimeException(e1);
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
		final RE l = res.pop();
		res.push(new Var("WEIGHT_NOT_SUPPORTED"));
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
			final char KLEENE_TYPE;
			switch (ctx.closure.getText()) {
			case "*":
				KLEENE_TYPE = Kleene.ZERO_OR_MORE;
				break;
			case "+":
				KLEENE_TYPE = Kleene.ONE_OR_MORE;
				break;
			case "?":
				KLEENE_TYPE = Kleene.ZERO_OR_ONE;
				break;
			default:
				throw new IllegalStateException(ctx.closure.getText());
			}
			res.push(new Kleene(res.pop(), KLEENE_TYPE));
		}

	}

	@Override
	public void enterStmtFunc(StmtFuncContext ctx) {
		fileImportHierarchy.peek().currentScope = new LinkedHashMap<>();
	}

	@Override
	public void exitStmtFunc(StmtFuncContext ctx) {
		final List<TerminalNode> ids = ctx.func_arguments().ID();
		final ArrayList<String> args = new ArrayList<>(ids.size());
		for (TerminalNode node : ids)
			args.add(node.getText());
		final FileScope file = fileImportHierarchy.peek();
		file.macros.put(ctx.ID().getText(), new Macro(args, file.currentScope));
		file.currentScope = file.globalVars;
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
		final String ID = ctx.ID().getText();
		introduceVar(ID, new V(res.pop(),ctx.export != null,fileImportHierarchy.peek().filePath));
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
		final RE rhs = res.pop();
		final RE lhs = res.pop();
		res.push(compose(lhs, rhs));
	}

	RE bijection(RE re) {
		if (re instanceof Str) {
			return output(re, re);
		} else {
			return new Func("bijection", re);
		}
	}

	RE compose(RE lhs, RE rhs) {
		return new Func("compose", lhs, rhs);
	}

	@Override
	public void enterFstWithDiff(FstWithDiffContext ctx) {

	}

	@Override
	public void exitFstWithDiff(FstWithDiffContext ctx) {
		final RE rhs = res.pop();
		final RE lhs = res.pop();
		res.push(diff(lhs, rhs));
	}

	RE diff(RE lhs, RE rhs) {
		if (lhs instanceof Set && rhs instanceof Set) {
			return composeSets((Set) lhs, (Set) rhs, (a, b) -> a && !b);
		} else {
			return new Func("subtract", lhs, rhs);
		}
	}

	@Override
	public void enterFstWithConcat(FstWithConcatContext ctx) {

	}

	@Override
	public void exitFstWithConcat(FstWithConcatContext ctx) {
		final RE rhs = res.pop();
		final RE lhs = res.pop();
		res.push(concat(lhs, rhs));
	}

	public RE concat(RE lhs, RE rhs) {
		final IntSeq rhsIn;
		final IntSeq rhsOut;
		if (rhs instanceof Str) {
			rhsOut = IntSeq.Epsilon;
			rhsIn = ((Str) rhs).str();
		} else if (rhs instanceof Output && ((Output) rhs).re() instanceof Str) {
			Output rOut = (Output) rhs;
			rhsOut = rOut.out();
			rhsIn = ((Str) rOut.re()).str();
		} else {
			rhsOut = null;
			rhsIn = null;
		}
		if (rhsOut != null) {
			final IntSeq lhsIn;
			final IntSeq lhsOut;
			if (lhs instanceof Str) {
				lhsOut = IntSeq.Epsilon;
				lhsIn = ((Str) lhs).str();
			} else if (lhs instanceof Output && ((Output) lhs).re() instanceof Str) {
				Output lOut = (Output) lhs;
				lhsOut = lOut.out();
				lhsIn = ((Str) lOut.re()).str();
			} else {
				lhsOut = null;
				lhsIn = null;
			}
			if (lhsOut != null) {
				final Str in = fromSeq(lhsIn.concat(rhsIn));
				final Str out = fromSeq(lhsOut.concat(rhsOut));
				return output(in, out);
			}
		}
		return new Concat(lhs, rhs);
	}

	private Str fromSeq(IntSeq seq) {
		if (seq.size() == 1) {
			return new Char(seq.get(0));
		} else {
			return new StrImpl(seq);
		}
	}

	@Override
	public void enterVar(VarContext ctx) {

	}

	@Override
	public void exitVar(VarContext ctx) {
		res.push(var(resolveID(ctx.id().ID())));
	}

	RE var(String id) {

		final V v = fileImportHierarchy.peek().currentScope.get(id);
		if (v != null) {
			if ((v.re instanceof Set || v.re instanceof Str)) {
				return v.re;
			} else if (v.re instanceof Output && ((Output) v.re).re() instanceof Str) {
				return v.re;
			}
		}
		return new Var(id);
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
		// TODO
	}

	@Override
	public void enterDQuoteString(DQuoteStringContext ctx) {

	}

	@Override
	public void exitDQuoteString(DQuoteStringContext ctx) {
		res.push(parseLiteral(ctx.DStringLiteral().getText()));
	}

	@Override
	public void enterFuncCall(FuncCallContext ctx) {

	}

	RE inverse(RE re) {
		return new Func("inverse", re);
	}

	@Override
	public void enterId(IdContext ctx) {
		// pass
	}

	@Override
	public void exitId(IdContext ctx) {
		// pass
	}

	String resolveID(List<TerminalNode> list) {
		if (list.size() == 1) {
			return list.get(0).getText();
		} else {
			final String importAlias = list.get(0).getText();
			final File file = fileImportHierarchy.peek().importAliasToFile.get(importAlias);
			final String namespace = fileToNamespace.get(file);
			final String variableName = list.get(1).getText();
			return namespace + "." + variableName;
		}
	}
	void introduceVar(String id, V re) {
		assert id==null||!id.contains("null"):id;
		assert id == null || (id.length() > 0 && id.equals(id.trim()) && !id.startsWith("."));
		assert re.introducedIn!=null;
		assert (re.introducedIn.equals(fileImportHierarchy.peek().filePath)) || (id.startsWith(fileToNamespace.get(re.introducedIn)));
		final V prev = fileImportHierarchy.peek().currentScope.put(id, re);
		assert prev == null;
	}

	@Override
	public void exitFuncCall(FuncCallContext ctx) {
		final String funcID = resolveID(ctx.id().ID());
		assert funcID != null;
		final int argsNum = ctx.funccall_arguments().fst_with_weight().size();// tells us how many automata to pop
		final Macro macro = fileImportHierarchy.peek().macros.get(funcID);
		if (macro != null) {
			assert argsNum == macro.args.size();
			final HashMap<String, RE> argMap = new HashMap<String, ThraxParser.RE>();
			for (int i = macro.args.size() - 1; i >= 0; i--) {
				argMap.put(macro.args.get(i), res.pop());
			}
			assert !funcID.startsWith(".");
			final String mangledFuncID = (id == null ? "" : (id + ".")) + "__" + (numberOfSubVariablesCreated++) + "."
					+ funcID;
			for (Entry<String, V> localVar : macro.localVars.entrySet()) {
				final String localVarID = localVar.getKey();
				assert localVarID == null || !localVarID.startsWith(".");
				final String mangledLocalVarID = mangledFuncID + (localVarID == null ? "" : ("." + localVarID));
				introduceVar(mangledLocalVarID, new V(localVar.getValue().re.substitute(argMap),false,fileImportHierarchy.peek().filePath));
				argMap.put(localVarID, var(mangledLocalVarID));
			}
			res.push(var(mangledFuncID));
		} else {
			switch (funcID) {
			case "CDRewrite": {
				if (argsNum >= 6) {
					res.pop();
				}
				if (argsNum >= 5) {
					res.pop();
				}
				final RE sigmaStar = res.pop();
				final RE rightCntx = res.pop();
				final RE leftCntx = res.pop();
				final RE replacement = res.pop();
				res.push(cdrewrite(replacement, leftCntx, rightCntx, sigmaStar));
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
			case "Inverse": {
				res.push(inverse(res.pop()));
				break;
			}
			case "Concat": {
				final RE rhs = res.pop();
				final RE lhs = res.pop();
				res.push(concat(lhs, rhs));
				break;
			}
			case "Union": {
				final RE rhs = res.pop();
				final RE lhs = res.pop();
				res.push(union(lhs, rhs));
				break;
			}
			case "Difference": {
				final RE rhs = res.pop();
				final RE lhs = res.pop();
				res.push(diff(lhs, rhs));
				break;
			}
			case "Compose": {
				final RE rhs = res.pop();
				final RE lhs = res.pop();
				res.push(compose(lhs, rhs));
				break;
			}
			default:
				res.setSize(res.size() - argsNum);
				res.push(new Var("UNDEFINED_FUNC_" + funcID));
				break;
			}
		}

	}

	private Kleene cdrewrite(final RE replacement, final RE leftCntx, final RE rightCntx, final RE sigmaStar) {
		return new Kleene(
				union(new WeightAfter(concat(bijection(leftCntx), concat(replacement, bijection(rightCntx))), 2),
						concat(output(EPSILON, REFLECT), sigmaStar)),
				'*');
	}

	public static <N, G extends IntermediateGraph<Pos, E, P, N>> ThraxParser<N, G> parse(File filePath, CharStream source,
			LexUnicodeSpecification<N, G> specs) throws CompilationError {
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
			ThraxParser<N, G> listener = new ThraxParser<N, G>(filePath,specs);
			ParseTreeWalker.DEFAULT.walk(listener, parser.start());
			assert !listener.fileToNamespace.containsKey(filePath.getAbsoluteFile());
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
