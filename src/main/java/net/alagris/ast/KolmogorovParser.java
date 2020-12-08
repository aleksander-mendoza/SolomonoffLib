package net.alagris.ast;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.BiFunction;

import net.alagris.*;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import net.alagris.HashMapIntermediateGraph.LexUnicodeSpecification;
import net.alagris.KolmogorovGrammarParser.FsaConcatContext;
import net.alagris.KolmogorovGrammarParser.FsaDiffContext;
import net.alagris.KolmogorovGrammarParser.FsaKleeneClosureContext;
import net.alagris.KolmogorovGrammarParser.FsaUnionContext;
import net.alagris.KolmogorovGrammarParser.Fsa_atomicContext;
import net.alagris.KolmogorovGrammarParser.FuncsContext;
import net.alagris.KolmogorovGrammarParser.MealyComposeContext;
import net.alagris.KolmogorovGrammarParser.MealyConcatContext;
import net.alagris.KolmogorovGrammarParser.MealyDiffContext;
import net.alagris.KolmogorovGrammarParser.MealyKleeneClosureContext;
import net.alagris.KolmogorovGrammarParser.MealyUnionContext;
import net.alagris.KolmogorovGrammarParser.Mealy_atomicContext;
import net.alagris.KolmogorovGrammarParser.StartContext;
import net.alagris.Pair.IntPair;
import net.alagris.Specification.NullTermIter;
import net.alagris.Specification.Range;

public class KolmogorovParser implements KolmogorovGrammarListener {

	
	

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

	private final Stack<PushedBack> stack = new Stack<>();
	private final LinkedHashMap<String, Kolmogorov> vars = new LinkedHashMap<>();

	PushedBack peek(int indexFromEnd) {
		return stack.get(stack.size() - 1 - indexFromEnd);
	}

	void popN(int n) {
		stack.setSize(stack.size() - n);
	}

	void foldAndPopLastN(int n, BiFunction<PushedBack,PushedBack,PushedBack> fold) {
		if (n < 2)
			return;
		int i = stack.size() - n;
		PushedBack folded = stack.get(i++);
		while (i < stack.size()) {
			folded = fold.apply(folded, stack.get(i++));
		}
		stack.set(stack.size() - n, folded);
		stack.setSize(stack.size() - n + 1);

	}

	@Override
	public void enterMealyDiff(MealyDiffContext ctx) {

	}

	@Override
	public void exitMealyDiff(MealyDiffContext ctx) {
		foldAndPopLastN(ctx.mealy_union().size(), PushedBack::diff);
	}

	@Override
	public void enterStart(StartContext ctx) {

	}

	@Override
	public void exitStart(StartContext ctx) {
		// pass
	}

	@Override
	public void enterFsa_atomic(Fsa_atomicContext ctx) {

	}

	@Override
	public void exitFsa_atomic(Fsa_atomicContext ctx) {
		if (ctx.StringLiteral() != null) {
			try {
				stack.push(PushedBack.str(ParserListener.parseQuotedLiteral(ctx.StringLiteral())));
			} catch (CompilationError e) {
				throw new RuntimeException(e);
			}
		} else if (ctx.Codepoint() != null) {
			stack.push(PushedBack.str(ParserListener.parseCodepoint(ctx.Codepoint())));
		} else if (ctx.CodepointRange() != null) {
			stack.push(PushedBack.range(ParserListener.parseCodepointRange(ctx.Codepoint())));
		} else if (ctx.Range() != null) {
			stack.push(PushedBack.range(ParserListener.parseRange(ctx.Codepoint())));
		} else if (ctx.ID() != null) {
			stack.push(PushedBack.var(ctx.ID().getText(), vars));
		} else if (ctx.nested != null) {
			// pass
		} else {
			throw new IllegalStateException("No case matched");
		}

	}

	@Override
	public void enterMealyConcat(MealyConcatContext ctx) {

	}

	@Override
	public void exitMealyConcat(MealyConcatContext ctx) {
		foldAndPopLastN(ctx.mealy_Kleene_closure().size(), PushedBack::concat);
	}

	@Override
	public void enterFsaKleeneClosure(FsaKleeneClosureContext ctx) {

	}

