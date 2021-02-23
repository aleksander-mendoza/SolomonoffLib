package net.alagris.cli.conv;

import net.alagris.core.ArrayIntermediateGraph;
import net.alagris.core.IntSeq;
import net.alagris.core.LexUnicodeSpecification;
import net.alagris.lib.Config;

import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Kolmogorov {

	public static final LexUnicodeSpecification<?,?> SPECS = new ArrayIntermediateGraph.LexUnicodeSpecification(Config.config());

	public Stacked toSolomonoff(VarQuery query);
	
	public void forEachVar(Consumer<AtomicVar> variableAssignment);

	public IntSeq representative(Function<AtomicVar, Kolmogorov> variableAssignment);

	public Kolmogorov substitute(HashMap<String, Kolmogorov> argMap);

	public Kolmogorov inv();

	public boolean producesOutput();

	public int compositionHeight();

	public boolean readsInput();

	public void toString(StringBuilder sb);

	public int precedence();

	public Kolmogorov clearOutput();

}