	@Override
	public void exitFsaKleeneClosure(FsaKleeneClosureContext ctx) {
		if (ctx.plus != null) {
			stack.push(stack.pop().kleene( '+'));
		} else if (ctx.power != null) {
			final int num = Integer.parseInt(ctx.Num().getText());
			stack.push(stack.pop().pow(  num));
		} else if (ctx.star != null) {
			stack.push(stack.pop().kleene( '*'));
		} else if (ctx.optional != null) {
			stack.push(stack.pop().kleene( '?'));
		}
	}

	@Override
	public void enterMealyCompose(MealyComposeContext ctx) {

	}

	@Override
	public void exitMealyCompose(MealyComposeContext ctx) {
		foldAndPopLastN(ctx.mealy_diff().size(), PushedBack::comp);
	}

	@Override
	public void enterFsaConcat(FsaConcatContext ctx) {

	}

	@Override
	public void exitFsaConcat(FsaConcatContext ctx) {
		foldAndPopLastN(ctx.fsa_Kleene_closure().size(), PushedBack::concat);
	}

	@Override
	public void enterMealyKleeneClosure(MealyKleeneClosureContext ctx) {

	}

	@Override
	public void exitMealyKleeneClosure(MealyKleeneClosureContext ctx) {
		if (ctx.plus != null) {
			stack.push(stack.pop().kleene( '+'));
		} else if (ctx.power != null) {
			final int num = Integer.parseInt(ctx.Num().getText());
			stack.push(stack.pop().pow( num));
		} else if (ctx.star != null) {
			stack.push(stack.pop().kleene( '*'));
		} else if (ctx.optional != null) {
			stack.push(stack.pop().kleene( '?'));
		}
	}

	@Override
	public void enterMealyUnion(MealyUnionContext ctx) {

	}

	@Override
	public void exitMealyUnion(MealyUnionContext ctx) {
		foldAndPopLastN(ctx.mealy_concat().size(), PushedBack::union);
	}

	@Override
	public void enterFsaUnion(FsaUnionContext ctx) {

	}

	@Override
	public void exitFsaUnion(FsaUnionContext ctx) {
		foldAndPopLastN(ctx.fsa_concat().size(), PushedBack::union);
	}

	@Override
	public void enterMealy_atomic(Mealy_atomicContext ctx) {
	}

	@Override
	public void exitMealy_atomic(Mealy_atomicContext ctx) {
		if (ctx.StringLiteral() != null) {
			try {
				stack.push(PushedBack.str(ParserListener.parseQuotedLiteral(ctx.StringLiteral())));
			} catch (CompilationError e) {
				throw new RuntimeException(e);
			}
		} else if (ctx.Codepoint() != null) {
			stack.push(PushedBack.str(ParserListener.parseCodepoint(ctx.Codepoint())));
		} else if (ctx.CodepointRange() != null) {
			stack.push(PushedBack.range(ParserListener.parseCodepointRange(ctx.Codepoint())));
		} else if (ctx.Range() != null) {
			stack.push(PushedBack.range(ParserListener.parseRange(ctx.Codepoint())));
		} else if (ctx.ID() != null) {
			stack.push(PushedBack.var(ctx.ID().getText(), vars));
		} else if (ctx.nested != null) {
			// pass
		} else {
			throw new IllegalStateException("No case matched");
		}
	}

	@Override
	public void enterFsaDiff(FsaDiffContext ctx) {

	}

	@Override
	public void exitFsaDiff(FsaDiffContext ctx) {
		foldAndPopLastN(ctx.fsa_union().size(), PushedBack::diff);
	}

	@Override
	public void enterFuncs(FuncsContext ctx) {

	}

	@Override
	public void exitFuncs(FuncsContext ctx) {
		for (int i = ctx.ID().size() - 1; i >= 0; i--) {
			final PushedBack m = stack.pop();
			final String id = ctx.ID(i).getText();
			final Kolmogorov prev;
			prev = vars.put(id, m.finish());
			assert prev == null;
		}
	}
	
	public static KolmogorovParser parse(File filePath, CharStream source) throws CompilationError {
		final KolmogorovGrammarLexer lexer = new KolmogorovGrammarLexer(source);
		final KolmogorovGrammarParser parser = new KolmogorovGrammarParser(new CommonTokenStream(lexer));
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				System.err.println("line " + line + ":" + charPositionInLine + " " + msg + " " + e);
			}
		});
		try {
			final KolmogorovParser listener = new KolmogorovParser();
			ParseTreeWalker.DEFAULT.walk(listener, parser.start());
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
